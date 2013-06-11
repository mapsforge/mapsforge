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
package org.mapsforge.core.model;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

public class PointTest {
	private static final String POINT_TO_STRING = "x=1.0, y=2.0";

	@Test
	public void compareToTest() {
		Point point1 = new Point(1, 2);
		Point point2 = new Point(1, 2);
		Point point3 = new Point(1, 1);
		Point point4 = new Point(2, 2);

		Assert.assertEquals(0, point1.compareTo(point2));

		TestUtils.notCompareToTest(point1, point3);
		TestUtils.notCompareToTest(point1, point4);
	}

	@Test
	public void distanceTest() {
		Point point1 = new Point(0, 0);
		Point point2 = new Point(1, 1);
		Point point3 = new Point(0, -1);

		Assert.assertEquals(0, point1.distance(point1), 0);
		Assert.assertEquals(0, point2.distance(point2), 0);
		Assert.assertEquals(0, point3.distance(point3), 0);

		Assert.assertEquals(Math.sqrt(2), point1.distance(point2), 0);
		Assert.assertEquals(Math.sqrt(2), point2.distance(point1), 0);

		Assert.assertEquals(1, point1.distance(point3), 0);
		Assert.assertEquals(1, point3.distance(point1), 0);
	}

	@Test
	public void equalsTest() {
		Point point1 = new Point(1, 2);
		Point point2 = new Point(1, 2);
		Point point3 = new Point(1, 1);
		Point point4 = new Point(2, 2);

		TestUtils.equalsTest(point1, point2);

		TestUtils.notEqualsTest(point1, point3);
		TestUtils.notEqualsTest(point1, point4);
		TestUtils.notEqualsTest(point1, new Object());
		TestUtils.notEqualsTest(point1, null);
	}

	@Test
	public void fieldsTest() {
		Point point = new Point(1, 2);
		Assert.assertEquals(1, point.x, 0);
		Assert.assertEquals(2, point.y, 0);
	}

	@Test
	public void serializeTest() throws IOException, ClassNotFoundException {
		Point point = new Point(1, 2);
		TestUtils.serializeTest(point);
	}

	@Test
	public void toStringTest() {
		Point point = new Point(1, 2);
		Assert.assertEquals(POINT_TO_STRING, point.toString());
	}
}
