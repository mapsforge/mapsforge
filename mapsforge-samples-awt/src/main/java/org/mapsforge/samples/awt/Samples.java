/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Christian Pesch
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2014-2022 devemux86
 * Copyright 2017-2022 usrusr
 * Copyright 2024 Sublimis
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

import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.util.LatLongUtils;
import org.mapsforge.core.util.Parameters;
import org.mapsforge.map.awt.graphics.AwtGraphicFactory;
import org.mapsforge.map.awt.util.AwtUtil;
import org.mapsforge.map.awt.view.MapView;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.datastore.MultiMapDataStore;
import org.mapsforge.map.layer.Layers;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.debug.TileCoordinatesLayer;
import org.mapsforge.map.layer.debug.TileGridLayer;
import org.mapsforge.map.layer.download.TileDownloadLayer;
import org.mapsforge.map.layer.download.tilesource.OpenStreetMapMapnik;
import org.mapsforge.map.layer.download.tilesource.TileSource;
import org.mapsforge.map.layer.hills.AdaptiveClasyHillShading;
import org.mapsforge.map.layer.hills.DemFolderFS;
import org.mapsforge.map.layer.hills.HillsRenderConfig;
import org.mapsforge.map.layer.hills.MemoryCachingHgtReaderTileSource;
import org.mapsforge.map.layer.labels.LabelLayer;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.model.Model;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.rendertheme.internal.MapsforgeThemes;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.prefs.Preferences;

public final class Samples {
    private static final GraphicFactory GRAPHIC_FACTORY = AwtGraphicFactory.INSTANCE;
    private static final boolean SHOW_DEBUG_LAYERS = false;
    private static final boolean SHOW_RASTER_MAP = false;

    private static final String MESSAGE = "Are you sure you want to exit the application?";
    private static final String TITLE = "Confirm close";

    private static final String LATITUDE = "latitude";
    private static final String LONGITUDE = "longitude";
    private static final String ZOOM_LEVEL = "zoomLevel";

    /**
     * Starts the {@code Samples}.
     *
     * @param args command line args: expects the map files as multiple parameters
     *             with possible SRTM hgt folder as 1st argument.
     */
    public static void main(String[] args) {
        // Square frame buffer
        Parameters.SQUARE_FRAME_BUFFER = false;

        final HillsRenderConfig hillsConfig;
        File demFolder = getDemFolder(args);
        if (demFolder != null) {
            final AdaptiveClasyHillShading algorithm = new AdaptiveClasyHillShading()
                    // You can make additional behavior adjustments
                    .setAdaptiveZoomEnabled(true)
                    // .setZoomMinOverride(0)
                    // .setZoomMaxOverride(17)
                    .setCustomQualityScale(1);

            MemoryCachingHgtReaderTileSource tileSource = new MemoryCachingHgtReaderTileSource(new DemFolderFS(demFolder), algorithm, AwtGraphicFactory.INSTANCE);

            hillsConfig = new HillsRenderConfig(tileSource);

            // You can override theme values
            // hillsConfig.setMagnitudeScaleFactor(1);
            // hillsConfig.setColor(0xff000000);

            hillsConfig.indexOnThread();
            args = Arrays.copyOfRange(args, 1, args.length);
        } else {
            hillsConfig = null;
        }

        final List<File> mapFiles = SHOW_RASTER_MAP ? null : getMapFiles(args);
        final MapView mapView = createMapView();

        final JavaPreferences preferences = new JavaPreferences(Preferences.userNodeForPackage(Samples.class));

        final JFrame frame = new JFrame();
        frame.setTitle("Mapsforge Samples");
        frame.add(mapView);
        frame.pack();
        frame.setSize(1024, 768);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int result = JOptionPane.showConfirmDialog(frame, MESSAGE, TITLE, JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                    final MapViewPosition mapViewPosition = mapView.getModel().mapViewPosition;
                    preferences.putDouble(LATITUDE, mapViewPosition.getCenter().latitude);
                    preferences.putDouble(LONGITUDE, mapViewPosition.getCenter().longitude);
                    preferences.putByte(ZOOM_LEVEL, mapViewPosition.getZoomLevel());
                    mapView.destroyAll();
                    AwtGraphicFactory.clearResourceMemoryCache();
                    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                }
            }

            @Override
            public void windowOpened(WindowEvent e) {
                final BoundingBox boundingBox = addLayers(mapView, mapFiles, hillsConfig);
                final Model model = mapView.getModel();
                double latitude = preferences.getDouble(LATITUDE, 0);
                double longitude = preferences.getDouble(LONGITUDE, 0);
                byte zoomLevel = preferences.getByte(ZOOM_LEVEL, (byte) 0);
                mapView.getModel().mapViewPosition.setMapPosition(new MapPosition(new LatLong(latitude, longitude), zoomLevel));
                if (model.mapViewPosition.getZoomLevel() == 0 || !boundingBox.contains(model.mapViewPosition.getCenter())) {
                    byte zoomForBounds = LatLongUtils.zoomForBounds(model.mapViewDimension.getDimension(), boundingBox, model.displayModel.getTileSize());
                    model.mapViewPosition.setMapPosition(new MapPosition(boundingBox.getCenterPoint(), zoomForBounds));
                }
            }
        });
        frame.setVisible(true);
    }

    private static BoundingBox addLayers(MapView mapView, List<File> mapFiles, HillsRenderConfig hillsRenderConfig) {
        Layers layers = mapView.getLayerManager().getLayers();

        int tileSize = SHOW_RASTER_MAP ? 256 : 512;

        // Tile cache
        TileCache tileCache = AwtUtil.createTileCache(
                tileSize,
                mapView.getModel().frameBufferModel.getOverdrawFactor(),
                1024,
                new File(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString()));

        final BoundingBox boundingBox;
        if (SHOW_RASTER_MAP) {
            // Raster
            mapView.getModel().displayModel.setFixedTileSize(tileSize);
            OpenStreetMapMapnik tileSource = OpenStreetMapMapnik.INSTANCE;
            tileSource.setUserAgent("mapsforge-samples-awt");
            TileDownloadLayer tileDownloadLayer = createTileDownloadLayer(tileCache, mapView.getModel().mapViewPosition, tileSource);
            layers.add(tileDownloadLayer);
            tileDownloadLayer.start();
            mapView.setZoomLevelMin(tileSource.getZoomLevelMin());
            mapView.setZoomLevelMax(tileSource.getZoomLevelMax());
            boundingBox = new BoundingBox(LatLongUtils.LATITUDE_MIN, LatLongUtils.LONGITUDE_MIN, LatLongUtils.LATITUDE_MAX, LatLongUtils.LONGITUDE_MAX);
        } else {
            // Vector
            mapView.getModel().displayModel.setFixedTileSize(tileSize);
            final MultiMapDataStore multiMapDataStore = new MultiMapDataStore(MultiMapDataStore.DataPolicy.DEDUPLICATE);
            for (File file : mapFiles) {
                final MapFile mapFileDataStore = new MapFile(file);

                if (getWorldMapFileName().equalsIgnoreCase(file.getName())) {
                    mapFileDataStore.setPriority(-1);
                }
                multiMapDataStore.addMapDataStore(mapFileDataStore, false, false);
            }
            TileRendererLayer tileRendererLayer = createTileRendererLayer(tileCache, multiMapDataStore, mapView.getModel().mapViewPosition, hillsRenderConfig);
            layers.add(tileRendererLayer);
            LabelLayer labelLayer = new LabelLayer(AwtGraphicFactory.INSTANCE, tileRendererLayer.getLabelStore());
            mapView.getLayerManager().getLayers().add(labelLayer);
            boundingBox = multiMapDataStore.boundingBox();
        }

        // Debug
        if (SHOW_DEBUG_LAYERS) {
            layers.add(new TileGridLayer(GRAPHIC_FACTORY, mapView.getModel().displayModel));
            layers.add(new TileCoordinatesLayer(GRAPHIC_FACTORY, mapView.getModel().displayModel));
        }

        return boundingBox;
    }

    private static MapView createMapView() {
        MapView mapView = new MapView();
        mapView.getMapScaleBar().setVisible(true);
        if (SHOW_DEBUG_LAYERS) {
            mapView.getFpsCounter().setVisible(true);
        }

        return mapView;
    }

    private static TileDownloadLayer createTileDownloadLayer(TileCache tileCache, final MapViewPosition mapViewPosition, TileSource tileSource) {
        return new TileDownloadLayer(tileCache, mapViewPosition, tileSource, GRAPHIC_FACTORY) {
            @Override
            public boolean onTap(LatLong tapLatLong, Point layerXY, Point tapXY) {
                final byte currentZoomLevel = mapViewPosition.getZoomLevel();
                System.out.println("Tap on: " + tapLatLong + ", zoom level: " + currentZoomLevel);
                return true;
            }
        };
    }

    private static TileRendererLayer createTileRendererLayer(TileCache tileCache, MapDataStore mapDataStore, final MapViewPosition mapViewPosition, HillsRenderConfig hillsRenderConfig) {
        TileRendererLayer tileRendererLayer = new TileRendererLayer(tileCache, mapDataStore, mapViewPosition, false, false, true, GRAPHIC_FACTORY, hillsRenderConfig) {
            @Override
            public boolean onTap(LatLong tapLatLong, Point layerXY, Point tapXY) {
                final byte currentZoomLevel = mapViewPosition.getZoomLevel();
                System.out.println("Tap on: " + tapLatLong + ", zoom level: " + currentZoomLevel);
                return true;
            }
        };
        tileRendererLayer.setXmlRenderTheme(MapsforgeThemes.MOTORIDER);
        return tileRendererLayer;
    }

    private static File getDemFolder(String[] args) {
        if (args.length == 0) {
            if (SHOW_RASTER_MAP) {
                return null;
            } else {
                throw new IllegalArgumentException("missing argument: <mapFile>");
            }
        }

        File demFolder = new File(args[0]);
        if (demFolder.exists() && demFolder.isDirectory() && demFolder.canRead()) {
            return demFolder;
        }
        return null;
    }

    private static List<File> getMapFiles(String[] args) {
        if (args.length == 0) {
            throw new IllegalArgumentException("missing argument: <mapFile>");
        }

        List<File> result = new ArrayList<>();
        for (String arg : args) {
            File mapFile = new File(arg);
            if (!mapFile.exists()) {
                System.err.println("file does not exist: " + mapFile);
            } else if (!mapFile.isFile()) {
                System.err.println("not a file: " + mapFile);
            } else if (!mapFile.canRead()) {
                System.err.println("cannot read file: " + mapFile);
            } else
                result.add(mapFile);
        }
        return result;
    }

    /**
     * @return the name of the low res world map file.
     */
    public static String getWorldMapFileName() {
        return "world.map";
    }

    private Samples() {
        throw new IllegalStateException();
    }
}
