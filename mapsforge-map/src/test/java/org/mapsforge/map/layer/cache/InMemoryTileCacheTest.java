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
package org.mapsforge.map.layer.cache;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.awt.AwtGraphicFactory;
import org.mapsforge.map.layer.download.DownloadJob;
import org.mapsforge.map.layer.download.tilesource.OpenStreetMapMapnik;
import org.mapsforge.map.layer.download.tilesource.TileSource;
import org.mapsforge.map.layer.queue.Job;
import org.mapsforge.map.layer.renderer.RendererJob;
import org.mapsforge.map.rendertheme.InternalRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderTheme;

public class InMemoryTileCacheTest {
	private static final GraphicFactory GRAPHIC_FACTORY = AwtGraphicFactory.INSTANCE;

	private static void verifyInvalidCapacity(InMemoryTileCache inMemoryTileCache, int capacity) {
		try {
			inMemoryTileCache.setCapacity(capacity);
			Assert.fail("capacity: " + capacity);
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

	@Test
	public void inMemoryTileCacheTestTest() {
		TileCache tileCache = new InMemoryTileCache(1);
		Assert.assertEquals(1, tileCache.getCapacity());

		Tile tile1 = new Tile(1, 1, (byte) 1);
		Tile tile2 = new Tile(2, 2, (byte) 2);
		TileSource tileSource = OpenStreetMapMapnik.INSTANCE;
		File mapFile = new File("map.file");
		XmlRenderTheme xmlRenderTheme = InternalRenderTheme.OSMARENDER;

		Job job1 = new DownloadJob(tile1, tileSource);
		Job job2 = new RendererJob(tile2, mapFile, xmlRenderTheme, 1);

		Assert.assertFalse(tileCache.containsKey(job1));
		Assert.assertFalse(tileCache.containsKey(job2));
		Assert.assertNull(tileCache.get(job1));
		Assert.assertNull(tileCache.get(job2));

		TileBitmap bitmap1 = GRAPHIC_FACTORY.createTileBitmap();
		tileCache.put(job1, bitmap1);
		Assert.assertTrue(tileCache.containsKey(job1));
		Assert.assertFalse(tileCache.containsKey(job2));
		Assert.assertEquals(bitmap1, tileCache.get(job1));
		Assert.assertNull(tileCache.get(job2));

		TileBitmap bitmap2 = GRAPHIC_FACTORY.createTileBitmap();
		tileCache.put(job2, bitmap2);
		Assert.assertFalse(tileCache.containsKey(job1));
		Assert.assertTrue(tileCache.containsKey(job2));
		Assert.assertNull(tileCache.get(job1));
		Assert.assertEquals(bitmap2, tileCache.get(job2));

		tileCache.destroy();
		Assert.assertFalse(tileCache.containsKey(job1));
		Assert.assertFalse(tileCache.containsKey(job2));
		Assert.assertNull(tileCache.get(job1));
		Assert.assertNull(tileCache.get(job2));
	}

	@Test
	public void putTest() {
		TileCache tileCache = new InMemoryTileCache(0);
		verifyInvalidPut(tileCache, null, GRAPHIC_FACTORY.createTileBitmap());
		verifyInvalidPut(tileCache, new DownloadJob(new Tile(0, 0, (byte) 0), OpenStreetMapMapnik.INSTANCE), null);
	}

	@Test
	public void setCapacityTest() {
		InMemoryTileCache inMemoryTileCache = new InMemoryTileCache(0);
		Assert.assertEquals(0, inMemoryTileCache.getCapacity());

		Tile tile1 = new Tile(1, 1, (byte) 1);
		TileSource tileSource = OpenStreetMapMapnik.INSTANCE;
		Job job1 = new DownloadJob(tile1, tileSource);

		TileBitmap bitmap1 = GRAPHIC_FACTORY.createTileBitmap();
		inMemoryTileCache.put(job1, bitmap1);
		Assert.assertFalse(inMemoryTileCache.containsKey(job1));

		inMemoryTileCache.setCapacity(1);
		Assert.assertEquals(1, inMemoryTileCache.getCapacity());

		inMemoryTileCache.put(job1, bitmap1);
		Assert.assertTrue(inMemoryTileCache.containsKey(job1));

		Tile tile2 = new Tile(2, 2, (byte) 2);
		Job job2 = new DownloadJob(tile2, tileSource);

		inMemoryTileCache.put(job2, GRAPHIC_FACTORY.createTileBitmap());
		Assert.assertFalse(inMemoryTileCache.containsKey(job1));
		Assert.assertTrue(inMemoryTileCache.containsKey(job2));

		verifyInvalidCapacity(inMemoryTileCache, -1);
	}
}
