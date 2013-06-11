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
package org.mapsforge.map.layer.queue;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.mapsforge.map.model.MapViewPosition;

public class JobQueue<T extends Job> {
	private static final int QUEUE_CAPACITY = 128;

	private final List<T> assignedJobs = new LinkedList<T>();
	private final MapViewPosition mapViewPosition;
	private final List<QueueItem<T>> queueItems = new LinkedList<QueueItem<T>>();
	private boolean scheduleNeeded;

	public JobQueue(MapViewPosition mapViewPosition) {
		this.mapViewPosition = mapViewPosition;
	}

	public synchronized void add(T job) {
		if (!this.assignedJobs.contains(job)) {
			QueueItem<T> queueItem = new QueueItem<T>(job);
			if (!this.queueItems.contains(queueItem)) {
				this.queueItems.add(queueItem);
				this.scheduleNeeded = true;
			}
		}
	}

	/**
	 * Returns the most important entry from this queue. The method blocks while this queue is empty.
	 */
	public synchronized T get() throws InterruptedException {
		while (this.queueItems.isEmpty()) {
			this.wait();
		}

		if (this.scheduleNeeded) {
			this.scheduleNeeded = false;
			schedule();
		}

		T job = this.queueItems.remove(0).object;
		this.assignedJobs.add(job);
		return job;
	}

	public synchronized void notifyWorkers() {
		this.notifyAll();
	}

	public synchronized void remove(T job) {
		if (!this.assignedJobs.remove(job)) {
			throw new IllegalArgumentException("job not assigned: " + job);
		}
	}

	/**
	 * @return the current number of entries in this queue.
	 */
	public synchronized int size() {
		return this.queueItems.size();
	}

	private void schedule() {
		QueueItemScheduler.schedule(this.queueItems, this.mapViewPosition.getMapPosition());
		Collections.sort(this.queueItems, QueueItemComparator.INSTANCE);
		trimToSize();
	}

	private void trimToSize() {
		int queueSize = this.queueItems.size();

		while (queueSize > QUEUE_CAPACITY) {
			this.queueItems.remove(--queueSize);
		}
	}
}
