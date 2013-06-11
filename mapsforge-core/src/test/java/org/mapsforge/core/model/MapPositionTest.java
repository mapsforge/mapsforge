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

public class MapPositionTest {
	private static final String MAP_POSITION_TO_STRING = "latLong=latitude=1.0, longitude=2.0, zoomLevel=3";

	private static MapPosition invokeConstructor(LatLong latLong, byte zoomLevel) {
		return new MapPosition(latLong, zoomLevel);
	}

	private static void verifyBadConstructor(LatLong latLong, byte zoomLevel) {
		try {
			invokeConstructor(latLong, zoomLevel);
			Assert.fail("latLong: " + latLong + ", zoomLevel: " + zoomLevel);
		} catch (IllegalArgumentException e) {
			Assert.assertTrue(true);
		}
	}

	@Test
	public void badConstructorTest() {
		verifyBadConstructor(new LatLong(1.0, 2.0), (byte) -1);
		verifyBadConstructor(null, (byte) 0);
	}

	@Test
	public void equalsTest() {
		MapPosition mapPosition1 = new MapPosition(new LatLong(1.0, 2.0), (byte) 3);
		MapPosition mapPosition2 = new MapPosition(new LatLong(1.0, 2.0), (byte) 3);
		MapPosition mapPosition3 = new MapPosition(new LatLong(1.0, 2.0), (byte) 0);
		MapPosition mapPosition4 = new MapPosition(new LatLong(0, 0), (byte) 3);

		TestUtils.equalsTest(mapPosition1, mapPosition2);

		TestUtils.notEqualsTest(mapPosition1, mapPosition3);
		TestUtils.notEqualsTest(mapPosition1, mapPosition4);
		TestUtils.notEqualsTest(mapPosition1, new Object());
		TestUtils.notEqualsTest(mapPosition1, null);
	}

	@Test
	public void fieldsTest() {
		MapPosition mapPosition = new MapPosition(new LatLong(1.0, 2.0), (byte) 3);
		Assert.assertEquals(new LatLong(1.0, 2.0), mapPosition.latLong);
		Assert.assertEquals(3, mapPosition.zoomLevel);
	}

	@Test
	public void serializeTest() throws IOException, ClassNotFoundException {
		MapPosition mapPosition = new MapPosition(new LatLong(1.0, 2.0), (byte) 3);
		TestUtils.serializeTest(mapPosition);
	}

	@Test
	public void toStringTest() {
		MapPosition mapPosition = new MapPosition(new LatLong(1.0, 2.0), (byte) 3);
		Assert.assertEquals(MAP_POSITION_TO_STRING, mapPosition.toString());
	}
}
