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

import org.junit.Assert;
import org.junit.Test;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.TestUtils;

public class QueueItemTest {

	private static final int[] TILE_SIZES = {256, 128, 376, 512, 100};

	private static QueueItem<?> createTileDownloadJob(Tile tile, int tileSize) {
		return new QueueItem<Job>(new Job(tile, tileSize, false));
	}

	private static void verifyInvalidPriority(QueueItem<Job> queueItem, double priority) {
		try {
			queueItem.setPriority(priority);
			Assert.fail("priority: " + priority);
		} catch (IllegalArgumentException e) {
			Assert.assertTrue(true);
		}
	}

	@Test
	public void equalsTest() {
		for (int tileSize : TILE_SIZES) {
			Tile tile1 = new Tile(1, 1, (byte) 1);
			Tile tile2 = new Tile(2, 2, (byte) 2);

			QueueItem<Job> queueItem1 = new QueueItem<Job>(new Job(tile1, tileSize, false));
			QueueItem<Job> queueItem2 = new QueueItem<Job>(new Job(tile1, tileSize, false));
			QueueItem<Job> queueItem3 = new QueueItem<Job>(new Job(tile2, tileSize, false));

			TestUtils.equalsTest(queueItem1, queueItem2);

			Assert.assertNotEquals(queueItem1, queueItem3);
			Assert.assertNotEquals(queueItem3, queueItem1);
			Assert.assertNotEquals(queueItem1, new Object());
		}
	}

	@Test
	public void invalidConstructorTest() {
		try {
			createTileDownloadJob(null, 1);
			Assert.fail();
		} catch (IllegalArgumentException e) {
			Assert.assertTrue(true);
		}
	}

	@Test
	public void priorityTest() {
		for (int tileSize : TILE_SIZES) {
			Tile tile = new Tile(0, 0, (byte) 0);
			QueueItem<Job> queueItem = new QueueItem<Job>(new Job(tile, tileSize, false));
			Assert.assertEquals(0, queueItem.getPriority(), 0);

			queueItem.setPriority(42);
			Assert.assertEquals(42, queueItem.getPriority(), 0);

			verifyInvalidPriority(queueItem, -1);
			verifyInvalidPriority(queueItem, Double.NEGATIVE_INFINITY);
			verifyInvalidPriority(queueItem, Double.NaN);
		}
	}
}
