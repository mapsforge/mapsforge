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
package org.mapsforge.map.layer.queue;

import org.junit.Assert;
import org.junit.Test;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.TestUtils;
import org.mapsforge.map.layer.download.DownloadJob;
import org.mapsforge.map.layer.download.tilesource.OpenStreetMapMapnik;
import org.mapsforge.map.layer.download.tilesource.TileSource;

public class QueueItemTest {
	private static QueueItem<?> createTileDownloadJob(Tile tile) {
		return new QueueItem<DownloadJob>(new DownloadJob(tile, OpenStreetMapMapnik.create()));
	}

	private static void verifyInvalidPriority(QueueItem<DownloadJob> queueItem, double priority) {
		try {
			queueItem.setPriority(priority);
			Assert.fail("priority: " + priority);
		} catch (IllegalArgumentException e) {
			Assert.assertTrue(true);
		}
	}

	@Test
	public void equalsTest() {
		Tile tile1 = new Tile(1, 1, (byte) 1);
		Tile tile2 = new Tile(2, 2, (byte) 2);
		TileSource tileSource = OpenStreetMapMapnik.create();

		QueueItem<DownloadJob> queueItem1 = new QueueItem<DownloadJob>(new DownloadJob(tile1, tileSource));
		QueueItem<DownloadJob> queueItem2 = new QueueItem<DownloadJob>(new DownloadJob(tile1, tileSource));
		QueueItem<DownloadJob> queueItem3 = new QueueItem<DownloadJob>(new DownloadJob(tile2, tileSource));

		TestUtils.equalsTest(queueItem1, queueItem2);

		Assert.assertNotEquals(queueItem1, queueItem3);
		Assert.assertNotEquals(queueItem3, queueItem1);
		Assert.assertNotEquals(queueItem1, new Object());
	}

	@Test
	public void invalidConstructorTest() {
		try {
			createTileDownloadJob(null);
			Assert.fail();
		} catch (IllegalArgumentException e) {
			Assert.assertTrue(true);
		}
	}

	@Test
	public void priorityTest() {
		Tile tile = new Tile(0, 0, (byte) 0);
		TileSource tileSource = OpenStreetMapMapnik.create();
		QueueItem<DownloadJob> queueItem = new QueueItem<DownloadJob>(new DownloadJob(tile, tileSource));
		Assert.assertEquals(0, queueItem.getPriority(), 0);

		queueItem.setPriority(42);
		Assert.assertEquals(42, queueItem.getPriority(), 0);

		verifyInvalidPriority(queueItem, -1);
		verifyInvalidPriority(queueItem, Double.NEGATIVE_INFINITY);
		verifyInvalidPriority(queueItem, Double.NaN);
	}
}
