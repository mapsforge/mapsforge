/*
 * Copyright 2017 Raymond Wu
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
package org.mapsforge.samples.awt;

import java.io.File;
import java.io.IOException;

import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.awt.graphics.AwtGraphicFactory;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.layer.cache.FileSystemTileCache;
import org.mapsforge.map.layer.labels.TileBasedLabelStore;
import org.mapsforge.map.layer.renderer.DatabaseRenderer;
import org.mapsforge.map.layer.renderer.RendererJob;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.model.FixedTileSizeDisplayModel;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.rendertheme.InternalRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderTheme;
import org.mapsforge.map.rendertheme.rule.RenderThemeFuture;

/**
 * This sample demo how to render & save a tile.
 */
public class JustDrawTile {
	
	private static final String HOME = System.getProperty("user.home");
	private static final String SAVE_PATH = "Documents/MyTiles";
	
	// Your compiled map. 
	private static final File DEFAULT_MAP_PATH = new File(HOME + "/osm-data/taiwan-taco.map");

	// Location you'd like to render.
	private static double LAT = 25.0808;
	private static double LNG = 121.5678;
	private static byte   ZOOM = 16;

	public static void main(String[] args) throws IOException {
		// Load map.
		File mapPath = null;
		if (args.length >= 2) {
			mapPath = new File(args[1]);
		}
		if (mapPath==null || !mapPath.exists()) {
			mapPath = DEFAULT_MAP_PATH;
		}
		if (!mapPath.exists()) {
			throw new IllegalStateException("File not found.");
		}
		MapDataStore mapData = new MapFile(mapPath);
		
		// Assign tile.
		final int ty = MercatorProjection.latitudeToTileY(LAT, ZOOM);
		final int tx = MercatorProjection.longitudeToTileX(LNG, ZOOM);
		Tile tile = new Tile(tx, ty, ZOOM, 256);
		
		// Create requirements.
		GraphicFactory gf = AwtGraphicFactory.INSTANCE;
		XmlRenderTheme theme = InternalRenderTheme.OSMARENDER;
		DisplayModel dm = new FixedTileSizeDisplayModel(256);
		RenderThemeFuture rtf = new RenderThemeFuture(gf, theme, dm);
		RendererJob theJob = new RendererJob(tile, mapData, rtf, dm, 1.0f, false, false);
		File cacheDir = new File(HOME, SAVE_PATH);
		FileSystemTileCache tileCache = new FileSystemTileCache(10, cacheDir, gf, false);
		TileBasedLabelStore tileBasedLabelStore = new TileBasedLabelStore(tileCache.getCapacityFirstLevel());
		
		// Create renderer.
		DatabaseRenderer renderer = new DatabaseRenderer(mapData, gf, tileCache, tileBasedLabelStore, true, true, null);
		
		// Create RendererTheme.
		Thread t = new Thread(rtf);
		t.start();
		
		// Draw tile and save as PNG.
		TileBitmap tb = renderer.executeJob(theJob);
		tileCache.put(theJob, tb);

		// Close map.
		mapData.close();
		
		System.out.printf("Tile has been saved at %s/%d/%d/%d.tile.\n", cacheDir.getPath(), ZOOM, tx, ty);
	}

}

