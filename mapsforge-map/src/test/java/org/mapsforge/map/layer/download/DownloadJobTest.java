/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright © 2014 Ludwig M Brinckmann
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

import java.io.File;

import org.junit.Assert;
import org.junit.Test;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.TestUtils;
import org.mapsforge.map.layer.download.tilesource.OpenCycleMap;
import org.mapsforge.map.layer.download.tilesource.OpenStreetMapMapnik;
import org.mapsforge.map.layer.download.tilesource.TileSource;
import org.mapsforge.map.layer.renderer.RendererJob;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.rendertheme.InternalRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderTheme;

public class DownloadJobTest {
	private static int tileSize = 256;

	private static DownloadJob createDownloadJob(Tile tile, TileSource tileSource) {
		return new DownloadJob(tile, tileSize, tileSource);
	}

	private static void verifyInvalidConstructor(Tile tile, TileSource tileSource) {
		try {
			createDownloadJob(tile, tileSource);
			Assert.fail("tile: " + tile + ", tileSource: " + tileSource);
		} catch (NullPointerException e) {
			Assert.assertTrue(true);
		}
	}

	@Test
	public void downloadJobTest() {
		Tile tile = new Tile(0, 0, (byte) 0);
		TileSource tileSource = OpenStreetMapMapnik.INSTANCE;

		DownloadJob downloadJob = createDownloadJob(tile, tileSource);
		Assert.assertEquals(tile, downloadJob.tile);
		Assert.assertEquals(tileSource, downloadJob.tileSource);

		verifyInvalidConstructor(tile, null);
	}

	@Test
	public void equalsTest() {
		Tile tile = new Tile(0, 0, (byte) 0);
		DownloadJob downloadJob1 = new DownloadJob(tile, tileSize, OpenStreetMapMapnik.INSTANCE);
		DownloadJob downloadJob2 = new DownloadJob(tile, tileSize, OpenStreetMapMapnik.INSTANCE);
		DownloadJob downloadJob3 = new DownloadJob(tile, tileSize, OpenCycleMap.INSTANCE);

		TestUtils.equalsTest(downloadJob1, downloadJob2);

		Assert.assertNotEquals(downloadJob1, downloadJob3);
		Assert.assertNotEquals(downloadJob3, downloadJob1);
		Assert.assertNotEquals(downloadJob1, new Object());

		File mapFile = new File("map.file");
		XmlRenderTheme xmlRenderTheme = InternalRenderTheme.OSMARENDER;
		Assert.assertNotEquals(downloadJob1, new RendererJob(tile, mapFile, xmlRenderTheme, new DisplayModel(), 1,
				false));
	}
}
