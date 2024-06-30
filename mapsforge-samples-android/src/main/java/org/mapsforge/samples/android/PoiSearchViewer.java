/*
 * Copyright 2015-2019 devemux86
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
package org.mapsforge.samples.android;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Rotation;
import org.mapsforge.map.android.graphics.AndroidBitmap;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.GroupLayer;
import org.mapsforge.map.layer.overlay.Marker;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.poi.android.storage.AndroidPoiPersistenceManagerFactory;
import org.mapsforge.poi.storage.*;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Collection;

/**
 * POI search.<br/>
 * Long press on map to search inside visible bounding box.<br/>
 * Tap on POIs to show their name (in default locale).
 */
@SuppressWarnings("deprecation")
public class PoiSearchViewer extends DefaultTheme {

    private static final String POI_FILE = "berlin.poi";
    private static final String POI_CATEGORY = "Restaurants";

    private GroupLayer groupLayer;
    private PoiPersistenceManager persistenceManager;

    @Override
    protected void createLayers() {
        TileRendererLayer tileRendererLayer = new TileRendererLayer(tileCaches.get(0), getMapFile(),
                mapView.getModel().mapViewPosition, AndroidGraphicFactory.INSTANCE) {
            @Override
            public boolean onLongPress(LatLong tapLatLong, Point layerXY, Point tapXY) {
                // Clear overlays
                if (groupLayer != null) {
                    mapView.getLayerManager().getLayers().remove(groupLayer);
                }
                mapView.getLayerManager().redrawLayers();
                // POI search
                new PoiSearchTask(PoiSearchViewer.this, POI_CATEGORY).execute(mapView.getBoundingBox());
                return true;
            }
        };
        tileRendererLayer.setXmlRenderTheme(getRenderTheme());
        mapView.getLayerManager().getLayers().add(tileRendererLayer);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        persistenceManager = AndroidPoiPersistenceManagerFactory.getPoiPersistenceManager(new File(getExternalMediaDirs()[0], POI_FILE).getAbsolutePath());
    }

    @Override
    protected void onDestroy() {
        persistenceManager.close();

        super.onDestroy();
    }

    private class PoiSearchTask extends android.os.AsyncTask<BoundingBox, Void, Collection<PointOfInterest>> {
        private final WeakReference<PoiSearchViewer> weakActivity;
        private final String category;

        private PoiSearchTask(PoiSearchViewer activity, String category) {
            this.weakActivity = new WeakReference<>(activity);
            this.category = category;
        }

        @Override
        protected Collection<PointOfInterest> doInBackground(BoundingBox... params) {
            // Search POI
            try {
                PoiCategoryManager categoryManager = persistenceManager.getCategoryManager();
                PoiCategoryFilter categoryFilter = new ExactMatchPoiCategoryFilter();
                categoryFilter.addCategory(categoryManager.getPoiCategoryByTitle(category));
                return persistenceManager.findInRect(params[0], categoryFilter, null, null, Integer.MAX_VALUE, true);
            } catch (Throwable t) {
                Log.e(SamplesApplication.TAG, t.getMessage(), t);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Collection<PointOfInterest> pointOfInterests) {
            final PoiSearchViewer activity = weakActivity.get();
            if (activity == null) {
                return;
            }
            Toast.makeText(activity, category + ": " + (pointOfInterests != null ? pointOfInterests.size() : 0), Toast.LENGTH_SHORT).show();
            if (pointOfInterests == null) {
                return;
            }

            // Overlay POI
            groupLayer = new GroupLayer();
            Bitmap bitmap = new AndroidBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.marker_green));
            for (final PointOfInterest pointOfInterest : pointOfInterests) {
                Marker marker = new MarkerImpl(pointOfInterest.getLatLong(), bitmap, 0, -bitmap.getHeight() / 2, pointOfInterest);
                groupLayer.layers.add(marker);
            }
            mapView.getLayerManager().getLayers().add(groupLayer);
            mapView.getLayerManager().redrawLayers();
        }
    }

    private class MarkerImpl extends Marker {
        private final PointOfInterest pointOfInterest;

        private MarkerImpl(LatLong latLong, Bitmap bitmap, int horizontalOffset, int verticalOffset, PointOfInterest pointOfInterest) {
            super(latLong, bitmap, horizontalOffset, verticalOffset);
            this.pointOfInterest = pointOfInterest;
        }

        @Override
        public boolean onTap(LatLong tapLatLong, Point layerXY, Point tapXY) {
            // GroupLayer does not have a position, layerXY is null
            layerXY = mapView.getMapViewProjection().toPixels(getPosition());
            if (!Rotation.noRotation(mapView.getMapRotation()) && layerXY != null) {
                layerXY = mapView.getMapRotation().rotate(layerXY, true);
            }
            if (contains(layerXY, tapXY, mapView)) {
                Toast.makeText(PoiSearchViewer.this, pointOfInterest.getName(), Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        }
    }
}
