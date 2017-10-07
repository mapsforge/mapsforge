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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A simple calculation future for cross-thread result sharing and optional eager parallelism via
 * {@link #withRunningThread()}.
 * <p>
 * <p>tested faster on android than LatchedLazyFuture, but does not support timeout</p>
 *
 * @param <X> result type
 */
public abstract class SyncLazyFuture<X> implements Future<X> {
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
    private volatile ExecutionException state = null;
//    private final AtomicReference<ExecutionException> state = new AtomicReference<>(null);


    private volatile X result;
    private volatile Thread thread;

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (state == CANCELLED) return true;
        if (state == DONE) return false;
        if (mayInterruptIfRunning) {
            Thread t = this.thread;
            if (t != null && state == STARTED) {
                state = CANCELLED;
                t.interrupt();
                return true;
            }
        }
        boolean ret = state == null;
        state = CANCELLED;
        return ret;
//        return state.compareAndSet(null, CANCELLED);
    }

    @Override
    public boolean isCancelled() {
        return state == CANCELLED;
    }

    @Override
    public boolean isDone() {
        return state != null && state != STARTED;
    }

    @Override
    public X get() throws InterruptedException, ExecutionException {
        synchronized (this) {
            if (state == null) {
                state = STARTED;
                internalCalc();

            }
        }
        throwIfException();
        return result;
    }

    private void throwIfException() throws ExecutionException {
        ExecutionException executionException = state;
        if (executionException != null && !(executionException instanceof DummyExecutionException)) {
            throw executionException;
        }
    }

    @Override
    public X get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        this.wait();
        return get();
//        synchronized (this) { // peers should block anyway
//            if (state == null) {
//                internalCalc();
//            }
//        }
//        throwIfException();
//        return result;
    }

    private void internalCalc() throws ExecutionException, InterruptedException {
        state = STARTED;
        try {
            thread = Thread.currentThread();
            result = calculate();
            state = DONE;
        } catch (RuntimeException e) {
            state = new ExecutionException(e);
        } catch (ExecutionException e) {
            state = e;
        } finally {
            thread = null;
        }
    }

    protected abstract X calculate() throws ExecutionException, InterruptedException;

    /**
     * spawns a new thread if not already started or done (otherwise, calculation happens in the first get call)
     *
     * @return this for chaining
     */
    public SyncLazyFuture<X> withRunningThread() {
        if (state != null) return this;
        Thread thread = new Thread(SyncLazyFuture.this.getClass().getName() + ".withRunningThread") {
            @Override
            public void run() {
                try {
                    SyncLazyFuture.this.internalCalc();
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
        return this;
    }
}
