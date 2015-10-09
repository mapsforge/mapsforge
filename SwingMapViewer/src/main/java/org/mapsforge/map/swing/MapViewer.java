/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Christian Pesch
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2014, 2015 devemux86
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

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.core.util.LatLongUtils;
import org.mapsforge.map.awt.AwtGraphicFactory;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.Layers;
import org.mapsforge.map.layer.cache.FileSystemTileCache;
import org.mapsforge.map.layer.cache.InMemoryTileCache;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.cache.TwoLevelTileCache;
import org.mapsforge.map.layer.debug.TileCoordinatesLayer;
import org.mapsforge.map.layer.debug.TileGridLayer;
import org.mapsforge.map.layer.download.TileDownloadLayer;
import org.mapsforge.map.layer.download.tilesource.OpenStreetMapMapnik;
import org.mapsforge.map.layer.download.tilesource.TileSource;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.model.Model;
import org.mapsforge.map.model.common.PreferencesFacade;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.reader.ReadBuffer;
import org.mapsforge.map.rendertheme.InternalRenderTheme;
import org.mapsforge.map.swing.controller.MapViewComponentListener;
import org.mapsforge.map.swing.controller.MouseEventListener;
import org.mapsforge.map.swing.util.JavaUtilPreferences;
import org.mapsforge.map.swing.view.MainFrame;
import org.mapsforge.map.swing.view.MapView;
import org.mapsforge.map.swing.view.WindowCloseDialog;

public final class MapViewer {
	private static final GraphicFactory GRAPHIC_FACTORY = AwtGraphicFactory.INSTANCE;
	private static final boolean SHOW_DEBUG_LAYERS = false;

	/**
	 * Starts the {@code MapViewer}.
	 * 
	 * @param args
	 *            command line args: expects the map files as multiple parameters.
	 */
	public static void main(String[] args) {
		// Increase read buffer limit
		ReadBuffer.setMaximumBufferSize(6500000);

		List<File> mapFiles = getMapFiles(args);
		MapView mapView = createMapView();
		final BoundingBox boundingBox = addLayers(mapView, mapFiles);

		PreferencesFacade preferencesFacade = new JavaUtilPreferences(Preferences.userNodeForPackage(MapViewer.class));
		final Model model = mapView.getModel();
		model.init(preferencesFacade);

		MainFrame mainFrame = new MainFrame();
		mainFrame.add(mapView);
		mainFrame.addWindowListener(new WindowCloseDialog(mainFrame, mapView, preferencesFacade));
		mainFrame.setVisible(true);

		mainFrame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowOpened(WindowEvent e) {
				byte zoomLevel = LatLongUtils.zoomForBounds(model.mapViewDimension.getDimension(), boundingBox,
						model.displayModel.getTileSize());
				model.mapViewPosition.setMapPosition(new MapPosition(boundingBox.getCenterPoint(), zoomLevel));
			}
		});
	}

	private static BoundingBox addLayers(MapView mapView, List<File> mapFiles) {
		Layers layers = mapView.getLayerManager().getLayers();

		// layers.add(createTileDownloadLayer(tileCache, mapView.getModel().mapViewPosition));
		BoundingBox result = null;
		for (int i = 0; i < mapFiles.size(); i++) {
			File mapFile = mapFiles.get(i);
			TileRendererLayer tileRendererLayer = createTileRendererLayer(createTileCache(i),
					mapView.getModel().mapViewPosition, true, true, mapFile);
			BoundingBox boundingBox = tileRendererLayer.getMapDataStore().boundingBox();
			result = result == null ? boundingBox : result.extendBoundingBox(boundingBox);
			layers.add(tileRendererLayer);
		}
		if (SHOW_DEBUG_LAYERS) {
			layers.add(new TileGridLayer(GRAPHIC_FACTORY, mapView.getModel().displayModel));
			layers.add(new TileCoordinatesLayer(GRAPHIC_FACTORY, mapView.getModel().displayModel));
		}
		return result;
	}

	private static MapView createMapView() {
		MapView mapView = new MapView();
		mapView.getMapScaleBar().setVisible(true);
		if (SHOW_DEBUG_LAYERS) {
			mapView.getFpsCounter().setVisible(true);
		}
		mapView.addComponentListener(new MapViewComponentListener(mapView, mapView.getModel().mapViewDimension));

		MouseEventListener mouseEventListener = new MouseEventListener(mapView.getModel());
		mapView.addMouseListener(mouseEventListener);
		mapView.addMouseMotionListener(mouseEventListener);
		mapView.addMouseWheelListener(mouseEventListener);

		return mapView;
	}

	private static TileCache createTileCache(int index) {
		TileCache firstLevelTileCache = new InMemoryTileCache(128);
		File cacheDirectory = new File(System.getProperty("java.io.tmpdir"), "mapsforge" + index);
		TileCache secondLevelTileCache = new FileSystemTileCache(1024, cacheDirectory, GRAPHIC_FACTORY);
		return new TwoLevelTileCache(firstLevelTileCache, secondLevelTileCache);
	}

	@SuppressWarnings("unused")
	private static Layer createTileDownloadLayer(TileCache tileCache, MapViewPosition mapViewPosition) {
		TileSource tileSource = OpenStreetMapMapnik.INSTANCE;
		TileDownloadLayer tileDownloadLayer = new TileDownloadLayer(tileCache, mapViewPosition, tileSource,
				GRAPHIC_FACTORY);
		tileDownloadLayer.start();
		return tileDownloadLayer;
	}

	private static TileRendererLayer createTileRendererLayer(
			TileCache tileCache,
			MapViewPosition mapViewPosition, boolean isTransparent, boolean renderLabels, File mapFile) {
		TileRendererLayer tileRendererLayer = new TileRendererLayer(tileCache, new MapFile(mapFile), mapViewPosition, isTransparent,
				renderLabels, GRAPHIC_FACTORY);
		tileRendererLayer.setXmlRenderTheme(InternalRenderTheme.OSMARENDER);
		return tileRendererLayer;
	}

	private static List<File> getMapFiles(String[] args) {
		if (args.length == 0) {
			throw new IllegalArgumentException("missing argument: <mapFile>");
		}

		List<File> result = new ArrayList<>();
		for(String arg : args) {
			File mapFile = new File(arg);
			if (!mapFile.exists()) {
				throw new IllegalArgumentException("file does not exist: " + mapFile);
			} else if (!mapFile.isFile()) {
				throw new IllegalArgumentException("not a file: " + mapFile);
			} else if (!mapFile.canRead()) {
				throw new IllegalArgumentException("cannot read file: " + mapFile);
			}
			result.add(mapFile);
		}
		return result;
	}

	private MapViewer() {
		throw new IllegalStateException();
	}
}
