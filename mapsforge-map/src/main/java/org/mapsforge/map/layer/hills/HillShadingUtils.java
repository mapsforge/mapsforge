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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class HillShadingUtils {

    public static final double SqrtTwo = Math.sqrt(2);

    public static double linearMapping(double start, double param, double paramLow, double paramHigh, double factor) {
        return start + (Math.max(paramLow, Math.min(paramHigh, param)) - paramLow) * factor;
    }

    public static double linearMappingWithoutLimits(double start, double param, double paramLow, double factor) {
        return start + (param - paramLow) * factor;
    }

    /**
     * Faster in the beginning, slower in the end.
     * To get a decreasing mapping, factor can be negative.
     */
    public static double sqrtMapping(double start, double param, double paramLow, double paramHigh, double factor) {
        return start + Math.sqrt(boundToLimits(0, 1, Math.max(0, param - paramLow) / (paramHigh - paramLow))) * (paramHigh - paramLow) * factor;
    }

    /**
     * Slower in the beginning, faster in the end.
     * To get a decreasing mapping, factor can be negative.
     */
    public static double squareMapping(double start, double param, double paramLow, double paramHigh, double factor) {
        return start + square(boundToLimits(0, 1, Math.max(0, param - paramLow) / (paramHigh - paramLow))) * (paramHigh - paramLow) * factor;
    }

    public static double boundToLimits(final double min, final double max, final double value) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * @return Approximation to {@code Math.sqrt()}, often significantly faster.
     */
    public static double sqrtApprox(final double x) {
        return Double.longBitsToDouble(((Double.doubleToLongBits(x) - (1L << 52)) >> 1) + (1L << 61));
    }

    /**
     * @return {@code x * x}
     */
    public static double square(final double x) {
        return x * x;
    }

    /**
     * Rounding mode is "half away from zero". Intended to be faster than {@code Math.round()} for small positive numbers.
     */
    public static byte crudeRoundSmallPositives(final double x) {
        return (byte) (x + 0.5d);
    }

    /**
     * Intended to be faster than {@code Math.abs()} for well-defined numbers: no NaN-s, infinities, etc.
     */
    public static double abs(final double x) {
        return (x < 0.0D) ? 0.0D - x : x;
    }

    /**
     * Compute the x-component of the cross product of vectors {@code a} and {@code b}.
     *
     * @param ay y-component of the vector {@code a}.
     * @param az z-component of the vector {@code a}.
     * @param by y-component of the vector {@code b}.
     * @param bz z-component of the vector {@code b}.
     * @return x-component of the cross product.
     */
    public static double crossProductX(double ay, double az, double by, double bz) {
        return ay * bz - az * by;
    }

    /**
     * Compute the y-component of the cross product of vectors {@code a} and {@code b}.
     *
     * @param ax x-component of the vector {@code a}.
     * @param az z-component of the vector {@code a}.
     * @param bx x-component of the vector {@code b}.
     * @param bz z-component of the vector {@code b}.
     * @return y-component of the cross product.
     */
    public static double crossProductY(double ax, double az, double bx, double bz) {
        return az * bx - ax * bz;
    }

    /**
     * Compute the z-component of the cross product of vectors {@code a} and {@code b}.
     *
     * @param ax x-component of the vector {@code a}.
     * @param ay y-component of the vector {@code a}.
     * @param bx x-component of the vector {@code b}.
     * @param by y-component of the vector {@code b}.
     * @return z-component of the cross product.
     */
    public static double crossProductZ(double ax, double ay, double bx, double by) {
        return ax * by - ay * bx;
    }

    public static void skipNBytes(InputStream stream, long n) throws IOException {
        if (stream != null) {
            while (true) {
                if (n > 0L) {
                    long ns = stream.skip(n);
                    if (ns > 0L && ns <= n) {
                        n -= ns;
                        continue;
                    }

                    if (ns == 0L) {
                        if (stream.read() == -1) {
                            throw new EOFException();
                        }

                        --n;
                        continue;
                    }

                    throw new IOException("Unable to skip exactly");
                }

                return;
            }
        }
    }

    /**
     * A {@link ThreadPoolExecutor} with normal priority threads and {@code allowCoreThreadTimeOut} set to true.
     */
    public static class HillShadingThreadPool {
        protected final Object mSync = new Object();
        protected final int mCorePoolSize, mMaxPoolSize, mIdleThreadReleaseTimeout, mQueueSize;
        protected final String mName;

        protected volatile ThreadPoolExecutor mThreadPool = null;

        public static class MyRejectedExecutionHandler implements RejectedExecutionHandler {
            public static class MyRejectedThrowable extends Throwable {
                public MyRejectedThrowable(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
                    super(message, cause, enableSuppression, writableStackTrace);
                }
            }

            @SuppressWarnings("unchecked")
            private static <T extends Throwable> void throwException(Throwable exception) throws T {
                throw (T) exception;
            }

            public void rejectedExecution(Runnable task, ThreadPoolExecutor executor) {
                throwException(new MyRejectedThrowable("Rejected", null, false, false));
            }
        }

        public static class MyThreadFactory implements ThreadFactory {
            protected final ThreadFactory mDefaultThreadFactory = Executors.defaultThreadFactory();
            protected final AtomicInteger mCounter = new AtomicInteger(1);
            protected final String mName;

            /**
             * @param name Name for new threads. A numbered suffix will be appended. May be {@code null},
             *             in which case the threads will have system default names.
             */
            public MyThreadFactory(final String name) {
                mName = name;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public Thread newThread(final Runnable task) {
                final Thread thread = mDefaultThreadFactory.newThread(task);

                if (mName != null) {
                    thread.setName(mName + "-thread-" + mCounter.getAndIncrement());
                }

                return thread;
            }
        }

        /**
         * A {@link ThreadPoolExecutor} with normal priority threads and {@code allowCoreThreadTimeOut} set to true.
         *
         * @param corePoolSize             Number of threads to keep in the pool until they are idle for {@code idleThreadReleaseTimeout} seconds.
         * @param maxPoolSize              Maximum number of threads to allow in the pool.
         * @param queueSize                The capacity of the execution waiting queue. Set to {@link Integer#MAX_VALUE} to use an unbounded {@link LinkedBlockingDeque}.
         * @param idleThreadReleaseTimeout How many seconds a thread must be idle before being released.
         *                                 If released, it will be created again the next time it is needed. [seconds]
         * @param name                     Name to give to the threads of this thread pool. A numbered suffix will be appended. May be {@code null},
         *                                 in which case the threads will have system default names.
         */
        public HillShadingThreadPool(final int corePoolSize, final int maxPoolSize, final int queueSize, final int idleThreadReleaseTimeout, final String name) {
            mCorePoolSize = corePoolSize;
            mMaxPoolSize = maxPoolSize;
            mQueueSize = queueSize;
            mIdleThreadReleaseTimeout = idleThreadReleaseTimeout;
            mName = name;
        }

        /**
         * Must be called before this thread pool is used.
         *
         * @return {@code this}
         */
        public HillShadingThreadPool start() {
            synchronized (mSync) {
                if (mThreadPool == null) {
                    final BlockingQueue<Runnable> queue;
                    {
                        if (mQueueSize <= 0) {
                            queue = new SynchronousQueue<>();
                        } else if (mQueueSize < Integer.MAX_VALUE) {
                            queue = new ArrayBlockingQueue<>(mQueueSize);
                        } else {
                            queue = new LinkedBlockingDeque<>();
                        }
                    }

                    mThreadPool = new ThreadPoolExecutor(mCorePoolSize, mMaxPoolSize, mIdleThreadReleaseTimeout, TimeUnit.SECONDS, queue, new MyThreadFactory(mName), new MyRejectedExecutionHandler());

                    if (mIdleThreadReleaseTimeout > 0) {
                        mThreadPool.allowCoreThreadTimeOut(true);
                    }
                }
            }

            return this;
        }

        /**
         * Calls {@code ThreadPoolExecutor.shutdown()}.
         *
         * @return {@code this}
         */
        public HillShadingThreadPool shutdown() {
            synchronized (mSync) {
                if (mThreadPool != null) {
                    mThreadPool.shutdown();
                    mThreadPool = null;
                }
            }

            return this;
        }

        /**
         * Calls {@code ThreadPoolExecutor.shutdownNow()}.
         *
         * @return {@code this}
         */
        public HillShadingThreadPool shutdownNow() {
            synchronized (mSync) {
                if (mThreadPool != null) {
                    mThreadPool.shutdownNow();
                    mThreadPool = null;
                }
            }

            return this;
        }

        /**
         * Submit a task to the thread pool for execution.
         *
         * @return {@code true} if task was successfully submitted.
         */
        public boolean execute(final Runnable task) {
            boolean retVal = false;

            if (task != null) {
                try {
                    synchronized (mSync) {
                        if (mThreadPool != null) {
                            mThreadPool.execute(task);

                            retVal = true;
                        }
                    }
                } catch (Throwable ignored) {
                }
            }

            return retVal;
        }

        /**
         * Submit a task to the thread pool for execution, or run on the calling thread if submit fails.
         */
        public void executeOrRun(final Runnable task) {
            final boolean status = execute(task);

            if (false == status) {
                if (task != null) {
                    task.run();
                }
            }
        }
    }

    public static class SilentFutureTask extends FutureTask<Boolean> {
        public SilentFutureTask(final Callable<Boolean> callable) {
            super(callable);
        }

        @Override
        public Boolean get() {
            Boolean output = null;

            try {
                output = super.get();
            } catch (Exception ignored) {
            }

            return output;
        }
    }

    /**
     * A pool that holds a number of recycled {@code short[]} arrays that can be reused, to save on allocating/initializing new arrays during intensive computations.
     * It is assumed that arrays in the pool are of similar size, not necessarily the exact same size.
     * Thus, a larger array may be returned sometimes, but an array smaller than requested will never be returned.
     */
    public static class ShortArraysPool {
        protected final Deque<short[]> mPool = new ArrayDeque<>();
        protected final int mPoolCapacity;

        /**
         * Create a pool that will hold at most {@code poolCapacity} recycled arrays.
         *
         * @param poolCapacity Maximum number of recycled arrays that this pool will hold.
         */
        public ShortArraysPool(final int poolCapacity) {
            mPoolCapacity = poolCapacity;
        }

        /**
         * Put the array into the pool (if capacity is not reached).
         *
         * @param array Array to be recycled. A caller should discard all references to the array after calling this.
         */
        public void recycleArray(final short[] array) {
            if (array != null) {
                synchronized (mPool) {
                    if (mPool.size() < mPoolCapacity) {
                        mPool.offerLast(array);
                    }
                }
            }
        }

        /**
         * Returns an array from the pool (possibly not zero-initialized), or a newly created array if an array from the pool is not available.
         * It is assumed that arrays in the pool are of similar size, not necessarily the exact same size.
         * Thus, a larger array may be returned sometimes, but an array smaller than requested will never be returned.
         *
         * @param length Required array length.
         * @return An array from the pool containing at least {@code length} elements, possibly not zero-initialized, or newly created array.
         */
        public short[] getArray(final int length) {
            short[] output = null;

            synchronized (mPool) {
                output = mPool.pollFirst();
            }

            if (output == null) {
                output = new short[length];
            } else if (output.length < length) {
                recycleArray(output);

                output = new short[length];
            }

            return output;
        }
    }

    public static class BlockingSumLimiter {
        protected final AtomicLong mAtomicLong;
        protected final long mInitialSumValue;

        public BlockingSumLimiter(long initialSumValue) {
            mAtomicLong = new AtomicLong(initialSumValue);
            mInitialSumValue = initialSumValue;
        }

        public BlockingSumLimiter() {
            this(0);
        }

        /**
         * Adds the given value iff the current sum is less than the limit, blocking until this is fulfilled.
         *
         * @param valueToAdd Value to add.
         * @param limit Limit sum.
         */
        public void add(final long valueToAdd, final long limit) {
            synchronized (mAtomicLong) {
                while (true) {
                    final long currValue = mAtomicLong.get();

                    if (currValue > limit) {
                        try {
                            mAtomicLong.wait();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    } else if (mAtomicLong.compareAndSet(currValue, currValue + valueToAdd)) {
                        return;
                    }
                }
            }
        }

        /**
         * Subtracts the given value ensuring that the sum doesn't go below the initial sum value as provided in the constructor (default: 0).
         *
         * @param valueToSubtract Value to subtract.
         */
        public void subtract(final long valueToSubtract) {
            synchronized (mAtomicLong) {
                mAtomicLong.addAndGet(-valueToSubtract);

                if (mAtomicLong.get() < mInitialSumValue) {
                    mAtomicLong.set(mInitialSumValue);
                }

                mAtomicLong.notify();
            }
        }
    }
}
