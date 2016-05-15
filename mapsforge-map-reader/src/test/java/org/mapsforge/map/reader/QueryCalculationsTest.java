/*
 * Copyright 2015 Ludwig M Brinckmann
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
import org.mapsforge.map.reader.header.SubFileParameter;

public class QueryCalculationsTest {
    private static final MapFile MAP_FILE_SINGLE_DELTA = new MapFile("src/test/resources/single_delta_encoding/output.map", null);
    private static final byte ZOOM_LEVEL_MAX = 25;
    private static final int ZOOM_LEVEL_MIN = 0;

    @Test
    public void calculationsTestTest() {
        MapFile mapFile = MAP_FILE_SINGLE_DELTA;

        for (byte zoomLevel = ZOOM_LEVEL_MIN; zoomLevel <= ZOOM_LEVEL_MAX; ++zoomLevel) {
            QueryParameters single = new QueryParameters();
            QueryParameters multi = new QueryParameters();
            SubFileParameter subFileParameter = mapFile.getMapFileHeader().getSubFileParameter(single.queryZoomLevel);
            Tile tile = new Tile(zoomLevel, zoomLevel, zoomLevel, 256);
            single.calculateBaseTiles(tile, subFileParameter);
            multi.calculateBaseTiles(tile, tile, subFileParameter);
            Assert.assertEquals(single, multi);
        }

        mapFile.close();
    }
}
