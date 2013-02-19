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

public class InMemoryTileCacheTest {
	private static void verifyInvalidCapacity(InMemoryTileCache<DownloadJob> inMemoryTileCache, int capacity) {
		try {
			inMemoryTileCache.setCapacity(capacity);
			Assert.fail("capacity: " + capacity);
		} catch (IllegalArgumentException e) {
			Assert.assertTrue(true);
		}
	}

	private static void verifyInvalidPut(TileCache<DownloadJob> tileCache, DownloadJob downloadJob, Bitmap bitmap) {
		try {
			tileCache.put(downloadJob, bitmap);
			Assert.fail("downloadJob: " + downloadJob + ", bitmap: " + bitmap);
		} catch (IllegalArgumentException e) {
			Assert.assertTrue(true);
		}
	}

	@Test
	public void inMemoryTileCacheTestTest() {
		TileCache<DownloadJob> tileCache = new InMemoryTileCache<DownloadJob>(1);
		Assert.assertEquals(1, tileCache.getCapacity());

		Tile tile1 = new Tile(1, 1, (byte) 1);
		Tile tile2 = new Tile(2, 2, (byte) 2);
		TileSource tileSource = OpenStreetMapMapnik.INSTANCE;
		DownloadJob downloadJob1 = new DownloadJob(tile1, tileSource);
		DownloadJob downloadJob2 = new DownloadJob(tile2, tileSource);

		Assert.assertFalse(tileCache.containsKey(downloadJob1));
		Assert.assertFalse(tileCache.containsKey(downloadJob2));
		Assert.assertNull(tileCache.get(downloadJob1));
		Assert.assertNull(tileCache.get(downloadJob2));

		Bitmap bitmap1 = new DummyBitmap(Tile.TILE_SIZE, Tile.TILE_SIZE);
		tileCache.put(downloadJob1, bitmap1);
		Assert.assertTrue(tileCache.containsKey(downloadJob1));
		Assert.assertFalse(tileCache.containsKey(downloadJob2));
		Assert.assertEquals(bitmap1, tileCache.get(downloadJob1));
		Assert.assertNull(tileCache.get(downloadJob2));

		Bitmap bitmap2 = new DummyBitmap(Tile.TILE_SIZE, Tile.TILE_SIZE);
		tileCache.put(downloadJob2, bitmap2);
		Assert.assertFalse(tileCache.containsKey(downloadJob1));
		Assert.assertTrue(tileCache.containsKey(downloadJob2));
		Assert.assertNull(tileCache.get(downloadJob1));
		Assert.assertEquals(bitmap2, tileCache.get(downloadJob2));

		tileCache.destroy();
		Assert.assertFalse(tileCache.containsKey(downloadJob1));
		Assert.assertFalse(tileCache.containsKey(downloadJob2));
		Assert.assertNull(tileCache.get(downloadJob1));
		Assert.assertNull(tileCache.get(downloadJob2));
	}

	@Test
	public void putTest() {
		TileCache<DownloadJob> tileCache = new InMemoryTileCache<DownloadJob>(0);
		verifyInvalidPut(tileCache, null, new DummyBitmap(Tile.TILE_SIZE, Tile.TILE_SIZE));
		verifyInvalidPut(tileCache, new DownloadJob(new Tile(0, 0, (byte) 0), OpenStreetMapMapnik.INSTANCE), null);
	}

	@Test
	public void setCapacityTest() {
		InMemoryTileCache<DownloadJob> inMemoryTileCache = new InMemoryTileCache<DownloadJob>(0);
		Assert.assertEquals(0, inMemoryTileCache.getCapacity());

		Tile tile1 = new Tile(1, 1, (byte) 1);
		TileSource tileSource = OpenStreetMapMapnik.INSTANCE;
		DownloadJob downloadJob1 = new DownloadJob(tile1, tileSource);

		Bitmap bitmap1 = new DummyBitmap(Tile.TILE_SIZE, Tile.TILE_SIZE);
		inMemoryTileCache.put(downloadJob1, bitmap1);
		Assert.assertFalse(inMemoryTileCache.containsKey(downloadJob1));

		inMemoryTileCache.setCapacity(1);
		Assert.assertEquals(1, inMemoryTileCache.getCapacity());

		inMemoryTileCache.put(downloadJob1, bitmap1);
		Assert.assertTrue(inMemoryTileCache.containsKey(downloadJob1));

		Tile tile2 = new Tile(2, 2, (byte) 2);
		DownloadJob downloadJob2 = new DownloadJob(tile2, tileSource);

		inMemoryTileCache.put(downloadJob2, new DummyBitmap(Tile.TILE_SIZE, Tile.TILE_SIZE));
		Assert.assertFalse(inMemoryTileCache.containsKey(downloadJob1));
		Assert.assertTrue(inMemoryTileCache.containsKey(downloadJob2));

		verifyInvalidCapacity(inMemoryTileCache, -1);
	}
}
