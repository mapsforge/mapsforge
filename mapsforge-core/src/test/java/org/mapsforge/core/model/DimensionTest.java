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

public class DimensionTest {
	private static final String POINT_TO_STRING = "width=1, height=2";

	private static Dimension createDimension(int width, int height) {
		return new Dimension(width, height);
	}

	private static void verifyInvalid(int width, int height) {
		try {
			createDimension(width, height);
			Assert.fail("width: " + width + ", height: " + height);
		} catch (IllegalArgumentException e) {
			Assert.assertTrue(true);
		}
	}

	@Test
	public void constructorTest() {
		Dimension dimension = new Dimension(0, 0);
		Assert.assertEquals(0, dimension.width);
		Assert.assertEquals(0, dimension.height);

		verifyInvalid(-1, 0);
		verifyInvalid(0, -1);
	}

	@Test
	public void equalsTest() {
		Dimension dimension1 = new Dimension(1, 2);
		Dimension dimension2 = new Dimension(1, 2);
		Dimension dimension3 = new Dimension(1, 1);
		Dimension dimension4 = new Dimension(2, 2);

		TestUtils.equalsTest(dimension1, dimension2);

		TestUtils.notEqualsTest(dimension1, dimension3);
		TestUtils.notEqualsTest(dimension1, dimension4);
		TestUtils.notEqualsTest(dimension1, new Object());
		TestUtils.notEqualsTest(dimension1, null);
	}

	@Test
	public void serializeTest() throws IOException, ClassNotFoundException {
		Dimension dimension = new Dimension(1, 2);
		TestUtils.serializeTest(dimension);
	}

	@Test
	public void toStringTest() {
		Dimension dimension = new Dimension(1, 2);
		Assert.assertEquals(POINT_TO_STRING, dimension.toString());
	}
}
