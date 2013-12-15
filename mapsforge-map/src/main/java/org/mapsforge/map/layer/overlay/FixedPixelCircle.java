/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
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

import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;

/**
 * A Circle class that is always drawn with the same size in pixels.
 */
public class FixedPixelCircle extends Circle {
	/**
	 * @param latLong
	 *            the initial center point of this circle (may be null).
	 * @param radius
	 *            the initial non-negative radius of this circle in pixels.
	 * @param paintFill
	 *            the initial {@code Paint} used to fill this circle (may be null).
	 * @param paintStroke
	 *            the initial {@code Paint} used to stroke this circle (may be null).
	 * @throws IllegalArgumentException
	 *             if the given {@code radius} is negative or {@link Float#NaN}.
	 */
	public FixedPixelCircle(LatLong latLong, float radius, Paint paintFill, Paint paintStroke) {
		super(latLong, radius, paintFill, paintStroke);
	}

	protected int getRadiusInPixels(double latitude, byte zoomLevel) {
		// TODO this needs to be scaled according to display resolution, but
		// TODO here we do not have access to the AndroidGraphicFactory
		return (int) this.getRadius();
	}

	public boolean contains(Point center, Point point) {
		return center.distance(point) < this.getRadius();
	}

}
