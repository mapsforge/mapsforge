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
package org.mapsforge.map.layer.download;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.HttpServerTest;
import org.mapsforge.map.awt.AwtGraphicFactory;
import org.mapsforge.map.layer.download.tilesource.OpenStreetMapMapnik;
import org.mapsforge.map.layer.download.tilesource.TileSource;

public class TileDownloaderTest extends HttpServerTest {
	private static final GraphicFactory GRAPHIC_FACTORY = AwtGraphicFactory.INSTANCE;

	private static TileDownloader createTileDownloader(DownloadJob downloadJob, GraphicFactory graphicFactory) {
		return new TileDownloader(downloadJob, graphicFactory);
	}

	private static void verifyInvalidConstructor(DownloadJob downloadJob, GraphicFactory graphicFactory) {
		try {
			createTileDownloader(downloadJob, graphicFactory);
			Assert.fail("downloadJob: " + downloadJob + ", graphicAdapter: " + graphicFactory);
		} catch (IllegalArgumentException e) {
			Assert.assertTrue(true);
		}
	}

	@Test
	public void constructorTest() {
		Tile tile = new Tile(0, 0, (byte) 0);
		TileSource tileSource = new OpenStreetMapMapnik("localhost", getPort());
		DownloadJob downloadJob = new DownloadJob(tile, tileSource);
		createTileDownloader(downloadJob, GRAPHIC_FACTORY);

		verifyInvalidConstructor(null, GRAPHIC_FACTORY);
		verifyInvalidConstructor(downloadJob, null);
	}

	@Test
	public void downloadImageTest() throws IOException {
		addFile("/0/0/0.png", new File("src/test/resources/0_0_0.png"));

		Tile tile = new Tile(0, 0, (byte) 0);
		TileSource tileSource = new OpenStreetMapMapnik("localhost", getPort());
		DownloadJob downloadJob = new DownloadJob(tile, tileSource);
		TileDownloader tileDownloader = new TileDownloader(downloadJob, GRAPHIC_FACTORY);
		Bitmap bitmap = tileDownloader.downloadImage();

		Assert.assertEquals(Tile.TILE_SIZE, bitmap.getWidth());
		Assert.assertEquals(Tile.TILE_SIZE, bitmap.getHeight());
	}
}
