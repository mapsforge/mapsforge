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

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.GeoPoint;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.layer.Layer;

/**
 * A {@code Polyline} draws a connected series of line segments specified by a list of {@link GeoPoint GeoPoints}.
 * <p>
 * A {@code Polyline} holds a {@link Paint} object which defines drawing parameters such as color, stroke width, pattern
 * and transparency.
 */
public class Polyline extends Layer {
	private final List<GeoPoint> geoPoints = new CopyOnWriteArrayList<GeoPoint>();
	private Paint paintStroke;

	/**
	 * @param paintStroke
	 *            the initial {@code Paint} used to stroke this polyline (may be null).
	 */
	public Polyline(Paint paintStroke) {
		super();

		this.paintStroke = paintStroke;
	}

	@Override
	public synchronized void draw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point canvasPosition) {
		if (this.geoPoints.isEmpty() || this.paintStroke == null) {
			return;
		}

		float previousX = Float.NaN;
		float previousY = Float.NaN;

		for (GeoPoint geoPoint : this.geoPoints) {
			float x = (float) (MercatorProjection.longitudeToPixelX(geoPoint.longitude, zoomLevel) - canvasPosition.x);
			float y = (float) (MercatorProjection.latitudeToPixelY(geoPoint.latitude, zoomLevel) - canvasPosition.y);

			if (!Float.isNaN(previousX)) {
				canvas.drawLine(previousX, previousY, x, y, this.paintStroke);
			}

			previousX = x;
			previousY = y;
		}
	}

	/**
	 * @return a thread-safe list of GeoPoints in this polyline.
	 */
	public List<GeoPoint> getGeoPoints() {
		return this.geoPoints;
	}

	/**
	 * @return the {@code Paint} used to stroke this polyline (may be null).
	 */
	public synchronized Paint getPaintStroke() {
		return this.paintStroke;
	}

	/**
	 * @param paintStroke
	 *            the new {@code Paint} used to stroke this polyline (may be null).
	 */
	public synchronized void setPaintStroke(Paint paintStroke) {
		this.paintStroke = paintStroke;
	}
}
