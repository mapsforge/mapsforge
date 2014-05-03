/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright © 2014 Ludwig M Brinckmann
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
import org.mapsforge.map.rendertheme.InternalRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderTheme;

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

	private final File cacheDirectory = new File(TMP_DIR, getClass().getSimpleName() + System.currentTimeMillis());

	@After
	public void afterTest() {
		if (this.cacheDirectory.exists() && !this.cacheDirectory.delete()) {
			throw new IllegalStateException("could not delete cache directory: " + this.cacheDirectory);
		}
	}

	@Test
	public void capacityZeroTest() {
		for (int tileSize : TILE_SIZES) {
			TileCache tileCache = new FileSystemTileCache(0, this.cacheDirectory, GRAPHIC_FACTORY);
			Tile tile = new Tile(0, 0, (byte) 0);
			TileSource tileSource = OpenStreetMapMapnik.INSTANCE;
			Job job = new DownloadJob(tile, tileSize, tileSource);

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
		Assert.assertTrue(this.cacheDirectory.mkdirs());

		for (int tileSize : TILE_SIZES) {
			Assert.assertFalse(this.cacheDirectory.mkdirs());
			File file1 = new File(this.cacheDirectory, 1 + FileSystemTileCache.FILE_EXTENSION);
			File file2 = new File(this.cacheDirectory, 2 + FileSystemTileCache.FILE_EXTENSION);
			Assert.assertTrue(file1.createNewFile());
			Assert.assertTrue(file2.createNewFile());

			TileCache tileCache = new FileSystemTileCache(1, this.cacheDirectory, GRAPHIC_FACTORY);
			Assert.assertEquals(2, this.cacheDirectory.list().length);

			Tile tile = new Tile(0, 0, (byte) 0);
			TileSource tileSource = OpenStreetMapMapnik.INSTANCE;
			Job job = new DownloadJob(tile, tileSize, tileSource);

			TileBitmap bitmap = GRAPHIC_FACTORY.createTileBitmap(tileSize, false);
			tileCache.put(job, bitmap);
			Assert.assertEquals(3, this.cacheDirectory.list().length);
			Assert.assertTrue(file1.exists());
			Assert.assertTrue(file2.exists());
			// Assert.assertTrue(new File(this.cacheDirectory, tile.hashCode() +
			// FileSystemTileCache.FILE_EXTENSION).exists());

			// verifyEquals(bitmap, tileCache.get(job));

			tileCache.destroy();
			Assert.assertEquals(0, this.cacheDirectory.list().length);
		}
	}

	@Test
	public void fileSystemTileCacheTest() {

		Assert.assertFalse(this.cacheDirectory.exists());

		for (int tileSize : TILE_SIZES) {
			TileCache tileCache = new FileSystemTileCache(1, this.cacheDirectory, GRAPHIC_FACTORY);
			Assert.assertEquals(1, tileCache.getCapacity());
			Assert.assertTrue(this.cacheDirectory.exists());
			Assert.assertEquals(0, this.cacheDirectory.list().length);

			Tile tile = new Tile(0, 0, (byte) 1);
			TileSource tileSource = OpenStreetMapMapnik.INSTANCE;
			File mapFile = new File("map.file");
			XmlRenderTheme xmlRenderTheme = InternalRenderTheme.OSMARENDER;

			Job job1 = new DownloadJob(tile, tileSize, tileSource);
			Job job2 = new RendererJob(tile, mapFile, xmlRenderTheme, new DisplayModel(), 1, false);

			Assert.assertFalse(tileCache.containsKey(job1));
			Assert.assertFalse(tileCache.containsKey(job2));
			Assert.assertNull(tileCache.get(job1));
			Assert.assertNull(tileCache.get(job2));

			TileBitmap bitmap1 = GRAPHIC_FACTORY.createTileBitmap(tileSize, false);
			tileCache.put(job1, bitmap1);
			Assert.assertTrue(tileCache.containsKey(job1));
			Assert.assertFalse(tileCache.containsKey(job2));
			verifyEquals(bitmap1, tileCache.get(job1));
			Assert.assertNull(tileCache.get(job2));

			TileBitmap bitmap2 = GRAPHIC_FACTORY.createTileBitmap(tileSize, false);
			tileCache.put(job2, bitmap2);
			Assert.assertFalse(tileCache.containsKey(job1));
			Assert.assertTrue(tileCache.containsKey(job2));
			Assert.assertNull(tileCache.get(job1));
			verifyEquals(bitmap2, tileCache.get(job2));

			tileCache.destroy();
			Assert.assertFalse(tileCache.containsKey(job1));
			Assert.assertFalse(tileCache.containsKey(job2));
			Assert.assertNull(tileCache.get(job1));
			Assert.assertNull(tileCache.get(job2));

			Assert.assertTrue(this.cacheDirectory.exists());
			Assert.assertEquals(0, this.cacheDirectory.list().length);
		}
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
		for (int tileSize : TILE_SIZES) {
			TileCache tileCache = new FileSystemTileCache(1, this.cacheDirectory, GRAPHIC_FACTORY);
			verifyInvalidPut(tileCache, null, GRAPHIC_FACTORY.createTileBitmap(tileSize, false));
			verifyInvalidPut(tileCache, new DownloadJob(new Tile(0, 0, (byte) 0), tileSize,
					OpenStreetMapMapnik.INSTANCE), null);
		}
	}
}
