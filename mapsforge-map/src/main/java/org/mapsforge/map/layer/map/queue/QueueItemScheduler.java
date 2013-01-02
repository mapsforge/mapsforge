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

import java.util.Collection;

import org.mapsforge.core.model.MapPosition;

final class QueueItemScheduler {
	static void schedule(Collection<QueueItem> queueItems, MapPosition mapPosition) {
		for (QueueItem queueItem : queueItems) {
			queueItem.priority = calculatePriority(queueItem, mapPosition);
		}
	}

	private static double calculatePriority(QueueItem queueItem, MapPosition mapPosition) {
		return Math.abs(queueItem.tile.zoomLevel - mapPosition.zoomLevel);
	}

	private QueueItemScheduler() {
		throw new IllegalStateException();
	}
}
