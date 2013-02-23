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

import org.junit.Assert;
import org.junit.Test;

public class CoordinatesUtilTest {
	private static final double DEGREES = 123.456789;
	private static final int MICRO_DEGREES = 123456789;

	private static void verifyInvalidLatitude(double latitude) {
		try {
			CoordinatesUtil.validateLatitude(latitude);
			Assert.fail("latitude: " + latitude);
		} catch (IllegalArgumentException e) {
			Assert.assertTrue(true);
		}
	}

	private static void verifyInvalidLongitude(double longitude) {
		try {
			CoordinatesUtil.validateLongitude(longitude);
			Assert.fail("longitude: " + longitude);
		} catch (IllegalArgumentException e) {
			Assert.assertTrue(true);
		}
	}

	@Test
	public void doubleToIntTest() {
		int microdegrees = CoordinatesUtil.degreesToMicrodegrees(DEGREES);
		Assert.assertEquals(MICRO_DEGREES, microdegrees);
	}

	@Test
	public void intToDoubleTest() {
		double degrees = CoordinatesUtil.microdegreesToDegrees(MICRO_DEGREES);
		Assert.assertEquals(DEGREES, degrees, 0);
	}

	@Test
	public void validateLatitudeTest() {
		CoordinatesUtil.validateLatitude(CoordinatesUtil.LATITUDE_MAX);
		CoordinatesUtil.validateLatitude(CoordinatesUtil.LATITUDE_MIN);

		verifyInvalidLatitude(Double.NaN);
		verifyInvalidLatitude(Math.nextAfter(CoordinatesUtil.LATITUDE_MAX, Double.POSITIVE_INFINITY));
		verifyInvalidLatitude(Math.nextAfter(CoordinatesUtil.LATITUDE_MIN, Double.NEGATIVE_INFINITY));
	}

	@Test
	public void validateLongitudeTest() {
		CoordinatesUtil.validateLongitude(CoordinatesUtil.LONGITUDE_MAX);
		CoordinatesUtil.validateLongitude(CoordinatesUtil.LONGITUDE_MIN);

		verifyInvalidLongitude(Double.NaN);
		verifyInvalidLongitude(Math.nextAfter(CoordinatesUtil.LONGITUDE_MAX, Double.POSITIVE_INFINITY));
		verifyInvalidLongitude(Math.nextAfter(CoordinatesUtil.LONGITUDE_MIN, Double.NEGATIVE_INFINITY));
	}
}
