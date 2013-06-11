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

import java.io.File;

import org.junit.Assert;
import org.junit.Test;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Tag;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.reader.header.FileOpenResult;
import org.mapsforge.map.reader.header.MapFileInfo;

public class MapDatabaseWithDataTest {
	private static final File MAP_FILE = new File("src/test/resources/with_data/output.map");
	private static final byte ZOOM_LEVEL_MAX = 11;
	private static final int ZOOM_LEVEL_MIN = 6;

	private static void assertLatLongsEquals(LatLong[][] latLongs1, LatLong[][] latLongs2) {
		Assert.assertEquals(latLongs1.length, latLongs2.length);

		for (int i = 0; i < latLongs1.length; ++i) {
			Assert.assertEquals(latLongs1[i].length, latLongs2[i].length);

			for (int j = 0; j < latLongs1[i].length; ++j) {
				LatLong latLong1 = latLongs1[i][j];
				LatLong latLong2 = latLongs2[i][j];

				Assert.assertEquals(latLong1.latitude, latLong2.latitude, 0.000001);
				Assert.assertEquals(latLong1.longitude, latLong2.longitude, 0.000001);
			}
		}
	}

	private static void checkPointOfInterest(PointOfInterest pointOfInterest) {
		Assert.assertEquals(7, pointOfInterest.layer);
		Assert.assertEquals(0.04, pointOfInterest.position.latitude, 0.000001);
		Assert.assertEquals(0.08, pointOfInterest.position.longitude, 0);
		Assert.assertEquals(4, pointOfInterest.tags.size());
		Assert.assertTrue(pointOfInterest.tags.contains(new Tag("place=country")));
		Assert.assertTrue(pointOfInterest.tags.contains(new Tag("name=АБВГДЕЖЗ")));
		Assert.assertTrue(pointOfInterest.tags.contains(new Tag("addr:housenumber=абвгдежз")));
		Assert.assertTrue(pointOfInterest.tags.contains(new Tag("ele=25")));
	}

	private static void checkWay(Way way) {
		Assert.assertEquals(4, way.layer);
		Assert.assertNull(way.labelPosition);

		LatLong latLong1 = new LatLong(0.00, 0.00);
		LatLong latLong2 = new LatLong(0.04, 0.08);
		LatLong latLong3 = new LatLong(0.08, 0.00);
		LatLong[][] latLongsExpected = new LatLong[][] { { latLong1, latLong2, latLong3 } };

		assertLatLongsEquals(latLongsExpected, way.latLongs);
		Assert.assertEquals(3, way.tags.size());
		Assert.assertTrue(way.tags.contains(new Tag("highway=motorway")));
		Assert.assertTrue(way.tags.contains(new Tag("name=ÄÖÜ")));
		Assert.assertTrue(way.tags.contains(new Tag("ref=äöü")));
	}

	@Test
	public void executeQueryTest() {
		MapDatabase mapDatabase = new MapDatabase();
		FileOpenResult fileOpenResult = mapDatabase.openFile(MAP_FILE);
		Assert.assertTrue(mapDatabase.hasOpenFile());
		Assert.assertTrue(fileOpenResult.getErrorMessage(), fileOpenResult.isSuccess());

		MapFileInfo mapFileInfo = mapDatabase.getMapFileInfo();
		Assert.assertTrue(mapFileInfo.debugFile);

		for (byte zoomLevel = ZOOM_LEVEL_MIN; zoomLevel <= ZOOM_LEVEL_MAX; ++zoomLevel) {
			long tileX = MercatorProjection.longitudeToTileX(0.04, zoomLevel);
			long tileY = MercatorProjection.latitudeToTileY(0.04, zoomLevel);
			Tile tile = new Tile(tileX, tileY, zoomLevel);

			MapReadResult mapReadResult = mapDatabase.readMapData(tile);

			Assert.assertEquals(1, mapReadResult.pointOfInterests.size());
			Assert.assertEquals(1, mapReadResult.ways.size());

			checkPointOfInterest(mapReadResult.pointOfInterests.get(0));
			checkWay(mapReadResult.ways.get(0));
		}

		mapDatabase.closeFile();
		Assert.assertFalse(mapDatabase.hasOpenFile());
	}
}
