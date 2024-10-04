/*
 * Copyright 2017 usrusr
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

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A simple calculation future for cross-thread result sharing and optional eager parallelism via
 * {@link #withRunningThread()}.
 *
 * @param <X> result type
 */
public abstract class LatchedLazyFuture<X> implements Future<X> {
    private static class DummyExecutionException extends ExecutionException {
        DummyExecutionException(String name) {
            super(name, null);
        }

        /**
         * static instances don't need a stacktrace
         */
        @Override
        public synchronized Throwable fillInStackTrace() {
            return null;
        }

        @Override
        public String toString() {
            return "[state marker " + getMessage() + "]";
        }
    }

    private static final ExecutionException STARTED = new DummyExecutionException("started");
    private static final ExecutionException CANCELLED = new DummyExecutionException("cancelled");
    private static final ExecutionException DONE = new DummyExecutionException("done");

    /**
     * can hold null (not even started), STARTED, CANCELLED, DONE or an actual exception
     */
    private final AtomicReference<ExecutionException> state = new AtomicReference<>(null);

    private final CountDownLatch latch = new CountDownLatch(1);
    private volatile X result;
    private volatile Thread thread;

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (state.get() == CANCELLED) return true;
        if (state.get() == DONE) return false;
        if (mayInterruptIfRunning) {
            Thread t = this.thread;
            if (t != null && state.compareAndSet(STARTED, CANCELLED)) {
                t.interrupt();
                return true;
            }
        }
        return state.compareAndSet(null, CANCELLED);
    }

    @Override
    public boolean isCancelled() {
        return state.get() == CANCELLED;
    }

    @Override
    public boolean isDone() {
        ExecutionException state = this.state.get();
        return state != null && state != STARTED;
    }

    @Override
    public X get() throws InterruptedException, ExecutionException {
        if (state.compareAndSet(null, STARTED)) {
            internalCalc();
        } else {
            latch.await();
        }
        throwIfException();
        return result;
    }

    private void throwIfException() throws ExecutionException {
        ExecutionException executionException = state.get();
        if (executionException != null && !(executionException instanceof DummyExecutionException))
            throw executionException;
    }

    @Override
    public X get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (state.compareAndSet(null, STARTED)) {
            internalCalc();
        } else {
            latch.await(timeout, unit);
        }
        throwIfException();
        return result;
    }

    private void internalCalc() throws ExecutionException, InterruptedException {
        thread = Thread.currentThread();
        try {
            result = calculate();
            state.compareAndSet(STARTED, DONE);
        } catch (RuntimeException e) {
            state.compareAndSet(STARTED, new ExecutionException(e));
        } catch (ExecutionException e) {
            state.compareAndSet(STARTED, e);
        } finally {
            thread = null;
            latch.countDown();
        }
    }

    protected abstract X calculate() throws ExecutionException, InterruptedException;

    /**
     * spawns a new thread if not already started or done (otherwise, calculation happens in the first get call)
     *
     * @return this for chaining
     */
    public LatchedLazyFuture<X> withRunningThread() {
        if (state.get() == DONE) return this;
        if (state.compareAndSet(null, STARTED)) {
            Thread thread = new Thread(this.getClass().getName() + ".withRunningThread") {
                @Override
                public void run() {
                    try {
                        internalCalc();
                    } catch (ExecutionException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            };
            thread.start();

            return this;
        } else {
            return this;
        }
    }
}
