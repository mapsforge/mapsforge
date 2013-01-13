/*
 * Copyright 2010, 2011, 2012 mapsforge.org
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
package org.mapsforge.map.layer.map.queue;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.mapsforge.core.model.Tile;
import org.mapsforge.map.model.MapViewPosition;

public class TileQueue {
	private static final int QUEUE_CAPACITY = 128;

	private final MapViewPosition mapViewPosition;
	private final List<QueueItem> queueItems = new LinkedList<QueueItem>();
	private boolean scheduleNeeded;

	public TileQueue(MapViewPosition mapViewPosition) {
		this.mapViewPosition = mapViewPosition;
	}

	public synchronized void add(Tile tile) {
		QueueItem queueItem = new QueueItem(tile);
		if (!this.queueItems.contains(queueItem)) {
			this.queueItems.add(queueItem);
			this.scheduleNeeded = true;
		}
	}

	/**
	 * Returns the most important entry from this queue. The method blocks while this queue is empty.
	 */
	public synchronized Tile remove() throws InterruptedException {
		while (this.queueItems.isEmpty()) {
			this.wait();
		}

		if (this.scheduleNeeded) {
			schedule();
			this.scheduleNeeded = false;
		}

		return this.queueItems.remove(0).tile;
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
