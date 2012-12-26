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
package org.mapsforge.core.model;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

public class PointTest {
	private static final String POINT_TO_STRING = "x=222.2, y=333.3";
	private static final double X = 222.2;
	private static final double Y = 333.3;

	@Test
	public void compareToTest() {
		Point point1 = new Point(X, Y);
		Point point2 = new Point(X, Y);
		Point point3 = new Point(0, 0);

		Assert.assertEquals(0, point1.compareTo(point2));
		Assert.assertNotEquals(0, point1.compareTo(point3));
		Assert.assertNotEquals(0, point3.compareTo(point1));
	}

	@Test
	public void equalsTest() {
		Point point1 = new Point(X, Y);
		Point point2 = new Point(X, Y);
		Point point3 = new Point(0, 0);

		TestUtils.equalsTest(point1, point2);

		Assert.assertNotEquals(point1, point3);
		Assert.assertNotEquals(point3, point1);
		Assert.assertNotEquals(point1, new Object());
	}

	@Test
	public void fieldsTest() {
		Point point = new Point(X, Y);
		Assert.assertEquals(X, point.x, 0);
		Assert.assertEquals(Y, point.y, 0);
	}

	@Test
	public void serializeTest() throws IOException, ClassNotFoundException {
		Point point = new Point(X, Y);
		TestUtils.serializeTest(point);
	}

	@Test
	public void toStringTest() {
		Point point = new Point(X, Y);
		Assert.assertEquals(POINT_TO_STRING, point.toString());
	}
}
