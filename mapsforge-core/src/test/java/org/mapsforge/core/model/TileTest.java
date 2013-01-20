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
package org.mapsforge.core.model;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

public class TileTest {
	private static final String TILE_TO_STRING = "tileX=1, tileY=2, zoomLevel=3";
	private static final long TILE_X = 1;
	private static final long TILE_Y = 2;
	private static final byte ZOOM_LEVEL = 3;

	@Test
	public void equalsTest() {
		Tile tile1 = new Tile(TILE_X, TILE_Y, ZOOM_LEVEL);
		Tile tile2 = new Tile(TILE_X, TILE_Y, ZOOM_LEVEL);
		Tile tile3 = new Tile(TILE_X, TILE_X, ZOOM_LEVEL);

		TestUtils.equalsTest(tile1, tile2);

		Assert.assertNotEquals(tile1, tile3);
		Assert.assertNotEquals(tile3, tile1);
		Assert.assertNotEquals(tile1, new Object());
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
		Tile tile = new Tile(TILE_X, TILE_Y, ZOOM_LEVEL);

		Assert.assertEquals(TILE_X, tile.tileX);
		Assert.assertEquals(TILE_Y, tile.tileY);
		Assert.assertEquals(ZOOM_LEVEL, tile.zoomLevel);
	}

	@Test
	public void serializeTest() throws IOException, ClassNotFoundException {
		Tile tile = new Tile(TILE_X, TILE_Y, ZOOM_LEVEL);
		TestUtils.serializeTest(tile);
	}

	@Test
	public void toStringTest() {
		Tile tile = new Tile(TILE_X, TILE_Y, ZOOM_LEVEL);
		Assert.assertEquals(TILE_TO_STRING, tile.toString());
	}
}
