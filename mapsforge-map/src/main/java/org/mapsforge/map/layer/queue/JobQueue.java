/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2016 ksaihtam
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

import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.model.IMapViewPosition;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class JobQueue<T extends Job> {
    private static final int QUEUE_CAPACITY = 128;

    private final List<T> assignedJobs = new LinkedList<>();
    private final DisplayModel displayModel;
    private boolean isInterrupted;
    private final IMapViewPosition mapViewPosition;
    private final List<QueueItem<T>> queueItems = new LinkedList<>();
    private boolean scheduleNeeded;

    public JobQueue(IMapViewPosition mapViewPosition, DisplayModel displayModel) {
        this.mapViewPosition = mapViewPosition;
        this.displayModel = displayModel;
    }

    public synchronized void add(T job) {
        if (!this.assignedJobs.contains(job)) {
            QueueItem<T> queueItem = new QueueItem<>(job);
            if (!this.queueItems.contains(queueItem)) {
                this.queueItems.add(queueItem);
                this.scheduleNeeded = true;
                this.notifyWorkers();
            }
        }
    }

    /**
     * Returns the most important entry from this queue. The method blocks while this queue is empty.
     */
    public synchronized T get() throws InterruptedException {
        return get(Integer.MAX_VALUE);
    }

    /**
     * Returns the most important entry from this queue. The method blocks while this queue is empty
     * or while there are already a certain number of jobs assigned.
     *
     * @param maxAssigned the maximum number of jobs that should be assigned at any one point. If there
     *                    are already so many jobs assigned, the queue will block. This is to ensure
     *                    that the scheduling will continue to work.
     */
    public synchronized T get(int maxAssigned) throws InterruptedException {
        while (this.queueItems.isEmpty() || this.assignedJobs.size() >= maxAssigned) {
            this.wait(200);
            if (this.isInterrupted) {
                this.isInterrupted = false;
                return null;
            }
        }

        if (this.scheduleNeeded) {
            this.scheduleNeeded = false;
            schedule(displayModel.getTileSize());
        }

        T job = this.queueItems.remove(0).object;
        this.assignedJobs.add(job);
        return job;
    }

    public synchronized void interrupt() {
        this.isInterrupted = true;
        notifyWorkers();
    }

    public synchronized void notifyWorkers() {
        this.notifyAll();
    }

    public synchronized void remove(T job) {
        this.assignedJobs.remove(job);
        this.notifyWorkers();
    }

    private void schedule(int tileSize) {
        QueueItemScheduler.schedule(this.queueItems, this.mapViewPosition.getMapPosition(), tileSize);
        Collections.sort(this.queueItems, QueueItemComparator.INSTANCE);
        trimToSize();
    }

    /**
     * @return the current number of entries in this queue.
     */
    public synchronized int size() {
        return this.queueItems.size();
    }

    private void trimToSize() {
        int queueSize = this.queueItems.size();

        while (queueSize > QUEUE_CAPACITY) {
            this.queueItems.remove(--queueSize);
        }
    }
}
