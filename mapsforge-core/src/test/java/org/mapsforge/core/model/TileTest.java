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

import junit.framework.Assert;

import org.junit.Test;

public class TileTest {
	private static final String TILE_TO_STRING = "Tile [tileX=1, tileY=2, zoomLevel=3]";
	private static final long TILE_X = 1;
	private static final long TILE_Y = 2;
	private static final byte ZOOM_LEVEL = 3;

	@Test
	public void equalsTest() {
		Tile tile1 = new Tile(TILE_X, TILE_Y, ZOOM_LEVEL);
		Tile tile2 = new Tile(TILE_X, TILE_Y, ZOOM_LEVEL);
		Tile tile3 = new Tile(TILE_X, TILE_X, ZOOM_LEVEL);

		TestUtils.equalsTest(tile1, tile2);

		Assert.assertFalse(tile1.equals(tile3));
		Assert.assertFalse(tile3.equals(tile1));
		Assert.assertFalse(tile1.equals(new Object()));
	}

	@Test
	public void getterTest() {
		Tile tile = new Tile(TILE_X, TILE_Y, ZOOM_LEVEL);

		Assert.assertEquals(TILE_X, tile.tileX);
		Assert.assertEquals(TILE_Y, tile.tileY);
		Assert.assertEquals(ZOOM_LEVEL, tile.zoomLevel);

		Assert.assertEquals(TILE_X * Tile.TILE_SIZE, tile.getPixelX());
		Assert.assertEquals(TILE_Y * Tile.TILE_SIZE, tile.getPixelY());
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
