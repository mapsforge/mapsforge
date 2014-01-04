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

public class QueueItemComparatorTest {

	private static final int[] TILE_SIZES = {256, 128, 376, 512, 100};

	@Test
	public void compareTest() {
		for (int tileSize : TILE_SIZES) {
			Tile tile1 = new Tile(0, 0, (byte) 1);
			Tile tile2 = new Tile(0, 0, (byte) 2);

			QueueItem<?> queueItem1 = new QueueItem<Job>(new Job(tile1, tileSize, false));
			QueueItem<?> queueItem2 = new QueueItem<Job>(new Job(tile2, tileSize, false));

			QueueItemComparator queueItemComparator = QueueItemComparator.INSTANCE;
			Assert.assertEquals(0, queueItemComparator.compare(queueItem1, queueItem2), 0);

			queueItem1.setPriority(1);
			queueItem2.setPriority(2);
			Assert.assertTrue(queueItemComparator.compare(queueItem1, queueItem2) < 0);
			Assert.assertTrue(queueItemComparator.compare(queueItem2, queueItem1) > 0);
		}
	}
}
