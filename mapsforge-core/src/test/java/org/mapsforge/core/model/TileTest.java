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
package org.mapsforge.core.model;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.mapsforge.core.util.MercatorProjection;

public class TileTest {
	private static final String TILE_TO_STRING = "x=1, y=2, z=3";

	private static final int TILE_SIZE = 256;

	private static Tile createTile(int tileX, int tileY, byte zoomLevel) {
		return new Tile(tileX, tileY, zoomLevel, TILE_SIZE);
	}

	private static void verifyInvalid(int tileX, int tileY, byte zoomLevel) {
		try {
			createTile(tileX, tileY, zoomLevel);
			Assert.fail("x: " + tileX + ", tileY: " + tileY + ", zoomLevel: " + zoomLevel);
		} catch (IllegalArgumentException e) {
			Assert.assertTrue(true);
		}
	}

	private static void verifyInvalidMaxTileNumber(byte zoomLevel) {
		try {
			Tile.getMaxTileNumber(zoomLevel);
			Assert.fail("zoomLevel: " + zoomLevel);
		} catch (IllegalArgumentException e) {
			Assert.assertTrue(true);
		}
	}

	@Test
	public void constructorTest() {
		createTile(0, 0, (byte) 0);
		createTile(1, 1, (byte) 1);

		verifyInvalid(-1, 0, (byte) 0);
		verifyInvalid(0, -1, (byte) 0);
		verifyInvalid(0, 0, (byte) -1);

		verifyInvalid(1, 0, (byte) 0);
		verifyInvalid(0, 2, (byte) 1);
	}

	@Test
	public void equalsTest() {
		Tile tile1 = new Tile(1, 2, (byte) 3, TILE_SIZE);
		Tile tile2 = new Tile(1, 2, (byte) 3, TILE_SIZE);
		Tile tile3 = new Tile(1, 1, (byte) 3, TILE_SIZE);
		Tile tile4 = new Tile(2, 2, (byte) 3, TILE_SIZE);
		Tile tile5 = new Tile(1, 2, (byte) 4, TILE_SIZE);

		TestUtils.equalsTest(tile1, tile2);

		TestUtils.notEqualsTest(tile1, tile3);
		TestUtils.notEqualsTest(tile1, tile4);
		TestUtils.notEqualsTest(tile1, tile5);
		TestUtils.notEqualsTest(tile1, new Object());
		TestUtils.notEqualsTest(tile1, null);
	}

	@Test
	public void getBoundingBoxTest() {
		Tile tile1 = new Tile(0, 0, (byte) 0, TILE_SIZE);
		Assert.assertTrue(tile1.getBoundingBox().equals(new BoundingBox(MercatorProjection.LATITUDE_MIN,
				-180, MercatorProjection.LATITUDE_MAX, 180)));

		Tile tile2 = new Tile(0, 0, (byte) 1, TILE_SIZE);
		Assert.assertEquals(tile1.getBoundingBox().maxLatitude, tile2.getBoundingBox().maxLatitude, 0.0001);
		Assert.assertEquals(tile1.getBoundingBox().minLongitude, tile2.getBoundingBox().minLongitude, 0.0001);

		Tile tile3 = new Tile(1, 1, (byte) 1, TILE_SIZE);
		Assert.assertEquals(tile1.getBoundingBox().minLatitude, tile3.getBoundingBox().minLatitude, 0.0001);
		Assert.assertNotEquals(tile1.getBoundingBox().minLongitude, tile3.getBoundingBox().minLongitude, 0.0001);
		Assert.assertEquals(tile3.getBoundingBox().minLongitude, 0, 0.0001);
		Assert.assertEquals(tile3.getBoundingBox().maxLongitude, 180, 0.0001);
		
		Tile tile4 = new Tile(0, 0, (byte) 12, TILE_SIZE);
		Assert.assertEquals(tile1.getBoundingBox().maxLatitude, tile4.getBoundingBox().maxLatitude, 0.0001);
		Assert.assertEquals(tile1.getBoundingBox().minLongitude, tile4.getBoundingBox().minLongitude, 0.0001);

		Tile tile5 = new Tile(0, 0, (byte) 24, TILE_SIZE);
		Assert.assertEquals(tile1.getBoundingBox().maxLatitude, tile5.getBoundingBox().maxLatitude, 0.0001);
		Assert.assertEquals(tile1.getBoundingBox().minLongitude, tile5.getBoundingBox().minLongitude, 0.0001);

	}

	@Test
	public void getMaxTileNumberTest() {
		Assert.assertEquals(0, Tile.getMaxTileNumber((byte) 0));
		Assert.assertEquals(1, Tile.getMaxTileNumber((byte) 1));
		Assert.assertEquals(3, Tile.getMaxTileNumber((byte) 2));
		Assert.assertEquals(7, Tile.getMaxTileNumber((byte) 3));
		Assert.assertEquals(1023, Tile.getMaxTileNumber((byte) 10));
		Assert.assertEquals(1048575, Tile.getMaxTileNumber((byte) 20));
		Assert.assertEquals(1073741823, Tile.getMaxTileNumber((byte) 30));

		verifyInvalidMaxTileNumber((byte) -1);
		verifyInvalidMaxTileNumber(Byte.MIN_VALUE);
	}

	@Test
	public void getParentTest() {
		Tile rootTile = new Tile(0, 0, (byte) 0, TILE_SIZE);
		Assert.assertNull(rootTile.getParent());

		Assert.assertEquals(rootTile, new Tile(0, 0, (byte) 1, TILE_SIZE).getParent());
		Assert.assertEquals(rootTile, new Tile(1, 0, (byte) 1, TILE_SIZE).getParent());
		Assert.assertEquals(rootTile, new Tile(0, 1, (byte) 1, TILE_SIZE).getParent());
		Assert.assertEquals(rootTile, new Tile(1, 1, (byte) 1, TILE_SIZE).getParent());
	}

	@Test
	public void getShiftXTest() {
		Tile tile0 = new Tile(0, 0, (byte) 0, TILE_SIZE);
		Tile tile1 = new Tile(0, 1, (byte) 1, TILE_SIZE);
		Tile tile2 = new Tile(1, 2, (byte) 2, TILE_SIZE);

		Assert.assertEquals(0, tile0.getShiftX(tile0));
		Assert.assertEquals(0, tile1.getShiftX(tile0));
		Assert.assertEquals(1, tile2.getShiftX(tile0));
		Assert.assertEquals(1, tile2.getShiftX(tile1));
	}

	@Test
	public void getShiftYTest() {
		Tile tile0 = new Tile(0, 0, (byte) 0, TILE_SIZE);
		Tile tile1 = new Tile(0, 1, (byte) 1, TILE_SIZE);
		Tile tile2 = new Tile(1, 2, (byte) 2, TILE_SIZE);

		Assert.assertEquals(0, tile0.getShiftY(tile0));
		Assert.assertEquals(1, tile1.getShiftY(tile0));
		Assert.assertEquals(2, tile2.getShiftY(tile0));
		Assert.assertEquals(0, tile2.getShiftY(tile1));
	}

	@Test
	public void getNeighbourTest() {
		Tile tile0 = new Tile(0, 0, (byte) 0, TILE_SIZE);
		Assert.assertTrue(tile0.getLeft().equals(tile0));
		Assert.assertTrue(tile0.getRight().equals(tile0));
		Assert.assertTrue(tile0.getBelow().equals(tile0));
		Assert.assertTrue(tile0.getAbove().equals(tile0));
		Assert.assertTrue(tile0.getAboveLeft().equals(tile0));
		Assert.assertTrue(tile0.getAboveRight().equals(tile0));
		Assert.assertTrue(tile0.getBelowRight().equals(tile0));
		Assert.assertTrue(tile0.getBelowLeft().equals(tile0));

		Tile tile1 = new Tile(0, 1, (byte) 1, TILE_SIZE);
		Assert.assertTrue(tile1.getLeft().getLeft().equals(tile1));
		Assert.assertTrue(tile1.getRight().getRight().equals(tile1));
		Assert.assertTrue(tile1.getBelow().getBelow().equals(tile1));
		Assert.assertTrue(tile1.getAbove().getAbove().equals(tile1));
		Assert.assertTrue(tile1.getLeft().getRight().equals(tile1));
		Assert.assertTrue(tile1.getRight().getLeft().equals(tile1));
		Assert.assertTrue(tile1.getBelow().getAbove().equals(tile1));
		Assert.assertTrue(tile1.getAbove().getBelow().equals(tile1));
		Assert.assertTrue(tile1.getLeft().getRight().getAbove().getBelow().equals(tile1));
		Assert.assertTrue(tile1.getLeft().getRight().getLeft().getRight().equals(tile1));
		Assert.assertTrue(tile1.getRight().getBelow().getAbove().getLeft().equals(tile1));
		Assert.assertTrue(tile1.getAbove().getLeft().getBelow().getRight().equals(tile1));
		Assert.assertTrue(tile1.getAboveLeft().getBelowRight().getAbove().getBelow().equals(tile1));
		Assert.assertTrue(tile1.getAboveLeft().getBelowRight().getLeft().getRight().equals(tile1));
		Assert.assertTrue(tile1.getRight().getBelow().getAbove().getLeft().equals(tile1));
		Assert.assertTrue(tile1.getAbove().getLeft().getBelowRight().equals(tile1));

		Assert.assertFalse(tile1.getAboveLeft().getLeft().getBelowRight().equals(tile1));
		Assert.assertFalse(tile1.getAbove().getLeft().getBelowRight().getLeft().equals(tile1));
		Assert.assertFalse(tile1.getAbove().getBelowLeft().getBelowRight().equals(tile1));
		Assert.assertFalse(tile1.getAboveLeft().getLeft().getBelowRight().equals(tile1));
		Assert.assertFalse(tile1.getAbove().getAboveLeft().getBelowRight().equals(tile1));
		Assert.assertTrue(tile1.getAbove().getLeft().getBelowRight().equals(tile1));

		Tile tile2 = new Tile(0, 1, (byte) 2, TILE_SIZE);
		Assert.assertFalse(tile2.getLeft().getLeft().equals(tile2));
		Assert.assertFalse(tile2.getRight().getRight().equals(tile2));
		Assert.assertFalse(tile2.getBelow().getBelow().equals(tile2));
		Assert.assertFalse(tile2.getAbove().getAbove().equals(tile2));
		Assert.assertTrue(tile2.getLeft().getRight().equals(tile2));
		Assert.assertTrue(tile2.getRight().getLeft().equals(tile2));
		Assert.assertTrue(tile2.getBelow().getAbove().equals(tile2));
		Assert.assertTrue(tile2.getAbove().getBelow().equals(tile2));
		Assert.assertTrue(tile2.getLeft().getRight().getAbove().getBelow().equals(tile2));
		Assert.assertTrue(tile2.getLeft().getRight().getLeft().getRight().equals(tile2));
		Assert.assertTrue(tile2.getRight().getBelow().getAbove().getLeft().equals(tile2));
		Assert.assertTrue(tile2.getAbove().getLeft().getBelow().getRight().equals(tile2));

		Tile tile5 = new Tile(0, 1, (byte) 5, TILE_SIZE);
		Assert.assertFalse(tile5.getLeft().getLeft().equals(tile5));
		Assert.assertFalse(tile5.getRight().getRight().equals(tile5));
		Assert.assertFalse(tile5.getBelow().getBelow().equals(tile5));
		Assert.assertFalse(tile5.getAbove().getAbove().equals(tile5));
		Assert.assertTrue(tile5.getLeft().getRight().equals(tile5));
		Assert.assertTrue(tile5.getRight().getLeft().equals(tile5));
		Assert.assertTrue(tile5.getBelow().getAbove().equals(tile5));
		Assert.assertTrue(tile5.getAbove().getBelow().equals(tile5));
		Assert.assertTrue(tile5.getLeft().getRight().getAboveLeft().getBelow().getRight().equals(tile5));
		Assert.assertTrue(tile5.getRight().getLeft().getBelowRight().getAboveLeft().equals(tile5));
		Assert.assertTrue(tile5.getBelow().getLeft().getAbove().getRight().equals(tile5));
		Assert.assertTrue(tile5.getAboveLeft().equals(tile5.getLeft().getAbove()));
		Assert.assertTrue(tile5.getBelowRight().equals(tile5.getBelow().getRight()));
		Assert.assertTrue(tile5.getAbove().getBelow().equals(tile5.getLeft().getRight()));


		tile5 = new Tile(0, 1, (byte) 14, TILE_SIZE);
		Assert.assertFalse(tile5.getLeft().getLeft().equals(tile5));
		Assert.assertFalse(tile5.getRight().getRight().equals(tile5));
		Assert.assertFalse(tile5.getBelow().getBelow().equals(tile5));
		Assert.assertFalse(tile5.getAbove().getAbove().equals(tile5));
		Assert.assertTrue(tile5.getLeft().getRight().equals(tile5));
		Assert.assertTrue(tile5.getRight().getLeft().equals(tile5));
		Assert.assertTrue(tile5.getBelow().getAbove().equals(tile5));
		Assert.assertTrue(tile5.getAbove().getBelow().equals(tile5));
		Assert.assertTrue(tile5.getLeft().getRight().getAboveLeft().getBelow().getRight().equals(tile5));
		Assert.assertTrue(tile5.getRight().getLeft().getBelowRight().getAboveLeft().equals(tile5));
		Assert.assertTrue(tile5.getBelow().getLeft().getAbove().getRight().equals(tile5));
		Assert.assertTrue(tile5.getAboveLeft().equals(tile5.getLeft().getAbove()));
		Assert.assertTrue(tile5.getBelowRight().equals(tile5.getBelow().getRight()));
		Assert.assertTrue(tile5.getAbove().getBelow().equals(tile5.getLeft().getRight()));

	}
	
	
	
	@Test
	public void getterTest() {
		Tile tile = new Tile(1, 2, (byte) 3, TILE_SIZE);

		Assert.assertEquals(1, tile.tileX);
		Assert.assertEquals(2, tile.tileY);
		Assert.assertEquals(3, tile.zoomLevel);
	}

	@Test
	public void serializeTest() throws IOException, ClassNotFoundException {
		Tile tile = new Tile(1, 2, (byte) 3, TILE_SIZE);
		TestUtils.serializeTest(tile);
	}

	@Test
	public void toStringTest() {
		Tile tile = new Tile(1, 2, (byte) 3, TILE_SIZE);
		Assert.assertEquals(TILE_TO_STRING, tile.toString());
	}
}
