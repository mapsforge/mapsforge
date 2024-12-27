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
import org.mapsforge.map.layer.hills.HillShadingUtils.HillShadingThreadPool;
import org.mapsforge.map.layer.hills.HillShadingUtils.ShortArraysPool;
import org.mapsforge.map.layer.hills.HillShadingUtils.SilentFutureTask;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

/**
 * <p>
 * Special abstract implementation of hill shading algorithm where input data can be divided into parts
 * that are processed in parallel by multiple threads.
 * </p>
 * <p>
 * The implementation is such that there are N (>=1) "producer" threads that read from the input
 * and do the synchronization, and M (>=0) "consumer" threads that do the computations.
 * It should be emphasized that all caller threads share one (static) thread pool.
 * </p>
 * <p>
 * Special attention is paid to reducing memory consumption.
 * A producer thread will throttle itself and stop reading until the computation catches up.
 * If this happens, {@link #notifyReadingPaced(long, int, int, int)} will be called.
 * </p>
 * <p>
 * Rough estimate of the <em>maximum</em> memory used at any given moment is as follows:
 * <br />
 * <br />
 * {@code max_bytes_used = (1 + 2 * M) *} {@link #ElementsPerComputingTask} {@code *} {@link Short#BYTES}
 * <br />
 * <br />
 * Default max memory usage:
 * <br />
 * For a system with 8 processors, N = 8 and M = 8, about 544000 * 2 bytes (cca 1.1 MB);
 * <br />
 * For a system with 4 processors, N = 4 and M = 4, about 288000 * 2 bytes (cca 600 kB);
 * <br />
 * For a system with 1 processor, N = 1 and M = 1, about 96000 * 2 bytes (cca 200 kB).
 * <br />
 * <br />
 * If the computations are fast enough, average memory usage will be several times smaller.
 * Note that this only counts the working memory used while the hill shading algorithm computes and that is freed when the algorithm finishes;
 * memory used by algorithm products, such as the final shading bitmap, is not counted.
 * </p>
 * <p>
 * You can set the algorithm to the "high quality" setting.
 * The unit element is then 4x4 data points in size instead of 2x2, allowing for better interpolation possibilities.
 * To make use of this, you should override both the
 * {@link #processRow_4x4(short[], int, int, int, int, double, int, ComputingParams)}
 * and {@link #processUnitElement_4x4(double, double, double, double, double, double, double, double, double, double, double, double, double, double, double, double, double, int, ComputingParams)}
 * methods.
 * The latter method will get called for the edges of the input data.
 * </p>
 * <p>
 * For standard quality, it's enough to override only one of those methods: Override {@code processRow_2x2()}
 * for efficiency; override {@code processUnitElement_4x4()} for simplicity.
 * </p>
 * <p>
 * 2x2 unit element layout:
 * <pre>{@code
 * nw ne
 * sw se}
 * </pre>
 * </p>
 * <p>
 * 4x4 unit element layout:
 * <pre>{@code
 * nwnw nnw nne nene
 * wnw   nw ne   ene
 * wsw   sw se   ese
 * swsw ssw sse sese}
 * </pre>
 * </p>
 */
public abstract class AThreadedHillShading extends AShadingAlgorithm {

    /**
     * The number of processors available to the Java runtime.
     */
    public static final int AvailableProcessors = Runtime
            .getRuntime()
            .availableProcessors();

    /**
     * Default number of reading threads ("producer" threads).
     * Number N (>=1) means there will be N threads that will do the reading.
     * Zero (0) is not permitted.
     */
    public static final int ReadingThreadsCountDefault = Math.max(1, AvailableProcessors);

    /**
     * Default number of computing threads ("consumer" threads).
     * Number M (>=0) means there will be M threads that will do the computing.
     * Zero means that reading/producer thread(s) will also do the computing.
     */
    public static final int ComputingThreadsCountDefault = AvailableProcessors;

    /**
     * Approximate number of unit elements that each computing task will process.
     * The actual number is calculated during execution and can be slightly different.
     */
    protected final int ElementsPerComputingTask = 32000;

    /**
     * Decides when to use full buffered streams and when to use "raw" streams.
     * Raw streams are useful to avoid wasteful buffering when skipping a lot.
     */
    protected final int StrideFactorRawStreamLimit = 10;

    /**
     * When high quality, a unit element is 4x4 data points in size; otherwise it is 2x2.
     */
    public static final boolean IsHighQualityDefault = false;

    /**
     * Whether input data preprocessing is enabled, to remove invalid values.
     */
    public static final boolean IsPreprocessDefault = true;

    /**
     * Default name prefix for additional reading threads created and used by hill shading. A numbered suffix will be appended.
     */
    public final String ReadingThreadPoolName = "MapsforgeHillShadingRead";

    /**
     * Default name prefix for additional computing threads created and used by hill shading. A numbered suffix will be appended.
     */
    public final String ComputingThreadPoolName = "MapsforgeHillShadingComp";

    /**
     * Number of "producer" threads that will do the reading, >= 1.
     */
    protected final int mReadingThreadsCount;

    /**
     * Number of "consumer" threads that will do the computations, >= 0.
     */
    protected final int mComputingThreadsCount;

    /**
     * Whether input data preprocessing is enabled, to remove invalid values.
     */
    protected final boolean mIsPreprocess;

    /**
     * Max number of active computing tasks per reading task; if this limit is exceeded the reading will be throttled.
     * It is computed as {@code (1 + 2 *} {@link #mComputingThreadsCount}{@code )} by default.
     * An active task is a task that is currently being processed or has been prepared and is waiting to be processed.
     */
    protected final int mActiveTasksCountMax;

    /**
     * Thread pools.
     */
    protected final AtomicReference<HillShadingThreadPool> mReadThreadPool = new AtomicReference<>(null);
    protected final AtomicReference<HillShadingThreadPool> mCompThreadPool = new AtomicReference<>(null);

    /**
     * Stop signal flag, indicating that processing should be stopped as soon as possible.
     *
     * @see #isNotStopped()
     */
    protected volatile boolean mStopSignal = false;

    /**
     * Counter used to assign IDs to reading tasks.
     */
    protected final AtomicLong mReadTaskCounter = new AtomicLong(0);

    /**
     * @param readingThreadsCount   Number of "producer" threads that will do the reading, >= 1.
     *                              Number N (>=1) means there will be N threads that will do the reading.
     *                              Zero (0) is not permitted.
     *                              The only time you'd want to set this to 1 is when your data source does not support skipping,
     *                              i.e. the data source is not a file and/or its {@link InputStream#skip(long)} is inefficient.
     *                              The default is computed as {@code Math.max(1,} {@link #AvailableProcessors}{@code )}.
     * @param computingThreadsCount Number of "consumer" threads that will do the computations, >= 0.
     *                              Number M (>=0) means there will be M threads that will do the computing.
     *                              Zero (0) means that producer thread(s) will also do the computing.
     *                              The only times you'd want to set this to zero are when memory conservation is a top priority
     *                              or when you're running on a single-threaded system.
     *                              The default is {@link #AvailableProcessors}, the number of processors available to the Java runtime.
     * @param preprocess            When {@code true}, input data will be preprocessed to remove possible invalid values.
     *                              The default is {@code true}.
     */
    public AThreadedHillShading(final int readingThreadsCount, final int computingThreadsCount, final boolean preprocess) {
        super();

        mReadingThreadsCount = Math.max(1, readingThreadsCount);
        mComputingThreadsCount = Math.max(0, computingThreadsCount);
        mActiveTasksCountMax = 1 + 2 * mComputingThreadsCount;
        mIsPreprocess = preprocess;
    }

    /**
     * Employs preprocessing by default.
     *
     * @param readingThreadsCount   Number of "producer" threads that will do the reading, >= 1.
     *                              Number N (>=1) means there will be N threads that will do the reading.
     *                              Zero (0) is not permitted.
     *                              The only time you'd want to set this to 1 is when your data source does not support skipping,
     *                              i.e. the data source is not a file and/or its {@link InputStream#skip(long)} is inefficient.
     *                              The default is computed as {@code Math.max(1,} {@link #AvailableProcessors}{@code )}.
     * @param computingThreadsCount Number of "consumer" threads that will do the computations, >= 0.
     *                              Number M (>=0) means there will be M threads that will do the computing.
     *                              Zero (0) means that producer thread(s) will also do the computing.
     *                              The only times you'd want to set this to zero are when memory conservation is a top priority
     *                              or when you're running on a single-threaded system.
     *                              The default is {@link #AvailableProcessors}, the number of processors available to the Java runtime.
     */
    public AThreadedHillShading(final int readingThreadsCount, final int computingThreadsCount) {
        this(readingThreadsCount, computingThreadsCount, IsPreprocessDefault);
    }

    /**
     * Uses default values for all parameters.
     */
    public AThreadedHillShading() {
        this(ReadingThreadsCountDefault, ComputingThreadsCountDefault);
    }

    /**
     * <p>
     * Process one line of the input array.
     * Subclasses using high-quality mode are required to override this method.
     * </p>
     * <p>
     * <strong>Note</strong>: This method should not process the first (westmost) and last (eastmost) elements of the line!
     * These and other elements at the edges of the input data are processed by calling
     * {@link #processUnitElement_4x4(double, double, double, double, double, double, double, double, double, double, double, double, double, double, double, double, double, int, ComputingParams)}.
     * This is why a high-quality implementation should override both methods.
     * </p>
     * <p>
     * 4x4 unit element layout:
     * <pre>{@code
     * nwnw nnw nne nene
     * wnw   nw ne   ene
     * wsw   sw se   ese
     * swsw ssw sse sese}
     * </pre>
     * </p>
     *
     * @param input            Input array.
     * @param firstLineIx      Index on the input array where the first row of the unit element is located. More exactly, this points to the {@code nnw} value.
     * @param secondLineOffset Index offset on the input array to get from the first row to the second row of the unit element. More exactly, this is the offset to the {@code nw} value.
     * @param thirdLineOffset  Index offset on the input array to get from the first row to the third row of the unit element. More exactly, this is the offset to the {@code sw} value.
     * @param fourthLineOffset Index offset on the input array to get from the first row to the fourth row of the unit element. More exactly, this is the offset to the {@code ssw} value.
     * @param dsf              Distance scale factor, that is half the length of one side of the standard 2x2 unit element inverted, i.e. {@code 0.5 / length}. [1/meters]
     * @param outputIx         Output array index, i.e. the index on the output array where writing should be performed.
     * @param computingParams  Various parameters that are to be used during computations.
     * @return Updated {@code outputIx}, to be used for the next iteration.
     * @see #processUnitElement_4x4(double, double, double, double, double, double, double, double, double, double, double, double, double, double, double, double, double, int, ComputingParams)
     * @see ComputingParams
     */
    protected int processRow_4x4(short[] input, int firstLineIx, int secondLineOffset, int thirdLineOffset, int fourthLineOffset, double dsf, int outputIx, ComputingParams computingParams) {
        throw new IllegalStateException("Please implement the processRow_4x4(short[], int, int, int, int, double, int, ComputingParams) method!");
    }

    /**
     * <p>
     * Process one line of the input array.
     * In default implementation the elements are processed one by one by calling
     * {@link #processUnitElement_2x2(double, double, double, double, double, int, ComputingParams)}.
     * Subclasses are encouraged to override this method and provide a more efficient implementation.
     * </p>
     * <p>
     * 2x2 unit element layout:
     * <pre>{@code
     * nw ne
     * sw se}
     * </pre>
     * </p>
     *
     * @param input            Input array.
     * @param firstLineIx      Index on the input array where the first row of the unit element is located. More exactly, this points to the {@code nw} value.
     * @param secondLineOffset Index offset on the input array to get from the first row to the second row of the unit element. More exactly, this is the offset to the {@code sw} value.
     * @param dsf              Distance scale factor, that is half the length of one side of the standard 2x2 unit element inverted, i.e. {@code 0.5 / length}. [1/meters]
     * @param outputIx         Output array index, i.e. the index on the output array where writing should be performed.
     * @param computingParams  Various parameters that are to be used during computations.
     * @return Updated {@code outputIx}, to be used for the next iteration.
     * @see #processUnitElement_2x2(double, double, double, double, double, int, ComputingParams)
     * @see ComputingParams
     */
    protected int processRow_2x2(short[] input, int firstLineIx, int secondLineOffset, double dsf, int outputIx, ComputingParams computingParams) {
        double nw = input[firstLineIx];
        double sw = input[firstLineIx + secondLineOffset];

        ++firstLineIx;

        final int limit = firstLineIx + computingParams.mInputAxisLen;

        for (; ; ) {
            final double ne = input[firstLineIx];
            final double se = input[firstLineIx + secondLineOffset];

            outputIx = processUnitElement_2x2(nw, sw, se, ne, dsf, outputIx, computingParams);

            if (++firstLineIx < limit) {
                nw = ne;
                sw = se;
            } else {
                break;
            }
        }

        return outputIx;
    }

    /**
     * <p>
     * Process one unit element, a smallest subdivision of the input, which consists of 4x4 points on a "square" with layout like shown below.
     * </p>
     * <p>
     * If you are working in the high-quality mode you should override this method in addition to the
     * {@link #processRow_4x4(short[], int, int, int, int, double, int, ComputingParams)},
     * because this one is used on the edges of the input data.
     * </p>
     * <p>
     * 4x4 unit element layout:
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
     * @param dsf             Distance scale factor, that is half the length of one side of the standard 2x2 unit element inverted, i.e. {@code 0.5 / length}. [1/meters]
     * @param outputIx        Output array index, i.e. the index on the output array where writing should be performed.
     * @param computingParams Various parameters that are to be used during computations.
     * @return Updated {@code outputIx}, to be used for the next iteration.
     * @see #processRow_4x4(short[], int, int, int, int, double, int, ComputingParams)
     * @see ComputingParams
     */
    protected int processUnitElement_4x4(double nw, double sw, double se, double ne, double nwnw, double wnw, double wsw, double swsw, double ssw, double sse, double sese, double ese, double ene, double nene, double nne, double nnw, double dsf, int outputIx, ComputingParams computingParams) {
        throw new IllegalStateException("Please implement the processUnitElement_4x4() method!");
    }

    /**
     * <p>
     * Process one unit element, a smallest subdivision of the input, which consists of 2x2 points on a "square" with layout like shown below.
     * </p>
     * <p>
     * It is preferable to override the
     * {@link #processRow_2x2(short[], int, int, double, int, ComputingParams)}
     * method instead to provide a more efficient implementation, if possible.
     * </p>
     * <p>
     * 2x2 unit element layout:
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
     * @param dsf             Distance scale factor, that is half the length of one side of the standard 2x2 unit element inverted, i.e. {@code 0.5 / length}. [1/meters]
     * @param outputIx        Output array index, i.e. the index on the output array where writing should be performed.
     * @param computingParams Various parameters that are to be used during computations.
     * @return Updated {@code outputIx}, to be used for the next iteration.
     * @see #processRow_2x2(short[], int, int, double, int, ComputingParams)
     * @see ComputingParams
     */
    protected int processUnitElement_2x2(double nw, double sw, double se, double ne, double dsf, int outputIx, ComputingParams computingParams) {
        throw new IllegalStateException("Please implement the processUnitElement_2x2() method!");
    }

    /**
     * Remove invalid values from the input array.
     * An invalid value is a value equal to {@link Short#MIN_VALUE}.
     *
     * @param input Input array to preprocess.
     * @param width Width of one line of the input array.
     */
    protected void preprocess(final short[] input, final int width) {
        int nw = 0, sw = nw + width;

        for (int line = 0; line < input.length / width - 1; line++) {
            for (int col = 0; col < width - 1; col++, nw++, sw++) {
                preprocess2x2Element(input, nw, sw, sw + 1, nw + 1);
            }
            nw++;
            sw++;
        }
    }

    /**
     * Remove invalid values from one 2x2 unit element defined by four indices on the input array.
     * An invalid value is a value equal to {@link Short#MIN_VALUE}.
     *
     * @param in Input array to preprocess.
     * @param nw Index of the north-west value.
     * @param sw Index of the south-west value.
     * @param se Index of the south-east value.
     * @param ne Index of the north-east value.
     */
    protected void preprocess2x2Element(short[] in, int nw, int sw, int se, int ne) {
        if (in[nw] == Short.MIN_VALUE || in[sw] == Short.MIN_VALUE || in[se] == Short.MIN_VALUE || in[ne] == Short.MIN_VALUE) {
            // Preprocessing step, to remove invalid input values.
            // We'll get here rarely, ideally never.

            double sum = 0;
            int count = 0;

            if (in[nw] != Short.MIN_VALUE) {
                sum += in[nw];
                count++;
            }

            if (in[sw] != Short.MIN_VALUE) {
                sum += in[sw];
                count++;
            }

            if (in[se] != Short.MIN_VALUE) {
                sum += in[se];
                count++;
            }

            if (in[ne] != Short.MIN_VALUE) {
                sum += in[ne];
                count++;
            }

            if (count == 3) {
                if (in[nw] == Short.MIN_VALUE) {
                    in[nw] = (short) Math.round((sum - in[se]) / 2);
                } else if (in[sw] == Short.MIN_VALUE) {
                    in[sw] = (short) Math.round((sum - in[ne]) / 2);
                } else if (in[se] == Short.MIN_VALUE) {
                    in[se] = (short) Math.round((sum - in[nw]) / 2);
                } else {
                    in[ne] = (short) Math.round((sum - in[sw]) / 2);
                }
            } else if (count == 2) {
                if (in[nw] == Short.MIN_VALUE && in[ne] == Short.MIN_VALUE) {
                    in[nw] = in[sw];
                    in[ne] = in[se];
                } else if (in[nw] == Short.MIN_VALUE && in[sw] == Short.MIN_VALUE) {
                    in[nw] = in[ne];
                    in[sw] = in[se];
                } else if (in[se] == Short.MIN_VALUE && in[sw] == Short.MIN_VALUE) {
                    in[sw] = in[nw];
                    in[se] = in[ne];
                } else if (in[se] == Short.MIN_VALUE && in[ne] == Short.MIN_VALUE) {
                    in[se] = in[sw];
                    in[ne] = in[nw];
                } else if (in[nw] == Short.MIN_VALUE) {
                    in[nw] = (short) Math.round(sum / 2);
                    in[se] = in[nw];
                } else {
                    in[sw] = (short) Math.round(sum / 2);
                    in[ne] = in[sw];
                }
            } else {
                if (in[nw] == Short.MIN_VALUE) {
                    in[nw] = (short) sum;
                }

                if (in[sw] == Short.MIN_VALUE) {
                    in[sw] = (short) sum;
                }

                if (in[se] == Short.MIN_VALUE) {
                    in[se] = (short) sum;
                }

                if (in[ne] == Short.MIN_VALUE) {
                    in[ne] = (short) sum;
                }
            }
        }
    }

    /**
     * @return Whether the zoom level is supported or not (possibly depending on the HGT file properties).
     */
    public boolean isZoomLevelSupported(int zoomLevel, HgtFileInfo hgtFileInfo) {
        return zoomLevel <= getZoomMax(hgtFileInfo) && zoomLevel >= getZoomMin(hgtFileInfo);
    }

    /**
     * <p>
     * Computes a "distance scale factor" or dsf, as a half the length of one side of the standard 2x2 unit element inverted, i.e. {@code dsf = 0.5 / length}.
     * Dsf is a concept that anticipates "average normal" calculations like it's being done in
     * {@link StandardClasyHillShading#unitElementToShadePixel(double, double, double, double, double)},
     * and by its very nature it simplifies the calculations there.
     * </p>
     * <p>
     * It takes into account the stride factor to heuristically amplify slope magnitudes when skipping through the data,
     * as slopes naturally tend to get smaller as the run gets larger (Slope=Rise/Run; Run ~ StrideFactor).
     * </p>
     * <p>
     * To get a more intuitive measure of "meters per unit element" from dsf, if you ever need it, simply do this:
     * {@code mpe = length = 0.5 / dsf}
     * </p>
     *
     * @param line            Current row being processed.
     * @param computingParams Various parameters that are to be used during computations.
     * @return Computed dsf (distance scale factor)
     */
    protected double computeDistanceScaleFactor(int line, ComputingParams computingParams) {
        // The purpose of the amplification factor is to amplify slope magnitudes when skipping through the data,
        // to counteract the tendency for hills to become fainter as more terrain data is discarded.
        // This is just a heuristic. The more data we discard, the less we worry about accuracy anyway.
        final double amplification = computingParams.mStrideFactor <= 4 ? 1 : Math.max(1, 0.5 * Math.sqrt(computingParams.mStrideFactor));

        return amplification * 0.5 / (computingParams.mSouthUnitDistancePerLine * line + computingParams.mNorthUnitDistancePerLine * (computingParams.mInputAxisLen - line));
    }

    protected int getStrideFactor(int inputAxisLen, int outputAxisLen) {
        return Math.max(1, inputAxisLen / outputAxisLen);
    }

    @Override
    protected byte[] convert(InputStream inputStream, int dummyAxisLen, int dummyRowLen, int padding, int zoomLevel, double pxPerLat, double pxPerLon, HgtFileInfo hgtFileInfo) throws IOException {
        return convert(hgtFileInfo, false, padding, zoomLevel, pxPerLat, pxPerLon);
    }

    /**
     * @param hgtFileInfo   HGT file info
     * @param isHighQuality When {@code true}, a unit element is 4x4 data points in size instead of 2x2, for better interpolation possibilities.
     * @param padding       Padding of the output, useful to minimize border interpolation artifacts (no need to be larger than 1)
     * @param zoomLevel     Zoom level (to determine shading quality requirements)
     * @param pxPerLat      Tile pixels per degree of latitude (to determine shading quality requirements)
     * @param pxPerLon      Tile pixels per degree of longitude (to determine shading quality requirements)
     * @return
     */
    protected byte[] convert(final HgtFileInfo hgtFileInfo, boolean isHighQuality, int padding, int zoomLevel, double pxPerLat, double pxPerLon) {
        final byte[] output;

        if (!isDebugTiming()) {
            output = doTheWork(hgtFileInfo, isHighQuality, padding, zoomLevel, pxPerLat, pxPerLon);
        } else {
            final long startTs, finishTs;

            if (isDebugTimingSequential()) {
                // We want to process one file at a time for more accurate timings
                synchronized (mDebugSync) {
                    startTs = System.nanoTime();

                    output = doTheWork(hgtFileInfo, isHighQuality, padding, zoomLevel, pxPerLat, pxPerLon);

                    finishTs = System.nanoTime();
                }
            } else {
                startTs = System.nanoTime();

                output = doTheWork(hgtFileInfo, isHighQuality, padding, zoomLevel, pxPerLat, pxPerLon);

                finishTs = System.nanoTime();
            }

            final long delayNano = finishTs - startTs;
            final double delayMs = Math.round(delayNano / 1e5) / 10.;

            final String debugTag = this.getClass().getSimpleName() + "-R" + mReadingThreadsCount + "-C" + mComputingThreadsCount + "-E" + ElementsPerComputingTask + "-HQ" + (isHighQuality ? 1 : 0) + "-Z" + (zoomLevel < 10 ? "0" : "") + zoomLevel + " T: " + delayMs + " ms";

            System.out.println(debugTag);
        }

        return output;
    }

    /**
     * @param hgtFileInfo   HGT file info
     * @param isHighQuality When {@code true}, a unit element is 4x4 data points in size instead of 2x2, for better interpolation possibilities.
     * @param padding       Padding of the output, useful to minimize border interpolation artifacts (no need to be larger than 1)
     * @param zoomLevel     Zoom level (to determine shading quality requirements)
     * @param pxPerLat      Tile pixels per degree of latitude (to determine shading quality requirements)
     * @param pxPerLon      Tile pixels per degree of longitude (to determine shading quality requirements)
     * @return
     */
    protected byte[] doTheWork(final HgtFileInfo hgtFileInfo, boolean isHighQuality, int padding, int zoomLevel, double pxPerLat, double pxPerLon) {

        byte[] output = null;

        final int outputAxisLen = getOutputAxisLen(hgtFileInfo, zoomLevel, pxPerLat, pxPerLon);
        final int inputAxisLen = getInputAxisLen(hgtFileInfo);
        final int resolutionFactor = Math.max(1, outputAxisLen / inputAxisLen);
        final int strideFactor = getStrideFactor(inputAxisLen, outputAxisLen);
        final int inputAxisLenScaled = inputAxisLen / strideFactor;
        final int outputWidth = getOutputWidth(hgtFileInfo, padding, zoomLevel, pxPerLat, pxPerLon);
        final int inputWidth = inputAxisLen + 1;
        final int inputWidthScaled = inputAxisLenScaled + 1;
        final double northUnitDistancePerLine = getLatUnitDistance(hgtFileInfo.northLat(), inputAxisLenScaled) / inputAxisLenScaled;
        final double southUnitDistancePerLine = getLatUnitDistance(hgtFileInfo.southLat(), inputAxisLenScaled) / inputAxisLenScaled;
        final int outputIxInit = outputWidth * padding + padding;
        // Must add two additional paddings (after possibly skipping a line) to get to a starting position of the next line
        final int outputIxIncrement = (resolutionFactor - 1) * outputWidth + 2 * padding;

        if (isZoomLevelSupported(zoomLevel, hgtFileInfo) && isNotStopped()) {

            createThreadPoolsMaybe();

            output = new byte[outputWidth * outputWidth];

            final Semaphore activeTasksCount = new Semaphore(mActiveTasksCountMax);
            final ShortArraysPool inputArraysPool = new ShortArraysPool((1 + mActiveTasksCountMax) * mReadingThreadsCount);

            final ComputingParams computingParams = new ComputingParams.Builder()
                    .setOutput(output)
                    .setInputAxisLen(inputAxisLenScaled)
                    .setOutputAxisLen(outputAxisLen)
                    .setOutputWidth(outputWidth)
                    .setInputWidth(inputWidth)
                    .setInputWidthScaled(inputWidthScaled)
                    .setPadding(padding)
                    .setResolutionFactor(resolutionFactor)
                    .setStrideFactor(strideFactor)
                    .setOutputIxInit(outputIxInit)
                    .setOutputIxIncrement(outputIxIncrement)
                    .setNorthUnitDistancePerLine(northUnitDistancePerLine)
                    .setSouthUnitDistancePerLine(southUnitDistancePerLine)
                    .setIsHighQuality(isHighQuality)
                    .setActiveTasksCount(activeTasksCount)
                    .setInputArraysPool(inputArraysPool)
                    .build();

            final int readingTasksCount;
            {
                if (hgtFileInfo.getFile() instanceof DemFileZipEntryFS) {
                    // 2024: Turns out that it's faster to read ZIP files "not-too-concurrently". (...right?)
                    if (isHighQuality) {
                        readingTasksCount = Math.min(2, mReadingThreadsCount);
                    } else {
                        readingTasksCount = 1;
                    }
                } else {
                    if (isHighQuality) {
                        readingTasksCount = mReadingThreadsCount;
                    } else {
                        readingTasksCount = Math.min(2, mReadingThreadsCount);
                    }
                }
            }

            final int computingTasksCount, linesPerComputeTask;
            {
                // Note, integer arithmetic and truncations are deliberate here.
                // We also want to make sure that the last task processes no less than "linesPerComputeTask" lines, and no more than 2x that.
                final int computingTasksCountCoarse = Math.max(readingTasksCount, determineComputingTasksCount(inputAxisLen / strideFactor));
                // Make sure that "linesPerComputeTask" is divisible by the strideFactor
                linesPerComputeTask = inputAxisLen / computingTasksCountCoarse / strideFactor * strideFactor;
                computingTasksCount = inputAxisLen / linesPerComputeTask;
            }

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
                    if (strideFactor >= StrideFactorRawStreamLimit) {
                        // We're going to be skipping a lot, so we want to avoid wasteful buffering
                        readStream = hgtFileInfo
                                .getFile()
                                .asRawStream();
                    } else {
                        readStream = hgtFileInfo
                                .getFile()
                                .asStream();
                    }
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, e.toString(), e);
                }

                if (readingTaskIndex > 0) {
                    final long skipAmount = inputWidth * ((long) linesPerComputeTask * computingTaskFrom - (isHighQuality ? 1 : 0));

                    try {
                        HillShadingUtils.skipNBytes(readStream, skipAmount * Short.SIZE / Byte.SIZE);
                    } catch (IOException e) {
                        LOGGER.log(Level.SEVERE, e.toString(), e);
                    }
                }

                final SilentFutureTask readingTask = getReadingTask(readStream, computingTasksCount, computingTaskFrom, computingTaskTo, linesPerComputeTask / strideFactor, computingParams);
                readingTasks[readingTaskIndex] = readingTask;

                postToThreadPoolOrRun(readingTask, mReadThreadPool);
            }

            for (final SilentFutureTask readingTask : readingTasks) {
                readingTask.get();
            }
        }

        return output;
    }

    /**
     * If there are already too many active computing tasks, this will cause the caller to wait
     * until at least one computing task completes, to conserve memory.
     */
    protected void paceReading(final Semaphore activeTasksCount, final long readingTaskId, final int compTaskIndex, final int compTasksCount, final int activeCompTasks) {
        if (mComputingThreadsCount > 0) {
            if (false == activeTasksCount.tryAcquire()) {

                notifyReadingPaced(readingTaskId, compTaskIndex, compTasksCount, activeCompTasks);

                try {
                    activeTasksCount.acquire();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * Called when there are already too many active computing tasks (being processed or waiting in queue), so the reading must be slowed down to conserve memory.
     * The thread will go to sleep waiting on a semaphore after returning from this.
     *
     * @param readingTaskId   ID of a reading task calling this.
     * @param compTaskIndex   Serial number of a computing task that was about to be prepared. Ranges from 0 to {@code compTasksCount}-1.
     * @param compTasksCount  Total number of computing tasks being prepared by this reading task.
     * @param activeCompTasks How many computing tasks are waiting for completion at the time this is called.
     */
    protected void notifyReadingPaced(final long readingTaskId, final int compTaskIndex, final int compTasksCount, final int activeCompTasks) {
    }

    /**
     * Decides a total number of computing tasks that will be used for the given input parameters.
     */
    protected int determineComputingTasksCount(final int inputAxisLen) {
        return (int) Math.max(1L, Math.min(inputAxisLen / 8, (long) inputAxisLen * inputAxisLen / ElementsPerComputingTask));
    }

    /**
     * The {@code Runnable} provided will be sent to the thread pool, or run on the calling thread if the thread pool rejects it (an unlikely event).
     *
     * @param code A code to run.
     */
    protected void postToThreadPoolOrRun(final Runnable code, final AtomicReference<HillShadingThreadPool> threadPoolReference) {
        final HillShadingThreadPool threadPool = threadPoolReference.get();

        if (threadPool != null) {
            threadPool.executeOrRun(code);
        }
    }

    protected void createThreadPoolsMaybe() {
        if (mReadingThreadsCount > 0) {
            final AtomicReference<HillShadingThreadPool> threadPoolReference = mReadThreadPool;

            if (threadPoolReference.get() == null) {
                synchronized (threadPoolReference) {
                    if (threadPoolReference.get() == null) {
                        threadPoolReference.set(createReadingThreadPool());
                    }
                }
            }
        }

        if (mComputingThreadsCount > 0) {
            final AtomicReference<HillShadingThreadPool> threadPoolReference = mCompThreadPool;

            if (threadPoolReference.get() == null) {
                synchronized (threadPoolReference) {
                    if (threadPoolReference.get() == null) {
                        threadPoolReference.set(createComputingThreadPool());
                    }
                }
            }
        }
    }

    protected HillShadingThreadPool createReadingThreadPool() {
        final int threadCount = mReadingThreadsCount;
        final int queueSize = Integer.MAX_VALUE;
        return new HillShadingThreadPool(threadCount, threadCount, queueSize, 10, ReadingThreadPoolName).start();
    }

    protected HillShadingThreadPool createComputingThreadPool() {
        final int threadCount = mComputingThreadsCount;
        final int queueSize = Integer.MAX_VALUE;
        return new HillShadingThreadPool(threadCount, threadCount, queueSize, 10, ComputingThreadPoolName).start();
    }

    protected void destroyReadingThreadPool() {
        final AtomicReference<HillShadingThreadPool> threadPoolReference = mReadThreadPool;

        synchronized (threadPoolReference) {
            final HillShadingThreadPool threadPool = threadPoolReference.getAndSet(null);

            if (threadPool != null) {
                threadPool.shutdownNow();
            }
        }
    }

    protected void destroyComputingThreadPool() {
        final AtomicReference<HillShadingThreadPool> threadPoolReference = mCompThreadPool;

        synchronized (threadPoolReference) {
            final HillShadingThreadPool threadPool = threadPoolReference.getAndSet(null);

            if (threadPool != null) {
                threadPool.shutdownNow();
            }
        }
    }

    public void interruptAndDestroy() {
        destroyReadingThreadPool();
        destroyComputingThreadPool();
    }

    /**
     * @return {@code false} to stop processing. Default implementation checks the thread interrupt state if a call to {@link #isStopped()} returns {@code false}.
     */
    protected boolean isNotStopped() {
        return !isStopped() && !Thread.currentThread().isInterrupted();
    }

    /**
     * Send a "stop" signal: Any active task will finish as soon as possible (possibly without completing),
     * and no new work will be done until a "continue" signal arrives.
     */
    public void stopSignal() {
        mStopSignal = true;
    }

    /**
     * Send a "continue" signal: Allow new work to be done.
     */
    public void continueSignal() {
        mStopSignal = false;
    }

    /**
     * Check if the "stop" signal is active.
     */
    public boolean isStopped() {
        return mStopSignal;
    }

    protected final Object mDebugSync = new Object();

    /**
     * @return {@code true} to measure and output rendering times per file.
     * @see #isDebugTimingSequential()
     */
    protected boolean isDebugTiming() {
        return false;
    }

    /**
     * @return {@code true} to process one file at a time for more accurate timings. Note: Rendering will be slower.
     * @see #isDebugTiming()
     */
    protected boolean isDebugTimingSequential() {
        return true;
    }

    protected SilentFutureTask getReadingTask(InputStream readStream, int computingTasksCount, int computingTaskFrom, int computingTaskTo, int linesPerComputeTask, ComputingParams computingParams) {
        if (computingParams.mIsHighQuality) {
            return new SilentFutureTask(new ReadingTask_4x4(readStream, computingTasksCount, computingTaskFrom, computingTaskTo, linesPerComputeTask, computingParams));
        } else {
            return new SilentFutureTask(new ReadingTask_2x2(readStream, computingTasksCount, computingTaskFrom, computingTaskTo, linesPerComputeTask, computingParams));
        }
    }

    protected SilentFutureTask getComputingTask(int lineFrom, int lineTo, short[] input, Semaphore activeTasksCount, ComputingParams computingParams) {
        if (computingParams.mIsHighQuality) {
            return new SilentFutureTask(new ComputingTask_4x4(lineFrom, lineTo, input, activeTasksCount, computingParams));
        } else {
            return new SilentFutureTask(new ComputingTask_2x2(lineFrom, lineTo, input, activeTasksCount, computingParams));
        }
    }

    /**
     * A "high quality" reading task which prepares (part of) the input for processing.
     */
    protected class ReadingTask_4x4 implements Callable<Boolean> {
        protected final InputStream mInputStream;
        protected final int mComputingTasksCount, mComputingTaskFrom, mComputingTaskTo, mLinesPerCompTask;
        protected final ComputingParams mComputingParams;
        protected final long mTaskId;

        public ReadingTask_4x4(InputStream inputStream, int computingTasksCount, int taskFrom, int taskTo, int linesPerTask, ComputingParams computingParams) {
            mInputStream = inputStream;
            mComputingTasksCount = computingTasksCount;
            mComputingTaskFrom = taskFrom;
            mComputingTaskTo = taskTo;
            mLinesPerCompTask = linesPerTask;
            mComputingParams = computingParams;
            mTaskId = mReadTaskCounter.getAndIncrement();
        }

        @Override
        public Boolean call() {
            boolean retVal = false;

            try {
                if (mInputStream != null) {
                    final SilentFutureTask[] computingTasks = new SilentFutureTask[mComputingTaskTo - mComputingTaskFrom];

                    final int inputAxisLen = mComputingParams.mInputAxisLen;
                    final int inputLineLen = mComputingParams.mInputWidthScaled;
                    final Semaphore activeTasksCount = mComputingParams.mActiveTasksCount;
                    final ShortArraysPool inputArraysPool = mComputingParams.mInputArraysPool;

                    short[] input = null, inputNext = null;

                    for (int compTaskIndex = mComputingTaskFrom; compTaskIndex < mComputingTaskTo; compTaskIndex++) {

                        paceReading(activeTasksCount, mTaskId, compTaskIndex, mComputingTasksCount, mActiveTasksCountMax);

                        final int lineFrom, lineTo;
                        final int inputSize, inputNextSize;
                        {
                            lineFrom = mLinesPerCompTask * compTaskIndex;

                            if (compTaskIndex < mComputingTasksCount - 1) {
                                lineTo = lineFrom + mLinesPerCompTask;
                            } else {
                                lineTo = inputAxisLen;
                            }

                            inputSize = 3 + lineTo - lineFrom;

                            if (compTaskIndex < mComputingTasksCount - 2) {
                                inputNextSize = inputSize;
                            } else if (compTaskIndex == mComputingTasksCount - 2) {
                                inputNextSize = 3 + inputAxisLen - mLinesPerCompTask * (mComputingTasksCount - 1);
                            } else {
                                inputNextSize = 3;
                            }
                        }

                        if (compTaskIndex > mComputingTaskFrom) {
                            input = inputNext;
                        } else {
                            input = inputArraysPool.getArray(inputLineLen * inputSize);

                            // First three lines are done separately
                            int inputIx = 0;
                            for (int line = 0; line < 3 && isNotStopped(); line++) {
                                for (int col = 0; col < inputLineLen; col++, inputIx++) {
                                    input[inputIx] = readNext(mInputStream);
                                }
                            }
                        }

                        inputNext = inputArraysPool.getArray(inputLineLen * inputNextSize);

                        final int mainLoopFrom = lineFrom + (compTaskIndex <= 0 ? 1 : 0);
                        final int mainLoopTo = lineTo - 3 - (compTaskIndex < mComputingTasksCount - 1 ? 0 : 1);

                        // Skip three lines already in the array
                        int inputIx = 3 * inputLineLen;

                        for (int line = mainLoopFrom; line < mainLoopTo && isNotStopped(); line++) {
                            // Inner loop, critical for performance
                            for (int col = 0; col < inputLineLen; col++, inputIx++) {
                                input[inputIx] = readNext(mInputStream);
                            }
                        }

                        int inputNextIx = 0;

                        // Last three lines are done separately
                        for (int line = 0; line < 3 && isNotStopped(); line++) {
                            for (int col = 0; col < inputLineLen; col++, inputIx++, inputNextIx++) {
                                final short point = readNext(mInputStream);
                                input[inputIx] = point;
                                inputNext[inputNextIx] = point;
                            }
                        }

                        final SilentFutureTask computingTask = getComputingTask(lineFrom, lineTo, input, activeTasksCount, mComputingParams);
                        computingTasks[compTaskIndex - mComputingTaskFrom] = computingTask;

                        if (compTaskIndex < mComputingTaskTo - 1 && mComputingThreadsCount > 0) {
                            postToThreadPoolOrRun(computingTask, mCompThreadPool);
                        } else {
                            computingTask.run();
                        }
                    }

                    IOUtils.closeQuietly(mInputStream);

                    for (final SilentFutureTask computingTask : computingTasks) {
                        computingTask.get();
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
        protected final long mTaskId;

        public ReadingTask_2x2(InputStream inputStream, int computingTasksCount, int taskFrom, int taskTo, int linesPerTask, ComputingParams computingParams) {
            mInputStream = inputStream;
            mComputingTasksCount = computingTasksCount;
            mComputingTaskFrom = taskFrom;
            mComputingTaskTo = taskTo;
            mLinesPerCompTask = linesPerTask;
            mComputingParams = computingParams;
            mTaskId = mReadTaskCounter.getAndIncrement();
        }

        @Override
        public Boolean call() {
            boolean retVal = false;

            try {
                if (mInputStream != null) {
                    final SilentFutureTask[] computingTasks = new SilentFutureTask[mComputingTaskTo - mComputingTaskFrom];

                    final int inputAxisLen = mComputingParams.mInputAxisLen;
                    final int inputLineLen = mComputingParams.mInputWidthScaled;
                    final int inputWidth = mComputingParams.mInputWidth;
                    final int strideFactor = mComputingParams.mStrideFactor;
                    final Semaphore activeTasksCount = mComputingParams.mActiveTasksCount;
                    final ShortArraysPool inputArraysPool = mComputingParams.mInputArraysPool;

                    short[] input = null, inputNext = null;

                    for (int compTaskIndex = mComputingTaskFrom; compTaskIndex < mComputingTaskTo; compTaskIndex++) {

                        paceReading(activeTasksCount, mTaskId, compTaskIndex, mComputingTasksCount, mActiveTasksCountMax);

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
                            input = inputNext;
                        } else {
                            input = inputArraysPool.getArray(inputLineLen * inputSize);

                            // First line is done separately
                            for (int col = 0; col < inputLineLen; col++) {
                                input[col] = readNext(mInputStream);
                                if (col < inputLineLen - 1) {
                                    // Skip stride-1 columns
                                    HillShadingUtils.skipNBytes(mInputStream, (strideFactor - 1) * Short.SIZE / Byte.SIZE);
                                }
                            }

                            // Skip stride-1 lines
                            HillShadingUtils.skipNBytes(mInputStream, (strideFactor - 1) * inputWidth * Short.SIZE / Byte.SIZE);
                        }

                        inputNext = inputArraysPool.getArray(inputLineLen * inputNextSize);

                        final int mainLoopFrom = lineFrom;
                        final int mainLoopTo = lineTo - 1;

                        // Skip the line already in the array
                        int inputIx = inputLineLen;

                        if (strideFactor <= 1) {
                            for (int line = mainLoopFrom; line < mainLoopTo && isNotStopped(); line++) {
                                // Inner loop, critical for performance
                                for (int col = 0; col < inputLineLen; col++, inputIx++) {
                                    input[inputIx] = readNext(mInputStream);
                                }
                            }
                        } else {
                            for (int line = mainLoopFrom; line < mainLoopTo && isNotStopped(); line++) {
                                // Inner loop, critical for performance
                                for (int col = 0; col < inputLineLen - 1; col++, inputIx++) {
                                    input[inputIx] = readNext(mInputStream);
                                    HillShadingUtils.skipNBytes(mInputStream, (strideFactor - 1) * Short.SIZE / Byte.SIZE);
                                }
                                input[inputIx] = readNext(mInputStream);
                                inputIx++;

                                // Skip stride-1 lines
                                HillShadingUtils.skipNBytes(mInputStream, (strideFactor - 1) * inputWidth * Short.SIZE / Byte.SIZE);
                            }
                        }

                        int inputNextIx = 0;

                        // Last line is done separately
                        for (int col = 0; col < inputLineLen; col++, inputIx++, inputNextIx++) {
                            final short point = readNext(mInputStream);
                            input[inputIx] = point;
                            inputNext[inputNextIx] = point;
                            if (col < inputLineLen - 1) {
                                // Skip stride-1 columns
                                HillShadingUtils.skipNBytes(mInputStream, (strideFactor - 1) * Short.SIZE / Byte.SIZE);
                            }
                        }

                        if (compTaskIndex < mComputingTaskTo - 1) {
                            HillShadingUtils.skipNBytes(mInputStream, (strideFactor - 1) * inputWidth * Short.SIZE / Byte.SIZE);
                        }

                        final SilentFutureTask computingTask = getComputingTask(lineFrom, lineTo, input, activeTasksCount, mComputingParams);
                        computingTasks[compTaskIndex - mComputingTaskFrom] = computingTask;

                        if (compTaskIndex < mComputingTaskTo - 1 && mComputingThreadsCount > 0) {
                            postToThreadPoolOrRun(computingTask, mCompThreadPool);
                        } else {
                            computingTask.run();
                        }
                    }

                    IOUtils.closeQuietly(mInputStream);

                    for (final SilentFutureTask computingTask : computingTasks) {
                        computingTask.get();
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
     * {@link #processRow_4x4(short[], int, int, int, int, double, int, ComputingParams)}
     * on all lines of input unit elements of size 4x4 from the given part (except the edges of the input data),
     * and {@link #processUnitElement_4x4(double, double, double, double, double, double, double, double, double, double, double, double, double, double, double, double, double, int, ComputingParams)}
     * on all input unit elements of size 4x4 that are on the edges of the input data.
     */
    protected class ComputingTask_4x4 implements Callable<Boolean> {
        protected final int mLineFrom, mLineTo;
        protected final short[] mInput;
        protected final Semaphore mActiveTasksCount;
        protected final ComputingParams mComputingParams;

        public ComputingTask_4x4(int lineFrom, int lineTo, short[] input, Semaphore activeTasksCount, ComputingParams computingParams) {
            mLineFrom = lineFrom;
            mLineTo = lineTo;
            mInput = input;
            mActiveTasksCount = activeTasksCount;
            mComputingParams = computingParams;
        }

        @Override
        public Boolean call() {
            // TODO (2024-10): Uses linear interpolation on the edges of a DEM file data, where there are too few points to use bicubic.
            //  It should be considered whether this can be improved by obtaining edge lines from neighboring DEM files, so we have a bicubic interpolation
            //  everywhere except at the outer edges of the entire DEM data set. (Probably not worth it...)

            boolean retVal = false;

            try {
                if (mIsPreprocess) {
                    preprocess(mInput, mComputingParams.mInputWidthScaled);
                }

                final int resolutionFactor = mComputingParams.mResolutionFactor;
                final int outputIxIncrement = mComputingParams.mOutputIxIncrement;
                final int secondLineOffset = mComputingParams.mInputWidthScaled;
                final int thirdLineOffset = secondLineOffset + mComputingParams.mInputWidthScaled;
                final int fourthLineOffset = thirdLineOffset + mComputingParams.mInputWidthScaled;

                int outputIx = mComputingParams.mOutputIxInit;
                outputIx += resolutionFactor * mLineFrom * mComputingParams.mOutputWidth;

                int line = mLineFrom;

                if (mLineFrom <= 0) {
                    // The very first line of the input data is done separately

                    int secondLineIx = 0;

                    final double distanceScaleFactor = computeDistanceScaleFactor(line, mComputingParams);

                    outputIx = processUnitElementFirstRowWest(mInput, secondLineIx, secondLineOffset, thirdLineOffset, distanceScaleFactor, outputIx, mComputingParams);
                    secondLineIx++;

                    for (int col = 0; col < mComputingParams.mInputAxisLen - 2; col++) {
                        outputIx = processUnitElementFirstRow(mInput, secondLineIx, secondLineOffset, thirdLineOffset, distanceScaleFactor, outputIx, mComputingParams);
                        secondLineIx++;
                    }

                    outputIx = processUnitElementFirstRowEast(mInput, secondLineIx, secondLineOffset, thirdLineOffset, distanceScaleFactor, outputIx, mComputingParams);

                    outputIx += outputIxIncrement;
                    line++;
                }

                // Bulk of the input data part is processed here
                {
                    int firstLineIx = -1;

                    // Outer loop
                    for (; line < Math.min(mLineTo, mComputingParams.mInputAxisLen - 1) && isNotStopped(); line++) {
                        final double distanceScaleFactor = computeDistanceScaleFactor(line, mComputingParams);

                        firstLineIx++;

                        outputIx = processUnitElementWest(mInput, firstLineIx, secondLineOffset, thirdLineOffset, fourthLineOffset, distanceScaleFactor, outputIx, mComputingParams);
                        firstLineIx++;

                        // Inner loop, critical for performance
                        outputIx = processRow_4x4(mInput, firstLineIx, secondLineOffset, thirdLineOffset, fourthLineOffset, distanceScaleFactor, outputIx, mComputingParams);

                        firstLineIx += mComputingParams.mInputAxisLen - 2;

                        outputIx = processUnitElementEast(mInput, firstLineIx, secondLineOffset, thirdLineOffset, fourthLineOffset, distanceScaleFactor, outputIx, mComputingParams);
                        firstLineIx++;

                        outputIx += outputIxIncrement;
                    }

                    if (mLineTo >= mComputingParams.mInputAxisLen) {
                        // The very last line of the input data is done separately

                        firstLineIx++;

                        final double distanceScaleFactor = computeDistanceScaleFactor(line, mComputingParams);

                        outputIx = processUnitElementLastRowWest(mInput, firstLineIx, secondLineOffset, thirdLineOffset, distanceScaleFactor, outputIx, mComputingParams);
                        firstLineIx++;

                        for (int col = 0; col < mComputingParams.mInputAxisLen - 2; col++) {
                            outputIx = processUnitElementLastRow(mInput, firstLineIx, secondLineOffset, thirdLineOffset, distanceScaleFactor, outputIx, mComputingParams);
                            firstLineIx++;
                        }

                        outputIx = processUnitElementLastRowEast(mInput, firstLineIx, secondLineOffset, thirdLineOffset, distanceScaleFactor, outputIx, mComputingParams);
                    }
                }

                retVal = true;

            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.toString());
            } finally {
                mComputingParams.mInputArraysPool.recycleArray(mInput);
                mActiveTasksCount.release();
            }

            return retVal;
        }

        protected int processUnitElementWest(short[] input, int firstLineIx, int secondLineOffset, int thirdLineOffset, int fourthLineOffset, double dsf, int outputIx, ComputingParams computingParams) {
            final int secondLineIx = firstLineIx + secondLineOffset;
            final int thirdLineIx = firstLineIx + thirdLineOffset;
            final int fourthLineIx = firstLineIx + fourthLineOffset;

            final short nw = input[secondLineIx];
            final short sw = input[thirdLineIx];
            final short se = input[thirdLineIx + 1];
            final short ne = input[secondLineIx + 1];

            final short ssw = input[fourthLineIx];
            final short sse = input[fourthLineIx + 1];
            final short sese = input[fourthLineIx + 2];
            final short ese = input[thirdLineIx + 2];

            final short ene = input[secondLineIx + 2];
            final short nene = input[firstLineIx + 2];
            final short nne = input[firstLineIx + 1];
            final short nnw = input[firstLineIx];

            // Linear interpolation
            final int nwnw = 2 * nw - se;
            final int wnw = 2 * nw - ne;
            final int wsw = 2 * sw - se;
            final int swsw = 2 * sw - ne;

            return processUnitElement_4x4(nw, sw, se, ne, nwnw, wnw, wsw, swsw, ssw, sse, sese, ese, ene, nene, nne, nnw, dsf, outputIx, computingParams);
        }

        protected int processUnitElementEast(short[] input, int firstLineIx, int secondLineOffset, int thirdLineOffset, int fourthLineOffset, double dsf, int outputIx, ComputingParams computingParams) {
            final int secondLineIx = firstLineIx + secondLineOffset;
            final int thirdLineIx = firstLineIx + thirdLineOffset;
            final int fourthLineIx = firstLineIx + fourthLineOffset;

            final short nw = input[secondLineIx];
            final short sw = input[thirdLineIx];
            final short se = input[thirdLineIx + 1];
            final short ne = input[secondLineIx + 1];

            final short nwnw = input[firstLineIx - 1];
            final short wnw = input[secondLineIx - 1];
            final short wsw = input[thirdLineIx - 1];
            final short swsw = input[fourthLineIx - 1];

            final short ssw = input[fourthLineIx];
            final short sse = input[fourthLineIx + 1];
            final short nne = input[firstLineIx + 1];
            final short nnw = input[firstLineIx];

            // Linear interpolation
            final int sese = 2 * se - nw;
            final int ese = 2 * se - sw;
            final int ene = 2 * ne - nw;
            final int nene = 2 * ne - sw;

            return processUnitElement_4x4(nw, sw, se, ne, nwnw, wnw, wsw, swsw, ssw, sse, sese, ese, ene, nene, nne, nnw, dsf, outputIx, computingParams);
        }

        protected int processUnitElementFirstRowWest(short[] input, int secondLineIx, int thirdLineOffset, int fourthLineOffset, double dsf, int outputIx, ComputingParams computingParams) {
            final int thirdLineIx = secondLineIx + thirdLineOffset;
            final int fourthLineIx = secondLineIx + fourthLineOffset;

            final short nw = input[secondLineIx];
            final short sw = input[thirdLineIx];
            final short se = input[thirdLineIx + 1];
            final short ne = input[secondLineIx + 1];

            final short ssw = input[fourthLineIx];
            final short sse = input[fourthLineIx + 1];
            final short sese = input[fourthLineIx + 2];
            final short ese = input[thirdLineIx + 2];

            final short ene = input[secondLineIx + 2];

            // Linear interpolation
            final int nene = 2 * ne - sw;
            final int nne = 2 * ne - se;
            final int nnw = 2 * nw - sw;

            final int nwnw = 2 * nw - se;
            final int wnw = 2 * nw - ne;
            final int wsw = 2 * sw - se;
            final int swsw = 2 * sw - ne;

            return processUnitElement_4x4(nw, sw, se, ne, nwnw, wnw, wsw, swsw, ssw, sse, sese, ese, ene, nene, nne, nnw, dsf, outputIx, computingParams);
        }

        protected int processUnitElementFirstRow(short[] input, int secondLineIx, int thirdLineOffset, int fourthLineOffset, double dsf, int outputIx, ComputingParams computingParams) {
            final int thirdLineIx = secondLineIx + thirdLineOffset;
            final int fourthLineIx = secondLineIx + fourthLineOffset;

            final short nw = input[secondLineIx];
            final short sw = input[thirdLineIx];
            final short se = input[thirdLineIx + 1];
            final short ne = input[secondLineIx + 1];

            final short wnw = input[secondLineIx - 1];
            final short wsw = input[thirdLineIx - 1];
            final short swsw = input[fourthLineIx - 1];

            final short ssw = input[fourthLineIx];
            final short sse = input[fourthLineIx + 1];
            final short sese = input[fourthLineIx + 2];
            final short ese = input[thirdLineIx + 2];

            final short ene = input[secondLineIx + 2];

            // Linear interpolation
            final int nene = 2 * ne - sw;
            final int nne = 2 * ne - se;
            final int nnw = 2 * nw - sw;
            final int nwnw = 2 * nw - se;

            return processUnitElement_4x4(nw, sw, se, ne, nwnw, wnw, wsw, swsw, ssw, sse, sese, ese, ene, nene, nne, nnw, dsf, outputIx, computingParams);
        }

        protected int processUnitElementFirstRowEast(short[] input, int secondLineIx, int thirdLineOffset, int fourthLineOffset, double dsf, int outputIx, ComputingParams computingParams) {
            final int thirdLineIx = secondLineIx + thirdLineOffset;
            final int fourthLineIx = secondLineIx + fourthLineOffset;

            final short nw = input[secondLineIx];
            final short sw = input[thirdLineIx];
            final short se = input[thirdLineIx + 1];
            final short ne = input[secondLineIx + 1];

            final short wnw = input[secondLineIx - 1];
            final short wsw = input[thirdLineIx - 1];
            final short swsw = input[fourthLineIx - 1];

            final short ssw = input[fourthLineIx];
            final short sse = input[fourthLineIx + 1];

            // Linear interpolation
            final int sese = 2 * se - nw;
            final int ese = 2 * se - sw;
            final int ene = 2 * ne - nw;

            final int nene = 2 * ne - sw;
            final int nne = 2 * ne - se;
            final int nnw = 2 * nw - sw;
            final int nwnw = 2 * nw - se;

            return processUnitElement_4x4(nw, sw, se, ne, nwnw, wnw, wsw, swsw, ssw, sse, sese, ese, ene, nene, nne, nnw, dsf, outputIx, computingParams);
        }

        protected int processUnitElementLastRowWest(short[] input, int firstLineIx, int secondLineOffset, int thirdLineOffset, double dsf, int outputIx, ComputingParams computingParams) {
            final int secondLineIx = firstLineIx + secondLineOffset;
            final int thirdLineIx = firstLineIx + thirdLineOffset;

            final short nw = input[secondLineIx];
            final short sw = input[thirdLineIx];
            final short se = input[thirdLineIx + 1];
            final short ne = input[secondLineIx + 1];

            final short ese = input[thirdLineIx + 2];

            final short ene = input[secondLineIx + 2];
            final short nene = input[firstLineIx + 2];
            final short nne = input[firstLineIx + 1];
            final short nnw = input[firstLineIx];

            // Linear interpolation
            final int swsw = 2 * sw - ne;
            final int ssw = 2 * sw - nw;
            final int sse = 2 * se - ne;
            final int sese = 2 * se - nw;

            final int nwnw = 2 * nw - se;
            final int wnw = 2 * nw - ne;
            final int wsw = 2 * sw - se;

            return processUnitElement_4x4(nw, sw, se, ne, nwnw, wnw, wsw, swsw, ssw, sse, sese, ese, ene, nene, nne, nnw, dsf, outputIx, computingParams);
        }

        protected int processUnitElementLastRow(short[] input, int firstLineIx, int secondLineOffset, int thirdLineOffset, double dsf, int outputIx, ComputingParams computingParams) {
            final int secondLineIx = firstLineIx + secondLineOffset;
            final int thirdLineIx = firstLineIx + thirdLineOffset;

            final short nw = input[secondLineIx];
            final short sw = input[thirdLineIx];
            final short se = input[thirdLineIx + 1];
            final short ne = input[secondLineIx + 1];

            final short nwnw = input[firstLineIx - 1];
            final short wnw = input[secondLineIx - 1];
            final short wsw = input[thirdLineIx - 1];
            final short ese = input[thirdLineIx + 2];

            final short ene = input[secondLineIx + 2];
            final short nene = input[firstLineIx + 2];
            final short nne = input[firstLineIx + 1];
            final short nnw = input[firstLineIx];

            // Linear interpolation
            final int swsw = 2 * sw - ne;
            final int ssw = 2 * sw - nw;
            final int sse = 2 * se - ne;
            final int sese = 2 * se - nw;

            return processUnitElement_4x4(nw, sw, se, ne, nwnw, wnw, wsw, swsw, ssw, sse, sese, ese, ene, nene, nne, nnw, dsf, outputIx, computingParams);
        }

        protected int processUnitElementLastRowEast(short[] input, int firstLineIx, int secondLineOffset, int thirdLineOffset, double dsf, int outputIx, ComputingParams computingParams) {
            final int secondLineIx = firstLineIx + secondLineOffset;
            final int thirdLineIx = firstLineIx + thirdLineOffset;

            final short nw = input[secondLineIx];
            final short sw = input[thirdLineIx];
            final short se = input[thirdLineIx + 1];
            final short ne = input[secondLineIx + 1];

            final short nwnw = input[firstLineIx - 1];
            final short wnw = input[secondLineIx - 1];
            final short wsw = input[thirdLineIx - 1];

            final short nne = input[firstLineIx + 1];
            final short nnw = input[firstLineIx];

            // Linear interpolation
            final int swsw = 2 * sw - ne;
            final int ssw = 2 * sw - nw;
            final int sse = 2 * se - ne;
            final int sese = 2 * se - nw;

            final int ese = 2 * se - sw;
            final int ene = 2 * ne - nw;
            final int nene = 2 * ne - sw;

            return processUnitElement_4x4(nw, sw, se, ne, nwnw, wnw, wsw, swsw, ssw, sse, sese, ese, ene, nene, nne, nnw, dsf, outputIx, computingParams);
        }
    }

    /**
     * A "standard quality" computing task which converts part of the input to part of the output, by calling
     * {@link #processRow_2x2(short[], int, int, double, int, ComputingParams)}
     * on all lines of input unit elements of size 2x2 from the given part.
     */
    protected class ComputingTask_2x2 implements Callable<Boolean> {
        protected final int mLineFrom, mLineTo;
        protected final short[] mInput;
        protected final Semaphore mActiveTasksCount;
        protected final ComputingParams mComputingParams;

        public ComputingTask_2x2(int lineFrom, int lineTo, short[] input, Semaphore activeTasksCount, ComputingParams computingParams) {
            mLineFrom = lineFrom;
            mLineTo = lineTo;
            mInput = input;
            mActiveTasksCount = activeTasksCount;
            mComputingParams = computingParams;
        }

        @Override
        public Boolean call() {
            boolean retVal = false;

            try {
                if (mIsPreprocess) {
                    preprocess(mInput, mComputingParams.mInputWidthScaled);
                }

                final int resolutionFactor = mComputingParams.mResolutionFactor;
                final int outputIxIncrement = mComputingParams.mOutputIxIncrement;
                final int secondLineOffset = mComputingParams.mInputWidthScaled;

                int outputIx = mComputingParams.mOutputIxInit;
                outputIx += resolutionFactor * mLineFrom * mComputingParams.mOutputWidth;

                int inputIx = 0;

                // Outer loop
                for (int line = mLineFrom; line < mLineTo && isNotStopped(); line++) {
                    final double distanceScaleFactor = computeDistanceScaleFactor(line, mComputingParams);

                    // Inner loop, critical for performance
                    outputIx = processRow_2x2(mInput, inputIx, secondLineOffset, distanceScaleFactor, outputIx, mComputingParams);

                    inputIx += mComputingParams.mInputWidthScaled;

                    outputIx += outputIxIncrement;
                }

                retVal = true;

            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.toString());
            } finally {
                mComputingParams.mInputArraysPool.recycleArray(mInput);
                mActiveTasksCount.release();
            }

            return retVal;
        }
    }

    /**
     * Parameters that are used by a {@link AThreadedHillShading}.
     * An instance should be created using the provided builder, {@link Builder}.
     */
    protected static class ComputingParams {
        public final byte[] mOutput;
        public final int mInputAxisLen;
        public final int mOutputAxisLen;
        public final int mInputWidth;
        public final int mInputWidthScaled;
        public final int mOutputWidth;
        public final int mPadding;
        public final int mResolutionFactor;
        public final int mStrideFactor;
        public final int mOutputIxInit;
        public final int mOutputIxIncrement;
        public final double mNorthUnitDistancePerLine;
        public final double mSouthUnitDistancePerLine;
        public final boolean mIsHighQuality;
        public final Semaphore mActiveTasksCount;
        public final ShortArraysPool mInputArraysPool;

        protected ComputingParams(final Builder builder) {
            mOutput = builder.mOutput;
            mInputAxisLen = builder.mInputAxisLen;
            mOutputAxisLen = builder.mOutputAxisLen;
            mInputWidth = builder.mInputWidth;
            mInputWidthScaled = builder.mInputWidthScaled;
            mOutputWidth = builder.mOutputWidth;
            mPadding = builder.mPadding;
            mResolutionFactor = builder.mResolutionFactor;
            mStrideFactor = builder.mStrideFactor;
            mOutputIxInit = builder.mOutputIxInit;
            mOutputIxIncrement = builder.mOutputIxIncrement;
            mNorthUnitDistancePerLine = builder.mNorthUnitDistancePerLine;
            mSouthUnitDistancePerLine = builder.mSouthUnitDistancePerLine;
            mIsHighQuality = builder.mIsHighQuality;
            mActiveTasksCount = builder.mActiveTasksCount;
            mInputArraysPool = builder.mInputArraysPool;
        }

        protected static class Builder {
            protected volatile byte[] mOutput;
            protected volatile int mInputAxisLen;
            protected volatile int mOutputAxisLen;
            protected volatile int mInputWidth;
            protected volatile int mInputWidthScaled;
            protected volatile int mOutputWidth;
            protected volatile int mPadding;
            protected volatile int mResolutionFactor;
            protected volatile int mStrideFactor;
            protected volatile int mOutputIxInit;
            protected volatile int mOutputIxIncrement;
            protected volatile double mNorthUnitDistancePerLine;
            protected volatile double mSouthUnitDistancePerLine;
            protected volatile boolean mIsHighQuality;
            protected volatile Semaphore mActiveTasksCount;
            protected volatile ShortArraysPool mInputArraysPool;

            protected Builder() {
            }

            /**
             * Create the {@link ComputingParams} instance using parameter values from this builder.
             * All parameters used in computations should be explicitly set.
             *
             * @return New {@link ComputingParams} instance built using parameter values from this {@link Builder}
             */
            protected ComputingParams build() {
                return new ComputingParams(this);
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

            public Builder setInputWidthScaled(int inputWidthScaled) {
                this.mInputWidthScaled = inputWidthScaled;
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

            public Builder setOutputIxInit(int outputIxInit) {
                this.mOutputIxInit = outputIxInit;
                return this;
            }

            public Builder setOutputIxIncrement(int outputIxIncrement) {
                this.mOutputIxIncrement = outputIxIncrement;
                return this;
            }

            public Builder setResolutionFactor(int resolutionFactor) {
                this.mResolutionFactor = resolutionFactor;
                return this;
            }

            public Builder setStrideFactor(int strideFactor) {
                this.mStrideFactor = strideFactor;
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

            public Builder setIsHighQuality(boolean isHighQuality) {
                this.mIsHighQuality = isHighQuality;
                return this;
            }

            public Builder setActiveTasksCount(Semaphore activeTasksCount) {
                this.mActiveTasksCount = activeTasksCount;
                return this;
            }

            public Builder setInputArraysPool(ShortArraysPool inputArraysPool) {
                this.mInputArraysPool = inputArraysPool;
                return this;
            }
        }
    }
}
