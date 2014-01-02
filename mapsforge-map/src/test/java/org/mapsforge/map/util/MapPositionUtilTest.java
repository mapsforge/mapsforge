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
package org.mapsforge.map.util;

import org.junit.Assert;
import org.junit.Test;
import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.awt.AwtGraphicFactory;

public class MapPositionUtilTest {
	private static final int[] TILE_SIZES = {256, 128, 376, 512, 100};
	private static final GraphicFactory GRAPHIC_FACTORY = AwtGraphicFactory.INSTANCE;

	@Test
	public void getBoundingBoxTest() {
		for (int tileSize : TILE_SIZES) {
			MapPosition mapPosition = new MapPosition(new LatLong(0, 0), (byte) 0);
			Canvas canvas = GRAPHIC_FACTORY.createCanvas();
			canvas.setBitmap(GRAPHIC_FACTORY.createBitmap(tileSize, tileSize));

			double latitudeMin = MercatorProjection.LATITUDE_MIN;
			double latitudeMax = MercatorProjection.LATITUDE_MAX;

			BoundingBox expectedBoundingBox = new BoundingBox(latitudeMin, -180, latitudeMax, 180);
			Assert.assertEquals(expectedBoundingBox, MapPositionUtil.getBoundingBox(mapPosition, canvas.getDimension(), tileSize));

			mapPosition = new MapPosition(new LatLong(0, 90), (byte) 0);
			expectedBoundingBox = new BoundingBox(latitudeMin, -90, latitudeMax, 180);
			Assert.assertEquals(expectedBoundingBox, MapPositionUtil.getBoundingBox(mapPosition, canvas.getDimension(), tileSize));

			mapPosition = new MapPosition(new LatLong(90, -180), (byte) 0);
			expectedBoundingBox = new BoundingBox(0, -180, latitudeMax, 0);
			Assert.assertEquals(expectedBoundingBox, MapPositionUtil.getBoundingBox(mapPosition, canvas.getDimension(), tileSize));
		}
	}

	@Test
	public void getTopLeftPointTest() {
		for (int tileSize : TILE_SIZES) {
			MapPosition mapPosition = new MapPosition(new LatLong(0, 0), (byte) 0);
			Canvas canvas = GRAPHIC_FACTORY.createCanvas();
			canvas.setBitmap(GRAPHIC_FACTORY.createBitmap(tileSize, tileSize));

			Point expectedPoint = new Point(0, 0);
			Assert.assertEquals(expectedPoint, MapPositionUtil.getTopLeftPoint(mapPosition, canvas.getDimension(), tileSize));

			mapPosition = new MapPosition(new LatLong(0, 90), (byte) 1);
			expectedPoint = new Point(tileSize, tileSize / 2);
			Assert.assertEquals(expectedPoint, MapPositionUtil.getTopLeftPoint(mapPosition, canvas.getDimension(), tileSize));
		}
	}
}
