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

public class BoundingBoxTest {
	private static final String BOUNDING_BOX_TO_STRING = "minLatitude=2.0, minLongitude=1.0, maxLatitude=4.0, maxLongitude=3.0";
	private static final String DELIMITER = ",";
	private static final double MAX_LATITUDE = 4.0;
	private static final double MAX_LONGITUDE = 3.0;
	private static final double MIN_LATITUDE = 2.0;
	private static final double MIN_LONGITUDE = 1.0;

	private static void assertIntersection(BoundingBox boundingBox1, BoundingBox boundingBox2) {
		Assert.assertTrue(boundingBox1.intersects(boundingBox2));
		Assert.assertTrue(boundingBox2.intersects(boundingBox1));
	}

	private static void assertNoIntersection(BoundingBox boundingBox1, BoundingBox boundingBox2) {
		Assert.assertFalse(boundingBox1.intersects(boundingBox2));
		Assert.assertFalse(boundingBox2.intersects(boundingBox1));
	}

	private static void verifyInvalid(String string) {
		try {
			BoundingBox.fromString(string);
			Assert.fail(string);
		} catch (IllegalArgumentException e) {
			Assert.assertTrue(true);
		}
	}

	@Test
	public void containsTest() {
		BoundingBox boundingBox = new BoundingBox(MIN_LATITUDE, MIN_LONGITUDE, MAX_LATITUDE, MAX_LONGITUDE);
		LatLong latLong1 = new LatLong(MIN_LATITUDE, MIN_LONGITUDE);
		LatLong latLong2 = new LatLong(MAX_LATITUDE, MAX_LONGITUDE);
		LatLong latLong3 = new LatLong(MIN_LONGITUDE, MIN_LONGITUDE);
		LatLong latLong4 = new LatLong(MAX_LATITUDE, MAX_LATITUDE);

		Assert.assertTrue(boundingBox.contains(latLong1));
		Assert.assertTrue(boundingBox.contains(latLong2));
		Assert.assertFalse(boundingBox.contains(latLong3));
		Assert.assertFalse(boundingBox.contains(latLong4));
	}

	@Test
	public void equalsTest() {
		BoundingBox boundingBox1 = new BoundingBox(MIN_LATITUDE, MIN_LONGITUDE, MAX_LATITUDE, MAX_LONGITUDE);
		BoundingBox boundingBox2 = new BoundingBox(MIN_LATITUDE, MIN_LONGITUDE, MAX_LATITUDE, MAX_LONGITUDE);
		BoundingBox boundingBox3 = new BoundingBox(MAX_LATITUDE, MIN_LONGITUDE, MAX_LATITUDE, MAX_LONGITUDE);
		BoundingBox boundingBox4 = new BoundingBox(MIN_LATITUDE, MAX_LONGITUDE, MAX_LATITUDE, MAX_LONGITUDE);
		BoundingBox boundingBox5 = new BoundingBox(MIN_LATITUDE, MIN_LONGITUDE, MIN_LATITUDE, MAX_LONGITUDE);
		BoundingBox boundingBox6 = new BoundingBox(MIN_LATITUDE, MIN_LONGITUDE, MAX_LATITUDE, MIN_LONGITUDE);

		TestUtils.equalsTest(boundingBox1, boundingBox2);

		TestUtils.notEqualsTest(boundingBox1, boundingBox3);
		TestUtils.notEqualsTest(boundingBox1, boundingBox4);
		TestUtils.notEqualsTest(boundingBox1, boundingBox5);
		TestUtils.notEqualsTest(boundingBox1, boundingBox6);
		TestUtils.notEqualsTest(boundingBox1, new Object());
		TestUtils.notEqualsTest(boundingBox1, null);
	}

	@Test
	public void fieldsTest() {
		BoundingBox boundingBox = new BoundingBox(MIN_LATITUDE, MIN_LONGITUDE, MAX_LATITUDE, MAX_LONGITUDE);
		Assert.assertEquals(MIN_LATITUDE, boundingBox.minLatitude, 0);
		Assert.assertEquals(MIN_LONGITUDE, boundingBox.minLongitude, 0);
		Assert.assertEquals(MAX_LATITUDE, boundingBox.maxLatitude, 0);
		Assert.assertEquals(MAX_LONGITUDE, boundingBox.maxLongitude, 0);
	}

	@Test
	public void fromStringInvalidTest() {
		// invalid strings
		verifyInvalid("1,2,3,4,5");
		verifyInvalid("1,2,3,,4");
		verifyInvalid(",1,2,3,4");
		verifyInvalid("1,2,3,4,");
		verifyInvalid("1,2,3,a");
		verifyInvalid("1,2,3,");
		verifyInvalid("1,2,3");
		verifyInvalid("foo");
		verifyInvalid("");

		// invalid coordinates
		verifyInvalid("1,-181,3,4");
		verifyInvalid("1,2,3,181");
		verifyInvalid("-91,2,3,4");
		verifyInvalid("1,2,91,4");
		verifyInvalid("3,2,1,4");
		verifyInvalid("1,4,3,2");
	}

	@Test
	public void fromStringValidTest() {
		String boundingBoxString = MIN_LATITUDE + DELIMITER + MIN_LONGITUDE + DELIMITER + MAX_LATITUDE + DELIMITER
				+ MAX_LONGITUDE;
		BoundingBox boundingBox = BoundingBox.fromString(boundingBoxString);
		Assert.assertEquals(MIN_LATITUDE, boundingBox.minLatitude, 0);
		Assert.assertEquals(MIN_LONGITUDE, boundingBox.minLongitude, 0);
		Assert.assertEquals(MAX_LATITUDE, boundingBox.maxLatitude, 0);
		Assert.assertEquals(MAX_LONGITUDE, boundingBox.maxLongitude, 0);
	}

	@Test
	public void getCenterPointTest() {
		BoundingBox boundingBox = new BoundingBox(MIN_LATITUDE, MIN_LONGITUDE, MAX_LATITUDE, MAX_LONGITUDE);
		LatLong centerPoint = boundingBox.getCenterPoint();
		Assert.assertEquals((MIN_LATITUDE + MAX_LATITUDE) / 2, centerPoint.latitude, 0);
		Assert.assertEquals((MIN_LONGITUDE + MAX_LONGITUDE) / 2, centerPoint.longitude, 0);
	}

	@Test
	public void getLatitudeSpanTest() {
		BoundingBox boundingBox = new BoundingBox(MIN_LATITUDE, MIN_LONGITUDE, MAX_LATITUDE, MAX_LONGITUDE);
		Assert.assertEquals(MAX_LATITUDE - MIN_LATITUDE, boundingBox.getLatitudeSpan(), 0);
	}

	@Test
	public void getLongitudeSpanTest() {
		BoundingBox boundingBox = new BoundingBox(MIN_LATITUDE, MIN_LONGITUDE, MAX_LATITUDE, MAX_LONGITUDE);
		Assert.assertEquals(MAX_LONGITUDE - MIN_LONGITUDE, boundingBox.getLongitudeSpan(), 0);
	}

	@Test
	public void intersectsTest() {
		BoundingBox boundingBox1 = new BoundingBox(MIN_LATITUDE, MIN_LONGITUDE, MAX_LATITUDE, MAX_LONGITUDE);
		BoundingBox boundingBox2 = new BoundingBox(MIN_LATITUDE, MIN_LONGITUDE, MAX_LATITUDE, MAX_LONGITUDE);
		BoundingBox boundingBox3 = new BoundingBox(0, 0, MIN_LATITUDE, MIN_LONGITUDE);
		BoundingBox boundingBox4 = new BoundingBox(MIN_LATITUDE - 1, MIN_LONGITUDE - 1, MAX_LATITUDE + 1,
				MAX_LONGITUDE + 1);
		BoundingBox boundingBox5 = new BoundingBox(0, 0, 0, 0);
		BoundingBox boundingBox6 = new BoundingBox(-4, -3, -2, -1);

		assertIntersection(boundingBox1, boundingBox1);
		assertIntersection(boundingBox1, boundingBox2);
		assertIntersection(boundingBox1, boundingBox3);
		assertIntersection(boundingBox1, boundingBox4);

		assertNoIntersection(boundingBox1, boundingBox5);
		assertNoIntersection(boundingBox1, boundingBox6);
		assertNoIntersection(boundingBox5, boundingBox6);
	}

	@Test
	public void serializeTest() throws IOException, ClassNotFoundException {
		BoundingBox boundingBox = new BoundingBox(MIN_LATITUDE, MIN_LONGITUDE, MAX_LATITUDE, MAX_LONGITUDE);
		TestUtils.serializeTest(boundingBox);
	}

	@Test
	public void toStringTest() {
		BoundingBox boundingBox = new BoundingBox(MIN_LATITUDE, MIN_LONGITUDE, MAX_LATITUDE, MAX_LONGITUDE);
		Assert.assertEquals(BOUNDING_BOX_TO_STRING, boundingBox.toString());
	}
}
