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
package org.mapsforge.map.reader;

import java.io.File;

import org.junit.Assert;
import org.mapsforge.core.model.GeoPoint;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.reader.header.FileOpenResult;

final class EncodingTest {
	private static final byte ZOOM_LEVEL = 8;

	static void runTest(File mapFile) {
		MapDatabase mapDatabase = new MapDatabase();
		FileOpenResult fileOpenResult = mapDatabase.openFile(mapFile);
		Assert.assertTrue(fileOpenResult.getErrorMessage(), fileOpenResult.isSuccess());

		long tileX = MercatorProjection.longitudeToTileX(0, ZOOM_LEVEL);
		long tileY = MercatorProjection.latitudeToTileY(0, ZOOM_LEVEL);
		Tile tile = new Tile(tileX, tileY, ZOOM_LEVEL);

		MapReadResult mapReadResult = mapDatabase.readMapData(tile);
		mapDatabase.closeFile();

		Assert.assertTrue(mapReadResult.pointOfInterests.isEmpty());
		Assert.assertEquals(1, mapReadResult.ways.size());

		GeoPoint geoPoint1 = new GeoPoint(0.0, 0.0);
		GeoPoint geoPoint2 = new GeoPoint(0.0, 0.1);
		GeoPoint geoPoint3 = new GeoPoint(-0.1, 0.1);
		GeoPoint geoPoint4 = new GeoPoint(-0.1, 0.0);
		GeoPoint[][] geoPointsExpected = new GeoPoint[][] { { geoPoint1, geoPoint2, geoPoint3, geoPoint4, geoPoint1 } };

		Way way = mapReadResult.ways.get(0);
		Assert.assertArrayEquals(geoPointsExpected, way.geoPoints);
	}

	private EncodingTest() {
		throw new IllegalStateException();
	}
}
