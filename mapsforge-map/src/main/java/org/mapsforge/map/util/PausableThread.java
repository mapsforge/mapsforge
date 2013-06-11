/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
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
package org.mapsforge.map.util;

/**
 * An abstract base class for threads which support pausing and resuming.
 */
public abstract class PausableThread extends Thread {
	/**
	 * Specifies the scheduling priority of a {@link Thread}.
	 */
	protected enum ThreadPriority {
		/**
		 * The priority between {@link #NORMAL} and {@link #HIGHEST}.
		 */
		ABOVE_NORMAL((Thread.NORM_PRIORITY + Thread.MAX_PRIORITY) / 2),

		/**
		 * The priority between {@link #LOWEST} and {@link #NORMAL}.
		 */
		BELOW_NORMAL((Thread.NORM_PRIORITY + Thread.MIN_PRIORITY) / 2),

		/**
		 * The maximum priority a thread can have.
		 */
		HIGHEST(MAX_PRIORITY),

		/**
		 * The minimum priority a thread can have.
		 */
		LOWEST(MIN_PRIORITY),

		/**
		 * The default priority of a thread.
		 */
		NORMAL(NORM_PRIORITY);

		final int priority;

		private ThreadPriority(int priority) {
			if (priority < Thread.MIN_PRIORITY || priority > Thread.MAX_PRIORITY) {
				throw new IllegalArgumentException("invalid priority: " + priority);
			}
			this.priority = priority;
		}
	}

	private boolean pausing;
	private boolean shouldPause;

	/**
	 * Causes the current thread to wait until this thread is pausing.
	 */
	public final void awaitPausing() {
		synchronized (this) {
			while (!isInterrupted() && !isPausing()) {
				try {
					wait(100);
				} catch (InterruptedException e) {
					// restore the interrupted status
					Thread.currentThread().interrupt();
				}
			}
		}
	}

	@Override
	public void interrupt() {
		// first acquire the monitor which is used to call wait()
		synchronized (this) {
			super.interrupt();
		}
	}

	/**
	 * @return true if this thread is currently pausing, false otherwise.
	 */
	public final synchronized boolean isPausing() {
		return this.pausing;
	}

	/**
	 * The thread should stop its work temporarily.
	 */
	public final synchronized void pause() {
		if (!this.shouldPause) {
			this.shouldPause = true;
			notify();
		}
	}

	/**
	 * The paused thread should continue with its work.
	 */
	public final synchronized void proceed() {
		if (this.shouldPause) {
			this.shouldPause = false;
			this.pausing = false;
			notify();
		}
	}

	@Override
	public final void run() {
		setName(getClass().getSimpleName());
		setPriority(getThreadPriority().priority);

		while (!isInterrupted()) {
			synchronized (this) {
				while (!isInterrupted() && (this.shouldPause || !hasWork())) {
					try {
						if (this.shouldPause) {
							this.pausing = true;
						}
						wait();
					} catch (InterruptedException e) {
						// restore the interrupted status
						interrupt();
					}
				}
			}

			if (isInterrupted()) {
				break;
			}

			try {
				doWork();
			} catch (InterruptedException e) {
				// restore the interrupted status
				interrupt();
			}
		}

		afterRun();
	}

	/**
	 * Called once at the end of the {@link #run()} method. The default implementation is empty.
	 */
	protected void afterRun() {
		// do nothing
	}

	/**
	 * Called when this thread is not paused and should do its work.
	 * 
	 * @throws InterruptedException
	 *             if the thread has been interrupted.
	 */
	protected abstract void doWork() throws InterruptedException;

	/**
	 * @return the priority which will be set for this thread.
	 */
	protected abstract ThreadPriority getThreadPriority();

	/**
	 * @return true if this thread has some work to do, false otherwise.
	 */
	protected abstract boolean hasWork();
}
