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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HillShadingUtils {

    public static final double SqrtTwo = Math.sqrt(2);

    /**
     * Intended to be faster than {@code Math.abs()} for well-behaved numbers: no NaN-s, infinities, etc.
     */
    public static double abs(final double x) {
        return (x < 0.0D) ? 0.0D - x : x;
    }

    /**
     * Rounding mode is "half away from zero". Intended to be faster than {@code Math.round()} for small positive numbers.
     */
    public static byte crudeRoundSmallPositives(final double x) {
        return (byte) (x + 0.5d);
    }

    public static double linearMapping(double start, double param, double paramLow, double paramHigh, double factor) {
        return start + (boundToLimits(paramLow, paramHigh, param) - paramLow) * factor;
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
        return Math.max(min, Math.min(value, max));
    }

    /**
     * @return Approximation to the {@code Math.sqrt()}, could be faster but probably isn't.
     */
    public static double sqrtApprox(final double x) {
        return Double.longBitsToDouble(((Double.doubleToLongBits(x) - (1L << 52)) >> 1) + (1L << 61));
    }

    /**
     * @return {@code Math.sqrt(0)} if {@code x <= 0}; {@code Math.sqrt(x)} otherwise.
     */
    public static double safeSqrt(final double x) {
        return Math.sqrt(Math.max(0, x));
    }

    /**
     * @return {@code x * x}
     */
    public static double square(final double x) {
        return x * x;
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

    public static void lock(final Lock lock) {
        lock.lock();
    }

    public static void unlock(final Lock lock) {
        lock.unlock();
    }

    /**
     * Calls {@link Thread#sleep(long)}, catching any {@link Exception} that could happen.
     *
     * @param milliseconds How many milliseconds (max) to wait.
     */
    public static void threadSleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (Exception ignored) {
        }
    }

    /**
     * Atomically increase the parameter if it is less than the limit value provided.
     *
     * @param atomic     Parameter to increase.
     * @param limitValue Limit value.
     * @return {@code true} iff the parameter was increased to a value <= {@code limitValue}, {@code false} otherwise.
     */
    public static boolean atomicIncreaseIfLess(final AtomicInteger atomic, final int limitValue) {
        while (true) {
            final int currValue = atomic.get();

            if (currValue >= limitValue) {
                return false;
            }

            if (atomic.compareAndSet(currValue, currValue + 1)) {
                return true;
            }
        }
    }

    /**
     * A {@link ThreadPoolExecutor} with a custom {@link BlockAndRetryOnRejection} handler when bounds are reached, normal priority threads,
     * and {@code allowCoreThreadTimeOut} set to true.
     */
    public static class HillShadingThreadPool {
        protected final ReentrantLock mSyncLock = new ReentrantLock();
        protected final int mCorePoolSize, mMaxPoolSize, mIdleThreadReleaseTimeout, mQueueSizeMax;
        protected final AtomicLong mRejectCount = new AtomicLong(0);
        protected final String mName;

        protected volatile ThreadPoolExecutor mThreadPool = null;

        public class BlockAndRetryOnRejection implements RejectedExecutionHandler {
            /**
             * {@inheritDoc}
             */
            @Override
            public void rejectedExecution(final Runnable task, final ThreadPoolExecutor executor) {
                final int maxRetries = 7;

                if (mRejectCount.getAndIncrement() < maxRetries) {
                    // The pool is full. Wait, then try again.
                    threadSleep(100);

                    try {
                        executor.execute(task);
                    } catch (Exception ignored) {
                    }
                } else {
                    notifyTaskRejected(task);
                }
            }
        }

        public static class NormPriorityThreadFactory implements ThreadFactory {
            protected final int mThreadPriority = Thread.NORM_PRIORITY;
            protected final ThreadFactory mDefaultThreadFactory = Executors.defaultThreadFactory();
            protected final AtomicInteger mCounter = new AtomicInteger(1);
            protected final String mName;

            /**
             * @param name Name for the new threads. A numbered suffix will be appended. May be {@code null},
             *             in which case the threads will have system default names.
             */
            public NormPriorityThreadFactory(final String name) {
                mName = name;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public Thread newThread(final Runnable task) {
                final Thread thread = mDefaultThreadFactory.newThread(task);

                thread.setPriority(mThreadPriority);

                if (mName != null) {
                    thread.setName(mName + "-thread-" + mCounter.getAndIncrement());
                }

                return thread;
            }
        }

        /**
         * A {@link ThreadPoolExecutor} with a custom {@link BlockAndRetryOnRejection} handler when bounds are reached, normal priority threads,
         * and {@code allowCoreThreadTimeOut} set to true.
         *
         * @param corePoolSize             The number of threads to keep in the pool until they are idle for {@code idleThreadReleaseTimeout} seconds.
         * @param maxPoolSize              The maximum number of threads to allow in the pool.
         * @param queueSizeMax             The maximum number of tasks to keep in the execution waiting queue.
         * @param idleThreadReleaseTimeout How many seconds a thread must be idle before being released.
         *                                 If released, it will be created again the next time it is needed. [seconds]
         * @param name                     Name to give to the threads of this thread pool. A numbered suffix will be appended. May be {@code null},
         *                                 in which case the threads will have system default names.
         */
        public HillShadingThreadPool(final int corePoolSize, final int maxPoolSize, final int queueSizeMax, final int idleThreadReleaseTimeout, final String name) {
            mCorePoolSize = corePoolSize;
            mMaxPoolSize = maxPoolSize;
            mQueueSizeMax = queueSizeMax;
            mIdleThreadReleaseTimeout = idleThreadReleaseTimeout;
            mName = name;
        }

        /**
         * Must be called before this thread pool is used.
         *
         * @return {@code this}
         */
        public HillShadingThreadPool start() {
            lock(mSyncLock);
            try {
                if (mThreadPool == null) {
                    mThreadPool = new ThreadPoolExecutor(mCorePoolSize, mMaxPoolSize, mIdleThreadReleaseTimeout, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(mQueueSizeMax), new NormPriorityThreadFactory(mName), new BlockAndRetryOnRejection()) {
                    };

                    if (mIdleThreadReleaseTimeout > 0) mThreadPool.allowCoreThreadTimeOut(true);
                }
            } finally {
                unlock(mSyncLock);
            }

            return this;
        }

        /**
         * Calls {@code ThreadPoolExecutor.shutdown()}.
         *
         * @return {@code this}
         */
        public HillShadingThreadPool stop() {
            lock(mSyncLock);
            try {
                if (mThreadPool != null) {
                    mThreadPool.shutdown();
                    mThreadPool = null;
                }
            } finally {
                unlock(mSyncLock);
            }

            return this;
        }

        /**
         * Submit a task to the thread pool for execution, or execute in the calling thread if any errors occur.
         *
         * @return {@code true} if task was successfully submitted or executed on the calling thread.
         */
        public boolean execute(final Runnable runnable) {
            lock(mSyncLock);
            try {
                boolean retVal = false;

                if (runnable != null) {
                    try {
                        if (mThreadPool != null) {
                            mThreadPool.execute(runnable);

                            retVal = true;
                        }

                        if (retVal) {
                            mRejectCount.set(0);
                        }
                    } catch (Exception | OutOfMemoryError e) {
                        runnable.run();

                        retVal = true;
                    }
                }

                return retVal;
            } finally {
                unlock(mSyncLock);
            }
        }

        /**
         * Does nothing by default; can be overridden.
         *
         * @param task A task which was rejected.
         */
        public void notifyTaskRejected(final Runnable task) {
        }
    }

    public static class Awaiter {
        protected final Logger LOGGER = Logger.getLogger(this
                .getClass()
                .getName());

        protected final int CheckTimeoutMillis = 100;
        protected final Object mSync = new Object();

        /**
         * Wait for the {@code condition} to become {@code true}.
         *
         * @param condition Condition to wait until it becomes {@code true}.
         */
        public void doWait(final Callable<Boolean> condition) {
            if (condition != null) {
                synchronized (mSync) {
                    while (true) {
                        try {
                            if (condition.call()) break;
                        } catch (Exception e) {
                            LOGGER.log(Level.WARNING, e.toString());
                        }

                        try {
                            mSync.wait(CheckTimeoutMillis);
                        } catch (InterruptedException ignored) {
                        }
                    }
                }
            }
        }

        /**
         * Notify {@code Awaiter} to check the condition again immediately, as it may have changed.
         */
        public synchronized void doNotify() {
            synchronized (mSync) {
                try {
                    mSync.notify();
                } catch (Exception ignored) {
                }
            }
        }
    }

    /**
     * A pool that holds a number of recycled {@code short[]} arrays that can be reused, to save on allocating/initializing new arrays during intensive computations.
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
                        mPool.offerFirst(array);
                    }
                }
            }
        }

        /**
         * Returns an array from the pool (possibly not zero-initialized), or a newly created array if an array from the pool is not available.
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
}
