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
package org.mapsforge.map.writer.util;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.mapsforge.core.util.LatLongUtils;
import org.mapsforge.map.writer.model.TDNode;
import org.mapsforge.map.writer.model.TDWay;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

public final class JTSUtils {
	private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();
	private static final Logger LOGGER = Logger.getLogger(GeoUtils.class.getName());

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

	/**
	 * Converts a way with potential inner ways to a JTS geometry.
	 * 
	 * @param way
	 *            the way
	 * @param innerWays
	 *            the inner ways or null
	 * @return the JTS geometry
	 */
	public static Geometry toJtsGeometry(TDWay way, List<TDWay> innerWays) {
		if (way == null) {
			LOGGER.warning("way is null");
			return null;
		}

		if (way.isValidClosedLine()) {
			// a closed line
			if (way.isForcePolygonLine()) {
				// may build a single line string if inner ways are empty
				return buildMultiLineString(way, innerWays);
			}
			// a true polygon
			// may contain holes if inner ways are not empty
			Polygon polygon = buildPolygon(way, innerWays);
			if (polygon.isValid()) {
				return polygon;
			}
			return repairInvalidPolygon(polygon);
		}
		// not a closed line
		return buildLineString(way);
	}

	static LinearRing buildLinearRing(TDWay way) {
		Coordinate[] coordinates = JTSUtils.toCoordinates(way);
		return GEOMETRY_FACTORY.createLinearRing(coordinates);
	}

	static LineString buildLineString(TDWay way) {
		Coordinate[] coordinates = JTSUtils.toCoordinates(way);
		return GEOMETRY_FACTORY.createLineString(coordinates);
	}

	static MultiLineString buildMultiLineString(TDWay outerWay, List<TDWay> innerWays) {
		List<LineString> lineStrings = new ArrayList<>();
		// outer way geometry
		lineStrings.add(buildLineString(outerWay));

		// inner strings
		if (innerWays != null) {
			for (TDWay innerWay : innerWays) {
				LineString innerRing = buildLineString(innerWay);
				lineStrings.add(innerRing);
			}
		}

		return GEOMETRY_FACTORY.createMultiLineString(lineStrings.toArray(new LineString[lineStrings.size()]));
	}

	static Polygon buildPolygon(TDWay way) {
		Coordinate[] coordinates = JTSUtils.toCoordinates(way);
		return GEOMETRY_FACTORY.createPolygon(GEOMETRY_FACTORY.createLinearRing(coordinates), null);
	}

	static Polygon buildPolygon(TDWay outerWay, List<TDWay> innerWays) {
		if (innerWays == null || innerWays.isEmpty()) {
			return buildPolygon(outerWay);
		}

		// outer way geometry
		LinearRing outerRing = buildLinearRing(outerWay);

		// inner rings
		List<LinearRing> innerRings = new ArrayList<>();

		for (TDWay innerWay : innerWays) {
			// build linear ring
			LinearRing innerRing = buildLinearRing(innerWay);
			innerRings.add(innerRing);
		}

		if (!innerRings.isEmpty()) {
			// create new polygon
			LinearRing[] holes = innerRings.toArray(new LinearRing[innerRings.size()]);
			return GEOMETRY_FACTORY.createPolygon(outerRing, holes);
		}

		return null;
	}

	static Geometry repairInvalidPolygon(Geometry p) {
		if (p instanceof Polygon || p instanceof MultiPolygon) {
			// apply zero buffer trick
			Geometry ret = p.buffer(0);
			if (ret.getArea() > 0) {
				return ret;
			}
			LOGGER.fine("unable to repair invalid polygon");
			return null;
		}
		return p;
	}

	/**
	 * Internal conversion method to convert our internal data structure for ways to geometry objects in JTS. It will
	 * care about ways and polygons and will create the right JTS objects.
	 * 
	 * @param way
	 *            TDway which will be converted. Null if we were not able to convert the way to a Geometry object.
	 * @return return Converted way as JTS object.
	 */
	static Geometry toJTSGeometry(TDWay way) {
		return toJtsGeometry(way, null);
	}

	private static Coordinate toCoordinate(int latitude, int longitude) {
		return new Coordinate(LatLongUtils.microdegreesToDegrees(longitude),
				LatLongUtils.microdegreesToDegrees(latitude));
	}

	private JTSUtils() {
		throw new IllegalStateException();
	}
}
