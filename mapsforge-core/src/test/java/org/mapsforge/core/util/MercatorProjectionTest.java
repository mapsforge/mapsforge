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
package org.mapsforge.core.util;

import org.junit.Assert;
import org.junit.Test;
import org.mapsforge.core.model.Tile;

public class MercatorProjectionTest {
	private static final int ZOOM_LEVEL_MAX = 30;
	private static final int ZOOM_LEVEL_MIN = 0;

	private static void verifyInvalidGetMapSize(byte zoomLevel) {
		try {
			MercatorProjection.getMapSize(zoomLevel);
			Assert.fail("zoomLevel: " + zoomLevel);
		} catch (IllegalArgumentException e) {
			Assert.assertTrue(true);
		}
	}

	private static void verifyInvalidPixelXToLongitude(double pixelX, byte zoomLevel) {
		try {
			MercatorProjection.pixelXToLongitude(pixelX, zoomLevel);
			Assert.fail("pixelX: " + pixelX + ", zoomLevel: " + zoomLevel);
		} catch (IllegalArgumentException e) {
			Assert.assertTrue(true);
		}
	}

	private static void verifyInvalidPixelYToLatitude(double pixelY, byte zoomLevel) {
		try {
			MercatorProjection.pixelYToLatitude(pixelY, zoomLevel);
			Assert.fail("pixelY: " + pixelY + ", zoomLevel: " + zoomLevel);
		} catch (IllegalArgumentException e) {
			Assert.assertTrue(true);
		}
	}

	@Test
	public void getMapSizeTest() {
		for (byte zoomLevel = ZOOM_LEVEL_MIN; zoomLevel <= ZOOM_LEVEL_MAX; ++zoomLevel) {
			long factor = Math.round(Math.pow(2, zoomLevel));
			Assert.assertEquals(Tile.TILE_SIZE * factor, MercatorProjection.getMapSize(zoomLevel));
		}

		verifyInvalidGetMapSize((byte) -1);
	}

	@Test
	public void latitudeToPixelYTest() {
		for (byte zoomLevel = ZOOM_LEVEL_MIN; zoomLevel <= ZOOM_LEVEL_MAX; ++zoomLevel) {
			double pixelY = MercatorProjection.latitudeToPixelY(MercatorProjection.LATITUDE_MAX, zoomLevel);
			Assert.assertEquals(0, pixelY, 0);

			long mapSize = MercatorProjection.getMapSize(zoomLevel);
			pixelY = MercatorProjection.latitudeToPixelY(0, zoomLevel);
			Assert.assertEquals(mapSize / 2, pixelY, 0);

			pixelY = MercatorProjection.latitudeToPixelY(MercatorProjection.LATITUDE_MIN, zoomLevel);
			Assert.assertEquals(mapSize, pixelY, 0);
		}
	}

	@Test
	public void latitudeToTileYTest() {
		for (byte zoomLevel = ZOOM_LEVEL_MIN; zoomLevel <= ZOOM_LEVEL_MAX; ++zoomLevel) {
			long tileY = MercatorProjection.latitudeToTileY(MercatorProjection.LATITUDE_MAX, zoomLevel);
			Assert.assertEquals(0, tileY);

			tileY = MercatorProjection.latitudeToTileY(MercatorProjection.LATITUDE_MIN, zoomLevel);
			Assert.assertEquals(Tile.getMaxTileNumber(zoomLevel), tileY);
		}
	}

	@Test
	public void longitudeToPixelXTest() {
		for (byte zoomLevel = ZOOM_LEVEL_MIN; zoomLevel <= ZOOM_LEVEL_MAX; ++zoomLevel) {
			double pixelX = MercatorProjection.longitudeToPixelX(LatLongUtils.LONGITUDE_MIN, zoomLevel);
			Assert.assertEquals(0, pixelX, 0);

			long mapSize = MercatorProjection.getMapSize(zoomLevel);
			pixelX = MercatorProjection.longitudeToPixelX(0, zoomLevel);
			Assert.assertEquals(mapSize / 2, pixelX, 0);

			pixelX = MercatorProjection.longitudeToPixelX(LatLongUtils.LONGITUDE_MAX, zoomLevel);
			Assert.assertEquals(mapSize, pixelX, 0);
		}
	}

	@Test
	public void longitudeToTileXTest() {
		for (byte zoomLevel = ZOOM_LEVEL_MIN; zoomLevel <= ZOOM_LEVEL_MAX; ++zoomLevel) {
			long tileX = MercatorProjection.longitudeToTileX(LatLongUtils.LONGITUDE_MIN, zoomLevel);
			Assert.assertEquals(0, tileX);

			tileX = MercatorProjection.longitudeToTileX(LatLongUtils.LONGITUDE_MAX, zoomLevel);
			Assert.assertEquals(Tile.getMaxTileNumber(zoomLevel), tileX);
		}
	}

	@Test
	public void pixelXToLongitudeTest() {
		for (byte zoomLevel = ZOOM_LEVEL_MIN; zoomLevel <= ZOOM_LEVEL_MAX; ++zoomLevel) {
			double longitude = MercatorProjection.pixelXToLongitude(0, zoomLevel);
			Assert.assertEquals(LatLongUtils.LONGITUDE_MIN, longitude, 0);

			long mapSize = MercatorProjection.getMapSize(zoomLevel);
			longitude = MercatorProjection.pixelXToLongitude(mapSize / 2, zoomLevel);
			Assert.assertEquals(0, longitude, 0);

			longitude = MercatorProjection.pixelXToLongitude(mapSize, zoomLevel);
			Assert.assertEquals(LatLongUtils.LONGITUDE_MAX, longitude, 0);
		}

		verifyInvalidPixelXToLongitude(-1, (byte) 0);
		verifyInvalidPixelXToLongitude(Tile.TILE_SIZE + 1, (byte) 0);
	}

	@Test
	public void pixelXToTileXTest() {
		for (byte zoomLevel = ZOOM_LEVEL_MIN; zoomLevel <= ZOOM_LEVEL_MAX; ++zoomLevel) {
			Assert.assertEquals(0, MercatorProjection.pixelXToTileX(0, zoomLevel));
		}
	}

	@Test
	public void pixelYToLatitudeTest() {
		for (byte zoomLevel = ZOOM_LEVEL_MIN; zoomLevel <= ZOOM_LEVEL_MAX; ++zoomLevel) {
			double latitude = MercatorProjection.pixelYToLatitude(0, zoomLevel);
			Assert.assertEquals(MercatorProjection.LATITUDE_MAX, latitude, 0);

			long mapSize = MercatorProjection.getMapSize(zoomLevel);
			latitude = MercatorProjection.pixelYToLatitude(mapSize / 2, zoomLevel);
			Assert.assertEquals(0, latitude, 0);

			latitude = MercatorProjection.pixelYToLatitude(mapSize, zoomLevel);
			Assert.assertEquals(MercatorProjection.LATITUDE_MIN, latitude, 0);
		}

		verifyInvalidPixelYToLatitude(-1, (byte) 0);
		verifyInvalidPixelYToLatitude(Tile.TILE_SIZE + 1, (byte) 0);
	}

	@Test
	public void pixelYToTileYTest() {
		for (byte zoomLevel = ZOOM_LEVEL_MIN; zoomLevel <= ZOOM_LEVEL_MAX; ++zoomLevel) {
			Assert.assertEquals(0, MercatorProjection.pixelYToTileY(0, zoomLevel));
		}
	}

	@Test
	public void tileToPixelTest() {
		Assert.assertEquals(0, MercatorProjection.tileToPixel(0));
		Assert.assertEquals(Tile.TILE_SIZE, MercatorProjection.tileToPixel(1));
		Assert.assertEquals(Tile.TILE_SIZE * 2, MercatorProjection.tileToPixel(2));
	}

	@Test
	public void tileXToLongitudeTest() {
		for (byte zoomLevel = ZOOM_LEVEL_MIN; zoomLevel <= ZOOM_LEVEL_MAX; ++zoomLevel) {
			double longitude = MercatorProjection.tileXToLongitude(0, zoomLevel);
			Assert.assertEquals(LatLongUtils.LONGITUDE_MIN, longitude, 0);

			long tileX = MercatorProjection.getMapSize(zoomLevel) / Tile.TILE_SIZE;
			longitude = MercatorProjection.tileXToLongitude(tileX, zoomLevel);
			Assert.assertEquals(LatLongUtils.LONGITUDE_MAX, longitude, 0);
		}
	}

	@Test
	public void tileYToLatitudeTest() {
		for (byte zoomLevel = ZOOM_LEVEL_MIN; zoomLevel <= ZOOM_LEVEL_MAX; ++zoomLevel) {
			double latitude = MercatorProjection.tileYToLatitude(0, zoomLevel);
			Assert.assertEquals(MercatorProjection.LATITUDE_MAX, latitude, 0);

			long tileY = MercatorProjection.getMapSize(zoomLevel) / Tile.TILE_SIZE;
			latitude = MercatorProjection.tileYToLatitude(tileY, zoomLevel);
			Assert.assertEquals(MercatorProjection.LATITUDE_MIN, latitude, 0);
		}
	}
}
