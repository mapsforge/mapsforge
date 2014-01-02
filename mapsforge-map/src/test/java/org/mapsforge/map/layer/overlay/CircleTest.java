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
import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.awt.AwtGraphicFactory;
import org.mapsforge.map.model.FixedTileSizeDisplayModel;

public class CircleTest {
	private static final int[] TILE_SIZES = {256, 128, 376, 512, 100};
	private static final GraphicFactory GRAPHIC_FACTORY = AwtGraphicFactory.INSTANCE;

	private static Circle createCircle(LatLong latLong, float radius, Paint paintFill, Paint paintStroke) {
		return new Circle(latLong, radius, paintFill, paintStroke);
	}

	private static void verifyInvalidRadius(LatLong latLong, float radius, Paint paintFill, Paint paintStroke) {
		try {
			createCircle(latLong, radius, paintFill, paintStroke);
			Assert.fail();
		} catch (IllegalArgumentException e) {
			Assert.assertTrue(true);
		}
	}

	@Test
	public void constructorTest() {
		LatLong latLong = new LatLong(0, 0);
		int radius = 3;
		Paint paintFill = GRAPHIC_FACTORY.createPaint();
		Paint paintStroke = GRAPHIC_FACTORY.createPaint();

		Circle circle = new Circle(latLong, radius, paintFill, paintStroke);
		Assert.assertEquals(latLong, circle.getPosition());
		Assert.assertEquals(radius, circle.getRadius(), 0);
		Assert.assertEquals(paintFill, circle.getPaintFill());
		Assert.assertEquals(paintStroke, circle.getPaintStroke());

		verifyInvalidRadius(latLong, -1, paintFill, paintStroke);
		verifyInvalidRadius(latLong, Float.NaN, paintFill, paintStroke);
	}

	@Test
	public void drawTest() {
		for (int tileSize : TILE_SIZES) {
			Circle circle = new Circle(null, 0, null, null);
			circle.setDisplayModel(new FixedTileSizeDisplayModel(tileSize));

			BoundingBox boundingBox = new BoundingBox(-1, -1, 1, 1);
			Canvas canvas = GRAPHIC_FACTORY.createCanvas();
			canvas.setBitmap(GRAPHIC_FACTORY.createBitmap(tileSize, tileSize));
			Point point = new Point(0, 0);
			circle.draw(boundingBox, (byte) 0, canvas, point);

			circle.setLatLong(new LatLong(0, 0));
			circle.draw(boundingBox, (byte) 0, canvas, point);

			circle.setRadius(1);
			circle.draw(boundingBox, (byte) 0, canvas, point);

			circle.setPaintFill(GRAPHIC_FACTORY.createPaint());
			circle.draw(boundingBox, (byte) 0, canvas, point);

			circle.setPaintStroke(GRAPHIC_FACTORY.createPaint());
			circle.draw(boundingBox, (byte) 0, canvas, point);
		}
	}

	@Test
	public void setterTest() {
		LatLong latLong = new LatLong(1, 2);
		Paint paintFill = GRAPHIC_FACTORY.createPaint();
		Paint paintStroke = GRAPHIC_FACTORY.createPaint();

		Circle circle = new Circle(null, 0, null, null);
		Assert.assertNull(circle.getPosition());
		Assert.assertEquals(0, circle.getRadius(), 0);
		Assert.assertNull(circle.getPaintFill());
		Assert.assertNull(circle.getPaintStroke());

		circle.setLatLong(latLong);
		Assert.assertEquals(latLong, circle.getPosition());

		circle.setRadius(1);
		Assert.assertEquals(1, circle.getRadius(), 0);

		circle.setPaintFill(paintFill);
		Assert.assertEquals(paintFill, circle.getPaintFill());

		circle.setPaintStroke(paintStroke);
		Assert.assertEquals(paintStroke, circle.getPaintStroke());
	}
}
