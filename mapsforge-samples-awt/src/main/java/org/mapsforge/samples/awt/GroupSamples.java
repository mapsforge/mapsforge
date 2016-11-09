/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Christian Pesch
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2014-2016 devemux86
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

import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.WindowConstants;

import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.util.LatLongUtils;
import org.mapsforge.map.awt.graphics.AwtBitmap;
import org.mapsforge.map.awt.graphics.AwtGraphicFactory;
import org.mapsforge.map.awt.util.JavaPreferences;
import org.mapsforge.map.awt.view.MapView;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.datastore.MultiMapDataStore;
import org.mapsforge.map.layer.Layers;
import org.mapsforge.map.layer.cache.FileSystemTileCache;
import org.mapsforge.map.layer.cache.InMemoryTileCache;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.cache.TwoLevelTileCache;
import org.mapsforge.map.layer.debug.TileCoordinatesLayer;
import org.mapsforge.map.layer.debug.TileGridLayer;
import org.mapsforge.map.layer.overlay.ChildMarker;
import org.mapsforge.map.layer.overlay.GroupMarker;
import org.mapsforge.map.layer.renderer.MapWorkerPool;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.model.Model;
import org.mapsforge.map.model.common.PreferencesFacade;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.reader.ReadBuffer;
import org.mapsforge.map.rendertheme.InternalRenderTheme;

public final class GroupSamples {
    private static final GraphicFactory GRAPHIC_FACTORY = AwtGraphicFactory.INSTANCE;
    private static final boolean SHOW_DEBUG_LAYERS = false;

    private static final String MESSAGE = "Are you sure you want to exit the application?";
    private static final String TITLE = "Confirm close";

    /**
     * Starts the {@code Samples}.
     *
     * @param args
     *            command line args: expects the map files as multiple parameters.
     */
    public static void main(final String[] args) {
        // Increase read buffer limit
        ReadBuffer.setMaximumBufferSize(6500000);

        // Multithreading rendering
        MapWorkerPool.NUMBER_OF_THREADS = 2;

        final List<File> mapFiles = GroupSamples.getMapFiles(args);
        final MapView mapView = GroupSamples.createMapView();
        final BoundingBox boundingBox = GroupSamples.addLayers(mapView, mapFiles);

        final PreferencesFacade preferencesFacade = new JavaPreferences(
                Preferences.userNodeForPackage(GroupSamples.class));

        final JFrame frame = new JFrame();
        frame.setTitle("Mapsforge Samples");
        frame.add(mapView);
        frame.pack();
        frame.setSize(new Dimension(800, 600));
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent e) {
                final int result = JOptionPane.showConfirmDialog(frame, GroupSamples.MESSAGE, GroupSamples.TITLE,
                        JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                    mapView.getModel().save(preferencesFacade);
                    mapView.destroyAll();
                    AwtGraphicFactory.clearResourceMemoryCache();
                    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                }
            }

            @Override
            public void windowOpened(final WindowEvent e) {
                final Model model = mapView.getModel();
                // model.init(preferencesFacade);
                final byte zoomLevel = LatLongUtils.zoomForBounds(model.mapViewDimension.getDimension(), boundingBox,
                        model.displayModel.getTileSize());
                model.mapViewPosition.setMapPosition(new MapPosition(boundingBox.getCenterPoint(), zoomLevel));
            }
        });
        frame.setVisible(true);
    }

    private static BoundingBox addLayers(final MapView mapView, final List<File> mapFiles) {
        final Layers layers = mapView.getLayerManager().getLayers();

        // Raster
        /*
         * mapView.getModel().displayModel.setFixedTileSize(256); TileSource tileSource = OpenStreetMapMapnik.INSTANCE; TileDownloadLayer tileDownloadLayer =
         * createTileDownloadLayer(createTileCache(256), mapView.getModel().mapViewPosition, tileSource); layers.add(tileDownloadLayer);
         * tileDownloadLayer.start(); BoundingBox boundingBox = new BoundingBox(LatLongUtils.LATITUDE_MIN, LatLongUtils.LONGITUDE_MIN,
         * LatLongUtils.LATITUDE_MAX, LatLongUtils.LONGITUDE_MAX); mapView.setZoomLevelMin(tileSource.getZoomLevelMin());
         * mapView.setZoomLevelMax(tileSource.getZoomLevelMax());
         */

        // Vector
        mapView.getModel().displayModel.setFixedTileSize(512);
        final MultiMapDataStore mapDataStore = new MultiMapDataStore(MultiMapDataStore.DataPolicy.RETURN_ALL);
        for (final File file : mapFiles) {
            mapDataStore.addMapDataStore(new MapFile(file), false, false);
        }
        final TileRendererLayer tileRendererLayer = GroupSamples.createTileRendererLayer(
                GroupSamples.createTileCache(64), mapDataStore, mapView.getModel().mapViewPosition);
        layers.add(tileRendererLayer);
        final BoundingBox boundingBox = mapDataStore.boundingBox();

        // Debug
        if (GroupSamples.SHOW_DEBUG_LAYERS) {
            layers.add(new TileGridLayer(GroupSamples.GRAPHIC_FACTORY, mapView.getModel().displayModel));
            layers.add(new TileCoordinatesLayer(GroupSamples.GRAPHIC_FACTORY, mapView.getModel().displayModel));
        }

        GroupSamples.addGroupMarker(mapView);

        return boundingBox;
    }

    /**
     * Add group marker
     * 
     */
    private static void addGroupMarker(final MapView mapView) {

        try {
            final InputStream is = GroupSamples.class.getResourceAsStream("groupmarker.png");
            final AwtBitmap bitmap = new AwtBitmap(is);
            final GroupMarker groupMarker = new GroupMarker(new LatLong(56.072035, 39.433590), bitmap, 0, 0,
                    mapView.getLayerManager().getLayers(), GroupSamples.GRAPHIC_FACTORY.createPaint());
            groupMarker.getChildren()
                    .add(new ChildMarker(bitmap, 15, 0, groupMarker, GroupSamples.GRAPHIC_FACTORY.createPaint()));
            groupMarker.getChildren()
                    .add(new ChildMarker(bitmap, 15, 0, groupMarker, GroupSamples.GRAPHIC_FACTORY.createPaint()));
            groupMarker.getChildren()
                    .add(new ChildMarker(bitmap, 15, 0, groupMarker, GroupSamples.GRAPHIC_FACTORY.createPaint()));
            groupMarker.getChildren()
                    .add(new ChildMarker(bitmap, 15, 0, groupMarker, GroupSamples.GRAPHIC_FACTORY.createPaint()));
            groupMarker.getChildren()
                    .add(new ChildMarker(bitmap, 15, 0, groupMarker, GroupSamples.GRAPHIC_FACTORY.createPaint()));
            groupMarker.getChildren()
                    .add(new ChildMarker(bitmap, 15, 0, groupMarker, GroupSamples.GRAPHIC_FACTORY.createPaint()));
            groupMarker.getChildren()
                    .add(new ChildMarker(bitmap, 15, 0, groupMarker, GroupSamples.GRAPHIC_FACTORY.createPaint()));
            groupMarker.getChildren()
                    .add(new ChildMarker(bitmap, 15, 0, groupMarker, GroupSamples.GRAPHIC_FACTORY.createPaint()));
            groupMarker.getChildren()
                    .add(new ChildMarker(bitmap, 15, 0, groupMarker, GroupSamples.GRAPHIC_FACTORY.createPaint()));

            mapView.addLayer(groupMarker);
        } catch (final IOException e) {
            e.printStackTrace();
        }

    }

    private static MapView createMapView() {
        final MapView mapView = new MapView();
        mapView.getMapScaleBar().setVisible(true);
        if (GroupSamples.SHOW_DEBUG_LAYERS) {
            mapView.getFpsCounter().setVisible(true);
        }

        return mapView;
    }

    private static TileCache createTileCache(final int capacity) {
        final TileCache firstLevelTileCache = new InMemoryTileCache(capacity);
        final File cacheDirectory = new File(System.getProperty("java.io.tmpdir"), "mapsforge");
        final TileCache secondLevelTileCache = new FileSystemTileCache(1024, cacheDirectory,
                GroupSamples.GRAPHIC_FACTORY);
        return new TwoLevelTileCache(firstLevelTileCache, secondLevelTileCache);
    }

    private static TileRendererLayer createTileRendererLayer(final TileCache tileCache, final MapDataStore mapDataStore,
            final MapViewPosition mapViewPosition) {
        final TileRendererLayer tileRendererLayer = new TileRendererLayer(tileCache, mapDataStore, mapViewPosition,
                GroupSamples.GRAPHIC_FACTORY) {
            @Override
            public boolean onTap(final LatLong tapLatLong, final Point layerXY, final Point tapXY) {
                System.out.println("Tap on: " + tapLatLong);
                return true;
            }
        };
        tileRendererLayer.setXmlRenderTheme(InternalRenderTheme.OSMARENDER);
        return tileRendererLayer;
    }

    private static List<File> getMapFiles(final String[] args) {

        final List<File> result = new ArrayList<>();
        for (final String arg : args) {
            final File mapFile = new File(arg);
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

    private GroupSamples() {
        throw new IllegalStateException();
    }
}
