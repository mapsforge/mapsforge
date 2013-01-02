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
	private final MapViewPosition mapViewPosition;
	private boolean scheduleNeeded;
	private final List<QueueItem> queueItems = new LinkedList<QueueItem>();

	public TileQueue(MapViewPosition mapViewPosition) {
		this.mapViewPosition = mapViewPosition;
	}

	public synchronized void insert(Tile tile) {
		QueueItem queueItem = new QueueItem(tile);
		if (!this.queueItems.contains(queueItem)) {
			this.queueItems.add(queueItem);
			this.scheduleNeeded = true;
		}
	}

	public synchronized Tile remove() throws InterruptedException {
		while (this.queueItems.isEmpty()) {
			this.wait();
		}

		if (this.scheduleNeeded) {
			QueueItemScheduler.schedule(this.queueItems, this.mapViewPosition.getMapPosition());
			Collections.sort(this.queueItems, QueueItemComparator.INSTANCE);
			this.scheduleNeeded = false;
		}

		return this.queueItems.remove(0).tile;
	}
}
