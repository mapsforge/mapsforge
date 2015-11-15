/*
 * Copyright 2015 devemux86
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
package org.mapsforge.applications.android.samples;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import org.mapsforge.core.GeoCoordinate;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.Layers;
import org.mapsforge.map.layer.overlay.Circle;
import org.mapsforge.map.layer.overlay.FixedPixelCircle;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.storage.poi.ExactMatchPoiCategoryFilter;
import org.mapsforge.storage.poi.PoiCategoryFilter;
import org.mapsforge.storage.poi.PoiCategoryManager;
import org.mapsforge.storage.poi.PoiPersistenceManager;
import org.mapsforge.storage.poi.PoiPersistenceManagerFactory;
import org.mapsforge.storage.poi.PointOfInterest;

import java.util.Collection;

/**
 * POI search.<br/>
 * Long press to initiate search inside visible bounding box.
 */
public class PoiSearchViewer extends RenderTheme4 {
    private static final String POI_FILE = Environment.getExternalStorageDirectory() + "/germany.poi";
    private static final String POI_CATEGORY = "Restaurants";

    @Override
    protected void createLayers() {
        TileRendererLayer tileRendererLayer = new TileRendererLayer(
                this.tileCaches.get(0), getMapFile(),
                this.mapView.getModel().mapViewPosition,
                false, true, AndroidGraphicFactory.INSTANCE) {
            @Override
            public boolean onLongPress(LatLong tapLatLong, Point layerXY, Point tapXY) {
                PoiSearchViewer.this.onLongPress();
                return true;
            }
        };
        tileRendererLayer.setXmlRenderTheme(getRenderTheme());
        this.mapView.getLayerManager().getLayers().add(tileRendererLayer);
    }

    private void onLongPress() {
        // Clear overlays
        Layers layers = this.mapView.getLayerManager().getLayers();
        for (Layer layer : layers) {
            if (layer instanceof Circle) {
                layers.remove(layer);
            }
        }
        redrawLayers();

        // POI search
        new PoiSearchTask(POI_CATEGORY).execute(this.mapView.getBoundingBox());
    }

    private class PoiSearchTask extends AsyncTask<BoundingBox, Void, Collection<PointOfInterest>> {
        private final String category;

        private PoiSearchTask(String category) {
            this.category = category;
        }

        @Override
        protected Collection<PointOfInterest> doInBackground(BoundingBox... params) {
            PoiPersistenceManager persistenceManager = null;
            try {
                persistenceManager = PoiPersistenceManagerFactory.getSQLitePoiPersistenceManager(POI_FILE);
                PoiCategoryManager categoryManager = persistenceManager.getCategoryManager();
                PoiCategoryFilter categoryFilter = new ExactMatchPoiCategoryFilter();
                categoryFilter.addCategory(categoryManager.getPoiCategoryByTitle(this.category));
                return persistenceManager.findInRectWithFilter(
                        new GeoCoordinate(params[0].minLatitude, params[0].minLongitude),
                        new GeoCoordinate(params[0].maxLatitude, params[0].maxLongitude),
                        categoryFilter, -1);
            } catch (Exception e) {
                Log.e(SamplesApplication.TAG, e.getMessage(), e);
            } finally {
                if (persistenceManager != null) {
                    persistenceManager.close();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Collection<PointOfInterest> pointOfInterests) {
            if (pointOfInterests == null) {
                return;
            }
            Toast.makeText(PoiSearchViewer.this, category + ": " + pointOfInterests.size(), Toast.LENGTH_SHORT).show();

            for (PointOfInterest pointOfInterest : pointOfInterests) {
                Log.d(SamplesApplication.TAG, pointOfInterest.toString());
                LatLong latLong = new LatLong(pointOfInterest.getLatitude(), pointOfInterest.getLongitude());
                float circleSize = 16 * PoiSearchViewer.this.mapView.getModel().displayModel.getScaleFactor();
                Circle circle = new FixedPixelCircle(latLong, circleSize, Utils.createPaint(
                        AndroidGraphicFactory.INSTANCE.createColor(128, 255, 0, 0), 0, Style.FILL),
                        null);
                PoiSearchViewer.this.mapView.getLayerManager().getLayers().add(circle);
            }
            redrawLayers();
        }
    }
}
