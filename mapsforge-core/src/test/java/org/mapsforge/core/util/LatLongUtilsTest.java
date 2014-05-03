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
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Dimension;
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

	@Test
	public void zoomForBoundsTest() {
		// TODO rewrite this unit tests to make it easier to understand
		Dimension[] dimensions = { new Dimension(200, 300), new Dimension(500, 400), new Dimension(1000, 600),
				new Dimension(3280, 1780), new Dimension(100, 200), new Dimension(500, 200) };
		BoundingBox[] boundingBoxes = { new BoundingBox(12.2, 0, 34.3, 120), new BoundingBox(-30, 20, 30, 30),
				new BoundingBox(20.3, 100, 30.4, 120), new BoundingBox(4.4, 2, 4.5, 2.2),
				new BoundingBox(50.43, 12.23, 50.44, 12.24), new BoundingBox(50.43, 12, 50.44, 40) };
		int[] tileSizes = { 256, 512, 500, 620, 451 };

		byte[] results = { 2, 1, 1, 1, 1, 3, 2, 2, 2, 2, 4, 4, 4, 4, 4, 10, 10, 10, 10, 10, 14, 13, 14, 13, 14, 4, 3,
				3, 3, 3, 3, 2, 2, 2, 2, 3, 3, 3, 2, 3, 5, 4, 4, 4, 4, 11, 10, 11, 10, 11, 14, 14, 14, 14, 14, 5, 4, 4,
				4, 4, 5, 3, 3, 3, 3, 4, 3, 3, 3, 3, 6, 5, 5, 5, 5, 13, 11, 12, 11, 12, 15, 14, 14, 14, 14, 7, 5, 5, 5,
				5, 10, 7, 7, 6, 7, 9, 5, 5, 5, 6, 11, 8, 8, 7, 8, 17, 14, 14, 13, 14, 20, 16, 16, 16, 17, 16, 9, 9, 8,
				10, 1, 1, 1, 1, 1, 3, 2, 2, 2, 2, 4, 4, 4, 4, 4, 10, 10, 10, 10, 10, 14, 13, 13, 13, 13, 3, 3, 3, 3, 3,
				3, 2, 2, 2, 2, 3, 2, 2, 2, 2, 5, 4, 4, 4, 4, 11, 10, 11, 10, 11, 14, 13, 13, 13, 13, 5, 4, 4, 4, 4 };

		int i = 0;
		for (Dimension dimension : dimensions) {
			for (BoundingBox boundingBox : boundingBoxes) {
				for (int tileSize : tileSizes) {
					Assert.assertEquals(results[i], LatLongUtils.zoomForBounds(dimension, boundingBox, tileSize));
					++i;
				}
			}
		}
	}
}
