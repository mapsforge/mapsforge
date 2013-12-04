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

public class TwoLevelTileCacheTest {
	private static final GraphicFactory GRAPHIC_FACTORY = AwtGraphicFactory.INSTANCE;

	@Test
	public void twoLevelTileCacheTest() {
		TileCache tileCache1 = new InMemoryTileCache(1);
		TileCache tileCache2 = new InMemoryTileCache(1);
		TwoLevelTileCache twoLevelTileCache = new TwoLevelTileCache(tileCache1, tileCache2);

		Assert.assertEquals(1, twoLevelTileCache.getCapacity());

		Tile tile = new Tile(0, 0, (byte) 0);
		TileSource tileSource = OpenStreetMapMapnik.INSTANCE;
		Job job = new DownloadJob(tile, tileSource);
		Assert.assertFalse(tileCache1.containsKey(job));
		Assert.assertFalse(tileCache2.containsKey(job));
		Assert.assertFalse(twoLevelTileCache.containsKey(job));

		TileBitmap bitmap = GRAPHIC_FACTORY.createTileBitmap();
		twoLevelTileCache.put(job, bitmap);
		//Assert.assertTrue(tileCache1.containsKey(job));
		Assert.assertTrue(tileCache2.containsKey(job));
		Assert.assertTrue(twoLevelTileCache.containsKey(job));
		Assert.assertEquals(bitmap, twoLevelTileCache.get(job));

		Assert.assertTrue(tileCache1.containsKey(job));
		Assert.assertTrue(tileCache2.containsKey(job));
		Assert.assertTrue(twoLevelTileCache.containsKey(job));
		Assert.assertEquals(bitmap, twoLevelTileCache.get(job));

		twoLevelTileCache.destroy();
		Assert.assertFalse(tileCache1.containsKey(job));
		Assert.assertFalse(tileCache2.containsKey(job));
		Assert.assertFalse(twoLevelTileCache.containsKey(job));
		Assert.assertNull(twoLevelTileCache.get(job));
	}
}
