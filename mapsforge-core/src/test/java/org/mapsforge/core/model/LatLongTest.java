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

public class LatLongTest {
	private static final String DELIMITER = ",";
	private static final String GEO_POINT_TO_STRING = "latitude=1.0, longitude=2.0";
	private static final double LATITUDE = 1.0;
	private static final double LONGITUDE = 2.0;

	private static void verifyInvalid(String string) {
		try {
			LatLong.fromString(string);
			Assert.fail(string);
		} catch (IllegalArgumentException e) {
			Assert.assertTrue(true);
		}
	}

	@Test
	public void compareToTest() {
		LatLong latLong1 = new LatLong(LATITUDE, LONGITUDE);
		LatLong latLong2 = new LatLong(LATITUDE, LONGITUDE);
		LatLong latLong3 = new LatLong(0, 0);

		Assert.assertEquals(0, latLong1.compareTo(latLong2));
		Assert.assertNotEquals(0, latLong1.compareTo(latLong3));
		Assert.assertNotEquals(0, latLong3.compareTo(latLong1));
	}

	@Test
	public void equalsTest() {
		LatLong latLong1 = new LatLong(LATITUDE, LONGITUDE);
		LatLong latLong2 = new LatLong(LATITUDE, LONGITUDE);
		LatLong latLong3 = new LatLong(0, 0);

		TestUtils.equalsTest(latLong1, latLong2);

		Assert.assertNotEquals(latLong1, latLong3);
		Assert.assertNotEquals(latLong3, latLong1);
		Assert.assertNotEquals(latLong1, new Object());
	}

	@Test
	public void fieldsTest() {
		LatLong latLong = new LatLong(LATITUDE, LONGITUDE);
		Assert.assertEquals(LATITUDE, latLong.latitude, 0);
		Assert.assertEquals(LONGITUDE, latLong.longitude, 0);
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
		LatLong latLong = LatLong.fromString(LATITUDE + DELIMITER + LONGITUDE);
		Assert.assertEquals(LATITUDE, latLong.latitude, 0);
		Assert.assertEquals(LONGITUDE, latLong.longitude, 0);
	}

	@Test
	public void serializeTest() throws IOException, ClassNotFoundException {
		LatLong latLong = new LatLong(LATITUDE, LONGITUDE);
		TestUtils.serializeTest(latLong);
	}

	@Test
	public void toStringTest() {
		LatLong latLong = new LatLong(LATITUDE, LONGITUDE);
		Assert.assertEquals(GEO_POINT_TO_STRING, latLong.toString());
	}
}
