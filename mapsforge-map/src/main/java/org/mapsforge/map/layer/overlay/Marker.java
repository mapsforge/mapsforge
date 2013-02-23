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

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.GeoPoint;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Rectangle;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.layer.Layer;

/**
 * A {@code Marker} draws a {@link Bitmap} at a given geographical position.
 */
public class Marker extends Layer {
	private Bitmap bitmap;
	private GeoPoint geoPoint;

	/**
	 * @param geoPoint
	 *            the initial geographical coordinates of this marker (may be null).
	 * @param bitmap
	 *            the initial {@code Bitmap} of this marker (may be null).
	 */
	public Marker(GeoPoint geoPoint, Bitmap bitmap) {
		super();

		this.geoPoint = geoPoint;
		this.bitmap = bitmap;
	}

	@Override
	public synchronized void draw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point canvasPosition) {
		if (this.geoPoint == null || this.bitmap == null) {
			return;
		}

		double left = MercatorProjection.longitudeToPixelX(this.geoPoint.longitude, zoomLevel) - canvasPosition.x;
		double top = MercatorProjection.latitudeToPixelY(this.geoPoint.latitude, zoomLevel) - canvasPosition.y;
		double right = left + this.bitmap.getWidth();
		double bottom = top + this.bitmap.getHeight();

		Rectangle bitmapRectangle = new Rectangle(left, top, right, bottom);
		Rectangle canvasRectangle = new Rectangle(0, 0, canvas.getWidth(), canvas.getHeight());
		if (!canvasRectangle.intersects(bitmapRectangle)) {
			return;
		}

		canvas.drawBitmap(this.bitmap, (float) left, (float) top);
	}

	/**
	 * @return the {@code Bitmap} of this marker (may be null).
	 */
	public synchronized Bitmap getBitmap() {
		return this.bitmap;
	}

	/**
	 * @return the geographical coordinates of this marker (may be null).
	 */
	public synchronized GeoPoint getGeoPoint() {
		return this.geoPoint;
	}

	/**
	 * @param bitmap
	 *            the new {@code Bitmap} of this marker (may be null).
	 */
	public synchronized void setBitmap(Bitmap bitmap) {
		this.bitmap = bitmap;
	}

	/**
	 * @param geoPoint
	 *            the new geographical coordinates of this marker (may be null).
	 */
	public synchronized void setGeoPoint(GeoPoint geoPoint) {
		this.geoPoint = geoPoint;
	}
}
