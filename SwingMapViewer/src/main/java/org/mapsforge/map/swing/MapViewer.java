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
package org.mapsforge.map.swing;

import java.awt.Component;
import java.io.File;
import java.util.List;
import java.util.prefs.Preferences;

import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.map.awt.AwtGraphicFactory;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.LayerManager;
import org.mapsforge.map.layer.cache.FileSystemTileCache;
import org.mapsforge.map.layer.cache.InMemoryTileCache;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.cache.TwoLevelTileCache;
import org.mapsforge.map.layer.download.TileDownloadLayer;
import org.mapsforge.map.layer.download.tilesource.OpenStreetMapMapnik;
import org.mapsforge.map.layer.download.tilesource.TileSource;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.model.Model;
import org.mapsforge.map.model.common.JavaUtilPreferences;
import org.mapsforge.map.model.common.PreferencesFacade;
import org.mapsforge.map.rendertheme.InternalRenderTheme;
import org.mapsforge.map.swing.controller.MapViewComponentListener;
import org.mapsforge.map.swing.controller.MouseEventListener;
import org.mapsforge.map.swing.view.MainFrame;
import org.mapsforge.map.swing.view.MapView;
import org.mapsforge.map.swing.view.WindowCloseDialog;

public final class MapViewer {
	private static final GraphicFactory GRAPHIC_FACTORY = AwtGraphicFactory.INSTANCE;

	public static void main(String[] args) {
		Model model = new Model();
		PreferencesFacade preferencesFacade = new JavaUtilPreferences(Preferences.userNodeForPackage(MapViewer.class));
		model.init(preferencesFacade);

		MainFrame mainFrame = new MainFrame();
		mainFrame.addWindowListener(new WindowCloseDialog(mainFrame, model, preferencesFacade));
		mainFrame.add(createMapView(model));

		mainFrame.setVisible(true);
	}

	private static Component createMapView(Model model) {
		MapView mapView = new MapView(model);
		mapView.addComponentListener(new MapViewComponentListener(mapView, model.mapViewModel));

		MouseEventListener mouseEventListener = new MouseEventListener(model);
		mapView.addMouseListener(mouseEventListener);
		mapView.addMouseMotionListener(mouseEventListener);
		mapView.addMouseWheelListener(mouseEventListener);

		LayerManager layerManager = mapView.getLayerManager();
		List<Layer> layers = layerManager.getLayers();
		TileCache tileCache = createTileCache();

		// layers.add(createTileDownloadLayer(tileCache, model.mapViewPosition, layerManager));
		layers.add(createTileRendererLayer(tileCache, model.mapViewPosition, layerManager));

		return mapView;
	}

	private static TileCache createTileCache() {
		TileCache firstLevelTileCache = new InMemoryTileCache(64);
		File cacheDirectory = new File(System.getProperty("java.io.tmpdir"), "mapsforge");
		TileCache secondLevelTileCache = new FileSystemTileCache(1024, cacheDirectory, GRAPHIC_FACTORY);
		return new TwoLevelTileCache(firstLevelTileCache, secondLevelTileCache);
	}

	private static Layer createTileDownloadLayer(TileCache tileCache, MapViewPosition mapViewPosition,
			LayerManager layerManager) {

		TileSource tileSource = OpenStreetMapMapnik.INSTANCE;
		return new TileDownloadLayer(tileCache, mapViewPosition, tileSource, layerManager, GRAPHIC_FACTORY);
	}

	private static Layer createTileRendererLayer(TileCache tileCache, MapViewPosition mapViewPosition,
			LayerManager layerManager) {
		TileRendererLayer tileRendererLayer = new TileRendererLayer(tileCache, mapViewPosition, layerManager,
				GRAPHIC_FACTORY);
		tileRendererLayer.setMapFile(new File("../../../Desktop/germany.map"));
		tileRendererLayer.setXmlRenderTheme(InternalRenderTheme.OSMARENDER);
		return tileRendererLayer;
	}

	private MapViewer() {
		throw new IllegalStateException();
	}
}
