/*
 * Copyright 2024 Sublimis
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.mapsforge.map.layer.hills;

import org.mapsforge.core.util.IOUtils;
import org.mapsforge.map.layer.hills.HgtCache.HgtFileInfo;
import org.mapsforge.map.layer.hills.HillShadingUtils.Awaiter;
import org.mapsforge.map.layer.hills.HillShadingUtils.HillShadingThreadPool;
import org.mapsforge.map.layer.hills.HillShadingUtils.ShortArraysPool;
import org.mapsforge.map.layer.hills.HillShadingUtils.SilentFutureTask;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

/**
 * <p>
 * Special abstract implementation of hill shading algorithm where input data can be divided into parts
 * that are processed in parallel by multiple threads.
 * </p>
 * <p>
 * The implementation is such that there are 1 + N (N>=0) "producer" threads (ie. the caller thread and N additional threads)
 * that read from the input and do the synchronization, and 1 + M (>=0) "consumer" threads that do the computations.
 * It should be emphasized that every caller thread has its own thread pool: Thread pools are not shared.
 * </p>
 * <p>
 * Special attention is paid to reducing memory consumption.
 * A producer thread will throttle itself and stop reading until the computation catches up.
 * If this happens, a {@link #notifyReadingPaced(int)} will be called.
 * </p>
 * <p>
 * Rough estimate of the <em>maximum</em> memory used per caller is as follows:
 * <br />
 * <br />
 * max_bytes_used = {@link #ElementsPerComputingTask} * (1 + 2 * M) * (1 + N) * {@link Short#BYTES}
 * <br />
 * <br />
 * By default, with two reading threads and one additional computing thread used, this is around 2 * 96000 bytes (cca 200 kB) max memory usage per caller.
 * If the computations are fast enough, the real memory usage is usually going to be several times smaller.
 * </p>
 * <p>
 * You can set the algorithm to the "high quality" setting.
 * The unit element is then 4x4 data points in size instead of 2x2, except at the global outer edges and vertices of the input data set where you will still get 2x2.
 * This allows better interpolation possibilities.
 * To make use of this, you should override the
 * {@link #processOneUnitElement(double, double, double, double, double, double, double, double, double, double, double, double, double, double, double, double, double, int, ComputingParams)}
 * method.
 * The default implementation will just call the standard quality method,
 * {@link #processOneUnitElement(double, double, double, double, double, int, ComputingParams)}.
 * </p>
 * <p>
 * A 2x2 unit element layout:
 * <pre>{@code
 * nw ne
 * sw se}
 * </pre>
 * </p>
 * <p>
 * A 4x4 unit element layout:
 * <pre>{@code
 * nwnw nnw nne nene
 * wnw   nw ne   ene
 * wsw   sw se   ese
 * swsw ssw sse sese}
 * </pre>
 * </p>
 */
public abstract class AThreadedHillShading extends AbsShadingAlgorithmDefaults {

    /**
     * Default number of additional reading threads ("producer" threads) per caller thread.
     * Number N (>0) means there will be N additional threads (per caller thread) that will do the reading,
     * while 0 means that only the caller thread will do the reading.
     */
    public static final int ReadingThreadsCountDefault = 1;

    /**
     * Default number of additional computing threads ("consumer" threads) per caller thread.
     * Number N (>0) means there will be N additional threads (per caller thread) that will do the computing,
     * while 0 means that producer thread(s) will also do the computing.
     */
    public static final int ComputingThreadsCountDefault = 1;

    /**
     * When high quality, a unit element is 4x4 data points in size; otherwise it is 2x2.
     */
    public static final boolean IsHighQualityDefault = false;

    /**
     * Default name prefix for additional threads created and used by hill shading. A numbered suffix will be appended.
     */
    public final String ThreadPoolName = "MapsforgeHillShading";

    /**
     * Approximate number of unit elements that each computing task will process.
     * The actual number is calculated during execution and can be slightly different.
     */
    protected final int ElementsPerComputingTask = 16000;

    /**
     * Number of additional "producer" threads that will do the reading (per caller thread), >= 0.
     */
    protected final int mReadingThreadsCount;

    /**
     * Number of additional "consumer" threads that will do the computations (per caller thread), >= 0.
     */
    protected final int mComputingThreadsCount;

    /**
     * When high quality, a unit element is 4x4 data points in size; otherwise it is 2x2.
     */
    protected final boolean mIsHighQuality;

    /**
     * Max number of active computing tasks per caller; if this limit is exceeded the reading will be throttled.
     * It is computed as (1 + 2 * {@link #mComputingThreadsCount}) * (1 + {@link #mReadingThreadsCount}) by default.
     * An active task is a task that is currently being processed or has been prepared and is waiting to be processed.
     */
    protected final int mActiveTasksCountMax;

    protected static final ThreadLocal<AtomicReference<HillShadingThreadPool>> mThreadPool = new ThreadLocal<AtomicReference<HillShadingThreadPool>>() {
        @Override
        protected AtomicReference<HillShadingThreadPool> initialValue() {
            return new AtomicReference<>(null);
        }
    };

    protected volatile boolean mStopSignal = false;

    /**
     * @param readingThreadsCount   Number of additional "producer" threads that will do the reading, >= 0.
     *                              Number N (>0) means there will be N additional threads (per caller thread) that will do the reading,
     *                              while 0 means that only the caller thread will do the reading.
     *                              The only time you'd want to set this to zero is when your data source does not support skipping,
     *                              ie. the data source is not a file and/or its {@link InputStream#skip(long)} is inefficient.
     *                              The default is 1.
     * @param computingThreadsCount Number of additional "consumer" threads that will do the computations, >= 0.
     *                              Number M (>0) means there will be M additional threads (per caller thread) that will do the computing,
     *                              while 0 means that producer thread(s) will also do the computing.
     *                              The only times you'd want to set this to zero are when memory conservation is a top priority
     *                              or when you're running on a single-threaded system.
     *                              The default is 1.
     * @param highQuality           When {@code true}, a unit element is 4x4 data points in size instead of 2x2, for better interpolation possibilities.
     *                              To make use of this, you should override the
     *                              {@link #processOneUnitElement(double, double, double, double, double, double, double, double, double, double, double, double, double, double, double, double, double, int, ComputingParams)}
     *                              method.
     *                              The default is {@code false}.
     */
    public AThreadedHillShading(final int readingThreadsCount, final int computingThreadsCount, final boolean highQuality) {
        super();

        mReadingThreadsCount = Math.max(0, readingThreadsCount);
        mComputingThreadsCount = Math.max(0, computingThreadsCount);
        mActiveTasksCountMax = (1 + 2 * mComputingThreadsCount) * (1 + mReadingThreadsCount);
        mIsHighQuality = highQuality;
    }

    /**
     * Employs standard quality unit elements (2x2 in size) by default.
     * Use {@link #AThreadedHillShading(int, int, boolean)} if you need high-quality unit elements (4x4 in size), for better interpolation possibilities.
     *
     * @param readingThreadsCount   Number of additional "producer" threads that will do the reading, >= 0.
     *                              Number N (>0) means there will be N additional threads (per caller thread) that will do the reading,
     *                              while 0 means that only the caller thread will do the reading.
     *                              The only time you'd want to set this to zero is when your data source does not support skipping,
     *                              ie. the data source is not a file and/or its {@link InputStream#skip(long)} is inefficient.
     *                              The default is 1.
     * @param computingThreadsCount Number of additional "consumer" threads that will do the computations, >= 0.
     *                              Number M (>0) means there will be M additional threads (per caller thread) that will do the computing,
     *                              while 0 means that producer thread(s) will also do the computing.
     *                              The only times you'd want to set this to zero are when memory conservation is a top priority
     *                              or when you're running on a single-threaded system.
     *                              The default is 1.
     */
    public AThreadedHillShading(final int readingThreadsCount, final int computingThreadsCount) {
        this(readingThreadsCount, computingThreadsCount, IsHighQualityDefault);
    }

    /**
     * Uses one additional reading thread, one additional computing thread, and standard quality unit elements (2x2 in size) by default.
     */
    public AThreadedHillShading() {
        this(ReadingThreadsCountDefault, ComputingThreadsCountDefault);
    }

    /**
     * <p>
     * Process one unit element, a smallest subdivision of the input, which consists of 2x2=4 points on a "square"
     * with vertices in NW-SW-SE-NE directions from the center.
     * </p>
     * <p>
     * Note: This method is not needed in the "high quality" mode -- just implement it as a dummy.
     * To work in the "high-quality" mode, you should override the overloaded method which takes 16 points as arguments.
     * </p>
     * <p>
     * A 2x2 unit element layout:
     * <pre>{@code
     * nw ne
     * sw se}
     * </pre>
     * </p>
     *
     * @param nw              North-west value. [meters]
     * @param sw              South-west value. [meters]
     * @param se              South-east value. [meters]
     * @param ne              North-east value. [meters]
     * @param mpe             Meters per unit element, ie. the length of one side of the unit element. [meters]
     * @param outputIx        Output array index, ie. index on the output array where.
     * @param computingParams Various parameters that are to be used during computations.
     * @return Updated {@code outputIx}, to be used for the next iteration.
     * @see ComputingParams
     * @see #processOneUnitElement(double, double, double, double, double, double, double, double, double, double, double, double, double, double, double, double, double, int, ComputingParams)
     */
    protected abstract int processOneUnitElement(double nw, double sw, double se, double ne, double mpe, int outputIx, ComputingParams computingParams);

    /**
     * Process one unit element, a smallest subdivision of the input, which consists of 4x4=16 points on a "square" with layout like shown below.
     * Override this method if you are working in the "high-quality" mode and need square unit elements of size 4x4 for better interpolation possibilities.
     * <p>
     * A 4x4 unit element layout:
     * <pre>{@code
     * nwnw nnw nne nene
     * wnw   nw ne   ene
     * wsw   sw se   ese
     * swsw ssw sse sese}
     * </pre>
     * </p>
     *
     * @param nw              North-west value. [meters]
     * @param sw              South-west value. [meters]
     * @param se              South-east value. [meters]
     * @param ne              North-east value. [meters]
     * @param nwnw            North-west-north-west value. [meters]
     * @param wnw             West-north-west value. [meters]
     * @param wsw             West-south-west value. [meters]
     * @param swsw            South-west-south-west value. [meters]
     * @param ssw             South-south-west value. [meters]
     * @param sse             South-south-east value. [meters]
     * @param sese            South-east-south-east value. [meters]
     * @param ese             East-south-east value. [meters]
     * @param ene             East-north-east value. [meters]
     * @param nene            North-east-north-east value. [meters]
     * @param nne             North-north-east value. [meters]
     * @param nnw             North-north-west value. [meters]
     * @param mpe             Meters per unit element, ie. the length of one side of the unit element. [meters]
     * @param outputIx        Output array index, ie. index on the output array where.
     * @param computingParams Various parameters that are to be used during computations.
     * @return Updated {@code outputIx}, to be used for the next iteration.
     * @see ComputingParams
     * @see #processOneUnitElement(double, double, double, double, double, int, ComputingParams)
     */
    protected int processOneUnitElement(double nw, double sw, double se, double ne, double nwnw, double wnw, double wsw, double swsw, double ssw, double sse, double sese, double ese, double ene, double nene, double nne, double nnw, double mpe, int outputIx, ComputingParams computingParams) {
        return processOneUnitElement(nw, sw, se, ne, mpe, outputIx, computingParams);
    }

    protected int processOneUnitElement(ComputingData computingData, double mpe, int firstLineIx, int secondLineIx, int thirdLineIx, int fourthLineIx, int outputIx, ComputingParams computingParams) {
        final short nw = computingData.mInput[secondLineIx - 1];
        final short sw = computingData.mInput[thirdLineIx - 1];
        final short se = computingData.mInput[thirdLineIx];
        final short ne = computingData.mInput[secondLineIx];

        final short nwnw = computingData.mInput[firstLineIx - 2];
        final short wnw = computingData.mInput[secondLineIx - 2];
        final short wsw = computingData.mInput[thirdLineIx - 2];
        final short swsw = computingData.mInput[fourthLineIx - 2];

        final short ssw = computingData.mInput[fourthLineIx - 1];
        final short sse = computingData.mInput[fourthLineIx];
        final short sese = computingData.mInput[fourthLineIx + 1];
        final short ese = computingData.mInput[thirdLineIx + 1];

        final short ene = computingData.mInput[secondLineIx + 1];
        final short nene = computingData.mInput[firstLineIx + 1];
        final short nne = computingData.mInput[firstLineIx];
        final short nnw = computingData.mInput[firstLineIx - 1];

        return processOneUnitElement(nw, sw, se, ne, nwnw, wnw, wsw, swsw, ssw, sse, sese, ese, ene, nene, nne, nnw, mpe, outputIx, computingParams);
    }

    protected int processOneUnitElementNorthNorthWest(ComputingData computingData, double mpe, int firstLineIx, int secondLineIx, int thirdLineIx, int fourthLineIx, int outputIx, ComputingParams computingParams) {
        final short nw = computingData.mSecondLine[secondLineIx - 1];
        final short sw = computingData.mInput[thirdLineIx - 1];
        final short se = computingData.mInput[thirdLineIx];
        final short ne = computingData.mSecondLine[secondLineIx];

        final short ssw = computingData.mInput[fourthLineIx - 1];
        final short sse = computingData.mInput[fourthLineIx];
        final short sese = computingData.mInput[fourthLineIx + 1];
        final short ese = computingData.mInput[thirdLineIx + 1];

        final short ene = computingData.mSecondLine[secondLineIx + 1];
        final short nene = computingData.mFirstLine[firstLineIx + 1];
        final short nne = computingData.mFirstLine[firstLineIx];
        final short nnw = computingData.mFirstLine[firstLineIx - 1];

        // Linear interpolation
        final short nwnw = (short) (2 * nw - se);
        final short wnw = (short) (2 * nw - ne);
        final short wsw = (short) (2 * sw - se);
        final short swsw = (short) (2 * sw - ne);

        return processOneUnitElement(nw, sw, se, ne, nwnw, wnw, wsw, swsw, ssw, sse, sese, ese, ene, nene, nne, nnw, mpe, outputIx, computingParams);
    }

    protected int processOneUnitElementNorthWest(ComputingData computingData, double mpe, int firstLineIx, int secondLineIx, int thirdLineIx, int fourthLineIx, int outputIx, ComputingParams computingParams) {
        final short nw = computingData.mInput[secondLineIx - 1];
        final short sw = computingData.mInput[thirdLineIx - 1];
        final short se = computingData.mInput[thirdLineIx];
        final short ne = computingData.mInput[secondLineIx];

        final short ssw = computingData.mInput[fourthLineIx - 1];
        final short sse = computingData.mInput[fourthLineIx];
        final short sese = computingData.mInput[fourthLineIx + 1];
        final short ese = computingData.mInput[thirdLineIx + 1];

        final short ene = computingData.mInput[secondLineIx + 1];
        final short nene = computingData.mSecondLine[firstLineIx + 1];
        final short nne = computingData.mSecondLine[firstLineIx];
        final short nnw = computingData.mSecondLine[firstLineIx - 1];

        // Linear interpolation
        final short nwnw = (short) (2 * nw - se);
        final short wnw = (short) (2 * nw - ne);
        final short wsw = (short) (2 * sw - se);
        final short swsw = (short) (2 * sw - ne);

        return processOneUnitElement(nw, sw, se, ne, nwnw, wnw, wsw, swsw, ssw, sse, sese, ese, ene, nene, nne, nnw, mpe, outputIx, computingParams);
    }

    protected int processOneUnitElementWest(ComputingData computingData, double mpe, int firstLineIx, int secondLineIx, int thirdLineIx, int fourthLineIx, int outputIx, ComputingParams computingParams) {
        final short nw = computingData.mInput[secondLineIx - 1];
        final short sw = computingData.mInput[thirdLineIx - 1];
        final short se = computingData.mInput[thirdLineIx];
        final short ne = computingData.mInput[secondLineIx];

        final short ssw = computingData.mInput[fourthLineIx - 1];
        final short sse = computingData.mInput[fourthLineIx];
        final short sese = computingData.mInput[fourthLineIx + 1];
        final short ese = computingData.mInput[thirdLineIx + 1];

        final short ene = computingData.mInput[secondLineIx + 1];
        final short nene = computingData.mInput[firstLineIx + 1];
        final short nne = computingData.mInput[firstLineIx];
        final short nnw = computingData.mInput[firstLineIx - 1];

        // Linear interpolation
        final short nwnw = (short) (2 * nw - se);
        final short wnw = (short) (2 * nw - ne);
        final short wsw = (short) (2 * sw - se);
        final short swsw = (short) (2 * sw - ne);

        return processOneUnitElement(nw, sw, se, ne, nwnw, wnw, wsw, swsw, ssw, sse, sese, ese, ene, nene, nne, nnw, mpe, outputIx, computingParams);
    }

    protected int processOneUnitElementSouthWest(ComputingData computingData, double mpe, int firstLineIx, int secondLineIx, int thirdLineIx, int outputIx, ComputingParams computingParams) {
        final short nw = computingData.mInput[secondLineIx - 1];
        final short sw = computingData.mInput[thirdLineIx - 1];
        final short se = computingData.mInput[thirdLineIx];
        final short ne = computingData.mInput[secondLineIx];

        final short ese = computingData.mInput[thirdLineIx + 1];

        final short ene = computingData.mInput[secondLineIx + 1];
        final short nene = computingData.mInput[firstLineIx + 1];
        final short nne = computingData.mInput[firstLineIx];
        final short nnw = computingData.mInput[firstLineIx - 1];

        // Linear interpolation
        final short swsw = (short) (2 * sw - ne);
        final short ssw = (short) (2 * sw - nw);
        final short sse = (short) (2 * se - ne);
        final short sese = (short) (2 * se - nw);

        final short nwnw = (short) (2 * nw - se);
        final short wnw = (short) (2 * nw - ne);
        final short wsw = (short) (2 * sw - se);

        return processOneUnitElement(nw, sw, se, ne, nwnw, wnw, wsw, swsw, ssw, sse, sese, ese, ene, nene, nne, nnw, mpe, outputIx, computingParams);
    }

    protected int processOneUnitElementSouth(ComputingData computingData, double mpe, int firstLineIx, int secondLineIx, int thirdLineIx, int outputIx, ComputingParams computingParams) {
        final short nw = computingData.mInput[secondLineIx - 1];
        final short sw = computingData.mInput[thirdLineIx - 1];
        final short se = computingData.mInput[thirdLineIx];
        final short ne = computingData.mInput[secondLineIx];

        final short nwnw = computingData.mInput[firstLineIx - 2];
        final short wnw = computingData.mInput[secondLineIx - 2];
        final short wsw = computingData.mInput[thirdLineIx - 2];
        final short ese = computingData.mInput[thirdLineIx + 1];

        final short ene = computingData.mInput[secondLineIx + 1];
        final short nene = computingData.mInput[firstLineIx + 1];
        final short nne = computingData.mInput[firstLineIx];
        final short nnw = computingData.mInput[firstLineIx - 1];

        // Linear interpolation
        final short swsw = (short) (2 * sw - ne);
        final short ssw = (short) (2 * sw - nw);
        final short sse = (short) (2 * se - ne);
        final short sese = (short) (2 * se - nw);

        return processOneUnitElement(nw, sw, se, ne, nwnw, wnw, wsw, swsw, ssw, sse, sese, ese, ene, nene, nne, nnw, mpe, outputIx, computingParams);
    }

    protected int processOneUnitElementSouthEast(ComputingData computingData, double mpe, int firstLineIx, int secondLineIx, int thirdLineIx, int outputIx, ComputingParams computingParams) {
        final short nw = computingData.mInput[secondLineIx - 1];
        final short sw = computingData.mInput[thirdLineIx - 1];
        final short se = computingData.mInput[thirdLineIx];
        final short ne = computingData.mInput[secondLineIx];

        final short nwnw = computingData.mInput[firstLineIx - 2];
        final short wnw = computingData.mInput[secondLineIx - 2];
        final short wsw = computingData.mInput[thirdLineIx - 2];

        final short nne = computingData.mInput[firstLineIx];
        final short nnw = computingData.mInput[firstLineIx - 1];

        // Linear interpolation
        final short swsw = (short) (2 * sw - ne);
        final short ssw = (short) (2 * sw - nw);
        final short sse = (short) (2 * se - ne);
        final short sese = (short) (2 * se - nw);

        final short ese = (short) (2 * se - sw);
        final short ene = (short) (2 * ne - nw);
        final short nene = (short) (2 * ne - sw);

        return processOneUnitElement(nw, sw, se, ne, nwnw, wnw, wsw, swsw, ssw, sse, sese, ese, ene, nene, nne, nnw, mpe, outputIx, computingParams);
    }

    protected int processOneUnitElementEast(ComputingData computingData, double mpe, int firstLineIx, int secondLineIx, int thirdLineIx, int fourthLineIx, int outputIx, ComputingParams computingParams) {
        final short nw = computingData.mInput[secondLineIx - 1];
        final short sw = computingData.mInput[thirdLineIx - 1];
        final short se = computingData.mInput[thirdLineIx];
        final short ne = computingData.mInput[secondLineIx];

        final short nwnw = computingData.mInput[firstLineIx - 2];
        final short wnw = computingData.mInput[secondLineIx - 2];
        final short wsw = computingData.mInput[thirdLineIx - 2];
        final short swsw = computingData.mInput[fourthLineIx - 2];

        final short ssw = computingData.mInput[fourthLineIx - 1];
        final short sse = computingData.mInput[fourthLineIx];
        final short nne = computingData.mInput[firstLineIx];
        final short nnw = computingData.mInput[firstLineIx - 1];

        // Linear interpolation
        final short sese = (short) (2 * se - nw);
        final short ese = (short) (2 * se - sw);
        final short ene = (short) (2 * ne - nw);
        final short nene = (short) (2 * ne - sw);

        return processOneUnitElement(nw, sw, se, ne, nwnw, wnw, wsw, swsw, ssw, sse, sese, ese, ene, nene, nne, nnw, mpe, outputIx, computingParams);
    }

    protected int processOneUnitElementNorthEast(ComputingData computingData, double mpe, int firstLineIx, int secondLineIx, int thirdLineIx, int fourthLineIx, int outputIx, ComputingParams computingParams) {
        final short nw = computingData.mInput[secondLineIx - 1];
        final short sw = computingData.mInput[thirdLineIx - 1];
        final short se = computingData.mInput[thirdLineIx];
        final short ne = computingData.mInput[secondLineIx];

        final short nwnw = computingData.mSecondLine[firstLineIx - 2];
        final short wnw = computingData.mInput[secondLineIx - 2];
        final short wsw = computingData.mInput[thirdLineIx - 2];
        final short swsw = computingData.mInput[fourthLineIx - 2];

        final short ssw = computingData.mInput[fourthLineIx - 1];
        final short sse = computingData.mInput[fourthLineIx];
        final short nne = computingData.mSecondLine[firstLineIx];
        final short nnw = computingData.mSecondLine[firstLineIx - 1];

        // Linear interpolation
        final short ene = (short) (2 * ne - nw);
        final short nene = (short) (2 * ne - sw);
        final short sese = (short) (2 * se - nw);
        final short ese = (short) (2 * se - sw);

        return processOneUnitElement(nw, sw, se, ne, nwnw, wnw, wsw, swsw, ssw, sse, sese, ese, ene, nene, nne, nnw, mpe, outputIx, computingParams);
    }

    protected int processOneUnitElementNorthNorthEast(ComputingData computingData, double mpe, int firstLineIx, int secondLineIx, int thirdLineIx, int fourthLineIx, int outputIx, ComputingParams computingParams) {
        final short nw = computingData.mSecondLine[secondLineIx - 1];
        final short sw = computingData.mInput[thirdLineIx - 1];
        final short se = computingData.mInput[thirdLineIx];
        final short ne = computingData.mSecondLine[secondLineIx];

        final short nwnw = computingData.mFirstLine[firstLineIx - 2];
        final short wnw = computingData.mSecondLine[secondLineIx - 2];
        final short wsw = computingData.mInput[thirdLineIx - 2];
        final short swsw = computingData.mInput[fourthLineIx - 2];

        final short ssw = computingData.mInput[fourthLineIx - 1];
        final short sse = computingData.mInput[fourthLineIx];
        final short nne = computingData.mFirstLine[firstLineIx];
        final short nnw = computingData.mFirstLine[firstLineIx - 1];

        // Linear interpolation
        final short ene = (short) (2 * ne - nw);
        final short nene = (short) (2 * ne - sw);
        final short sese = (short) (2 * se - nw);
        final short ese = (short) (2 * se - sw);

        return processOneUnitElement(nw, sw, se, ne, nwnw, wnw, wsw, swsw, ssw, sse, sese, ese, ene, nene, nne, nnw, mpe, outputIx, computingParams);
    }

    protected int processOneUnitElementNorth(ComputingData computingData, double mpe, int firstLineIx, int secondLineIx, int thirdLineIx, int fourthLineIx, int outputIx, ComputingParams computingParams) {
        final short nw = computingData.mInput[secondLineIx - 1];
        final short sw = computingData.mInput[thirdLineIx - 1];
        final short se = computingData.mInput[thirdLineIx];
        final short ne = computingData.mInput[secondLineIx];

        final short nwnw = computingData.mSecondLine[firstLineIx - 2];
        final short wnw = computingData.mInput[secondLineIx - 2];
        final short wsw = computingData.mInput[thirdLineIx - 2];
        final short swsw = computingData.mInput[fourthLineIx - 2];

        final short ssw = computingData.mInput[fourthLineIx - 1];
        final short sse = computingData.mInput[fourthLineIx];
        final short sese = computingData.mInput[fourthLineIx + 1];
        final short ese = computingData.mInput[thirdLineIx + 1];

        final short ene = computingData.mInput[secondLineIx + 1];
        final short nene = computingData.mSecondLine[firstLineIx + 1];
        final short nne = computingData.mSecondLine[firstLineIx];
        final short nnw = computingData.mSecondLine[firstLineIx - 1];

        return processOneUnitElement(nw, sw, se, ne, nwnw, wnw, wsw, swsw, ssw, sse, sese, ese, ene, nene, nne, nnw, mpe, outputIx, computingParams);
    }

    protected int processOneUnitElementNorthNorth(ComputingData computingData, double mpe, int firstLineIx, int secondLineIx, int thirdLineIx, int fourthLineIx, int outputIx, ComputingParams computingParams) {
        final short nw = computingData.mSecondLine[secondLineIx - 1];
        final short sw = computingData.mInput[thirdLineIx - 1];
        final short se = computingData.mInput[thirdLineIx];
        final short ne = computingData.mSecondLine[secondLineIx];

        final short nwnw = computingData.mFirstLine[firstLineIx - 2];
        final short wnw = computingData.mSecondLine[secondLineIx - 2];
        final short wsw = computingData.mInput[thirdLineIx - 2];
        final short swsw = computingData.mInput[fourthLineIx - 2];

        final short ssw = computingData.mInput[fourthLineIx - 1];
        final short sse = computingData.mInput[fourthLineIx];
        final short sese = computingData.mInput[fourthLineIx + 1];
        final short ese = computingData.mInput[thirdLineIx + 1];

        final short ene = computingData.mSecondLine[secondLineIx + 1];
        final short nene = computingData.mFirstLine[firstLineIx + 1];
        final short nne = computingData.mFirstLine[firstLineIx];
        final short nnw = computingData.mFirstLine[firstLineIx - 1];

        return processOneUnitElement(nw, sw, se, ne, nwnw, wnw, wsw, swsw, ssw, sse, sese, ese, ene, nene, nne, nnw, mpe, outputIx, computingParams);
    }

    protected int processOneUnitElementNorthFirstWest(ComputingData computingData, double mpe, int secondLineIx, int thirdLineIx, int fourthLineIx, int outputIx, ComputingParams computingParams) {
        final short nw = computingData.mFirstLine[secondLineIx - 1];
        final short sw = computingData.mSecondLine[thirdLineIx - 1];
        final short se = computingData.mSecondLine[thirdLineIx];
        final short ne = computingData.mFirstLine[secondLineIx];

        final short ssw = computingData.mInput[fourthLineIx - 1];
        final short sse = computingData.mInput[fourthLineIx];
        final short sese = computingData.mInput[fourthLineIx + 1];
        final short ese = computingData.mInput[thirdLineIx + 1];

        final short ene = computingData.mInput[secondLineIx + 1];

        // Linear interpolation
        final short nene = (short) (2 * ne - sw);
        final short nne = (short) (2 * ne - se);
        final short nnw = (short) (2 * nw - sw);

        final short nwnw = (short) (2 * nw - se);
        final short wnw = (short) (2 * nw - ne);
        final short wsw = (short) (2 * sw - se);
        final short swsw = (short) (2 * sw - ne);

        return processOneUnitElement(nw, sw, se, ne, nwnw, wnw, wsw, swsw, ssw, sse, sese, ese, ene, nene, nne, nnw, mpe, outputIx, computingParams);
    }

    protected int processOneUnitElementNorthFirst(ComputingData computingData, double mpe, int secondLineIx, int thirdLineIx, int fourthLineIx, int outputIx, ComputingParams computingParams) {
        final short nw = computingData.mFirstLine[secondLineIx - 1];
        final short sw = computingData.mSecondLine[thirdLineIx - 1];
        final short se = computingData.mSecondLine[thirdLineIx];
        final short ne = computingData.mFirstLine[secondLineIx];

        final short wnw = computingData.mFirstLine[secondLineIx - 2];
        final short wsw = computingData.mSecondLine[thirdLineIx - 2];
        final short swsw = computingData.mInput[fourthLineIx - 2];

        final short ssw = computingData.mInput[fourthLineIx - 1];
        final short sse = computingData.mInput[fourthLineIx];
        final short sese = computingData.mInput[fourthLineIx + 1];
        final short ese = computingData.mInput[thirdLineIx + 1];

        final short ene = computingData.mInput[secondLineIx + 1];

        // Linear interpolation
        final short nene = (short) (2 * ne - sw);
        final short nne = (short) (2 * ne - se);
        final short nnw = (short) (2 * nw - sw);
        final short nwnw = (short) (2 * nw - se);

        return processOneUnitElement(nw, sw, se, ne, nwnw, wnw, wsw, swsw, ssw, sse, sese, ese, ene, nene, nne, nnw, mpe, outputIx, computingParams);
    }

    protected int processOneUnitElementNorthFirstEast(ComputingData computingData, double mpe, int secondLineIx, int thirdLineIx, int fourthLineIx, int outputIx, ComputingParams computingParams) {
        final short nw = computingData.mFirstLine[secondLineIx - 1];
        final short sw = computingData.mSecondLine[thirdLineIx - 1];
        final short se = computingData.mSecondLine[thirdLineIx];
        final short ne = computingData.mFirstLine[secondLineIx];

        final short wnw = computingData.mFirstLine[secondLineIx - 2];
        final short wsw = computingData.mSecondLine[thirdLineIx - 2];
        final short swsw = computingData.mInput[fourthLineIx - 2];

        final short ssw = computingData.mInput[fourthLineIx - 1];
        final short sse = computingData.mInput[fourthLineIx];

        // Linear interpolation
        final short sese = (short) (2 * se - nw);
        final short ese = (short) (2 * se - sw);
        final short ene = (short) (2 * ne - nw);

        final short nene = (short) (2 * ne - sw);
        final short nne = (short) (2 * ne - se);
        final short nnw = (short) (2 * nw - sw);
        final short nwnw = (short) (2 * nw - se);

        return processOneUnitElement(nw, sw, se, ne, nwnw, wnw, wsw, swsw, ssw, sse, sese, ese, ene, nene, nne, nnw, mpe, outputIx, computingParams);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected byte[] convert(InputStream inputStream, int dummyAxisLen, int dummyRowLen, final int padding, HgtFileInfo fileInfo) throws IOException {
        return doTheWork(padding, fileInfo);
    }

    protected byte[] doTheWork(final int padding, final HgtFileInfo fileInfo) {
        final int outputAxisLen = getOutputAxisLen(fileInfo);
        final int inputAxisLen = getInputAxisLen(fileInfo);
        final int outputWidth = outputAxisLen + 2 * padding;
        final int inputWidth = inputAxisLen + 1;
        final double northUnitDistancePerLine = getLatUnitDistance(fileInfo.northLat(), inputAxisLen) / inputAxisLen;
        final double southUnitDistancePerLine = getLatUnitDistance(fileInfo.southLat(), inputAxisLen) / inputAxisLen;

        final byte[] output = new byte[outputWidth * outputWidth];

        if (isNotStopped()) {
            final AtomicInteger activeTasksCount = new AtomicInteger(0);
            final int lineArraysPoolSize = 1 + (mIsHighQuality ? 2 : 1) * mActiveTasksCountMax;
            final ShortArraysPool inputArraysPool = new ShortArraysPool(1 + mActiveTasksCountMax), lineBuffersPool = new ShortArraysPool(lineArraysPoolSize);

            final int readingTasksCount = 1 + mReadingThreadsCount;

            final int computingTasksCount = Math.max(readingTasksCount, determineComputingTasksCount(inputAxisLen));
            final int linesPerComputeTask = inputAxisLen / computingTasksCount;

            final int computeTasksPerReadingTask = computingTasksCount / readingTasksCount;
            final SilentFutureTask[] readingTasks = new SilentFutureTask[readingTasksCount];

            for (int readingTaskIndex = 0; readingTaskIndex < readingTasksCount; readingTaskIndex++) {

                final int computingTaskFrom, computingTaskTo;
                {
                    computingTaskFrom = computeTasksPerReadingTask * readingTaskIndex;

                    if (readingTaskIndex < readingTasksCount - 1) {
                        computingTaskTo = computingTaskFrom + computeTasksPerReadingTask;
                    } else {
                        computingTaskTo = computingTasksCount;
                    }
                }

                InputStream readStream = null;
                try {
                    readStream = fileInfo
                            .getFile()
                            .asStream();
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, e.toString(), e);
                }

                if (readingTaskIndex > 0) {
                    final long skipAmount = inputWidth * ((long) linesPerComputeTask * computingTaskFrom - (mIsHighQuality ? 1 : 0));

                    try {
                        HillShadingUtils.skipNBytes(readStream, skipAmount * Short.SIZE / Byte.SIZE);
                    } catch (IOException e) {
                        LOGGER.log(Level.SEVERE, e.toString(), e);
                    }
                }

                final ComputingParams computingParams = new ComputingParams.Builder()
                        .setInputStream(readStream)
                        .setOutput(output)
                        .setAwaiter(new Awaiter())
                        .setInputAxisLen(inputAxisLen)
                        .setOutputAxisLen(outputAxisLen)
                        .setOutputWidth(outputWidth)
                        .setInputWidth(inputWidth)
                        .setPadding(padding)
                        .setNorthUnitDistancePerLine(northUnitDistancePerLine)
                        .setSouthUnitDistancePerLine(southUnitDistancePerLine)
                        .setActiveTasksCount(activeTasksCount)
                        .setInputArraysPool(inputArraysPool)
                        .setLineBuffersPool(lineBuffersPool)
                        .build();

                final SilentFutureTask readingTask = getReadingTask(readStream, computingTasksCount, computingTaskFrom, computingTaskTo, linesPerComputeTask, computingParams);
                readingTasks[readingTaskIndex] = readingTask;

                if (readingTaskIndex < readingTasksCount - 1) {
                    postToThreadPoolOrRun(readingTask);
                } else {
                    readingTask.run();
                }
            }

            if (readingTasksCount > 1) {
                for (final SilentFutureTask readingTask : readingTasks) {
                    readingTask.get();
                }
            }
        }

        return output;
    }

    /**
     * If there are already too many active computing tasks, this will cause the caller to wait
     * until at least one computing task completes, to conserve memory.
     */
    protected void paceReading(final Awaiter awaiter, final AtomicInteger activeTasksCount, final int activeTasksCountMax) {
        if (mComputingThreadsCount > 0) {
            if (false == HillShadingUtils.atomicIncreaseIfLess(activeTasksCount, activeTasksCountMax)) {
                notifyReadingPaced(activeTasksCount.get());

                awaiter.doWait(new Callable<Boolean>() {
                    @Override
                    public Boolean call() {
                        return HillShadingUtils.atomicIncreaseIfLess(activeTasksCount, activeTasksCountMax);
                    }
                });
            }
        }
    }

    /**
     * Called when there are already too many active computing tasks (being processed or waiting in queue), so the reading must be slowed down to conserve memory.
     * If you get this notification, you may consider increasing the number of computing threads.
     *
     * @param activeComputingTasksCount How many computing tasks are waiting for completion at the time this is called.
     */
    protected void notifyReadingPaced(final int activeComputingTasksCount) {
    }

    /**
     * Decides a total number of computing tasks that will be used for the given input parameters.
     */
    protected int determineComputingTasksCount(final int inputAxisLen) {
        return (int) Math.max(1L, Math.min(inputAxisLen / 8, (long) inputAxisLen * inputAxisLen / ((mIsHighQuality ? 2 : 1) * ElementsPerComputingTask)));
    }

    protected int threadCount() {
        return mComputingThreadsCount + mReadingThreadsCount;
    }

    /**
     * The {@code Runnable} provided will be sent to the thread pool, or run on the calling thread if the thread pool rejects it (an unlikely event).
     *
     * @param code A code to run.
     */
    protected void postToThreadPoolOrRun(final Runnable code) {
        boolean status = false;

        final AtomicReference<HillShadingThreadPool> threadPoolReference = mThreadPool.get();

        if (threadPoolReference != null) {
            if (threadPoolReference.get() == null && threadCount() > 0) {
                synchronized (threadPoolReference) {
                    if (threadPoolReference.get() == null) {
                        threadPoolReference.set(createThreadPool());
                    }
                }
            }

            final HillShadingThreadPool threadPool = threadPoolReference.get();

            if (threadPool != null) {
                status = threadPool.execute(code);
            }
        }

        if (false == status) {
            if (code != null) {
                code.run();
            }
        }
    }

    protected HillShadingThreadPool createThreadPool() {
        final int threadCount = threadCount();
        return new HillShadingThreadPool(threadCount, threadCount, Integer.MAX_VALUE, 1, ThreadPoolName).start();
    }

    public short readNext(final InputStream is, final short[] fallbackInput, final int fallbackIx, final int offset) throws IOException {
        final int read1 = is.read();
        final int read2 = is.read();

        if (read1 != -1 && read2 != -1) {
            short read = (short) ((read1 << 8) | read2);

            if (read == Short.MIN_VALUE) {
                return fallbackInput[fallbackIx - offset];
            }

            return read;
        } else {
            return fallbackInput[fallbackIx - offset];
        }
    }

    /**
     * Default implementation always returns {@code true}.
     * Override to return a more meaningful value if needed, e.g. {@code return }!{@link #isStopped()}.
     *
     * @return {@code false} to stop processing. Default implementation always returns {@code true}.
     */
    protected boolean isNotStopped() {
        return true;
    }

    /**
     * Send a "stop" signal: Any active task will finish as soon as possible (possibly without completing),
     * and no new work will be done until a "continue" signal arrives.
     * Note: You should override {@link #isNotStopped()} if you need the stopping functionality.
     * Calling this without overriding {@link #isNotStopped()} will have no effect.
     */
    public void stopSignal() {
        mStopSignal = true;
    }

    /**
     * Send a "continue" signal: Allow new work to be done.
     * Note: You should override {@link #isNotStopped()} if you need the stopping functionality.
     * Calling this without overriding {@link #isNotStopped()} will have no effect.
     */
    public void continueSignal() {
        mStopSignal = false;
    }

    /**
     * Note: You should override {@link #isNotStopped()} if you need the stopping functionality.
     */
    public boolean isStopped() {
        return mStopSignal;
    }

    protected SilentFutureTask getReadingTask(InputStream readStream, int computingTasksCount, int computingTaskFrom, int computingTaskTo, int linesPerComputeTask, ComputingParams computingParams) {
        if (mIsHighQuality) {
            return new SilentFutureTask(new ReadingTask_4x4(readStream, computingTasksCount, computingTaskFrom, computingTaskTo, linesPerComputeTask, computingParams));
        } else {
            return new SilentFutureTask(new ReadingTask_2x2(readStream, computingTasksCount, computingTaskFrom, computingTaskTo, linesPerComputeTask, computingParams));
        }
    }

    protected SilentFutureTask getComputingTask(int lineFrom, int lineTo, ComputingData computingData, ComputingParams computingParams) {
        if (mIsHighQuality) {
            return new SilentFutureTask(new ComputingTask_4x4(lineFrom, lineTo, computingData, computingParams));
        } else {
            return new SilentFutureTask(new ComputingTask_2x2(lineFrom, lineTo, computingData, computingParams));
        }
    }

    /**
     * A "high quality" reading task which prepares (part of) the input for processing.
     */
    protected class ReadingTask_4x4 implements Callable<Boolean> {
        protected final InputStream mInputStream;
        protected final int mComputingTasksCount, mComputingTaskFrom, mComputingTaskTo, mLinesPerCompTask;
        protected final ComputingParams mComputingParams;

        public ReadingTask_4x4(InputStream inputStream, int computingTasksCount, int taskFrom, int taskTo, int linesPerTask, ComputingParams computingParams) {
            mInputStream = inputStream;
            mComputingTasksCount = computingTasksCount;
            mComputingTaskFrom = taskFrom;
            mComputingTaskTo = taskTo;
            mLinesPerCompTask = linesPerTask;
            mComputingParams = computingParams;
        }

        @Override
        public Boolean call() {
            boolean retVal = false;

            try {
                if (mInputStream != null) {
                    final SilentFutureTask[] computingTasks = new SilentFutureTask[mComputingTaskTo - mComputingTaskFrom];

                    final int inputAxisLen = mComputingParams.mInputAxisLen;
                    final int inputLineLen = mComputingParams.mInputWidth;
                    final AtomicInteger activeTasksCount = mComputingParams.mActiveTasksCount;
                    final Awaiter awaiter = mComputingParams.mAwaiter;
                    final ShortArraysPool inputArraysPool = mComputingParams.mInputArraysPool, lineBuffersPool = mComputingParams.mLineBuffersPool;

                    short[] firstLine = null, firstLineNext = null;
                    short[] secondLine = null, secondLineNext = null;
                    short[] input = null, inputNext = null;

                    for (int compTaskIndex = mComputingTaskFrom; compTaskIndex < mComputingTaskTo; compTaskIndex++) {

                        paceReading(awaiter, activeTasksCount, mActiveTasksCountMax);

                        final int lineFrom, lineTo;
                        final int inputSize, inputNextSize;
                        {
                            lineFrom = mLinesPerCompTask * compTaskIndex;

                            if (compTaskIndex < mComputingTasksCount - 1) {
                                lineTo = lineFrom + mLinesPerCompTask;
                            } else {
                                lineTo = inputAxisLen;
                            }

                            inputSize = 1 + lineTo - lineFrom;

                            if (compTaskIndex < mComputingTasksCount - 2) {
                                inputNextSize = inputSize;
                            } else if (compTaskIndex == mComputingTasksCount - 2) {
                                inputNextSize = 1 + inputAxisLen - mLinesPerCompTask * (mComputingTasksCount - 1);
                            } else {
                                inputNextSize = 1;
                            }
                        }

                        if (compTaskIndex > mComputingTaskFrom) {
                            firstLine = firstLineNext;
                            secondLine = secondLineNext;
                            input = inputNext;
                        } else {
                            firstLine = lineBuffersPool.getArray(inputLineLen);
                            secondLine = lineBuffersPool.getArray(inputLineLen);
                            input = inputArraysPool.getArray(inputLineLen * inputSize);

                            {
                                short last = 0;

                                // First line is done separately
                                for (int col = 0; col < inputLineLen; col++) {
                                    last = readNext(mInputStream, last);
                                    firstLine[col] = last;
                                }
                            }

                            // Second line is done separately
                            for (int col = 0; col < inputLineLen; col++) {
                                secondLine[col] = readNext(mInputStream, firstLine, col, 0);
                            }

                            // Third line is done separately
                            for (int col = 0; col < inputLineLen; col++) {
                                input[col] = readNext(mInputStream, secondLine, col, 0);
                            }
                        }

                        firstLineNext = lineBuffersPool.getArray(inputLineLen);
                        secondLineNext = lineBuffersPool.getArray(inputLineLen);
                        inputNext = inputArraysPool.getArray(inputLineLen * inputNextSize);

                        final int mainLoopFrom = lineFrom + 1 + (compTaskIndex <= 0 ? 1 : 0);
                        final int mainLoopTo = lineTo - 3 + 1;

                        // Skip the line already in the array
                        int inputIx = inputLineLen;

                        for (int line = mainLoopFrom; line < mainLoopTo && isNotStopped(); line++) {
                            // Inner loop, critical for performance
                            for (int col = 0; col < inputLineLen; col++, inputIx++) {
                                input[inputIx] = readNext(mInputStream, input, inputIx, inputLineLen);
                            }
                        }

                        // Third-to-last line is done separately
                        for (int col = 0; col < inputLineLen; col++, inputIx++) {
                            final short point = readNext(mInputStream, input, inputIx, inputLineLen);
                            input[inputIx] = point;
                            firstLineNext[col] = point;
                        }

                        // Second-to-last line is done separately
                        for (int col = 0; col < inputLineLen; col++, inputIx++) {
                            final short point = readNext(mInputStream, input, inputIx, inputLineLen);
                            input[inputIx] = point;
                            secondLineNext[col] = point;
                        }

                        // Last line is done separately
                        for (int col = 0; col < inputLineLen; col++, inputIx++) {
                            final short point = readNext(mInputStream, input, inputIx, inputLineLen);
                            input[inputIx] = point;
                            inputNext[col] = point;
                        }

                        final SilentFutureTask computingTask = getComputingTask(lineFrom, lineTo, new ComputingData(firstLine, secondLine, input), mComputingParams);
                        computingTasks[compTaskIndex - mComputingTaskFrom] = computingTask;

                        if (compTaskIndex < mComputingTaskTo - 1) {
                            postToThreadPoolOrRun(computingTask);
                        } else {
                            computingTask.run();
                        }
                    }

                    if (computingTasks.length > 1) {
                        for (final SilentFutureTask computingTask : computingTasks) {
                            computingTask.get();
                        }
                    }
                }

                retVal = true;

            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.toString());
            } finally {
                IOUtils.closeQuietly(mInputStream);
            }

            return retVal;
        }
    }

    /**
     * A "standard quality" reading task which prepares (part of) the input for processing.
     */
    protected class ReadingTask_2x2 implements Callable<Boolean> {
        protected final InputStream mInputStream;
        protected final int mComputingTasksCount, mComputingTaskFrom, mComputingTaskTo, mLinesPerCompTask;
        protected final ComputingParams mComputingParams;

        public ReadingTask_2x2(InputStream inputStream, int computingTasksCount, int taskFrom, int taskTo, int linesPerTask, ComputingParams computingParams) {
            mInputStream = inputStream;
            mComputingTasksCount = computingTasksCount;
            mComputingTaskFrom = taskFrom;
            mComputingTaskTo = taskTo;
            mLinesPerCompTask = linesPerTask;
            mComputingParams = computingParams;
        }

        @Override
        public Boolean call() {
            boolean retVal = false;

            try {
                if (mInputStream != null) {
                    final SilentFutureTask[] computingTasks = new SilentFutureTask[mComputingTaskTo - mComputingTaskFrom];

                    final int inputAxisLen = mComputingParams.mInputAxisLen;
                    final int inputLineLen = mComputingParams.mInputWidth;
                    final AtomicInteger activeTasksCount = mComputingParams.mActiveTasksCount;
                    final Awaiter awaiter = mComputingParams.mAwaiter;
                    final ShortArraysPool inputArraysPool = mComputingParams.mInputArraysPool, lineBuffersPool = mComputingParams.mLineBuffersPool;

                    short[] secondLine = null, secondLineNext = null;

                    for (int compTaskIndex = mComputingTaskFrom; compTaskIndex < mComputingTaskTo; compTaskIndex++) {

                        paceReading(awaiter, activeTasksCount, mActiveTasksCountMax);

                        final int lineFrom, lineTo;
                        {
                            lineFrom = mLinesPerCompTask * compTaskIndex;

                            if (compTaskIndex < mComputingTasksCount - 1) {
                                lineTo = lineFrom + mLinesPerCompTask;
                            } else {
                                lineTo = inputAxisLen;
                            }
                        }

                        if (compTaskIndex > mComputingTaskFrom) {
                            secondLine = secondLineNext;
                        } else {
                            secondLine = lineBuffersPool.getArray(inputLineLen);

                            short last = 0;

                            // First line is done separately
                            for (int col = 0; col < inputLineLen; col++) {
                                last = readNext(mInputStream, last);
                                secondLine[col] = last;
                            }
                        }

                        final short[] input;
                        {
                            secondLineNext = lineBuffersPool.getArray(inputLineLen);
                            input = inputArraysPool.getArray(inputLineLen * (lineTo - lineFrom));

                            int inputIx = 0;

                            // First line is done separately
                            for (; inputIx < inputLineLen; inputIx++) {
                                input[inputIx] = readNext(mInputStream, secondLine, inputIx, 0);
                            }

                            for (int line = lineFrom + 1; line < lineTo - 1 && isNotStopped(); line++) {
                                // Inner loop, critical for performance
                                for (int col = 0; col < inputLineLen; col++, inputIx++) {
                                    input[inputIx] = readNext(mInputStream, input, inputIx, inputLineLen);
                                }
                            }

                            // Last line is done separately
                            for (int col = 0; col < inputLineLen; col++, inputIx++) {
                                final short point = readNext(mInputStream, input, inputIx, inputLineLen);
                                input[inputIx] = point;
                                secondLineNext[col] = point;
                            }
                        }

                        final SilentFutureTask computingTask = getComputingTask(lineFrom, lineTo, new ComputingData(null, secondLine, input), mComputingParams);
                        computingTasks[compTaskIndex - mComputingTaskFrom] = computingTask;

                        if (compTaskIndex < mComputingTaskTo - 1) {
                            postToThreadPoolOrRun(computingTask);
                        } else {
                            computingTask.run();
                        }
                    }

                    if (computingTasks.length > 1) {
                        for (final SilentFutureTask computingTask : computingTasks) {
                            computingTask.get();
                        }
                    }
                }

                retVal = true;

            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.toString());
            } finally {
                IOUtils.closeQuietly(mInputStream);
            }

            return retVal;
        }
    }

    /**
     * A "high quality" computing task which converts part of the input to part of the output, by calling
     * {@link #processOneUnitElement(double, double, double, double, double, double, double, double, double, double, double, double, double, double, double, double, double, int, ComputingParams)}
     * on all input unit elements of size 4x4 from the given part.
     */
    protected class ComputingTask_4x4 implements Callable<Boolean> {
        protected final int mLineFrom, mLineTo;
        protected final ComputingData mComputingData;
        protected final ComputingParams mComputingParams;

        public ComputingTask_4x4(int lineFrom, int lineTo, ComputingData computingData, ComputingParams computingParams) {
            mLineFrom = lineFrom;
            mLineTo = lineTo;
            mComputingData = computingData;
            mComputingParams = computingParams;
        }

        @Override
        public Boolean call() {
            // TODO 2024-10: Uses linear interpolation on the edge lines of a DEM file data, where there are too few points to use bicubic.
            //  It should be considered whether this can be improved by obtaining edge lines from neighboring DEM files, so we have a bicubic interpolation
            //  everywhere except at the outer edges of the entire DEM data set.

            boolean retVal = false;

            try {
                final int resolutionFactor = mComputingParams.mOutputAxisLen / mComputingParams.mInputAxisLen;

                // Must add two additional paddings (after possibly skipping a line) to get to a starting position of the next line
                final int outputIxIncrement = (resolutionFactor - 1) * mComputingParams.mOutputWidth + 2 * mComputingParams.mPadding;

                int inputIx = 1;
                int fourthLineIx = inputIx + mComputingParams.mInputWidth;
                int line = mLineFrom;

                int outputIx = mComputingParams.mOutputWidth * mComputingParams.mPadding + mComputingParams.mPadding;
                outputIx += resolutionFactor * mLineFrom * mComputingParams.mOutputWidth;

                if (mLineFrom <= 0) {
                    // The very first line of the input data is done separately
                    {
                        int secondLineIx = 1;
                        final double metersPerElement = mComputingParams.mSouthUnitDistancePerLine * line + mComputingParams.mNorthUnitDistancePerLine * (mComputingParams.mInputAxisLen - line);

                        outputIx = processOneUnitElementNorthFirstWest(mComputingData, metersPerElement, secondLineIx, secondLineIx, secondLineIx, outputIx, mComputingParams);
                        secondLineIx++;

                        for (int col = 2; col <= mComputingParams.mInputAxisLen - 1; col++) {
                            outputIx = processOneUnitElementNorthFirst(mComputingData, metersPerElement, secondLineIx, secondLineIx, secondLineIx, outputIx, mComputingParams);
                            secondLineIx++;
                        }

                        outputIx = processOneUnitElementNorthFirstEast(mComputingData, metersPerElement, secondLineIx, secondLineIx, secondLineIx, outputIx, mComputingParams);

                        outputIx += outputIxIncrement;
                        line++;
                    }
                }

                // The first line of the input data part is done separately
                {
                    final double metersPerElement = mComputingParams.mSouthUnitDistancePerLine * line + mComputingParams.mNorthUnitDistancePerLine * (mComputingParams.mInputAxisLen - line);

                    outputIx = processOneUnitElementNorthNorthWest(mComputingData, metersPerElement, inputIx, inputIx, inputIx, fourthLineIx, outputIx, mComputingParams);
                    inputIx++;
                    fourthLineIx++;

                    for (int col = 2; col <= mComputingParams.mInputAxisLen - 1; col++) {
                        outputIx = processOneUnitElementNorthNorth(mComputingData, metersPerElement, inputIx, inputIx, inputIx, fourthLineIx, outputIx, mComputingParams);
                        inputIx++;
                        fourthLineIx++;
                    }

                    outputIx = processOneUnitElementNorthNorthEast(mComputingData, metersPerElement, inputIx, inputIx, inputIx, fourthLineIx, outputIx, mComputingParams);
                    inputIx++;
                    fourthLineIx++;

                    outputIx += outputIxIncrement;
                    line++;

                    mComputingParams.mLineBuffersPool.recycleArray(mComputingData.mFirstLine);
                }

                // The second line of the input data part is done separately
                {
                    inputIx++;
                    fourthLineIx++;

                    int firstLineIx = 1;
                    int secondLineIx = inputIx - mComputingParams.mInputWidth;
                    final double metersPerElement = mComputingParams.mSouthUnitDistancePerLine * line + mComputingParams.mNorthUnitDistancePerLine * (mComputingParams.mInputAxisLen - line);

                    outputIx = processOneUnitElementNorthWest(mComputingData, metersPerElement, firstLineIx, secondLineIx, inputIx, fourthLineIx, outputIx, mComputingParams);
                    inputIx++;
                    fourthLineIx++;
                    secondLineIx++;
                    firstLineIx++;

                    for (int col = 2; col <= mComputingParams.mInputAxisLen - 1; col++) {
                        outputIx = processOneUnitElementNorth(mComputingData, metersPerElement, firstLineIx, secondLineIx, inputIx, fourthLineIx, outputIx, mComputingParams);
                        inputIx++;
                        fourthLineIx++;
                        secondLineIx++;
                        firstLineIx++;
                    }

                    outputIx = processOneUnitElementNorthEast(mComputingData, metersPerElement, firstLineIx, secondLineIx, inputIx, fourthLineIx, outputIx, mComputingParams);
                    inputIx++;
                    fourthLineIx++;

                    outputIx += outputIxIncrement;
                    line++;

                    mComputingParams.mLineBuffersPool.recycleArray(mComputingData.mSecondLine);
                }

                // Bulk of the input data part is processed here
                {
                    int secondLineIx = inputIx - mComputingParams.mInputWidth;
                    int firstLineIx = secondLineIx - mComputingParams.mInputWidth;

                    // Outer loop
                    for (; line < mLineTo && isNotStopped(); line++) {
                        final double metersPerElement = mComputingParams.mSouthUnitDistancePerLine * line + mComputingParams.mNorthUnitDistancePerLine * (mComputingParams.mInputAxisLen - line);

                        inputIx++;
                        fourthLineIx++;
                        secondLineIx++;
                        firstLineIx++;

                        outputIx = processOneUnitElementWest(mComputingData, metersPerElement, firstLineIx, secondLineIx, inputIx, fourthLineIx, outputIx, mComputingParams);
                        inputIx++;
                        fourthLineIx++;
                        secondLineIx++;
                        firstLineIx++;

                        // Inner loop, critical for performance
                        for (int col = 2; col <= mComputingParams.mInputAxisLen - 1; col++) {
                            outputIx = processOneUnitElement(mComputingData, metersPerElement, firstLineIx, secondLineIx, inputIx, fourthLineIx, outputIx, mComputingParams);
                            inputIx++;
                            fourthLineIx++;
                            secondLineIx++;
                            firstLineIx++;
                        }

                        outputIx = processOneUnitElementEast(mComputingData, metersPerElement, firstLineIx, secondLineIx, inputIx, fourthLineIx, outputIx, mComputingParams);
                        inputIx++;
                        fourthLineIx++;
                        secondLineIx++;
                        firstLineIx++;

                        outputIx += outputIxIncrement;
                    }
                }

                mComputingParams.mInputArraysPool.recycleArray(mComputingData.mInput);

                retVal = true;

            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.toString());
            } finally {
                mComputingParams.mActiveTasksCount.decrementAndGet();
                mComputingParams.mAwaiter.doNotify();
            }

            return retVal;
        }
    }

    /**
     * A "standard quality" computing task which converts part of the input to part of the output, by calling
     * {@link #processOneUnitElement(double, double, double, double, double, int, ComputingParams)}
     * on all input unit elements of size 2x2 from the given part.
     */
    protected class ComputingTask_2x2 implements Callable<Boolean> {
        protected final int mLineFrom, mLineTo;
        protected final ComputingData mComputingData;
        protected final ComputingParams mComputingParams;

        public ComputingTask_2x2(int lineFrom, int lineTo, ComputingData computingData, ComputingParams computingParams) {
            mLineFrom = lineFrom;
            mLineTo = lineTo;
            mComputingData = computingData;
            mComputingParams = computingParams;
        }

        @Override
        public Boolean call() {
            boolean retVal = false;

            try {
                final int resolutionFactor = mComputingParams.mOutputAxisLen / mComputingParams.mInputAxisLen;

                // Must add two additional paddings (after possibly skipping a line) to get to a starting position of the next line
                final int outputIxIncrement = (resolutionFactor - 1) * mComputingParams.mOutputWidth + 2 * mComputingParams.mPadding;

                int outputIx = mComputingParams.mOutputWidth * mComputingParams.mPadding + mComputingParams.mPadding;
                outputIx += resolutionFactor * mLineFrom * mComputingParams.mOutputWidth;

                int inputIx = 0;

                // First line done separately, using the line buffer
                {
                    short nw = mComputingData.mSecondLine[inputIx];
                    short sw = mComputingData.mInput[inputIx++];

                    final double metersPerElement = mComputingParams.mSouthUnitDistancePerLine * mLineFrom + mComputingParams.mNorthUnitDistancePerLine * (mComputingParams.mInputAxisLen - mLineFrom);

                    for (int col = 1; col <= mComputingParams.mInputAxisLen; col++) {
                        final short ne = mComputingData.mSecondLine[inputIx];
                        final short se = mComputingData.mInput[inputIx++];

                        outputIx = processOneUnitElement(nw, sw, se, ne, metersPerElement, outputIx, mComputingParams);

                        nw = ne;
                        sw = se;
                    }

                    outputIx += outputIxIncrement;
                }

                mComputingParams.mLineBuffersPool.recycleArray(mComputingData.mSecondLine);

                int offsetInputIx = inputIx - mComputingParams.mInputWidth;

                for (int line = mLineFrom + 1; line < mLineTo && isNotStopped(); line++) {
                    short nw = mComputingData.mInput[offsetInputIx++];
                    short sw = mComputingData.mInput[inputIx++];

                    final double metersPerElement = mComputingParams.mSouthUnitDistancePerLine * line + mComputingParams.mNorthUnitDistancePerLine * (mComputingParams.mInputAxisLen - line);

                    // Inner loop, critical for performance
                    for (int col = 1; col <= mComputingParams.mInputAxisLen; col++) {
                        final short ne = mComputingData.mInput[offsetInputIx++];
                        final short se = mComputingData.mInput[inputIx++];

                        outputIx = processOneUnitElement(nw, sw, se, ne, metersPerElement, outputIx, mComputingParams);

                        nw = ne;
                        sw = se;
                    }

                    outputIx += outputIxIncrement;
                }

                mComputingParams.mInputArraysPool.recycleArray(mComputingData.mInput);

                retVal = true;

            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.toString());
            } finally {
                mComputingParams.mActiveTasksCount.decrementAndGet();
                mComputingParams.mAwaiter.doNotify();
            }

            return retVal;
        }
    }

    /**
     * Input data used for computing.
     */
    protected class ComputingData {
        // Data representing the first two lines of the input
        protected short[] mFirstLine, mSecondLine;
        // Data representing the rest of the input
        protected short[] mInput;

        public ComputingData(short[] firstLine, short[] secondLine, short[] input) {
            mFirstLine = firstLine;
            mSecondLine = secondLine;
            mInput = input;
        }
    }

    /**
     * Parameters that are used by a {@link AThreadedHillShading}.
     * An instance should be created using the provided builder, {@link Builder}.
     */
    public static class ComputingParams {
        public final InputStream mInputStream;
        public final byte[] mOutput;
        public final int mInputAxisLen;
        public final int mOutputAxisLen;
        public final int mInputWidth;
        public final int mOutputWidth;
        public final int mPadding;
        public final double mNorthUnitDistancePerLine;
        public final double mSouthUnitDistancePerLine;
        public final Awaiter mAwaiter;
        public final AtomicInteger mActiveTasksCount;
        public final ShortArraysPool mInputArraysPool, mLineBuffersPool;

        protected ComputingParams(final Builder builder) {
            mInputStream = builder.mInputStream;
            mOutput = builder.mOutput;
            mInputAxisLen = builder.mInputAxisLen;
            mOutputAxisLen = builder.mOutputAxisLen;
            mInputWidth = builder.mInputWidth;
            mOutputWidth = builder.mOutputWidth;
            mPadding = builder.mPadding;
            mNorthUnitDistancePerLine = builder.mNorthUnitDistancePerLine;
            mSouthUnitDistancePerLine = builder.mSouthUnitDistancePerLine;
            mAwaiter = builder.mAwaiter;
            mActiveTasksCount = builder.mActiveTasksCount;
            mInputArraysPool = builder.mInputArraysPool;
            mLineBuffersPool = builder.mLineBuffersPool;
        }

        public static class Builder {
            protected volatile InputStream mInputStream;
            protected volatile byte[] mOutput;
            protected volatile int mInputAxisLen;
            protected volatile int mOutputAxisLen;
            protected volatile int mInputWidth;
            protected volatile int mOutputWidth;
            protected volatile int mPadding;
            protected volatile double mNorthUnitDistancePerLine;
            protected volatile double mSouthUnitDistancePerLine;
            protected volatile Awaiter mAwaiter;
            protected volatile AtomicInteger mActiveTasksCount;
            protected volatile ShortArraysPool mInputArraysPool, mLineBuffersPool;

            public Builder() {
            }

            /**
             * Create the {@link ComputingParams} instance using parameter values from this builder.
             * All parameters used in computations should be explicitly set.
             *
             * @return New {@link ComputingParams} instance built using parameter values from this {@link Builder}
             */
            public ComputingParams build() {
                return new ComputingParams(this);
            }

            public Builder setInputStream(InputStream inputStream) {
                this.mInputStream = inputStream;
                return this;
            }

            public Builder setOutput(byte[] output) {
                this.mOutput = output;
                return this;
            }

            public Builder setInputAxisLen(int inputAxisLen) {
                this.mInputAxisLen = inputAxisLen;
                return this;
            }

            public Builder setOutputAxisLen(int outputAxisLen) {
                this.mOutputAxisLen = outputAxisLen;
                return this;
            }

            public Builder setInputWidth(int inputWidth) {
                this.mInputWidth = inputWidth;
                return this;
            }

            public Builder setOutputWidth(int outputWidth) {
                this.mOutputWidth = outputWidth;
                return this;
            }

            public Builder setPadding(int padding) {
                this.mPadding = padding;
                return this;
            }

            public Builder setNorthUnitDistancePerLine(double northUnitDistancePerLine) {
                this.mNorthUnitDistancePerLine = northUnitDistancePerLine;
                return this;
            }

            public Builder setSouthUnitDistancePerLine(double southUnitDistancePerLine) {
                this.mSouthUnitDistancePerLine = southUnitDistancePerLine;
                return this;
            }

            public Builder setAwaiter(Awaiter awaiter) {
                this.mAwaiter = awaiter;
                return this;
            }

            public Builder setActiveTasksCount(AtomicInteger activeTasksCount) {
                this.mActiveTasksCount = activeTasksCount;
                return this;
            }

            public Builder setInputArraysPool(ShortArraysPool inputArraysPool) {
                this.mInputArraysPool = inputArraysPool;
                return this;
            }

            public Builder setLineBuffersPool(ShortArraysPool lineBuffersPool) {
                this.mLineBuffersPool = lineBuffersPool;
                return this;
            }
        }
    }
}
