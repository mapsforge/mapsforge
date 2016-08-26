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
package org.mapsforge.map.reader;

import org.junit.Assert;
import org.junit.Test;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.datastore.MapReadResult;

import java.io.File;

public class MapFileEmptyTest {
    private static final File MAP_FILE = new File("src/test/resources/empty/output.map");
    private static final byte ZOOM_LEVEL_MAX = 25;

    @Test
    public void executeQueryTest() {
        MapFile mapFile = new MapFile(MAP_FILE);

        for (byte zoomLevel = 0; zoomLevel <= ZOOM_LEVEL_MAX; ++zoomLevel) {
            int tileX = MercatorProjection.longitudeToTileX(1, zoomLevel);
            int tileY = MercatorProjection.latitudeToTileY(1, zoomLevel);
            Tile tile = new Tile(tileX, tileY, zoomLevel, 256);

            MapReadResult mapReadResult = mapFile.readMapData(tile);

            Assert.assertTrue(mapReadResult.pointOfInterests.isEmpty());
            Assert.assertTrue(mapReadResult.ways.isEmpty());
        }

        mapFile.close();
    }
}
