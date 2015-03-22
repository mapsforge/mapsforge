/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright © 2014 Ludwig M Brinckmann
 * Copyright 2014 mvglasow <michael -at- vonglasow.com>
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

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.awt.AwtGraphicFactory;
import org.mapsforge.map.layer.download.DownloadJob;
import org.mapsforge.map.layer.download.tilesource.OpenStreetMapMapnik;
import org.mapsforge.map.layer.download.tilesource.TileSource;
import org.mapsforge.map.layer.queue.Job;
import org.mapsforge.map.layer.renderer.RendererJob;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.reader.MapFile;

public class FileSystemTileCacheTest {
	private static final GraphicFactory GRAPHIC_FACTORY = AwtGraphicFactory.INSTANCE;
	private static final int[] TILE_SIZES = { 256, 128, 376, 512, 100 };
	private static final String TMP_DIR = System.getProperty("java.io.tmpdir");

	private static TileCache createNewTileCache(int capacity, File cacheDirectory) {
		return new FileSystemTileCache(capacity, cacheDirectory, GRAPHIC_FACTORY);
	}

	private static void verifyEquals(Bitmap bitmap1, Bitmap bitmap2) {
		Assert.assertEquals(bitmap1.getWidth(), bitmap2.getWidth());
		Assert.assertEquals(bitmap1.getHeight(), bitmap2.getHeight());
	}

	private static void verifyInvalidConstructor(int capacity, File cacheDirectory) {
		try {
			createNewTileCache(capacity, cacheDirectory);
			Assert.fail("capacity: " + capacity + ", cacheDirectory: " + cacheDirectory);
		} catch (IllegalArgumentException e) {
			Assert.assertTrue(true);
		}
	}

	private static void verifyInvalidPut(TileCache tileCache, Job job, TileBitmap bitmap) {
		try {
			tileCache.put(job, bitmap);
			Assert.fail("job: " + job + ", bitmap: " + bitmap);
		} catch (IllegalArgumentException e) {
			Assert.assertTrue(true);
		}
	}

	private final File cacheDirectory = new File(TMP_DIR, getClass().getSimpleName() + System.currentTimeMillis()
			+ ".1");
	private final File cacheDirectory2 = new File(TMP_DIR, getClass().getSimpleName() + System.currentTimeMillis()
			+ ".2");

	@After
	public void afterTest() {
		if (this.cacheDirectory.exists() && !this.cacheDirectory.delete()) {
			throw new IllegalStateException("could not delete cache directory: " + this.cacheDirectory);
		}
		if (this.cacheDirectory2.exists() && !this.cacheDirectory2.delete()) {
			throw new IllegalStateException("could not delete cache directory: " + this.cacheDirectory2);
		}
	}

	@Test
	public void capacityZeroTest() {
		for (int tileSize : TILE_SIZES) {
			TileCache tileCache = new FileSystemTileCache(0, this.cacheDirectory, GRAPHIC_FACTORY);
			Tile tile = new Tile(0, 0, (byte) 0, tileSize);
			TileSource tileSource = OpenStreetMapMapnik.INSTANCE;
			Job job = new DownloadJob(tile, tileSource);

			TileBitmap bitmap = GRAPHIC_FACTORY.createTileBitmap(tileSize, false);
			tileCache.put(job, bitmap);
			Assert.assertEquals(0, this.cacheDirectory.list().length);
			Assert.assertFalse(tileCache.containsKey(job));
			Assert.assertNull(tileCache.get(job));

			tileCache.destroy();
		}
	}

	@Test
	public void existingFilesTest() throws IOException {
		for (int tileSize : TILE_SIZES) {
			Assert.assertTrue(this.cacheDirectory.mkdirs());
			File file1 = new File(this.cacheDirectory, 1 + FileSystemTileCache.FILE_EXTENSION);
			File file2 = new File(this.cacheDirectory, 2 + FileSystemTileCache.FILE_EXTENSION);
			Assert.assertTrue(file1.createNewFile());
			Assert.assertTrue(file2.createNewFile());

			FileSystemTileCache tileCache = new FileSystemTileCache(1, this.cacheDirectory, GRAPHIC_FACTORY);
			Assert.assertEquals(2, this.cacheDirectory.list().length);

			Tile tile = new Tile(0, 0, (byte) 0, tileSize);
			TileSource tileSource = OpenStreetMapMapnik.INSTANCE;
			Job job = new DownloadJob(tile, tileSource);

			TileBitmap bitmap = GRAPHIC_FACTORY.createTileBitmap(tileSize, false);
			tileCache.put(job, bitmap);
			Assert.assertEquals(3, this.cacheDirectory.list().length);
			Assert.assertTrue(file1.exists());
			Assert.assertTrue(file2.exists());

			tileCache.destroy();
			Assert.assertFalse(this.cacheDirectory.exists());
		}
	}

	@Test
	public void fileSystemTileCacheTest() {
		Assert.assertFalse(this.cacheDirectory.exists());
		Assert.assertFalse(this.cacheDirectory2.exists());

		for (int tileSize : TILE_SIZES) {
			FileSystemTileCache tileCache1 = new FileSystemTileCache(1, this.cacheDirectory, GRAPHIC_FACTORY);
			FileSystemTileCache tileCache2 = new FileSystemTileCache(1, this.cacheDirectory2, GRAPHIC_FACTORY);
			Assert.assertEquals(1, tileCache1.getCapacity());
			Assert.assertEquals(1, tileCache2.getCapacity());
			Assert.assertTrue(this.cacheDirectory.exists());
			Assert.assertTrue(this.cacheDirectory2.exists());
			Assert.assertEquals(0, this.cacheDirectory.list().length);
			Assert.assertEquals(0, this.cacheDirectory2.list().length);

			Tile tile1 = new Tile(0, 0, (byte) 1, tileSize);
			Tile tile2 = new Tile(0, 1, (byte) 2, tileSize);
			TileSource tileSource = OpenStreetMapMapnik.INSTANCE;
			MapFile mapFile = MapFile.TEST_MAP_FILE;

			// Note that job1 and job2 refer to the same tile (X/Y/Z) but from different sources. Since tiles are
			// referred to ONLY by these three parameters, the following code -
			// tileCache.put(job1, bitmap1)
			// tileCache.containsKey(job2)
			// - will return true, but -
			// tileCache.get(job2)
			// - will return the bitmap for job1. This is, however, improper usage (a separate cache should be used for
			// each different combination of tile size and tile source configuration) and therefore omitted from the
			// test cases.

			Job job1 = new DownloadJob(tile1, tileSource);
			Job job2 = new RendererJob(tile1, mapFile, null, new DisplayModel(), 1, false, false);
			Job job3 = new DownloadJob(tile2, tileSource);

			Assert.assertFalse(tileCache1.containsKey(job1));
			Assert.assertFalse(tileCache1.containsKey(job2));
			Assert.assertFalse(tileCache1.containsKey(job3));
			Assert.assertNull(tileCache1.get(job1));
			Assert.assertNull(tileCache1.get(job2));
			Assert.assertNull(tileCache1.get(job3));
			Assert.assertFalse(tileCache2.containsKey(job1));
			Assert.assertFalse(tileCache2.containsKey(job2));
			Assert.assertFalse(tileCache2.containsKey(job3));
			Assert.assertNull(tileCache2.get(job1));
			Assert.assertNull(tileCache2.get(job2));
			Assert.assertNull(tileCache2.get(job3));

			TileBitmap bitmap1 = GRAPHIC_FACTORY.createTileBitmap(tileSize, false);
			tileCache1.put(job1, bitmap1);

			Assert.assertTrue(tileCache1.containsKey(job1));
			Assert.assertFalse(tileCache1.containsKey(job3));
			verifyEquals(bitmap1, tileCache1.get(job1));
			Assert.assertNull(tileCache1.get(job3));
			Assert.assertFalse(tileCache2.containsKey(job1));
			Assert.assertFalse(tileCache2.containsKey(job2));
			Assert.assertFalse(tileCache2.containsKey(job3));
			Assert.assertNull(tileCache2.get(job1));
			Assert.assertNull(tileCache2.get(job2));
			Assert.assertNull(tileCache2.get(job3));

			TileBitmap bitmap2 = GRAPHIC_FACTORY.createTileBitmap(tileSize, false);
			tileCache2.put(job2, bitmap2);

			Assert.assertTrue(tileCache1.containsKey(job1));
			Assert.assertFalse(tileCache1.containsKey(job3));
			verifyEquals(bitmap1, tileCache1.get(job1));
			Assert.assertNull(tileCache1.get(job3));
			Assert.assertTrue(tileCache2.containsKey(job2));
			Assert.assertFalse(tileCache2.containsKey(job3));
			verifyEquals(bitmap2, tileCache2.get(job2));
			Assert.assertNull(tileCache2.get(job3));

			TileBitmap bitmap3 = GRAPHIC_FACTORY.createTileBitmap(tileSize, false);
			tileCache1.put(job3, bitmap3);
			Assert.assertFalse(tileCache1.containsKey(job1));
			Assert.assertFalse(tileCache1.containsKey(job2));
			Assert.assertTrue(tileCache1.containsKey(job3));
			Assert.assertNull(tileCache1.get(job1));
			Assert.assertNull(tileCache1.get(job2));
			verifyEquals(bitmap3, tileCache1.get(job3));
			Assert.assertTrue(tileCache2.containsKey(job2));
			Assert.assertFalse(tileCache2.containsKey(job3));
			verifyEquals(bitmap2, tileCache2.get(job2));
			Assert.assertNull(tileCache2.get(job3));

			tileCache1.destroy();
			tileCache2.destroy();
			Assert.assertFalse(tileCache1.containsKey(job1));
			Assert.assertFalse(tileCache1.containsKey(job2));
			Assert.assertFalse(tileCache1.containsKey(job3));
			Assert.assertNull(tileCache1.get(job1));
			Assert.assertNull(tileCache1.get(job2));
			Assert.assertNull(tileCache1.get(job3));
			Assert.assertFalse(tileCache2.containsKey(job1));
			Assert.assertFalse(tileCache2.containsKey(job2));
			Assert.assertFalse(tileCache2.containsKey(job3));
			Assert.assertNull(tileCache2.get(job1));
			Assert.assertNull(tileCache2.get(job2));
			Assert.assertNull(tileCache2.get(job3));

			Assert.assertFalse(this.cacheDirectory.exists());
			Assert.assertFalse(this.cacheDirectory2.exists());
		}
	}

	@Test
	public void invalidConstructorTest() throws IOException {
		Assert.assertTrue(this.cacheDirectory.createNewFile());
		Assert.assertTrue(this.cacheDirectory.delete());
		Assert.assertTrue(this.cacheDirectory.mkdirs());
		verifyInvalidConstructor(-1, this.cacheDirectory);
	}

	@Test
	public void invalidPutTest() {
		for (int tileSize : TILE_SIZES) {
			TileCache tileCache = new FileSystemTileCache(1, this.cacheDirectory, GRAPHIC_FACTORY);
			verifyInvalidPut(tileCache, null, GRAPHIC_FACTORY.createTileBitmap(tileSize, false));
			verifyInvalidPut(tileCache, new DownloadJob(new Tile(0, 0, (byte) 0, tileSize),
					OpenStreetMapMapnik.INSTANCE), null);
		}
	}

	/**
	 * Tests specific behavior of a persistent tile cache.
	 * <p>
	 * This test creates a persistent cache instance with a cache size of 2, requests two tiles and destroys the
	 * instance. Then it creates another persistent cache instance in the same cache directory with a size of 3 and
	 * performs the following tests:
	 * <ul>
	 * <li>Request the tiles cached by the previous instance and verify its content.</li>
	 * <li>Request two new tiles (thus exceeding the cache size) and verify the oldest tile of the previous instance is
	 * no longer in the cache</li>
	 * <li>Purge the cache and verify none of the tiles are available from it any more</li>
	 * </ul>
	 */
	@Test
	public void persistentCacheTest() {
		int i;
		int tileSize = TILE_SIZES[0];
		TileSource tileSource = OpenStreetMapMapnik.INSTANCE;
		FileSystemTileCache tileCache1;
		FileSystemTileCache tileCache2;
		Tile[] tile = new Tile[4];
		Job[] job = new Job[4];
		TileBitmap[] bitmap = new TileBitmap[4];

		Assert.assertFalse(this.cacheDirectory.exists());

		tileCache1 = new FileSystemTileCache(2, this.cacheDirectory, GRAPHIC_FACTORY, false);

		Assert.assertEquals(2, tileCache1.getCapacity());
		Assert.assertTrue(this.cacheDirectory.exists());
		Assert.assertEquals(0, this.cacheDirectory.list().length);

		for (i = 0; i < 4; i++) {
			tile[i] = new Tile(i, 0, (byte) 4, tileSize);
			job[i] = new DownloadJob(tile[i], tileSource);
			bitmap[i] = GRAPHIC_FACTORY.createTileBitmap(tileSize, false);

			Assert.assertFalse(tileCache1.containsKey(job[i]));
			Assert.assertNull(tileCache1.get(job[i]));
		}

		for (i = 0; i < 2; i++) {
			tileCache1.put(job[i], bitmap[i]);
			Assert.assertTrue(tileCache1.containsKey(job[i]));
			verifyEquals(bitmap[i], tileCache1.get(job[i]));
		}

		for (i = 2; i < 4; i++) {
			Assert.assertFalse(tileCache1.containsKey(job[i]));
			Assert.assertNull(tileCache1.get(job[i]));
		}

		tileCache1.destroy();

		Assert.assertFalse(this.cacheDirectory.exists());

		tileCache2 = new FileSystemTileCache(3, this.cacheDirectory, GRAPHIC_FACTORY, true);

		Assert.assertEquals(3, tileCache2.getCapacity());
		Assert.assertTrue(this.cacheDirectory.exists());

		for (i = 0; i < 2; i++) {
			tileCache2.put(job[i], bitmap[i]);
			Assert.assertTrue(tileCache2.containsKey(job[i]));
			Assert.assertNotNull(tileCache2.get(job[i]));
			verifyEquals(bitmap[i], tileCache2.get(job[i]));
		}

		for (i = 2; i < 4; i++) {
			Assert.assertFalse(tileCache2.containsKey(job[i]));
			Assert.assertNull(tileCache2.get(job[i]));

			tileCache2.put(job[i], bitmap[i]);

			Assert.assertTrue(tileCache2.containsKey(job[i]));
			verifyEquals(bitmap[i], tileCache2.get(job[i]));
		}

		Assert.assertFalse(tileCache2.containsKey(job[0]));
		Assert.assertNull(tileCache2.get(job[0]));
		Assert.assertTrue(tileCache2.containsKey(job[1]));
		verifyEquals(bitmap[1], tileCache2.get(job[1]));

		tileCache2.purge();

		for (i = 0; i < 4; i++) {
			Assert.assertFalse(tileCache2.containsKey(job[i]));
			Assert.assertNull(tileCache2.get(job[i]));
		}

		tileCache2.destroy();
	}
}
