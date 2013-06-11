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
package org.mapsforge.core.util;

import org.junit.Assert;
import org.junit.Test;
import org.mapsforge.core.model.LatLong;

public class LatLongUtilsTest {
	private static final double DEGREES = 123.456789;
	private static final String DELIMITER = ",";
	private static final double LATITUDE = 1.0;
	private static final double LONGITUDE = 2.0;
	private static final int MICRO_DEGREES = 123456789;

	private static void verifyInvalid(String string) {
		try {
			LatLongUtils.fromString(string);
			Assert.fail(string);
		} catch (IllegalArgumentException e) {
			Assert.assertTrue(true);
		}
	}

	private static void verifyInvalidLatitude(double latitude) {
		try {
			LatLongUtils.validateLatitude(latitude);
			Assert.fail("latitude: " + latitude);
		} catch (IllegalArgumentException e) {
			Assert.assertTrue(true);
		}
	}

	private static void verifyInvalidLongitude(double longitude) {
		try {
			LatLongUtils.validateLongitude(longitude);
			Assert.fail("longitude: " + longitude);
		} catch (IllegalArgumentException e) {
			Assert.assertTrue(true);
		}
	}

	@Test
	public void doubleToIntTest() {
		int microdegrees = LatLongUtils.degreesToMicrodegrees(DEGREES);
		Assert.assertEquals(MICRO_DEGREES, microdegrees);
	}

	@Test
	public void fromStringInvalidTest() {
		// invalid strings
		verifyInvalid("1,2,3");
		verifyInvalid("1,,2");
		verifyInvalid(",1,2");
		verifyInvalid("1,2,");
		verifyInvalid("1,a");
		verifyInvalid("1,");
		verifyInvalid("1");
		verifyInvalid("foo");
		verifyInvalid("");

		// invalid coordinates
		verifyInvalid("1,-181");
		verifyInvalid("1,181");
		verifyInvalid("-91,2");
		verifyInvalid("91,2");
	}

	@Test
	public void fromStringValidTest() {
		LatLong latLong = LatLongUtils.fromString(LATITUDE + DELIMITER + LONGITUDE);
		Assert.assertEquals(LATITUDE, latLong.latitude, 0);
		Assert.assertEquals(LONGITUDE, latLong.longitude, 0);
	}

	@Test
	public void intToDoubleTest() {
		double degrees = LatLongUtils.microdegreesToDegrees(MICRO_DEGREES);
		Assert.assertEquals(DEGREES, degrees, 0);
	}

	@Test
	public void validateLatitudeTest() {
		LatLongUtils.validateLatitude(LatLongUtils.LATITUDE_MAX);
		LatLongUtils.validateLatitude(LatLongUtils.LATITUDE_MIN);

		verifyInvalidLatitude(Double.NaN);
		verifyInvalidLatitude(Math.nextAfter(LatLongUtils.LATITUDE_MAX, Double.POSITIVE_INFINITY));
		verifyInvalidLatitude(Math.nextAfter(LatLongUtils.LATITUDE_MIN, Double.NEGATIVE_INFINITY));
	}

	@Test
	public void validateLongitudeTest() {
		LatLongUtils.validateLongitude(LatLongUtils.LONGITUDE_MAX);
		LatLongUtils.validateLongitude(LatLongUtils.LONGITUDE_MIN);

		verifyInvalidLongitude(Double.NaN);
		verifyInvalidLongitude(Math.nextAfter(LatLongUtils.LONGITUDE_MAX, Double.POSITIVE_INFINITY));
		verifyInvalidLongitude(Math.nextAfter(LatLongUtils.LONGITUDE_MIN, Double.NEGATIVE_INFINITY));
	}
}
