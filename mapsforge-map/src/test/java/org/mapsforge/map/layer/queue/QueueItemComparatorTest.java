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
import org.mapsforge.map.layer.download.DownloadJob;
import org.mapsforge.map.layer.download.tilesource.OpenStreetMapMapnik;
import org.mapsforge.map.layer.download.tilesource.TileSource;

public class QueueItemComparatorTest {
	@Test
	public void compareTest() {
		Tile tile1 = new Tile(0, 0, (byte) 1);
		Tile tile2 = new Tile(0, 0, (byte) 2);
		TileSource tileSource = OpenStreetMapMapnik.create();

		QueueItem<?> queueItem1 = new QueueItem<DownloadJob>(new DownloadJob(tile1, tileSource));
		QueueItem<?> queueItem2 = new QueueItem<DownloadJob>(new DownloadJob(tile2, tileSource));

		QueueItemComparator queueItemComparator = QueueItemComparator.INSTANCE;
		Assert.assertEquals(0, queueItemComparator.compare(queueItem1, queueItem2), 0);

		queueItem1.setPriority(1);
		queueItem2.setPriority(2);
		Assert.assertTrue(queueItemComparator.compare(queueItem1, queueItem2) < 0);
		Assert.assertTrue(queueItemComparator.compare(queueItem2, queueItem1) > 0);
	}
}
