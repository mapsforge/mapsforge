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
package org.mapsforge.map.writer.util;

import org.mapsforge.core.model.CoordinatesUtil;
import org.mapsforge.map.writer.model.TDNode;
import org.mapsforge.map.writer.model.TDWay;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * @author bross
 */
public final class JTSUtils {
	/**
	 * Translates a {@link TDNode} object to a JTS {@link Coordinate}.
	 * 
	 * @param node
	 *            the node
	 * @return the coordinate
	 */
	public static Coordinate toCoordinate(TDNode node) {
		return toCoordinate(node.getLatitude(), node.getLongitude());
	}

	/**
	 * Translates a {@link TDWay} object to an array of JTS {@link Coordinate}.
	 * 
	 * @param way
	 *            the way
	 * @return the array of coordinates
	 */
	public static Coordinate[] toCoordinates(TDWay way) {
		Coordinate[] coordinates = new Coordinate[way.getWayNodes().length];
		if (way.isReversedInRelation()) {
			for (int i = 0; i < coordinates.length; i++) {
				coordinates[coordinates.length - 1 - i] = toCoordinate(way.getWayNodes()[i]);
			}
		} else {
			for (int i = 0; i < coordinates.length; i++) {
				coordinates[i] = toCoordinate(way.getWayNodes()[i]);
			}
		}
		return coordinates;
	}

	private static Coordinate toCoordinate(int latitude, int longitude) {
		return new Coordinate(CoordinatesUtil.microdegreesToDegrees(longitude),
				CoordinatesUtil.microdegreesToDegrees(latitude));
	}

	private JTSUtils() {
		throw new IllegalStateException();
	}
}
