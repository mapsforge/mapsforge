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
package org.mapsforge.core.util;

import junit.framework.Assert;

import org.junit.Test;
import org.mapsforge.core.model.CoordinatesUtil;
import org.mapsforge.core.model.Tile;

public class MercatorProjectionTest {
	private static final double LATITUDE_DELTA = 0.00001;
	private static final int ZOOM_LEVEL_MAX = 25;
	private static final int ZOOM_LEVEL_MIN = 0;

	private static long getMapSize(byte zoomLevel) {
		return (long) Tile.TILE_SIZE << zoomLevel;
	}

	@Test
	public void coordinateToPixelTest() {
		for (byte zoomLevel = ZOOM_LEVEL_MIN; zoomLevel <= ZOOM_LEVEL_MAX; ++zoomLevel) {
			double pixelX = MercatorProjection.longitudeToPixelX(CoordinatesUtil.LONGITUDE_MIN, zoomLevel);
			double pixelY = MercatorProjection.latitudeToPixelY(MercatorProjection.LATITUDE_MAX, zoomLevel);
			Assert.assertEquals(0, pixelX, 0);
			Assert.assertEquals(0, pixelY, LATITUDE_DELTA);

			long mapSize = getMapSize(zoomLevel);
			long mapCenter = mapSize / 2;
			pixelX = MercatorProjection.longitudeToPixelX(0, zoomLevel);
			pixelY = MercatorProjection.latitudeToPixelY(0, zoomLevel);
			Assert.assertEquals(mapCenter, pixelX, 0);
			Assert.assertEquals(mapCenter, pixelY, LATITUDE_DELTA);

			pixelX = MercatorProjection.longitudeToPixelX(CoordinatesUtil.LONGITUDE_MAX, zoomLevel);
			pixelY = MercatorProjection.latitudeToPixelY(MercatorProjection.LATITUDE_MIN, zoomLevel);
			Assert.assertEquals(mapSize, pixelX, 0);
			Assert.assertEquals(mapSize, pixelY, LATITUDE_DELTA);
		}
	}

	@Test
	public void getMapSizeTest() {
		Assert.assertEquals(Tile.TILE_SIZE, MercatorProjection.getMapSize((byte) 0));
		Assert.assertEquals(Tile.TILE_SIZE * 2, MercatorProjection.getMapSize((byte) 1));
		Assert.assertEquals(Tile.TILE_SIZE * 4, MercatorProjection.getMapSize((byte) 2));
		Assert.assertEquals(Tile.TILE_SIZE * 8, MercatorProjection.getMapSize((byte) 3));
		Assert.assertEquals(Tile.TILE_SIZE * 4194304L, MercatorProjection.getMapSize((byte) 22));
		Assert.assertEquals(Tile.TILE_SIZE * 8388608L, MercatorProjection.getMapSize((byte) 23));
		Assert.assertEquals(Tile.TILE_SIZE * 16777216L, MercatorProjection.getMapSize((byte) 24));

		try {
			MercatorProjection.getMapSize((byte) -1);
			Assert.fail();
		} catch (IllegalArgumentException e) {
			Assert.assertTrue(true);
		}
	}

	@Test
	public void pixelToCoordinateTest() {
		for (byte zoomLevel = ZOOM_LEVEL_MIN; zoomLevel <= ZOOM_LEVEL_MAX; ++zoomLevel) {
			double longitude = MercatorProjection.pixelXToLongitude(0, zoomLevel);
			double latitude = MercatorProjection.pixelYToLatitude(0, zoomLevel);
			Assert.assertEquals(CoordinatesUtil.LONGITUDE_MIN, longitude, 0);
			Assert.assertEquals(MercatorProjection.LATITUDE_MAX, latitude, LATITUDE_DELTA);

			long mapSize = getMapSize(zoomLevel);
			long mapCenter = mapSize / 2;
			longitude = MercatorProjection.pixelXToLongitude(mapCenter, zoomLevel);
			latitude = MercatorProjection.pixelYToLatitude(mapCenter, zoomLevel);
			Assert.assertEquals(0, longitude, 0);
			Assert.assertEquals(0, latitude, LATITUDE_DELTA);

			longitude = MercatorProjection.pixelXToLongitude(mapSize, zoomLevel);
			latitude = MercatorProjection.pixelYToLatitude(mapSize, zoomLevel);
			Assert.assertEquals(CoordinatesUtil.LONGITUDE_MAX, longitude, 0);
			Assert.assertEquals(MercatorProjection.LATITUDE_MIN, latitude, LATITUDE_DELTA);
		}
	}

	@Test
	public void tileToCoordinateTest() {
		for (byte zoomLevel = ZOOM_LEVEL_MIN; zoomLevel <= ZOOM_LEVEL_MAX; ++zoomLevel) {
			double longitude = MercatorProjection.tileXToLongitude(0, zoomLevel);
			double latitude = MercatorProjection.tileYToLatitude(0, zoomLevel);
			Assert.assertEquals(CoordinatesUtil.LONGITUDE_MIN, longitude, 0);
			Assert.assertEquals(MercatorProjection.LATITUDE_MAX, latitude, LATITUDE_DELTA);

			long mapSize = getMapSize(zoomLevel);
			long mapSizeTile = mapSize / Tile.TILE_SIZE;
			longitude = MercatorProjection.tileXToLongitude(mapSizeTile, zoomLevel);
			latitude = MercatorProjection.tileYToLatitude(mapSizeTile, zoomLevel);
			Assert.assertEquals(CoordinatesUtil.LONGITUDE_MAX, longitude, 0);
			Assert.assertEquals(MercatorProjection.LATITUDE_MIN, latitude, LATITUDE_DELTA);
		}
	}
}
