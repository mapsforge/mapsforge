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
package org.mapsforge.map.layer.download;

import org.junit.Assert;
import org.junit.Test;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.TestUtils;
import org.mapsforge.map.layer.download.tilesource.OpenCycleMap;
import org.mapsforge.map.layer.download.tilesource.OpenStreetMapMapnik;
import org.mapsforge.map.layer.download.tilesource.TileSource;

public class DownloadJobTest {
	private static DownloadJob createDownloadJob(Tile tile, TileSource tileSource) {
		return new DownloadJob(tile, tileSource);
	}

	private static void verifyInvalidConstructor(Tile tile, TileSource tileSource) {
		try {
			createDownloadJob(tile, tileSource);
			Assert.fail("tile: " + tile + ", tileSource: " + tileSource);
		} catch (IllegalArgumentException e) {
			Assert.assertTrue(true);
		}
	}

	@Test
	public void downloadJobTest() {
		Tile tile = new Tile(0, 1, (byte) 2);
		TileSource tileSource = OpenStreetMapMapnik.create();

		DownloadJob downloadJob = createDownloadJob(tile, tileSource);
		Assert.assertEquals(tile, downloadJob.tile);
		Assert.assertEquals(tileSource, downloadJob.tileSource);

		verifyInvalidConstructor(tile, null);
	}

	@Test
	public void equalsTest() {
		DownloadJob downloadJob1 = new DownloadJob(new Tile(0, 1, (byte) 2), OpenStreetMapMapnik.create());
		DownloadJob downloadJob2 = new DownloadJob(new Tile(0, 1, (byte) 2), OpenStreetMapMapnik.create());
		DownloadJob downloadJob3 = new DownloadJob(new Tile(0, 1, (byte) 2), OpenCycleMap.create());

		TestUtils.equalsTest(downloadJob1, downloadJob2);

		Assert.assertNotEquals(downloadJob1, downloadJob3);
		Assert.assertNotEquals(downloadJob3, downloadJob1);
		Assert.assertNotEquals(downloadJob1, new Object());
	}
}
