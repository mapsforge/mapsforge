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
package org.mapsforge.map.layer.overlay;

import org.junit.Assert;
import org.junit.Test;
import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.awt.AwtGraphicFactory;
import org.mapsforge.map.model.FixedTileSizeDisplayModel;

public class MarkerTest {
	private static final GraphicFactory GRAPHIC_FACTORY = AwtGraphicFactory.INSTANCE;
	private static final int[] TILE_SIZES = { 256 };

	@Test
	public void constructorTest() {
		LatLong latLong = new LatLong(0, 0);
		Bitmap bitmap = GRAPHIC_FACTORY.createBitmap(10, 20);

		Marker marker = new Marker(latLong, bitmap, 1, 2);
		Assert.assertEquals(latLong, marker.getLatLong());
		Assert.assertEquals(bitmap, marker.getBitmap());
		Assert.assertEquals(1, marker.getHorizontalOffset());
		Assert.assertEquals(2, marker.getVerticalOffset());
	}

	@Test
	public void drawTest() {
		for (int tileSize : TILE_SIZES) {
			Marker marker = new Marker(null, null, 0, 0);
			marker.setDisplayModel(new FixedTileSizeDisplayModel(tileSize));

			BoundingBox boundingBox = new BoundingBox(-1, -1, 1, 1);
			Canvas canvas = GRAPHIC_FACTORY.createCanvas();
			canvas.setBitmap(GRAPHIC_FACTORY.createBitmap(tileSize, tileSize));
			Point point = new Point(0, 0);
			marker.draw(boundingBox, (byte) 0, canvas, point);

			marker.setLatLong(new LatLong(0, 0));
			marker.draw(boundingBox, (byte) 0, canvas, point);

			marker.setBitmap(GRAPHIC_FACTORY.createBitmap(10, 20));
			marker.draw(boundingBox, (byte) 0, canvas, point);
		}
	}

	@Test
	public void setterTest() {
		LatLong latLong = new LatLong(0, 0);
		Bitmap bitmap = GRAPHIC_FACTORY.createBitmap(10, 20);

		Marker marker = new Marker(null, null, 0, 0);
		Assert.assertNull(marker.getLatLong());
		Assert.assertNull(marker.getBitmap());
		Assert.assertEquals(0, marker.getHorizontalOffset());
		Assert.assertEquals(0, marker.getVerticalOffset());

		marker.setLatLong(latLong);
		Assert.assertEquals(latLong, marker.getLatLong());

		marker.setBitmap(bitmap);
		Assert.assertEquals(bitmap, marker.getBitmap());

		marker.setHorizontalOffset(-1);
		Assert.assertEquals(-1, marker.getHorizontalOffset());

		marker.setVerticalOffset(-2);
		Assert.assertEquals(-2, marker.getVerticalOffset());
	}
}
