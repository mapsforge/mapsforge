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

import org.mapsforge.map.layer.hills.HgtCache.HgtFileInfo;
import org.mapsforge.map.layer.hills.HillShadingUtils.Awaiter;
import org.mapsforge.map.layer.hills.HillShadingUtils.HillShadingThreadPool;
import org.mapsforge.map.layer.hills.HillShadingUtils.ShortArraysPool;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>
 * Special abstract implementation of hill shading algorithm where input data can be divided into parts
 * that are processed in parallel by other threads.
 * </p>
 * <p>
 * The implementation is such that there is one "producer" thread (ie. the caller thread) that just reads from the input (and does the synchronization),
 * and a custom number (>=0) of other "consumer" threads that just do the computations (a "thread pool").
 * It should be emphasized that every producer/caller thread has its own thread pool: Thread pools are not shared.
 * </p>
 * <p>
 * Special attention is paid to reducing memory consumption.
 * The producer thread will throttle itself and stop reading until the computation catches up.
 * If this happens, a {@link #notifyReadingPaced(int)} will be called.
 * </p>
 * <p>
 * Rough estimate of the <em>maximum</em> memory used per caller is as follows:
 * <br />
 * <br />
 * max_bytes_used = {@link #ElementsPerComputingTask} * (1 + 2 * {@link #mComputingThreadsCount}) * {@link Short#BYTES}
 * <br />
 * <br />
 * By default, with one additional computing thread used, this is around 96000 bytes (cca 100 kB) max memory usage per caller.
 * If the computations are fast enough, the real memory usage is usually going to be several times smaller.
 * </p>
 */
public abstract class AThreadedHillShading extends AbsShadingAlgorithmDefaults {

    protected final Logger LOGGER = Logger.getLogger(this
            .getClass()
            .getName());

    /**
     * Default number of computing threads per caller thread. 1 means an additional thread (per caller thread) will do the computations,
     * while the caller thread will do the reading and synchronization.
     */
    public static final int ComputingThreadsCountDefault = 1;

    public final String ThreadPoolName = "MapsforgeHillShading";

    /**
     * Approximate number of unit elements that each computing task will process.
     * The actual number is calculated during execution and can be slightly different.
     */
    protected final int ElementsPerComputingTask = 16000;

    /**
     * Number of threads that will do the computations, >= 0.
     */
    protected final int mComputingThreadsCount;

    /**
     * Max number of active computing tasks per caller; if this limit is exceeded the reading will be throttled.
     * It is computed as (1 + 2 * {@link #mComputingThreadsCount}) by default.
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
     * @param computingThreadsCount Number of threads that will do the computations.
     *                              This is in addition to the calling thread, which only does the reading and synchronization in this case.
     *                              Can be zero, in which case the calling thread does all the work.
     *                              The only times you'd want to set this to zero are when memory conservation is a top priority
     *                              or when you're running on a single-threaded system.
     *                              The default is 1.
     */
    public AThreadedHillShading(final int computingThreadsCount) {
        super();

        mComputingThreadsCount = Math.max(0, computingThreadsCount);
        mActiveTasksCountMax = 1 + 2 * mComputingThreadsCount;
    }

    /**
     * Uses one separate computing thread by default.
     */
    public AThreadedHillShading() {
        this(ComputingThreadsCountDefault);
    }

    /**
     * Process one unit element, a smallest subdivision of the input, which consists of four points on a "square"
     * with vertices in NW-SW-SE-NE directions from the center.
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
     */
    protected abstract int processOneUnitElement(double nw, double sw, double se, double ne, double mpe, int outputIx, ComputingParams computingParams);

    /**
     * {@inheritDoc}
     */
    @Override
    protected byte[] convert(InputStream inputStream, int dummyAxisLen, int dummyRowLen, final int padding, HgtFileInfo fileInfo) throws IOException {
        return doTheWork(inputStream, padding, fileInfo);
    }

    protected byte[] doTheWork(final InputStream inputStream, final int padding, final HgtFileInfo fileInfo) throws IOException {
        final int outputAxisLen = getOutputAxisLen(fileInfo);
        final int inputAxisLen = getInputAxisLen(fileInfo);
        final int outputWidth = outputAxisLen + 2 * padding;
        final int lineBufferSize = inputAxisLen + 1;
        final double northHalfUnitDistancePerLine = 0.5 * getLatUnitDistance(fileInfo.northLat(), inputAxisLen) / inputAxisLen;
        final double southHalfUnitDistancePerLine = 0.5 * getLatUnitDistance(fileInfo.southLat(), inputAxisLen) / inputAxisLen;

        final byte[] output = new byte[outputWidth * outputWidth];

        if (isNotStopped()) {
            final Awaiter awaiter = new Awaiter();
            final AtomicInteger activeTasksCount = new AtomicInteger(0);
            final ShortArraysPool inputArraysPool = new ShortArraysPool(1 + mActiveTasksCountMax), lineBuffersPool = new ShortArraysPool(1 + mActiveTasksCountMax);

            final ComputingParams computingParams = new ComputingParams.Builder()
                    .setInputStream(inputStream)
                    .setOutput(output)
                    .setAwaiter(awaiter)
                    .setInputAxisLen(inputAxisLen)
                    .setOutputAxisLen(outputAxisLen)
                    .setOutputWidth(outputWidth)
                    .setLineBufferSize(lineBufferSize)
                    .setPadding(padding)
                    .setNorthUnitDistancePerLine(northHalfUnitDistancePerLine)
                    .setSouthUnitDistancePerLine(southHalfUnitDistancePerLine)
                    .setActiveTasksCount(activeTasksCount)
                    .setInputArraysPool(inputArraysPool)
                    .setLineBuffersPool(lineBuffersPool)
                    .build();

            final int tasksCount = determineComputingTasksCount(computingParams);
            final int linesPerTask = inputAxisLen / tasksCount;

            final ComputingTask[] computingTasks = new ComputingTask[tasksCount];

            short[] lineBuffer = new short[lineBufferSize], lineBufferTmp = null;

            for (int taskIndex = 0; taskIndex < tasksCount; taskIndex++) {
                paceReading(awaiter, activeTasksCount, mActiveTasksCountMax);

                if (taskIndex > 0) {
                    lineBuffer = lineBufferTmp;
                    lineBufferTmp = null;
                } else {
                    short last = 0;

                    // First line for the first task is done separately
                    for (int col = 0; col < lineBufferSize; col++) {
                        last = readNext(inputStream, last);

                        lineBuffer[col] = last;
                    }
                }

                final int lineFrom, lineTo;
                {
                    lineFrom = 1 + linesPerTask * taskIndex;

                    if (taskIndex < tasksCount - 1) {
                        lineTo = lineFrom + linesPerTask - 1;
                    } else {
                        lineTo = inputAxisLen;
                    }
                }

                final short[] input;
                {
                    if (taskIndex < tasksCount - 1) {
                        input = inputArraysPool.getArray(lineBufferSize * (lineTo - lineFrom + 1));
                        lineBufferTmp = lineBuffersPool.getArray(lineBufferSize);

                        int inputIx = 0;

                        // First line is done separately
                        for (; inputIx <= inputAxisLen && isNotStopped(); inputIx++) {
                            input[inputIx] = readNextFromStream(inputStream, lineBuffer, inputIx, 0);
                        }

                        for (int line = lineFrom + 1; line <= lineTo - 1 && isNotStopped(); line++) {
                            // Inner loop, critical for performance
                            for (int col = 0; col <= inputAxisLen; col++, inputIx++) {
                                input[inputIx] = readNextFromStream(inputStream, input, inputIx, lineBufferSize);
                            }
                        }

                        // Last line is done separately
                        for (int col = 0; col <= inputAxisLen && isNotStopped(); col++, inputIx++) {
                            final short point = readNextFromStream(inputStream, input, inputIx, lineBufferSize);
                            input[inputIx] = point;
                            lineBufferTmp[col] = point;
                        }
                    } else {
                        input = null;
                    }
                }

                final ComputingTask computingTask = new ComputingTask(lineFrom, lineTo, input, lineBuffer, computingParams);
                computingTasks[taskIndex] = computingTask;

                if (taskIndex < tasksCount - 1) {
                    postToThreadPoolOrRun(computingTask);
                } else {
                    // Delegate
                    computingTask.run();
                }
            }

            notifyReadingDone(activeTasksCount.get());

            if (tasksCount > 1) {
                final Callable<Boolean> condition = new Callable<Boolean>() {
                    @Override
                    public Boolean call() {
                        boolean retVal = true;

                        for (final ComputingTask computingTask : computingTasks) {
                            retVal &= computingTask.mIsDone;
                        }

                        return retVal;
                    }
                };

                awaiter.doWait(condition);
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
     * Called when reading from the input is complete.
     * Computing may still be ongoing.
     * Can be used to profile performance, for example.
     *
     * @param activeComputingTasksCount How many computing tasks are not yet completed at the time this is called.
     */
    protected void notifyReadingDone(final int activeComputingTasksCount) {
    }

    /**
     * Called when there are already too many active computing tasks, so the reading must be slowed down to conserve memory.
     * An active task is a task that is currently being processed or has been prepared and is waiting to be processed.
     * Can be used to profile performance, for example.
     *
     * @param activeComputingTasksCount How many computing tasks are not yet completed at the time this is called.
     */
    protected void notifyReadingPaced(final int activeComputingTasksCount) {
    }

    /**
     * Decides a total number of computing tasks will be used for the given input parameters.
     */
    protected int determineComputingTasksCount(final ComputingParams computingParams) {
        final int retVal;

        if (mComputingThreadsCount > 0) {
            final long computingTasksCount = Math.max(1L, Math.min(computingParams.mInputAxisLen / 2, (long) computingParams.mInputAxisLen * computingParams.mInputAxisLen / ElementsPerComputingTask));

            retVal = (int) computingTasksCount;
        } else {
            // If multi threading is not used, return a single task for the whole job
            retVal = 1;
        }

        return retVal;
    }

    /**
     * The {@code Runnable} provided will be sent to the thread pool, or run on the calling thread if the thread pool rejects it (an unlikely event).
     *
     * @param code A code to run.
     */
    protected void postToThreadPoolOrRun(final Runnable code) {
        boolean retVal = false;

        final AtomicReference<HillShadingThreadPool> threadPoolReference = mThreadPool.get();

        if (threadPoolReference != null) {
            if (threadPoolReference.get() == null && mComputingThreadsCount > 0) {
                synchronized (threadPoolReference) {
                    if (threadPoolReference.get() == null) {
                        threadPoolReference.set(createThreadPool());
                    }
                }
            }

            final HillShadingThreadPool threadPool = threadPoolReference.get();

            if (threadPool != null) {
                retVal = threadPool.execute(code);
            }
        }

        if (false == retVal) {
            if (code != null) {
                code.run();

                retVal = true;
            }
        }
    }

    protected HillShadingThreadPool createThreadPool() {
        return new HillShadingThreadPool(mComputingThreadsCount, mComputingThreadsCount, Integer.MAX_VALUE, 1, ThreadPoolName).start();
    }

    public short readNextFromStream(final InputStream is, final short[] input, final int inputIx, final int fallbackIxDelta) throws IOException {
        final int read1 = is.read();
        final int read2 = is.read();

        if (read1 != -1 && read2 != -1) {
            short read = (short) ((read1 << 8) | read2);

            if (read == Short.MIN_VALUE) return input[inputIx - fallbackIxDelta];

            return read;
        } else {
            return input[inputIx - fallbackIxDelta];
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

    /**
     * A computing task which converts part of the input to part of the output, by calling
     * {@link #processOneUnitElement(double, double, double, double, double, int, ComputingParams)}
     * on all input unit elements from the given part.
     * The part will be the whole input and output, if multi threading is not used.
     */
    protected class ComputingTask implements Runnable {
        protected final short[] mInput, mLineBuffer;
        protected final int mLineFrom, mLineTo;
        protected final ComputingParams mComputingParams;

        protected volatile boolean mIsDone = false;

        public ComputingTask(int lineFrom, int lineTo, short[] input, short[] lineBuffer, ComputingParams computingParams) {
            mInput = input;
            mLineBuffer = lineBuffer;
            mLineFrom = lineFrom;
            mLineTo = lineTo;
            mComputingParams = computingParams;
        }

        @Override
        public void run() {
            try {
                final int resolutionFactor = mComputingParams.mOutputAxisLen / mComputingParams.mInputAxisLen;

                // Must add two additional paddings (after possibly skipping a line) to get to a starting position of the next line
                final int outputIxIncrement = (resolutionFactor - 1) * mComputingParams.mOutputWidth + 2 * mComputingParams.mPadding;

                int outputIx = mComputingParams.mOutputWidth * mComputingParams.mPadding + mComputingParams.mPadding;
                outputIx += resolutionFactor * (mLineFrom - 1) * mComputingParams.mOutputWidth;

                if (mInput != null) {
                    int inputIx = 0;

                    // First line done separately, using the line buffer
                    {
                        short nw = mLineBuffer[inputIx];
                        short sw = mInput[inputIx++];

                        final double metersPerElement = mComputingParams.mSouthUnitDistancePerLine * mLineFrom + mComputingParams.mNorthUnitDistancePerLine * (mComputingParams.mInputAxisLen - mLineFrom);

                        for (int col = 1; col <= mComputingParams.mInputAxisLen && isNotStopped(); col++) {
                            final short ne = mLineBuffer[inputIx];
                            final short se = mInput[inputIx++];

                            outputIx = processOneUnitElement(nw, sw, se, ne, metersPerElement, outputIx, mComputingParams);

                            nw = ne;
                            sw = se;
                        }

                        outputIx += outputIxIncrement;
                    }

                    mComputingParams.mLineBuffersPool.recycleArray(mLineBuffer);

                    int offsetInputIx = inputIx - mComputingParams.mLineBufferSize;

                    for (int line = mLineFrom + 1; line <= mLineTo && isNotStopped(); line++) {
                        short nw = mInput[offsetInputIx++];
                        short sw = mInput[inputIx++];

                        final double metersPerElement = mComputingParams.mSouthUnitDistancePerLine * line + mComputingParams.mNorthUnitDistancePerLine * (mComputingParams.mInputAxisLen - line);

                        // Inner loop, critical for performance
                        for (int col = 1; col <= mComputingParams.mInputAxisLen; col++) {
                            final short ne = mInput[offsetInputIx++];
                            final short se = mInput[inputIx++];

                            outputIx = processOneUnitElement(nw, sw, se, ne, metersPerElement, outputIx, mComputingParams);

                            nw = ne;
                            sw = se;
                        }

                        outputIx += outputIxIncrement;
                    }

                    mComputingParams.mInputArraysPool.recycleArray(mInput);
                } else {
                    int lineBufferIx = 0;

                    for (int line = mLineFrom; line <= mLineTo && isNotStopped(); line++) {
                        if (lineBufferIx >= mComputingParams.mLineBufferSize) {
                            lineBufferIx = 0;
                        }

                        short nw = mLineBuffer[lineBufferIx];
                        short sw = readNextFromStream(nw);
                        mLineBuffer[lineBufferIx++] = sw;

                        final double metersPerElement = mComputingParams.mSouthUnitDistancePerLine * line + mComputingParams.mNorthUnitDistancePerLine * (mComputingParams.mInputAxisLen - line);

                        // Inner loop, critical for performance
                        for (int col = 1; col <= mComputingParams.mInputAxisLen; col++) {
                            final short ne = mLineBuffer[lineBufferIx];
                            final short se = readNextFromStream(ne);
                            mLineBuffer[lineBufferIx++] = se;

                            outputIx = processOneUnitElement(nw, sw, se, ne, metersPerElement, outputIx, mComputingParams);

                            nw = ne;
                            sw = se;
                        }

                        outputIx += outputIxIncrement;
                    }

                    mComputingParams.mLineBuffersPool.recycleArray(mLineBuffer);
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.toString());
            } finally {
                mIsDone = true;
                mComputingParams.mActiveTasksCount.decrementAndGet();
                mComputingParams.mAwaiter.doNotify();
            }
        }

        protected short readNextFromStream(final short fallback) throws IOException {
            return readNext(mComputingParams.mInputStream, fallback);
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
        public final int mOutputWidth;
        public final int mLineBufferSize;
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
            mOutputWidth = builder.mOutputWidth;
            mLineBufferSize = builder.mLineBufferSize;
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
            protected volatile int mOutputWidth;
            protected volatile int mLineBufferSize;
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

            public Builder setOutputWidth(int outputWidth) {
                this.mOutputWidth = outputWidth;
                return this;
            }

            public Builder setLineBufferSize(int lineBufferSize) {
                this.mLineBufferSize = lineBufferSize;
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
