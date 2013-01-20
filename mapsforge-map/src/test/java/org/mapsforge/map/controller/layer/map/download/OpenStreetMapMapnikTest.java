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
package org.mapsforge.map.controller.layer.map.download;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.mapsforge.core.model.Tile;

public class OpenStreetMapMapnikTest {
	private static OpenStreetMapMapnik create(Set<String> hostNames, int port) {
		return new OpenStreetMapMapnik(hostNames, port);
	}

	private static void verifyInvalidConstructor(Set<String> hostNames, int port) {
		try {
			create(hostNames, port);
			Assert.fail("hostNames: " + hostNames + ", port: " + port);
		} catch (IllegalArgumentException e) {
			Assert.assertTrue(true);
		}
	}

	@Test
	public void constructorTest() {
		create(new HashSet<String>(Arrays.asList("foo")), 0);

		verifyInvalidConstructor(null, 0);
		verifyInvalidConstructor(Collections.<String> emptySet(), 0);
		verifyInvalidConstructor(new HashSet<String>(Arrays.asList("foo")), -1);
		verifyInvalidConstructor(new HashSet<String>(Arrays.asList("foo")), Integer.MAX_VALUE);
	}

	@Test
	public void getParallelRequestsLimitTest() {
		TileSource tileSource = OpenStreetMapMapnik.create();
		Assert.assertTrue(tileSource.getParallelRequestsLimit() > 0);
	}

	@Test
	public void getTileUrlTest() throws MalformedURLException {
		TileSource tileSource = OpenStreetMapMapnik.create();

		URL tileUrl = tileSource.getTileUrl(new Tile(0, 1, (byte) 2));
		Assert.assertEquals("http://a.tile.openstreetmap.org:80/2/0/1.png", tileUrl.toExternalForm());

		tileUrl = tileSource.getTileUrl(new Tile(1, 1, (byte) 1));
		Assert.assertEquals("http://b.tile.openstreetmap.org:80/1/1/1.png", tileUrl.toExternalForm());

		tileUrl = tileSource.getTileUrl(new Tile(2, 2, (byte) 2));
		Assert.assertEquals("http://c.tile.openstreetmap.org:80/2/2/2.png", tileUrl.toExternalForm());

		tileUrl = tileSource.getTileUrl(new Tile(3, 3, (byte) 3));
		Assert.assertEquals("http://a.tile.openstreetmap.org:80/3/3/3.png", tileUrl.toExternalForm());
	}

	@Test
	public void getZoomLevelTest() {
		TileSource tileSource = OpenStreetMapMapnik.create();
		Assert.assertTrue(tileSource.getZoomLevelMin() <= tileSource.getZoomLevelMax());
	}
}
