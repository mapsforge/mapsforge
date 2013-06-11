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
package org.mapsforge.map.layer.download.tilesource;

import org.junit.Assert;
import org.junit.Test;
import org.mapsforge.map.TestUtils;

public class AbstractTileSourceTest {
	private static AbstractTileSource create(String hostName, int port) {
		return new OpenStreetMapMapnik(hostName, port);
	}

	private static void verifyInvalidConstructor(String hostName, int port) {
		try {
			create(hostName, port);
			Assert.fail("hostName: " + hostName + ", port: " + port);
		} catch (IllegalArgumentException e) {
			Assert.assertTrue(true);
		}
	}

	@Test
	public void constructorTest() {
		create("preferences", 0);

		verifyInvalidConstructor(null, 0);
		verifyInvalidConstructor("", 0);
		verifyInvalidConstructor("preferences", -1);
		verifyInvalidConstructor("preferences", Integer.MAX_VALUE);
	}

	@Test
	public void equalsTest() {
		AbstractTileSource abstractTileSource1 = OpenStreetMapMapnik.INSTANCE;
		AbstractTileSource abstractTileSource2 = OpenStreetMapMapnik.INSTANCE;
		AbstractTileSource abstractTileSource3 = OpenCycleMap.INSTANCE;

		TestUtils.equalsTest(abstractTileSource1, abstractTileSource2);

		Assert.assertNotEquals(abstractTileSource1, abstractTileSource3);
		Assert.assertNotEquals(abstractTileSource3, abstractTileSource1);
		Assert.assertNotEquals(abstractTileSource1, new Object());
	}
}
