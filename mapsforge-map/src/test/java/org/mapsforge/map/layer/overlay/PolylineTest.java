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
package org.mapsforge.map.layer.overlay;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;
import org.mapsforge.core.graphics.AwtGraphicFactory;
import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.GeoPoint;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Tile;

public class PolylineTest {
	private static final GraphicFactory GRAPHIC_FACTORY = AwtGraphicFactory.INSTANCE;

	@Test
	public void constructorTest() {
		Paint paint = GRAPHIC_FACTORY.createPaint();

		Polyline polyline = new Polyline(paint);
		Assert.assertTrue(polyline.getGeoPoints().isEmpty());
		Assert.assertEquals(paint, polyline.getPaintStroke());
	}

	@Test
	public void drawTest() {
		Polyline polyline = new Polyline(null);

		BoundingBox boundingBox = new BoundingBox(-1, -1, 1, 1);
		Canvas canvas = GRAPHIC_FACTORY.createCanvas();
		canvas.setBitmap(GRAPHIC_FACTORY.createBitmap(Tile.TILE_SIZE, Tile.TILE_SIZE));
		Point point = new Point(0, 0);
		polyline.draw(boundingBox, (byte) 0, canvas, point);

		polyline.getGeoPoints().add(new GeoPoint(0, 0));
		polyline.getGeoPoints().add(new GeoPoint(1, 1));
		polyline.draw(boundingBox, (byte) 0, canvas, point);

		polyline.setPaintStroke(GRAPHIC_FACTORY.createPaint());
		polyline.draw(boundingBox, (byte) 0, canvas, point);
	}

	@Test
	public void setterTest() {
		GeoPoint geoPoint = new GeoPoint(0, 0);
		Paint paint = GRAPHIC_FACTORY.createPaint();

		Polyline polyline = new Polyline(null);
		Assert.assertTrue(polyline.getGeoPoints().isEmpty());
		Assert.assertNull(polyline.getPaintStroke());

		polyline.getGeoPoints().add(geoPoint);
		Assert.assertEquals(Arrays.asList(geoPoint), polyline.getGeoPoints());

		polyline.setPaintStroke(paint);
		Assert.assertEquals(paint, polyline.getPaintStroke());
	}
}
