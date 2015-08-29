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
package org.mapsforge.map.layer.renderer;

import org.mapsforge.core.model.Point;

final class GeometryUtils {
	/**
	 * Calculates the center of the minimum bounding rectangle for the given coordinates.
	 * 
	 * @param coordinates
	 *            the coordinates for which calculation should be done.
	 * @return the center coordinates of the minimum bounding rectangle.
	 */
	static Point calculateCenterOfBoundingBox(Point[] coordinates) {
		double pointXMin = coordinates[0].x;
		double pointXMax = coordinates[0].x;
		double pointYMin = coordinates[0].y;
		double pointYMax = coordinates[0].y;

		for (Point immutablePoint : coordinates) {
			if (immutablePoint.x < pointXMin) {
				pointXMin = immutablePoint.x;
			} else if (immutablePoint.x > pointXMax) {
				pointXMax = immutablePoint.x;
			}

			if (immutablePoint.y < pointYMin) {
				pointYMin = immutablePoint.y;
			} else if (immutablePoint.y > pointYMax) {
				pointYMax = immutablePoint.y;
			}
		}

		return new Point((pointXMin + pointXMax) / 2, (pointYMax + pointYMin) / 2);
	}

	private GeometryUtils() {
		throw new IllegalStateException();
	}
}
