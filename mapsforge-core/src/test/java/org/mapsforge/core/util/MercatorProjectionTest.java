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
	private static final int[] TILE_SIZES = {256, 128, 376, 512, 100};


	private static void verifyInvalidGetMapSize(byte zoomLevel, int tileSize) {
		try {
			MercatorProjection.getMapSize(zoomLevel, tileSize);
			Assert.fail("zoomLevel: " + zoomLevel);
		} catch (IllegalArgumentException e) {
			Assert.assertTrue(true);
		}
	}

	private static void verifyInvalidPixelXToLongitude(double pixelX, byte zoomLevel, int tileSize) {
		try {
			MercatorProjection.pixelXToLongitude(pixelX, zoomLevel, tileSize);
			Assert.fail("pixelX: " + pixelX + ", zoomLevel: " + zoomLevel);
		} catch (IllegalArgumentException e) {
			Assert.assertTrue(true);
		}
	}

	private static void verifyInvalidPixelYToLatitude(double pixelY, byte zoomLevel, int tileSize) {
		try {
			MercatorProjection.pixelYToLatitude(pixelY, zoomLevel, tileSize);
			Assert.fail("pixelY: " + pixelY + ", zoomLevel: " + zoomLevel);
		} catch (IllegalArgumentException e) {
			Assert.assertTrue(true);
		}
	}

	@Test
	public void getMapSizeTest() {
		for (int tileSize : TILE_SIZES) {
			for (byte zoomLevel = ZOOM_LEVEL_MIN; zoomLevel <= ZOOM_LEVEL_MAX; ++zoomLevel) {
				long factor = Math.round(Math.pow(2, zoomLevel));
				Assert.assertEquals(tileSize * factor, MercatorProjection.getMapSize(zoomLevel, tileSize));
			}
			verifyInvalidGetMapSize((byte) -1, tileSize);
		}
	}

	@Test
	public void latitudeToPixelYTest() {
		for (int tileSize : TILE_SIZES) {
			for (byte zoomLevel = ZOOM_LEVEL_MIN; zoomLevel <= ZOOM_LEVEL_MAX; ++zoomLevel) {
				double pixelY = MercatorProjection.latitudeToPixelY(MercatorProjection.LATITUDE_MAX, zoomLevel, tileSize);
				Assert.assertEquals(0, pixelY, 0);

				long mapSize = MercatorProjection.getMapSize(zoomLevel, tileSize);
				pixelY = MercatorProjection.latitudeToPixelY(0, zoomLevel, tileSize);
				Assert.assertEquals((float) mapSize / 2, pixelY, 0);

				pixelY = MercatorProjection.latitudeToPixelY(MercatorProjection.LATITUDE_MIN, zoomLevel, tileSize);
				Assert.assertEquals(mapSize, pixelY, 0);
			}
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
		for (int tileSize : TILE_SIZES) {
			for (byte zoomLevel = ZOOM_LEVEL_MIN; zoomLevel <= ZOOM_LEVEL_MAX; ++zoomLevel) {
				double pixelX = MercatorProjection.longitudeToPixelX(LatLongUtils.LONGITUDE_MIN, zoomLevel, tileSize);
				Assert.assertEquals(0, pixelX, 0);

				long mapSize = MercatorProjection.getMapSize(zoomLevel, tileSize);
				pixelX = MercatorProjection.longitudeToPixelX(0, zoomLevel, tileSize);
				Assert.assertEquals((float) mapSize / 2, pixelX, 0);

				pixelX = MercatorProjection.longitudeToPixelX(LatLongUtils.LONGITUDE_MAX, zoomLevel, tileSize);
				Assert.assertEquals(mapSize, pixelX, 0);
			}
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
	public void metersToPixelTest() {
		for (int tileSize : TILE_SIZES) {
			Assert.assertTrue(MercatorProjection.metersToPixels(10, 10.0, (byte) 1, tileSize) < 1);
			Assert.assertTrue(MercatorProjection.metersToPixels((int) (40 * 10e7), 10.0, (byte) 1, tileSize) > 1);
			Assert.assertTrue(MercatorProjection.metersToPixels(10, 10.0, (byte) 20, tileSize) > 1);
			Assert.assertTrue(MercatorProjection.metersToPixels(10, 89.0, (byte) 1, tileSize) < 1);
			Assert.assertTrue(MercatorProjection.metersToPixels((int) (40 * 10e3), 50, (byte) 10, tileSize) > 1);
		}
	}


	@Test
	public void pixelXToLongitudeTest() {
		for (int tileSize : TILE_SIZES) {
			for (byte zoomLevel = ZOOM_LEVEL_MIN; zoomLevel <= ZOOM_LEVEL_MAX; ++zoomLevel) {
				double longitude = MercatorProjection.pixelXToLongitude(0, zoomLevel, tileSize);
				Assert.assertEquals(LatLongUtils.LONGITUDE_MIN, longitude, 0);

				long mapSize = MercatorProjection.getMapSize(zoomLevel, tileSize);
				longitude = MercatorProjection.pixelXToLongitude((float) mapSize / 2, zoomLevel, tileSize);
				Assert.assertEquals(0, longitude, 0);

				longitude = MercatorProjection.pixelXToLongitude(mapSize, zoomLevel, tileSize);
				Assert.assertEquals(LatLongUtils.LONGITUDE_MAX, longitude, 0);
			}

			verifyInvalidPixelXToLongitude(-1, (byte) 0, tileSize);
			verifyInvalidPixelXToLongitude(tileSize + 1, (byte) 0, tileSize);
		}
	}

	@Test
	public void pixelXToTileXTest() {
		for (int tileSize : TILE_SIZES) {
			for (byte zoomLevel = ZOOM_LEVEL_MIN; zoomLevel <= ZOOM_LEVEL_MAX; ++zoomLevel) {
				Assert.assertEquals(0, MercatorProjection.pixelXToTileX(0, zoomLevel, tileSize));
			}
		}
	}

	@Test
	public void pixelYToLatitudeTest() {
		for (int tileSize : TILE_SIZES) {
			for (byte zoomLevel = ZOOM_LEVEL_MIN; zoomLevel <= ZOOM_LEVEL_MAX; ++zoomLevel) {
				double latitude = MercatorProjection.pixelYToLatitude(0, zoomLevel, tileSize);
				Assert.assertEquals(MercatorProjection.LATITUDE_MAX, latitude, 0);

				long mapSize = MercatorProjection.getMapSize(zoomLevel, tileSize);
				latitude = MercatorProjection.pixelYToLatitude((float) mapSize / 2, zoomLevel, tileSize);
				Assert.assertEquals(0, latitude, 0);

				latitude = MercatorProjection.pixelYToLatitude(mapSize, zoomLevel, tileSize);
				Assert.assertEquals(MercatorProjection.LATITUDE_MIN, latitude, 0);
			}

			verifyInvalidPixelYToLatitude(-1, (byte) 0, tileSize);
			verifyInvalidPixelYToLatitude(tileSize + 1, (byte) 0, tileSize);
		}
	}

	@Test
	public void pixelYToTileYTest() {
		for (int tileSize : TILE_SIZES) {
			for (byte zoomLevel = ZOOM_LEVEL_MIN; zoomLevel <= ZOOM_LEVEL_MAX; ++zoomLevel) {
				Assert.assertEquals(0, MercatorProjection.pixelYToTileY(0, zoomLevel, tileSize));
			}
		}
	}

	@Test
	public void tileToPixelTest() {
		for (int tileSize : TILE_SIZES) {
			Assert.assertEquals(0, MercatorProjection.tileToPixel(0, tileSize));
			Assert.assertEquals(tileSize, MercatorProjection.tileToPixel(1, tileSize));
			Assert.assertEquals(tileSize * 2, MercatorProjection.tileToPixel(2, tileSize));
		}
	}

	@Test
	public void tileXToLongitudeTest() {
		for (int tileSize : TILE_SIZES) {
			for (byte zoomLevel = ZOOM_LEVEL_MIN; zoomLevel <= ZOOM_LEVEL_MAX; ++zoomLevel) {
				double longitude = MercatorProjection.tileXToLongitude(0, zoomLevel);
				Assert.assertEquals(LatLongUtils.LONGITUDE_MIN, longitude, 0);

				long tileX = MercatorProjection.getMapSize(zoomLevel, tileSize) / tileSize;
				longitude = MercatorProjection.tileXToLongitude(tileX, zoomLevel);
				Assert.assertEquals(LatLongUtils.LONGITUDE_MAX, longitude, 0);
			}
		}
	}

	@Test
	public void tileYToLatitudeTest() {
		for (int tileSize : TILE_SIZES) {
			for (byte zoomLevel = ZOOM_LEVEL_MIN; zoomLevel <= ZOOM_LEVEL_MAX; ++zoomLevel) {
				double latitude = MercatorProjection.tileYToLatitude(0, zoomLevel);
				Assert.assertEquals(MercatorProjection.LATITUDE_MAX, latitude, 0);

				long tileY = MercatorProjection.getMapSize(zoomLevel, tileSize) / tileSize;
				latitude = MercatorProjection.tileYToLatitude(tileY, zoomLevel);
				Assert.assertEquals(MercatorProjection.LATITUDE_MIN, latitude, 0);
			}
		}
	}
}
