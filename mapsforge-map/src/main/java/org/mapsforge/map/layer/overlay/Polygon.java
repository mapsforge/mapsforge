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

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Path;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.layer.Layer;

/**
 * A {@code Polygon} draws a connected series of line segments specified by a list of {@link LatLong LatLongs}. If the
 * first and the last {@code LatLong} are not equal, the {@code Polygon} will be closed automatically.
 * <p>
 * A {@code Polygon} holds two {@link Paint} objects to allow for different outline and filling. These paints define
 * drawing parameters such as color, stroke width, pattern and transparency.
 */
public class Polygon extends Layer {
	private final GraphicFactory graphicFactory;
	private final List<LatLong> latLongs = new CopyOnWriteArrayList<LatLong>();
	private Paint paintFill;
	private Paint paintStroke;

	/**
	 * @param paintFill
	 *            the initial {@code Paint} used to fill this polygon (may be null).
	 * @param paintStroke
	 *            the initial {@code Paint} used to stroke this polygon (may be null).
	 */
	public Polygon(Paint paintFill, Paint paintStroke, GraphicFactory graphicFactory) {
		super();

		this.paintFill = paintFill;
		this.paintStroke = paintStroke;
		this.graphicFactory = graphicFactory;
	}

	@Override
	public synchronized void draw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point topLeftPoint) {
		if (this.latLongs.size() < 2 || (this.paintStroke == null && this.paintFill == null)) {
			return;
		}

		Iterator<LatLong> iterator = this.latLongs.iterator();

		Path path = this.graphicFactory.createPath();
		LatLong latLong = iterator.next();
		int tileSize = displayModel.getTileSize();
		float x = (float) (MercatorProjection.longitudeToPixelX(latLong.longitude, zoomLevel, tileSize) - topLeftPoint.x);
		float y = (float) (MercatorProjection.latitudeToPixelY(latLong.latitude, zoomLevel, tileSize) - topLeftPoint.y);
		path.moveTo(x, y);

		while (iterator.hasNext()) {
			latLong = iterator.next();
			x = (float) (MercatorProjection.longitudeToPixelX(latLong.longitude, zoomLevel, tileSize) - topLeftPoint.x);
			y = (float) (MercatorProjection.latitudeToPixelY(latLong.latitude, zoomLevel, tileSize) - topLeftPoint.y);
			path.lineTo(x, y);
		}

		if (this.paintStroke != null) {
			canvas.drawPath(path, this.paintStroke);
		}
		if (this.paintFill != null) {
			canvas.drawPath(path, this.paintFill);
		}
	}

	/**
	 * @return a thread-safe list of LatLongs in this polygon.
	 */
	public List<LatLong> getLatLongs() {
		return this.latLongs;
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
