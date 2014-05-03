/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright © 2014 Ludwig M Brinckmann
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
package org.mapsforge.map.layer;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Tile;

public class LayerUtilTest {
	private static final int[] TILE_SIZES = { 256, 128, 376, 512, 100 };

	@Test
	public void getTilePositionsTest() {
		for (int tileSize : TILE_SIZES) {
			BoundingBox boundingBox = new BoundingBox(-1, -1, 1, 1);
			List<TilePosition> tilePositions = LayerUtil.getTilePositions(boundingBox, (byte) 0, new Point(0, 0),
					tileSize);
			Assert.assertEquals(1, tilePositions.size());

			TilePosition tilePosition = tilePositions.get(0);
			Assert.assertEquals(new Tile(0, 0, (byte) 0), tilePosition.tile);
			Assert.assertEquals(new Point(0, 0), tilePosition.point);
		}
	}
}
