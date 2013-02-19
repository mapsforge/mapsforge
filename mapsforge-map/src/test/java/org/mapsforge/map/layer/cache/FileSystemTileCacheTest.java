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

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.graphics.Bitmap;
import org.mapsforge.map.layer.download.DownloadJob;
import org.mapsforge.map.layer.download.tilesource.OpenStreetMapMapnik;
import org.mapsforge.map.layer.download.tilesource.TileSource;
import org.mapsforge.map.rendertheme.GraphicAdapter;

public class FileSystemTileCacheTest {
	private static final GraphicAdapter GRAPHIC_ADAPTER = DummyGraphicAdapter.INSTANCE;
	private static final String TMP_DIR = System.getProperty("java.io.tmpdir");

	private static void assertEquals(Bitmap bitmap1, Bitmap bitmap2) {
		Assert.assertEquals(bitmap1.getWidth(), bitmap2.getWidth());
		Assert.assertEquals(bitmap1.getHeight(), bitmap2.getHeight());

		ByteBuffer byteBuffer1 = ByteBuffer.allocate(bitmap1.getWidth() * bitmap1.getHeight() * 4);
		ByteBuffer byteBuffer2 = ByteBuffer.allocate(bitmap2.getWidth() * bitmap2.getHeight() * 4);
		bitmap1.copyPixelsToBuffer(byteBuffer1);
		bitmap2.copyPixelsToBuffer(byteBuffer2);

		Assert.assertArrayEquals(byteBuffer1.array(), byteBuffer2.array());
	}

	private static TileCache<?> createNewTileCache(int capacity, File cacheDirectory) {
		return new FileSystemTileCache<DownloadJob>(capacity, cacheDirectory, GRAPHIC_ADAPTER);
	}

	private static void verifyInvalidConstructor(int capacity, File cacheDirectory) {
		try {
			createNewTileCache(capacity, cacheDirectory);
			Assert.fail("capacity: " + capacity + ", cacheDirectory: " + cacheDirectory);
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

	private final File cacheDirectory = new File(TMP_DIR, getClass().getSimpleName() + System.currentTimeMillis());

	@After
	public void afterTest() {
		if (this.cacheDirectory.exists() && !this.cacheDirectory.delete()) {
			throw new IllegalStateException("could not delete cache directory: " + this.cacheDirectory);
		}
	}

	@Test
	public void capacityZeroTest() {
		TileCache<DownloadJob> tileCache = new FileSystemTileCache<DownloadJob>(0, this.cacheDirectory, GRAPHIC_ADAPTER);
		Tile tile = new Tile(0, 0, (byte) 0);
		TileSource tileSource = OpenStreetMapMapnik.INSTANCE;
		DownloadJob downloadJob = new DownloadJob(tile, tileSource);

		Bitmap bitmap = new DummyBitmap(Tile.TILE_SIZE, Tile.TILE_SIZE);
		tileCache.put(downloadJob, bitmap);
		Assert.assertEquals(0, this.cacheDirectory.list().length);
		Assert.assertFalse(tileCache.containsKey(downloadJob));
		Assert.assertNull(tileCache.get(downloadJob));

		tileCache.destroy();
	}

	@Test
	public void existingFilesTest() throws IOException {
		Assert.assertTrue(this.cacheDirectory.mkdirs());
		File file1 = new File(this.cacheDirectory, 1 + FileSystemTileCache.FILE_EXTENSION);
		File file2 = new File(this.cacheDirectory, 2 + FileSystemTileCache.FILE_EXTENSION);
		Assert.assertTrue(file1.createNewFile());
		Assert.assertTrue(file2.createNewFile());

		TileCache<DownloadJob> tileCache = new FileSystemTileCache<DownloadJob>(1, this.cacheDirectory, GRAPHIC_ADAPTER);
		Assert.assertEquals(2, this.cacheDirectory.list().length);

		Tile tile = new Tile(0, 0, (byte) 0);
		TileSource tileSource = OpenStreetMapMapnik.INSTANCE;
		DownloadJob downloadJob = new DownloadJob(tile, tileSource);

		Bitmap bitmap = new DummyBitmap(Tile.TILE_SIZE, Tile.TILE_SIZE);
		tileCache.put(downloadJob, bitmap);
		Assert.assertEquals(3, this.cacheDirectory.list().length);
		Assert.assertTrue(file1.exists());
		Assert.assertTrue(file2.exists());
		Assert.assertTrue(new File(this.cacheDirectory, 3 + FileSystemTileCache.FILE_EXTENSION).exists());

		assertEquals(bitmap, tileCache.get(downloadJob));

		tileCache.destroy();
		Assert.assertEquals(0, this.cacheDirectory.list().length);
	}

	@Test
	public void fileSystemTileCacheTest() {
		Assert.assertFalse(this.cacheDirectory.exists());

		TileCache<DownloadJob> tileCache = new FileSystemTileCache<DownloadJob>(1, this.cacheDirectory, GRAPHIC_ADAPTER);
		Assert.assertEquals(1, tileCache.getCapacity());
		Assert.assertTrue(this.cacheDirectory.exists());
		Assert.assertEquals(0, this.cacheDirectory.list().length);

		Tile tile1 = new Tile(0, 0, (byte) 1);
		Tile tile2 = new Tile(0, 0, (byte) 2);
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
		assertEquals(bitmap1, tileCache.get(downloadJob1));
		Assert.assertNull(tileCache.get(downloadJob2));

		Bitmap bitmap2 = new DummyBitmap(Tile.TILE_SIZE, Tile.TILE_SIZE);
		tileCache.put(downloadJob2, bitmap2);
		Assert.assertFalse(tileCache.containsKey(downloadJob1));
		Assert.assertTrue(tileCache.containsKey(downloadJob2));
		Assert.assertNull(tileCache.get(downloadJob1));
		assertEquals(bitmap2, tileCache.get(downloadJob2));

		tileCache.destroy();
		Assert.assertFalse(tileCache.containsKey(downloadJob1));
		Assert.assertFalse(tileCache.containsKey(downloadJob2));
		Assert.assertNull(tileCache.get(downloadJob1));
		Assert.assertNull(tileCache.get(downloadJob2));

		Assert.assertTrue(this.cacheDirectory.exists());
		Assert.assertEquals(0, this.cacheDirectory.list().length);
	}

	@Test
	public void invalidConstructorTest() throws IOException {
		Assert.assertTrue(this.cacheDirectory.createNewFile());
		verifyInvalidConstructor(0, this.cacheDirectory);

		Assert.assertTrue(this.cacheDirectory.delete());
		Assert.assertTrue(this.cacheDirectory.mkdirs());
		verifyInvalidConstructor(-1, this.cacheDirectory);
	}

	@Test
	public void invalidPutTest() {
		TileCache<DownloadJob> tileCache = new FileSystemTileCache<DownloadJob>(1, this.cacheDirectory, GRAPHIC_ADAPTER);
		verifyInvalidPut(tileCache, null, new DummyBitmap(Tile.TILE_SIZE, Tile.TILE_SIZE));
		verifyInvalidPut(tileCache, new DownloadJob(new Tile(0, 0, (byte) 0), OpenStreetMapMapnik.INSTANCE), null);
	}
}
