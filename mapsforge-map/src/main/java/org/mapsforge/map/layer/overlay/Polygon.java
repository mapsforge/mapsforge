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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.GeoPoint;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.layer.Layer;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Path.FillType;

/**
 * A {@code Polygon} draws a connected series of line segments specified by a list of {@link GeoPoint GeoPoints}. If the
 * first and the last {@code GeoPoint} are not equal, the {@code Polygon} will be closed automatically.
 * <p>
 * A {@code Polygon} holds two {@link Paint} objects to allow for different outline and filling. These paints define
 * drawing parameters such as color, stroke width, pattern and transparency. {@link Paint#setAntiAlias Anti-aliasing}
 * should be enabled to improve the overall drawing quality.
 */
public class Polygon extends Layer {
	private final List<GeoPoint> geoPoints = new CopyOnWriteArrayList<GeoPoint>();
	private Paint paintFill;
	private Paint paintStroke;

	/**
	 * @param paintFill
	 *            the initial {@code Paint} used to fill this polygon (may be null).
	 * @param paintStroke
	 *            the initial {@code Paint} used to stroke this polygon (may be null).
	 */
	public Polygon(Paint paintFill, Paint paintStroke) {
		this.paintFill = paintFill;
		this.paintStroke = paintStroke;
	}

	@Override
	public synchronized void draw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point canvasPosition) {
		if (this.geoPoints.isEmpty() || (this.paintStroke == null && this.paintFill == null)) {
			return;
		}

		Path path = new Path();
		path.setFillType(FillType.EVEN_ODD);
		boolean firstGeoPoint = true;

		for (GeoPoint geoPoint : this.geoPoints) {
			float x = (float) (MercatorProjection.longitudeToPixelX(geoPoint.longitude, zoomLevel) - canvasPosition.x);
			float y = (float) (MercatorProjection.latitudeToPixelY(geoPoint.latitude, zoomLevel) - canvasPosition.y);

			if (firstGeoPoint) {
				firstGeoPoint = false;
				path.moveTo(x, y);
			} else {
				path.lineTo(x, y);
			}
		}

		path.close();

		if (this.paintStroke != null) {
			canvas.drawPath(path, this.paintStroke);
		}
		if (this.paintFill != null) {
			canvas.drawPath(path, this.paintFill);
		}
	}

	/**
	 * @return a thread-safe list of GeoPoints in this polygon.
	 */
	public List<GeoPoint> getGeoPoints() {
		return this.geoPoints;
	}

	/**
	 * @return the {@code Paint} used to fill this polygon (may be null).
	 */
	public synchronized Paint getPaintFill() {
		return this.paintFill;
	}

	/**
	 * @return the {@code Paint} used to stroke this polygon (may be null).
	 */
	public synchronized Paint getPaintStroke() {
		return this.paintStroke;
	}

	/**
	 * @param paintFill
	 *            the new {@code Paint} used to fill this polygon (may be null).
	 */
	public synchronized void setPaintFill(Paint paintFill) {
		this.paintFill = paintFill;
	}

	/**
	 * @param paintStroke
	 *            the new {@code Paint} used to stroke this polygon (may be null).
	 */
	public synchronized void setPaintStroke(Paint paintStroke) {
		this.paintStroke = paintStroke;
	}
}
