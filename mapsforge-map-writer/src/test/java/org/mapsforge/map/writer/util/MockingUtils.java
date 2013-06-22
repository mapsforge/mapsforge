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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.mapsforge.core.util.LatLongUtils;
import org.mapsforge.map.writer.model.TDNode;
import org.mapsforge.map.writer.model.TDWay;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

public class MockingUtils {
	private static class MockTDNode extends TDNode {
		public MockTDNode(double lat, double lon) {
			super(0, LatLongUtils.degreesToMicrodegrees(lat), LatLongUtils.degreesToMicrodegrees(lon), (short) 0,
					(byte) 0, null, null);
		}
	}

	private static class MockTDWay extends TDWay {
		private final boolean area;

		public MockTDWay(TDNode[] wayNodes, boolean area) {
			super(0, (byte) 0, null, null, null, null, (byte) 0, wayNodes);
			this.area = area;
		}

		@Override
		public boolean isForcePolygonLine() {
			return !this.area;
		}
	}

	private static final GeometryFactory geometryFactory = new GeometryFactory();
	private static final String TEST_GEOMETRIES_RESOURCES_DIR = "src/test/resources/geometries";

	static Geometry readWKTFile(String wktFile) {
		File f = new File(TEST_GEOMETRIES_RESOURCES_DIR, wktFile);
		WKTReader wktReader = new WKTReader(geometryFactory);

		FileReader reader = null;
		try {
			reader = new FileReader(f);
			return wktReader.read(reader);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		} catch (ParseException e) {
			e.printStackTrace();
			return null;
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					// nothing to do
				}
			}
		}
	}

	static List<TDWay> wktMultiLineStringToWays(String wktFile) {
		Geometry geometry = readWKTFile(wktFile);
		if (geometry == null || !(geometry instanceof MultiLineString)) {
			return null;
		}

		MultiLineString mls = (MultiLineString) geometry;
		List<TDWay> ret = new ArrayList<>();
		for (int i = 0; i < mls.getNumGeometries(); i++) {
			ret.add(fromLinestring((LineString) mls.getGeometryN(i), false));
		}
		return ret;
	}

	static List<TDWay> wktPolygonToWays(String wktFile) {
		Geometry geometry = readWKTFile(wktFile);
		if (geometry == null || !(geometry instanceof Polygon)) {
			return null;
		}

		Polygon polygon = (Polygon) geometry;
		List<TDWay> ret = new ArrayList<>();
		TDWay outer = fromLinestring(polygon.getExteriorRing(), true);
		ret.add(outer);
		for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
			ret.add(fromLinestring(polygon.getInteriorRingN(i), false));
		}
		return ret;
	}

	private static TDNode fromCoordinate(Coordinate c) {
		return new MockTDNode(c.y, c.x);
	}

	private static TDNode[] fromCoordinates(Coordinate[] coordinates) {
		TDNode[] nodes = new TDNode[coordinates.length];
		for (int i = 0; i < coordinates.length; i++) {
			nodes[i] = fromCoordinate(coordinates[i]);
		}
		return nodes;
	}

	private static TDWay fromLinestring(LineString l, boolean area) {
		return new MockTDWay(fromCoordinates(l.getCoordinates()), area);
	}
}
