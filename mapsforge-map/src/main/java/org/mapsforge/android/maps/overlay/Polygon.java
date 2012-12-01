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
package org.mapsforge.android.maps.overlay;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Point;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Path.FillType;

/**
 * A {@code Polygon} draws a list of {@link PolygonalChain PolygonalChains}. As a polygon represents a closed area, any
 * open {@code PolygonalChain} will automatically be closed during the draw process.
 * <p>
 * A {@code Polygon} holds two {@link Paint} objects to allow for different outline and filling. These paints define
 * drawing parameters such as color, stroke width, pattern and transparency. {@link Paint#setAntiAlias Anti-aliasing}
 * should be enabled to minimize visual distortions and to improve the overall drawing quality.
 */
public class Polygon implements OverlayItem {
	private Paint paintFill;
	private Paint paintStroke;
	private final List<PolygonalChain> polygonalChains;

	/**
	 * @param polygonalChains
	 *            the initial polygonal chains on this polygon (may be null).
	 * @param paintFill
	 *            the initial {@code Paint} used to fill this polygon (may be null).
	 * @param paintStroke
	 *            the initial {@code Paint} used to stroke this polygon (may be null).
	 */
	public Polygon(Collection<PolygonalChain> polygonalChains, Paint paintFill, Paint paintStroke) {
		if (polygonalChains == null) {
			this.polygonalChains = Collections.synchronizedList(new ArrayList<PolygonalChain>());
		} else {
			this.polygonalChains = Collections.synchronizedList(new ArrayList<PolygonalChain>(polygonalChains));
		}
		this.paintFill = paintFill;
		this.paintStroke = paintStroke;
	}

	@Override
	public synchronized boolean draw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point canvasPosition) {
		synchronized (this.polygonalChains) {
			if (this.polygonalChains.isEmpty() || (this.paintStroke == null && this.paintFill == null)) {
				return false;
			}

			Path path = new Path();
			path.setFillType(FillType.EVEN_ODD);
			for (int i = 0; i < this.polygonalChains.size(); ++i) {
				PolygonalChain polygonalChain = this.polygonalChains.get(i);
				Path closedPath = polygonalChain.draw(zoomLevel, canvasPosition, true);
				if (closedPath != null) {
					path.addPath(closedPath);
				}
			}

			if (this.paintStroke != null) {
				canvas.drawPath(path, this.paintStroke);
			}
			if (this.paintFill != null) {
				canvas.drawPath(path, this.paintFill);
			}
			return true;
		}
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
	 * @return a synchronized (thread-safe) list of all polygonal chains on this polygon. Manual synchronization on this
	 *         list is necessary when iterating over it.
	 */
	public List<PolygonalChain> getPolygonalChains() {
		synchronized (this.polygonalChains) {
			return this.polygonalChains;
		}
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
