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
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.datastore.MapReadResult;
import org.mapsforge.map.datastore.Way;

final class EncodingTest {
    private static final byte ZOOM_LEVEL = 8;

    static void runTest(MapFile mapFile) {

        int tileX = MercatorProjection.longitudeToTileX(0, ZOOM_LEVEL);
        int tileY = MercatorProjection.latitudeToTileY(0, ZOOM_LEVEL);
        Tile tile = new Tile(tileX, tileY, ZOOM_LEVEL, 256);

        // read all labels data, ways should be empty as in the example the way does not carry a name tag
        MapReadResult mapReadResult = mapFile.readNamedItems(tile);
        Assert.assertFalse(mapReadResult == null);
        Assert.assertTrue(mapReadResult.pointOfInterests.isEmpty());
        Assert.assertTrue(mapReadResult.ways.isEmpty());

        // read only poi data, ways should be empty
        mapReadResult = mapFile.readPoiData(tile);
        Assert.assertFalse(mapReadResult == null);
        Assert.assertTrue(mapReadResult.pointOfInterests.isEmpty());
        Assert.assertTrue(mapReadResult.ways.isEmpty());

        mapReadResult = mapFile.readMapData(tile);

        Assert.assertTrue(mapReadResult.pointOfInterests.isEmpty());
        Assert.assertEquals(1, mapReadResult.ways.size());

        LatLong latLong1 = new LatLong(0.0, 0.0);
        LatLong latLong2 = new LatLong(0.0, 0.1);
        LatLong latLong3 = new LatLong(-0.1, 0.1);
        LatLong latLong4 = new LatLong(-0.1, 0.0);
        LatLong[][] latLongsExpected = new LatLong[][]{{latLong1, latLong2, latLong3, latLong4, latLong1}};

        Way way = mapReadResult.ways.get(0);
        Assert.assertArrayEquals(latLongsExpected, way.latLongs);

        mapFile.close();
    }

    private EncodingTest() {
        throw new IllegalStateException();
    }
}
