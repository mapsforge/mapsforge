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

import java.util.Arrays;

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

public class PolygonTest {
	private static final GraphicFactory GRAPHIC_FACTORY = AwtGraphicFactory.INSTANCE;
	private static final int[] TILE_SIZES = { 256, 128, 376, 512, 100 };

	@Test
	public void constructorTest() {
		Paint paintFill = GRAPHIC_FACTORY.createPaint();
		Paint paintStroke = GRAPHIC_FACTORY.createPaint();

		Polygon polygon = new Polygon(paintFill, paintStroke, GRAPHIC_FACTORY);
		Assert.assertTrue(polygon.getLatLongs().isEmpty());
		Assert.assertEquals(paintFill, polygon.getPaintFill());
		Assert.assertEquals(paintStroke, polygon.getPaintStroke());
	}

	@Test
	public void drawTest() {
		for (int tileSize : TILE_SIZES) {
			Polygon polygon = new Polygon(null, null, GRAPHIC_FACTORY);
			polygon.setDisplayModel(new FixedTileSizeDisplayModel(tileSize));

			BoundingBox boundingBox = new BoundingBox(-1, -1, 1, 1);
			Canvas canvas = GRAPHIC_FACTORY.createCanvas();
			canvas.setBitmap(GRAPHIC_FACTORY.createBitmap(tileSize, tileSize));
			Point point = new Point(0, 0);
			polygon.draw(boundingBox, (byte) 0, canvas, point);

			polygon.getLatLongs().add(new LatLong(0, 0));
			polygon.getLatLongs().add(new LatLong(1, 1));
			polygon.draw(boundingBox, (byte) 0, canvas, point);

			polygon.setPaintFill(GRAPHIC_FACTORY.createPaint());
			polygon.setPaintStroke(GRAPHIC_FACTORY.createPaint());
			polygon.draw(boundingBox, (byte) 0, canvas, point);
		}
	}

	@Test
	public void setterTest() {
		LatLong latLong = new LatLong(0, 0);
		Paint paintFill = GRAPHIC_FACTORY.createPaint();
		Paint paintStroke = GRAPHIC_FACTORY.createPaint();

		Polygon polygon = new Polygon(null, null, GRAPHIC_FACTORY);
		Assert.assertTrue(polygon.getLatLongs().isEmpty());
		Assert.assertNull(polygon.getPaintFill());
		Assert.assertNull(polygon.getPaintStroke());

		polygon.getLatLongs().add(latLong);
		Assert.assertEquals(Arrays.asList(latLong), polygon.getLatLongs());

		polygon.setPaintFill(paintFill);
		polygon.setPaintStroke(paintStroke);
		Assert.assertEquals(paintFill, polygon.getPaintFill());
		Assert.assertEquals(paintStroke, polygon.getPaintStroke());
	}
}
