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

public class TileTest {
	private static final String TILE_TO_STRING = "tileX=1, tileY=2, zoomLevel=3";

	private static Tile createTile(long tileX, long tileY, byte zoomLevel) {
		return new Tile(tileX, tileY, zoomLevel);
	}

	private static void verifyInvalid(long tileX, long tileY, byte zoomLevel) {
		try {
			createTile(tileX, tileY, zoomLevel);
			Assert.fail("tileX: " + tileX + ", tileY: " + tileY + ", zoomLevel: " + zoomLevel);
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
		Tile tile1 = new Tile(1, 2, (byte) 3);
		Tile tile2 = new Tile(1, 2, (byte) 3);
		Tile tile3 = new Tile(1, 1, (byte) 3);
		Tile tile4 = new Tile(2, 2, (byte) 3);
		Tile tile5 = new Tile(1, 2, (byte) 4);

		TestUtils.equalsTest(tile1, tile2);

		TestUtils.notEqualsTest(tile1, tile3);
		TestUtils.notEqualsTest(tile1, tile4);
		TestUtils.notEqualsTest(tile1, tile5);
		TestUtils.notEqualsTest(tile1, new Object());
		TestUtils.notEqualsTest(tile1, null);
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
		Tile rootTile = new Tile(0, 0, (byte) 0);
		Assert.assertNull(rootTile.getParent());

		Assert.assertEquals(rootTile, new Tile(0, 0, (byte) 1).getParent());
		Assert.assertEquals(rootTile, new Tile(1, 0, (byte) 1).getParent());
		Assert.assertEquals(rootTile, new Tile(0, 1, (byte) 1).getParent());
		Assert.assertEquals(rootTile, new Tile(1, 1, (byte) 1).getParent());
	}

	@Test
	public void getShiftXTest() {
		Tile tile0 = new Tile(0, 0, (byte) 0);
		Tile tile1 = new Tile(0, 1, (byte) 1);
		Tile tile2 = new Tile(1, 2, (byte) 2);

		Assert.assertEquals(0, tile0.getShiftX(tile0));
		Assert.assertEquals(0, tile1.getShiftX(tile0));
		Assert.assertEquals(1, tile2.getShiftX(tile0));
		Assert.assertEquals(1, tile2.getShiftX(tile1));
	}

	@Test
	public void getShiftYTest() {
		Tile tile0 = new Tile(0, 0, (byte) 0);
		Tile tile1 = new Tile(0, 1, (byte) 1);
		Tile tile2 = new Tile(1, 2, (byte) 2);

		Assert.assertEquals(0, tile0.getShiftY(tile0));
		Assert.assertEquals(1, tile1.getShiftY(tile0));
		Assert.assertEquals(2, tile2.getShiftY(tile0));
		Assert.assertEquals(0, tile2.getShiftY(tile1));
	}

	@Test
	public void getterTest() {
		Tile tile = new Tile(1, 2, (byte) 3);

		Assert.assertEquals(1, tile.tileX);
		Assert.assertEquals(2, tile.tileY);
		Assert.assertEquals(3, tile.zoomLevel);
	}

	@Test
	public void serializeTest() throws IOException, ClassNotFoundException {
		Tile tile = new Tile(1, 2, (byte) 3);
		TestUtils.serializeTest(tile);
	}

	@Test
	public void toStringTest() {
		Tile tile = new Tile(1, 2, (byte) 3);
		Assert.assertEquals(TILE_TO_STRING, tile.toString());
	}
}
