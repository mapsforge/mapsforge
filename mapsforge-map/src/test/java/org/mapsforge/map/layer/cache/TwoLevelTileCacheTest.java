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
package org.mapsforge.map.layer.cache;

import org.junit.Assert;
import org.junit.Test;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.graphics.Bitmap;
import org.mapsforge.map.layer.download.DownloadJob;
import org.mapsforge.map.layer.download.tilesource.OpenStreetMapMapnik;
import org.mapsforge.map.layer.download.tilesource.TileSource;

public class TwoLevelTileCacheTest {
	@Test
	public void twoLevelTileCacheTest() {
		TileCache<DownloadJob> tileCache1 = new InMemoryTileCache<DownloadJob>(1);
		TileCache<DownloadJob> tileCache2 = new InMemoryTileCache<DownloadJob>(1);
		TwoLevelTileCache<DownloadJob> twoLevelTileCache = new TwoLevelTileCache<DownloadJob>(tileCache1, tileCache2);

		Assert.assertEquals(1, twoLevelTileCache.getCapacity());

		Tile tile = new Tile(0, 0, (byte) 0);
		TileSource tileSource = OpenStreetMapMapnik.INSTANCE;
		DownloadJob downloadJob = new DownloadJob(tile, tileSource);
		Assert.assertFalse(tileCache1.containsKey(downloadJob));
		Assert.assertFalse(tileCache2.containsKey(downloadJob));
		Assert.assertFalse(twoLevelTileCache.containsKey(downloadJob));

		Bitmap bitmap = new DummyBitmap(Tile.TILE_SIZE, Tile.TILE_SIZE);
		twoLevelTileCache.put(downloadJob, bitmap);
		Assert.assertFalse(tileCache1.containsKey(downloadJob));
		Assert.assertTrue(tileCache2.containsKey(downloadJob));
		Assert.assertTrue(twoLevelTileCache.containsKey(downloadJob));
		Assert.assertEquals(bitmap, twoLevelTileCache.get(downloadJob));

		Assert.assertTrue(tileCache1.containsKey(downloadJob));
		Assert.assertTrue(tileCache2.containsKey(downloadJob));
		Assert.assertTrue(twoLevelTileCache.containsKey(downloadJob));
		Assert.assertEquals(bitmap, twoLevelTileCache.get(downloadJob));

		twoLevelTileCache.destroy();
		Assert.assertFalse(tileCache1.containsKey(downloadJob));
		Assert.assertFalse(tileCache2.containsKey(downloadJob));
		Assert.assertFalse(twoLevelTileCache.containsKey(downloadJob));
		Assert.assertNull(twoLevelTileCache.get(downloadJob));
	}
}
