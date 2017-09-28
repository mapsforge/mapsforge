/*
 * Copyright 2015-2017 devemux86
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

import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.GroupLayer;
import org.mapsforge.map.layer.overlay.Marker;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.poi.android.storage.AndroidPoiPersistenceManagerFactory;
import org.mapsforge.poi.storage.ExactMatchPoiCategoryFilter;
import org.mapsforge.poi.storage.PoiCategoryFilter;
import org.mapsforge.poi.storage.PoiCategoryManager;
import org.mapsforge.poi.storage.PoiPersistenceManager;
import org.mapsforge.poi.storage.PointOfInterest;

import java.lang.ref.WeakReference;
import java.util.Collection;

/**
 * POI search.<br/>
 * Long press on map to search inside visible bounding box.<br/>
 * Tap on POIs to show their name (in default locale).
 */
public class PoiSearchViewer extends DefaultTheme {

    private static final String POI_FILE = Environment.getExternalStorageDirectory() + "/germany.poi";
    private static final String POI_CATEGORY = "Restaurants";

    private GroupLayer groupLayer;

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

    private class PoiSearchTask extends AsyncTask<BoundingBox, Void, Collection<PointOfInterest>> {
        private final WeakReference<PoiSearchViewer> weakActivity;
        private final String category;

        private PoiSearchTask(PoiSearchViewer activity, String category) {
            this.weakActivity = new WeakReference<>(activity);
            this.category = category;
        }

        @Override
        protected Collection<PointOfInterest> doInBackground(BoundingBox... params) {
            // Search POI
            PoiPersistenceManager persistenceManager = null;
            try {
                persistenceManager = AndroidPoiPersistenceManagerFactory.getPoiPersistenceManager(POI_FILE);
                PoiCategoryManager categoryManager = persistenceManager.getCategoryManager();
                PoiCategoryFilter categoryFilter = new ExactMatchPoiCategoryFilter();
                categoryFilter.addCategory(categoryManager.getPoiCategoryByTitle(category));
                return persistenceManager.findInRect(params[0], categoryFilter, null, Integer.MAX_VALUE);
            } catch (Throwable t) {
                Log.e(SamplesApplication.TAG, t.getMessage(), t);
            } finally {
                if (persistenceManager != null) {
                    persistenceManager.close();
                }
            }
            return null;
        }

        @SuppressWarnings("deprecation")
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
            Bitmap bitmap = AndroidGraphicFactory.convertToBitmap(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? activity.getDrawable(R.drawable.marker_green) : activity.getResources().getDrawable(R.drawable.marker_green));
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
            if (contains(layerXY, tapXY)) {
                Toast.makeText(PoiSearchViewer.this, pointOfInterest.getName(), Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        }
    }
}
