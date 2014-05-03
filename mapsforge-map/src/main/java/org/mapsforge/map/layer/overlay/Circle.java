/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
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

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Rectangle;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.layer.Layer;

/**
 * A {@code Circle} consists of a center {@link LatLong} and a non-negative radius in meters.
 * <p>
 * A {@code Circle} holds two {@link Paint} objects to allow for different outline and filling. These paints define
 * drawing parameters such as color, stroke width, pattern and transparency.
 */
public class Circle extends Layer {

	private LatLong latLong;
	private Paint paintFill;
	private Paint paintStroke;
	private float radius;

	/**
	 * @param latLong
	 *            the initial center point of this circle (may be null).
	 * @param radius
	 *            the initial non-negative radius of this circle in meters.
	 * @param paintFill
	 *            the initial {@code Paint} used to fill this circle (may be null).
	 * @param paintStroke
	 *            the initial {@code Paint} used to stroke this circle (may be null).
	 * @throws IllegalArgumentException
	 *             if the given {@code radius} is negative or {@link Float#NaN}.
	 */
	public Circle(LatLong latLong, float radius, Paint paintFill, Paint paintStroke) {
		super();

		this.latLong = latLong;
		setRadiusInternal(radius);
		this.paintFill = paintFill;
		this.paintStroke = paintStroke;
	}

	@Override
	public synchronized void draw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point topLeftPoint) {
		if (this.latLong == null || (this.paintStroke == null && this.paintFill == null)) {
			return;
		}

		double latitude = this.latLong.latitude;
		double longitude = this.latLong.longitude;
		int tileSize = displayModel.getTileSize();
		int pixelX = (int) (MercatorProjection.longitudeToPixelX(longitude, zoomLevel, tileSize) - topLeftPoint.x);
		int pixelY = (int) (MercatorProjection.latitudeToPixelY(latitude, zoomLevel, tileSize) - topLeftPoint.y);
		int radiusInPixel = getRadiusInPixels(latitude, zoomLevel);

		Rectangle canvasRectangle = new Rectangle(0, 0, canvas.getWidth(), canvas.getHeight());
		if (!canvasRectangle.intersectsCircle(pixelX, pixelY, radiusInPixel)) {
			return;
		}

		if (this.paintStroke != null) {
			canvas.drawCircle(pixelX, pixelY, radiusInPixel, this.paintStroke);
		}
		if (this.paintFill != null) {
			canvas.drawCircle(pixelX, pixelY, radiusInPixel, this.paintFill);
		}
	}

	/**
	 * @return the {@code Paint} used to fill this circle (may be null).
	 */
	public synchronized Paint getPaintFill() {
		return this.paintFill;
	}

	/**
	 * @return the {@code Paint} used to stroke this circle (may be null).
	 */
	public synchronized Paint getPaintStroke() {
		return this.paintStroke;
	}

	/**
	 * @return the center point of this circle (may be null).
	 */
	@Override
	public synchronized LatLong getPosition() {
		return this.latLong;
	}

	/**
	 * @return the non-negative radius of this circle in meters.
	 */
	public synchronized float getRadius() {
		return this.radius;
	}

	/**
	 * @param latLong
	 *            the new center point of this circle (may be null).
	 */
	public synchronized void setLatLong(LatLong latLong) {
		this.latLong = latLong;
	}

	/**
	 * @param paintFill
	 *            the new {@code Paint} used to fill this circle (may be null).
	 */
	public synchronized void setPaintFill(Paint paintFill) {
		this.paintFill = paintFill;
	}

	/**
	 * @param paintStroke
	 *            the new {@code Paint} used to stroke this circle (may be null).
	 */
	public synchronized void setPaintStroke(Paint paintStroke) {
		this.paintStroke = paintStroke;
	}

	/**
	 * @param radius
	 *            the new non-negative radius of this circle in meters.
	 * @throws IllegalArgumentException
	 *             if the given {@code radius} is negative or {@link Float#NaN}.
	 */
	public synchronized void setRadius(float radius) {
		setRadiusInternal(radius);
	}

	protected int getRadiusInPixels(double latitude, byte zoomLevel) {
		return (int) MercatorProjection.metersToPixels(this.radius, latitude, zoomLevel, displayModel.getTileSize());
	}

	private void setRadiusInternal(float radius) {
		if (radius < 0 || Float.isNaN(radius)) {
			throw new IllegalArgumentException("invalid radius: " + radius);
		}
		this.radius = radius;
	}

}
