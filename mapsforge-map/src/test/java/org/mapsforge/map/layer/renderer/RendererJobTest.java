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
package org.mapsforge.map.layer.renderer;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.TestUtils;
import org.mapsforge.map.layer.download.DownloadJob;
import org.mapsforge.map.layer.download.tilesource.OpenStreetMapMapnik;
import org.mapsforge.map.layer.download.tilesource.TileSource;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.rendertheme.InternalRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderTheme;

public class RendererJobTest {

	private static final int TILE_SIZE = 256;

	private static final MapFile MAP_FILE = MapFile.TEST_MAP_FILE;

	private static RendererJob create(Tile tile, MapFile mapFile, XmlRenderTheme xmlRenderTheme, float textScale) {
		return new RendererJob(tile, mapFile, xmlRenderTheme, new DisplayModel(), textScale, false, false);
	}

	private static void verifyInvalidConstructor(Tile tile, MapFile mapFile, XmlRenderTheme xmlRenderTheme, float textScale) {
		try {
			create(tile, mapFile, xmlRenderTheme, textScale);
			Assert.fail("tile: " + tile + ", mapFile: " + mapFile + ", xmlRenderTheme: " + xmlRenderTheme
					+ ", textScale: " + textScale);
		} catch (IllegalArgumentException e) {
			Assert.assertTrue(true);
		}
	}

	@Test
	public void constructorTest() {
		Tile tile = new Tile(0, 0, (byte) 0, TILE_SIZE);
		XmlRenderTheme xmlRenderTheme = InternalRenderTheme.OSMARENDER;

		create(tile, MAP_FILE, xmlRenderTheme, 1);

		verifyInvalidConstructor(null, MAP_FILE, xmlRenderTheme, 1);
		verifyInvalidConstructor(tile, null, xmlRenderTheme, 1);
		verifyInvalidConstructor(tile, MAP_FILE, null, 1);
		verifyInvalidConstructor(tile, MAP_FILE, xmlRenderTheme, 0);
		verifyInvalidConstructor(tile, MAP_FILE, xmlRenderTheme, -1);
		verifyInvalidConstructor(tile, MAP_FILE, xmlRenderTheme, Float.NEGATIVE_INFINITY);
		verifyInvalidConstructor(tile, MAP_FILE, xmlRenderTheme, Float.NaN);
	}

	@Test
	public void equalsTest() {
		XmlRenderTheme xmlRenderTheme = InternalRenderTheme.OSMARENDER;

		Tile tile = new Tile(0, 0, (byte) 0, TILE_SIZE);
		DisplayModel displayModel = new DisplayModel();
		RendererJob rendererJob1 = new RendererJob(tile, MAP_FILE, xmlRenderTheme, displayModel, 1, false, false);
		RendererJob rendererJob2 = new RendererJob(tile, MAP_FILE, xmlRenderTheme, displayModel, 1, false, false);
		RendererJob rendererJob3 = new RendererJob(tile, MAP_FILE, xmlRenderTheme, displayModel, 2, false, false);

		TestUtils.equalsTest(rendererJob1, rendererJob2);

		Assert.assertNotEquals(rendererJob1, rendererJob3);
		Assert.assertNotEquals(rendererJob3, rendererJob1);
		Assert.assertNotEquals(rendererJob1, new Object());

		TileSource tileSource = OpenStreetMapMapnik.INSTANCE;
		Assert.assertNotEquals(rendererJob1, new DownloadJob(tile, tileSource));
	}
}
