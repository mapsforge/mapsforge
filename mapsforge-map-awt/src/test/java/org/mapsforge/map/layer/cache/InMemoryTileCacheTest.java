/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
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
import org.mapsforge.map.awt.graphics.AwtGraphicFactory;
import org.mapsforge.map.layer.download.DownloadJob;
import org.mapsforge.map.layer.download.tilesource.OpenStreetMapMapnik;
import org.mapsforge.map.layer.download.tilesource.TileSource;
import org.mapsforge.map.layer.queue.Job;
import org.mapsforge.map.layer.renderer.RendererJob;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.reader.MapFile;

public class InMemoryTileCacheTest {
    private static final GraphicFactory GRAPHIC_FACTORY = AwtGraphicFactory.INSTANCE;
    private static final int[] TILE_SIZES = {256, 128, 376, 512, 100};

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
        for (int tileSize : TILE_SIZES) {
            TileCache tileCache = new InMemoryTileCache(1);
            Assert.assertEquals(1, tileCache.getCapacity());

            Tile tile1 = new Tile(1, 1, (byte) 1, tileSize);
            Tile tile2 = new Tile(2, 2, (byte) 2, tileSize);
            TileSource tileSource = OpenStreetMapMapnik.INSTANCE;
            MapFile mapFile = MapFile.TEST_MAP_FILE;

            Job job1 = new DownloadJob(tile1, tileSource);
            Job job2 = new RendererJob(tile2, mapFile, null, new DisplayModel(), 1, false, false);

            Assert.assertFalse(tileCache.containsKey(job1));
            Assert.assertFalse(tileCache.containsKey(job2));
            Assert.assertNull(tileCache.get(job1));
            Assert.assertNull(tileCache.get(job2));

            TileBitmap bitmap1 = GRAPHIC_FACTORY.createTileBitmap(tileSize, true);
            tileCache.put(job1, bitmap1);
            Assert.assertTrue(tileCache.containsKey(job1));
            Assert.assertFalse(tileCache.containsKey(job2));
            Assert.assertEquals(bitmap1, tileCache.get(job1));
            Assert.assertNull(tileCache.get(job2));

            TileBitmap bitmap2 = GRAPHIC_FACTORY.createTileBitmap(tileSize, true);
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
    }

    @Test
    public void putTest() {
        for (int tileSize : TILE_SIZES) {
            TileCache tileCache = new InMemoryTileCache(0);
            verifyInvalidPut(tileCache, null, GRAPHIC_FACTORY.createTileBitmap(tileSize, true));
            verifyInvalidPut(tileCache, new DownloadJob(new Tile(0, 0, (byte) 0, tileSize),
                    OpenStreetMapMapnik.INSTANCE), null);
        }
    }

    @Test
    public void setCapacityTest() {
        for (int tileSize : TILE_SIZES) {
            InMemoryTileCache inMemoryTileCache = new InMemoryTileCache(0);
            Assert.assertEquals(0, inMemoryTileCache.getCapacity());

            Tile tile1 = new Tile(1, 1, (byte) 1, tileSize);
            TileSource tileSource = OpenStreetMapMapnik.INSTANCE;
            Job job1 = new DownloadJob(tile1, tileSource);

            TileBitmap bitmap1 = GRAPHIC_FACTORY.createTileBitmap(tileSize, true);
            inMemoryTileCache.put(job1, bitmap1);
            Assert.assertFalse(inMemoryTileCache.containsKey(job1));

            inMemoryTileCache.setCapacity(1);
            Assert.assertEquals(1, inMemoryTileCache.getCapacity());

            inMemoryTileCache.put(job1, bitmap1);
            Assert.assertTrue(inMemoryTileCache.containsKey(job1));

            Tile tile2 = new Tile(2, 2, (byte) 2, tileSize);
            Job job2 = new DownloadJob(tile2, tileSource);

            inMemoryTileCache.put(job2, GRAPHIC_FACTORY.createTileBitmap(tileSize, true));
            Assert.assertFalse(inMemoryTileCache.containsKey(job1));
            Assert.assertTrue(inMemoryTileCache.containsKey(job2));

            verifyInvalidCapacity(inMemoryTileCache, -1);
        }
    }
}
