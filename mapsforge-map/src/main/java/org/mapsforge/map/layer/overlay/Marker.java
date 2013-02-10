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

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.GeoPoint;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Rectangle;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.layer.Layer;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/**
 * A {@code Marker} draws a {@link Drawable} at a given geographical position.
 */
public class Marker extends Layer {
	/**
	 * Sets the bounds of the given drawable so that (0,0) is the center of its bounding box.
	 * 
	 * @param drawable
	 *            the drawable whose bounds should be set.
	 * @return the given drawable with set bounds.
	 */
	public static Drawable boundCenter(Drawable drawable) {
		int intrinsicWidth = drawable.getIntrinsicWidth();
		int intrinsicHeight = drawable.getIntrinsicHeight();
		drawable.setBounds(intrinsicWidth / -2, intrinsicHeight / -2, intrinsicWidth / 2, intrinsicHeight / 2);
		return drawable;
	}

	/**
	 * Sets the bounds of the given drawable so that (0,0) is the center of its bottom row.
	 * 
	 * @param drawable
	 *            the drawable whose bounds should be set.
	 * @return the given drawable with set bounds.
	 */
	public static Drawable boundCenterBottom(Drawable drawable) {
		int intrinsicWidth = drawable.getIntrinsicWidth();
		int intrinsicHeight = drawable.getIntrinsicHeight();
		drawable.setBounds(intrinsicWidth / -2, -intrinsicHeight, intrinsicWidth / 2, 0);
		return drawable;
	}

	private Drawable drawable;
	private GeoPoint geoPoint;

	/**
	 * @param geoPoint
	 *            the initial geographical coordinates of this marker (may be null).
	 * @param drawable
	 *            the initial {@code Drawable} of this marker (may be null).
	 */
	public Marker(GeoPoint geoPoint, Drawable drawable) {
		this.geoPoint = geoPoint;
		this.drawable = drawable;
	}

	@Override
	public synchronized void draw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point canvasPosition) {
		if (this.geoPoint == null || this.drawable == null) {
			return;
		}

		double pixelX = MercatorProjection.longitudeToPixelX(this.geoPoint.longitude, zoomLevel) - canvasPosition.x;
		double pixelY = MercatorProjection.latitudeToPixelY(this.geoPoint.latitude, zoomLevel) - canvasPosition.y;

		Rect originalBounds = this.drawable.copyBounds();
		double left = pixelX + originalBounds.left;
		double top = pixelY + originalBounds.top;
		double right = pixelX + originalBounds.right;
		double bottom = pixelY + originalBounds.bottom;

		Rectangle drawableRectangle = new Rectangle(left, top, right, bottom);
		Rectangle canvasRectangle = new Rectangle(0, 0, canvas.getWidth(), canvas.getHeight());
		if (!canvasRectangle.intersects(drawableRectangle)) {
			return;
		}

		this.drawable.setBounds((int) left, (int) top, (int) right, (int) bottom);
		this.drawable.draw(canvas);
		this.drawable.setBounds(originalBounds);
	}

	/**
	 * @return the {@code Drawable} of this marker (may be null).
	 */
	public synchronized Drawable getDrawable() {
		return this.drawable;
	}

	/**
	 * @return the geographical coordinates of this marker (may be null).
	 */
	public synchronized GeoPoint getGeoPoint() {
		return this.geoPoint;
	}

	/**
	 * @param drawable
	 *            the new {@code Drawable} of this marker (may be null).
	 */
	public synchronized void setDrawable(Drawable drawable) {
		this.drawable = drawable;
	}

	/**
	 * @param geoPoint
	 *            the new geographical coordinates of this marker (may be null).
	 */
	public synchronized void setGeoPoint(GeoPoint geoPoint) {
		this.geoPoint = geoPoint;
	}
}
