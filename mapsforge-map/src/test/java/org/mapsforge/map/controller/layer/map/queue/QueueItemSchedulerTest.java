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
package org.mapsforge.map.controller.layer.map.queue;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;
import org.mapsforge.core.model.GeoPoint;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.core.model.Tile;

public class QueueItemSchedulerTest {
	@Test
	public void scheduleTest() {
		Tile tile0 = new Tile(0, 0, (byte) 0);
		QueueItem queueItem = new QueueItem(tile0);
		Assert.assertEquals(0, queueItem.priority, 0);

		MapPosition mapPosition = new MapPosition(new GeoPoint(0, 0), (byte) 0);
		QueueItemScheduler.schedule(Arrays.asList(queueItem), mapPosition);
		Assert.assertEquals(0, queueItem.priority, 0);

		mapPosition = new MapPosition(new GeoPoint(0, 180), (byte) 0);
		QueueItemScheduler.schedule(Arrays.asList(queueItem), mapPosition);
		int halfTileSize = Tile.TILE_SIZE / 2;
		Assert.assertEquals(halfTileSize, queueItem.priority, 0);

		mapPosition = new MapPosition(new GeoPoint(0, -180), (byte) 0);
		QueueItemScheduler.schedule(Arrays.asList(queueItem), mapPosition);
		Assert.assertEquals(halfTileSize, queueItem.priority, 0);

		mapPosition = new MapPosition(new GeoPoint(0, 0), (byte) 1);
		QueueItemScheduler.schedule(Arrays.asList(queueItem), mapPosition);
		double expectedPriority = Math.hypot(halfTileSize, halfTileSize) + QueueItemScheduler.PENALTY_PER_ZOOM_LEVEL;
		Assert.assertEquals(expectedPriority, queueItem.priority, 0);
	}
}
