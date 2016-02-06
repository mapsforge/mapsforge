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
package org.mapsforge.map.layer.download;


import org.junit.Assert;
import org.junit.Test;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.TestUtils;
import org.mapsforge.map.layer.download.tilesource.OpenCycleMap;
import org.mapsforge.map.layer.download.tilesource.OpenStreetMapMapnik;
import org.mapsforge.map.layer.download.tilesource.TileSource;
import org.mapsforge.map.layer.renderer.RendererJob;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.reader.MapFile;

public class DownloadJobTest {
    private static final int TILE_SIZE = 256;

    private static DownloadJob createDownloadJob(Tile tile, TileSource tileSource) {
        return new DownloadJob(tile, tileSource);
    }

    private static void verifyInvalidConstructor(Tile tile, TileSource tileSource) {
        try {
            createDownloadJob(tile, tileSource);
            Assert.fail("tile: " + tile + ", tileSource: " + tileSource);
        } catch (NullPointerException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void downloadJobTest() {
        Tile tile = new Tile(0, 0, (byte) 0, TILE_SIZE);
        TileSource tileSource = OpenStreetMapMapnik.INSTANCE;

        DownloadJob downloadJob = createDownloadJob(tile, tileSource);
        Assert.assertEquals(tile, downloadJob.tile);
        Assert.assertEquals(tileSource, downloadJob.tileSource);

        verifyInvalidConstructor(tile, null);
    }

    @Test
    public void equalsTest() {
        Tile tile = new Tile(0, 0, (byte) 0, TILE_SIZE);
        DownloadJob downloadJob1 = new DownloadJob(tile, OpenStreetMapMapnik.INSTANCE);
        DownloadJob downloadJob2 = new DownloadJob(tile, OpenStreetMapMapnik.INSTANCE);
        DownloadJob downloadJob3 = new DownloadJob(tile, OpenCycleMap.INSTANCE);

        TestUtils.equalsTest(downloadJob1, downloadJob2);

        Assert.assertNotEquals(downloadJob1, downloadJob3);
        Assert.assertNotEquals(downloadJob3, downloadJob1);
        Assert.assertNotEquals(downloadJob1, new Object());

        MapFile mapFile = MapFile.TEST_MAP_FILE;
        Assert.assertNotEquals(downloadJob1, new RendererJob(tile, mapFile, null, new DisplayModel(), 1,
                false, false));
    }
}
