/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
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
	private static AbstractTileSource create(String[] hostNames, int port) {
		return new OpenStreetMapMapnik(hostNames, port);
	}

	private static void verifyInvalidConstructor(String[] hostNames, int port) {
		try {
			create(hostNames, port);
			String names = "Empty host names";
			if (hostNames != null) {
				StringBuilder builder = new StringBuilder();
				for (String s : hostNames) {
					builder.append(s);
				}
				names = builder.toString();
			}
			Assert.fail("hostName: " + names + ", port: " + port);
		} catch (IllegalArgumentException e) {
			Assert.assertTrue(true);
		}
	}

	@Test
	public void constructorTest() {
		create(new String[] {"hostname"}, 0);

		verifyInvalidConstructor(null, 0);
		verifyInvalidConstructor(new String[] {""}, 0);
		verifyInvalidConstructor(new String[] {"hostname", ""}, 0);
		verifyInvalidConstructor(new String[] {"hostname"}, -1);
		verifyInvalidConstructor(new String[] {"hostname"}, Integer.MAX_VALUE);
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
