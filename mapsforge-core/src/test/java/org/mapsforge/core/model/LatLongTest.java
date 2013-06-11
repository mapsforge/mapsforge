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

public class LatLongTest {
	private static final String GEO_POINT_TO_STRING = "latitude=1.0, longitude=2.0";
	private static final double LATITUDE = 1.0;
	private static final double LONGITUDE = 2.0;

	@Test
	public void compareToTest() {
		LatLong latLong1 = new LatLong(LATITUDE, LONGITUDE);
		LatLong latLong2 = new LatLong(LATITUDE, LONGITUDE);
		LatLong latLong3 = new LatLong(LATITUDE, LATITUDE);
		LatLong latLong4 = new LatLong(LONGITUDE, LONGITUDE);

		Assert.assertEquals(0, latLong1.compareTo(latLong2));

		TestUtils.notCompareToTest(latLong1, latLong3);
		TestUtils.notCompareToTest(latLong1, latLong4);
	}

	@Test
	public void equalsTest() {
		LatLong latLong1 = new LatLong(LATITUDE, LONGITUDE);
		LatLong latLong2 = new LatLong(LATITUDE, LONGITUDE);
		LatLong latLong3 = new LatLong(LATITUDE, LATITUDE);
		LatLong latLong4 = new LatLong(LONGITUDE, LONGITUDE);

		TestUtils.equalsTest(latLong1, latLong2);

		TestUtils.notEqualsTest(latLong1, latLong3);
		TestUtils.notEqualsTest(latLong1, latLong4);
		TestUtils.notEqualsTest(latLong1, new Object());
		TestUtils.notEqualsTest(latLong1, null);
	}

	@Test
	public void fieldsTest() {
		LatLong latLong = new LatLong(LATITUDE, LONGITUDE);
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
