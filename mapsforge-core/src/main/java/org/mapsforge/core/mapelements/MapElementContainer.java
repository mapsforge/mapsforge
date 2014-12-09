/*
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

package org.mapsforge.core.mapelements;

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.Display;
import org.mapsforge.core.graphics.Matrix;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Rectangle;

/**
 * The MapElementContainer is the abstract base class for annotations that can be placed on the
 * map, e.g. labels and icons.
 *
 * A MapElementContainer has a central pivot point, which denotes the geographic point for the entity
 * translated into absolute map pixels. The boundary denotes the space that the item requires
 * around this central point.
 *
 * A MapElementContainer has a priority (higher value means higher priority) that should be used to determine
 * the drawing order, i.e. elements with higher priority should be drawn before elements with lower
 * priority. If there is not enough space on the map, elements with lower priority should then not be
 * drawn.
 */
public abstract class MapElementContainer implements Comparable<MapElementContainer> {

	protected Rectangle boundary;
	protected Rectangle boundaryAbsolute;
	protected Display display;
	protected final int priority;
	protected final Point xy;

	protected MapElementContainer(Point xy, Display display, int priority) {
		this.xy = xy;
		this.display = display;
		this.priority = priority;
	}

	/**
	 * Compares elements according to their priority.
	 *
	 * @param other
	 * @return priority order
	 */

	@Override
	public int compareTo(MapElementContainer other) {
		if (this.priority < other.priority) {
			return -1;
		}
		if (this.priority > other.priority) {
			return 1;
		}
		return 0;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (!(obj instanceof MapElementContainer)) {
			return false;
		}
		MapElementContainer other = (MapElementContainer) obj;
		if (this.priority != other.priority) {
			return false;
		} else if (!this.xy.equals(other.xy)) {
			return false;
		}
		return true;
	}

	/**
	 * Drawing method: element will draw itself on canvas shifted by origin point of canvas and
	 * using the matrix if rotation is required.
	 *
	 * @param canvas
	 * @param origin
	 * @param matrix
	 */
	public abstract void draw(Canvas canvas, Point origin, Matrix matrix);

	/**
	 * Gets the pixel absolute boundary for this element.
	 *
	 * @return Rectangle with absolute pixel coordinates.
	 */
	protected Rectangle getBoundaryAbsolute() {
		if (boundaryAbsolute == null) {
			boundaryAbsolute = this.boundary.shift(xy);
		}
		return boundaryAbsolute;
	}

	public boolean intersects(Rectangle rectangle) {
		return this.getBoundaryAbsolute().intersects(rectangle);
	}

	/**
	 * Returns if MapElementContainers clash with each other
	 * @param other element to test against
	 * @return true if they overlap
	 */
	public boolean clashesWith(MapElementContainer other) {
		// if either of the elements is always drawn, the elements do not clash
		if (Display.ALWAYS == this.display || Display.ALWAYS == other.display) {
			return false;
		}
 		return this.getBoundaryAbsolute().intersects(other.getBoundaryAbsolute());
	}

	@Override
	public int hashCode() {
		int result = 7;
		result = 31 * result + xy.hashCode();
		result = 31 * result + priority;
		return result;
	}

	/**
	 * Gets the center point of this element.
	 * @return Point with absolute center pixel coordinates.
	 */
	public Point getPoint() {
		return this.xy;
	}

	public int getPriority() {
		return priority;
	}

	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("xy=");
		stringBuilder.append(this.xy);
		stringBuilder.append(", priority=");
		stringBuilder.append(this.priority);
		return stringBuilder.toString();
	}

}
