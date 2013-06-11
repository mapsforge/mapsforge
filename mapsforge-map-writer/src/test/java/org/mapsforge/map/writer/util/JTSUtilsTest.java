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

import java.util.List;

import org.junit.Test;
import org.mapsforge.map.writer.model.TDWay;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.util.Assert;

public class JTSUtilsTest {
	@Test
	public void testBuildGeometryFromInValidPolygon() {
		String testfile = "invalid-polygon.wkt";
		String expectedfile = "invalid-polygon-repaired.wkt";

		List<TDWay> ways = MockingUtils.wktPolygonToWays(testfile);
		Geometry geometry = JTSUtils.toJtsGeometry(ways.get(0), ways.subList(1, ways.size()));
		Assert.isTrue(geometry instanceof Polygon);
		Assert.isTrue(geometry.isValid());

		Geometry expected = MockingUtils.readWKTFile(expectedfile);
		Assert.equals(expected, geometry);
	}

	@Test
	public void testBuildGeometryFromInValidPolygonWithHoles() {
		String testfile = "invalid-polygon-2-inner-rings.wkt";
		String expectedfile = "invalid-polygon-2-inner-rings-repaired.wkt";

		List<TDWay> ways = MockingUtils.wktPolygonToWays(testfile);
		Geometry geometry = JTSUtils.toJtsGeometry(ways.get(0), ways.subList(1, ways.size()));
		Assert.isTrue(geometry instanceof Polygon);
		Assert.isTrue(geometry.isValid());

		Geometry expected = MockingUtils.readWKTFile(expectedfile);
		Assert.equals(expected, geometry);
	}

	@Test
	public void testBuildGeometryFromValidPolygon() {
		String testfile = "valid-polygon.wkt";

		List<TDWay> ways = MockingUtils.wktPolygonToWays(testfile);
		Geometry geometry = JTSUtils.toJtsGeometry(ways.get(0), ways.subList(1, ways.size()));
		Assert.isTrue(geometry instanceof Polygon);
		Assert.isTrue(geometry.isValid());

		Geometry expected = MockingUtils.readWKTFile(testfile);
		Assert.equals(expected, geometry);
	}

	@Test
	public void testBuildInvalidPolygon() {
		String testfile = "invalid-polygon.wkt";

		List<TDWay> ways = MockingUtils.wktPolygonToWays(testfile);
		Polygon polygon = JTSUtils.buildPolygon(ways.get(0));
		Geometry expected = MockingUtils.readWKTFile(testfile);
		Assert.isTrue(!polygon.isValid());
		Assert.equals(expected, polygon);
	}

	@Test
	public void testBuildInValidPolygonWith2InnerRings() {
		String testfile = "invalid-polygon-2-inner-rings.wkt";

		List<TDWay> ways = MockingUtils.wktPolygonToWays(testfile);
		Polygon polygon = JTSUtils.buildPolygon(ways.get(0), ways.subList(1, ways.size()));
		Geometry expected = MockingUtils.readWKTFile(testfile);
		Assert.isTrue(!polygon.isValid());
		Assert.equals(expected, polygon);
	}

	@Test
	public void testBuildNonSimpleMultiLineString() {
		String testfile = "non-simple-multilinestring.wkt";

		List<TDWay> ways = MockingUtils.wktMultiLineStringToWays(testfile);
		MultiLineString mls = JTSUtils.buildMultiLineString(ways.get(0), ways.subList(1, ways.size()));
		Geometry expected = MockingUtils.readWKTFile(testfile);
		Assert.isTrue(!mls.isSimple());
		Assert.equals(expected, mls);
	}

	@Test
	public void testBuildValidMultiLineString() {
		String testfile = "valid-multilinestring.wkt";

		List<TDWay> ways = MockingUtils.wktMultiLineStringToWays(testfile);
		MultiLineString mls = JTSUtils.buildMultiLineString(ways.get(0), ways.subList(1, ways.size()));
		Geometry expected = MockingUtils.readWKTFile(testfile);
		Assert.isTrue(mls.isValid());
		Assert.equals(expected, mls);
	}

	@Test
	public void testBuildValidPolygon() {
		String testfile = "valid-polygon.wkt";

		List<TDWay> ways = MockingUtils.wktPolygonToWays(testfile);
		Polygon polygon = JTSUtils.buildPolygon(ways.get(0));
		Geometry expected = MockingUtils.readWKTFile(testfile);
		Assert.isTrue(polygon.isValid());
		Assert.equals(expected, polygon);
	}

	@Test
	public void testBuildValidPolygonWith2InnerRings() {
		String testfile = "valid-polygon-2-inner-rings.wkt";

		List<TDWay> ways = MockingUtils.wktPolygonToWays(testfile);
		Polygon polygon = JTSUtils.buildPolygon(ways.get(0), ways.subList(1, ways.size()));
		Geometry expected = MockingUtils.readWKTFile(testfile);
		Assert.isTrue(polygon.isValid());
		Assert.equals(expected, polygon);
	}
}
