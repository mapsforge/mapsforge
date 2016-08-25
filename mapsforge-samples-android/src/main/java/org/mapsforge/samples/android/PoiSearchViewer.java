/*
 * Copyright 2015-2016 devemux86
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
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.GroupLayer;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.Layers;
import org.mapsforge.map.layer.overlay.Circle;
import org.mapsforge.map.layer.overlay.FixedPixelCircle;
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
 * Tap on POIs to show their name (in device's locale).
 */
public class PoiSearchViewer extends RenderTheme4 {
    private static final String POI_FILE = Environment.getExternalStorageDirectory() + "/germany.poi";
    private static final String POI_CATEGORY = "Restaurants";

    private static final Paint CIRCLE = Utils.createPaint(AndroidGraphicFactory.INSTANCE.createColor(128, 0, 0, 255), 0, Style.FILL);

    @Override
    protected void createLayers() {
        TileRendererLayer tileRendererLayer = new TileRendererLayer(
                this.tileCaches.get(0), getMapFile(),
                this.mapView.getModel().mapViewPosition,
                false, true, false, AndroidGraphicFactory.INSTANCE) {
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
            if (layer instanceof GroupLayer) {
                layers.remove(layer);
            }
        }
        redrawLayers();

        // POI search
        new PoiSearchTask(this, POI_CATEGORY).execute(this.mapView.getBoundingBox());
    }

    private static class PoiSearchTask extends AsyncTask<BoundingBox, Void, Collection<PointOfInterest>> {
        private final WeakReference<PoiSearchViewer> weakActivity;
        private final String category;

        private PoiSearchTask(PoiSearchViewer activity, String category) {
            this.weakActivity = new WeakReference<>(activity);
            this.category = category;
        }

        @Override
        protected Collection<PointOfInterest> doInBackground(BoundingBox... params) {
            PoiPersistenceManager persistenceManager = null;
            try {
                persistenceManager = AndroidPoiPersistenceManagerFactory.getPoiPersistenceManager(POI_FILE);
                PoiCategoryManager categoryManager = persistenceManager.getCategoryManager();
                PoiCategoryFilter categoryFilter = new ExactMatchPoiCategoryFilter();
                categoryFilter.addCategory(categoryManager.getPoiCategoryByTitle(this.category));
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

            GroupLayer groupLayer = new GroupLayer();
            for (final PointOfInterest pointOfInterest : pointOfInterests) {
                final Circle circle = new FixedPixelCircle(pointOfInterest.getLatLong(), 16, CIRCLE, null) {
                    @Override
                    public boolean onTap(LatLong tapLatLong, Point layerXY, Point tapXY) {
                        // GroupLayer does not have a position, layerXY is null
                        Point circleXY = activity.mapView.getMapViewProjection().toPixels(getPosition());
                        if (this.contains(circleXY, tapXY)) {
                            Toast.makeText(activity, pointOfInterest.getName(), Toast.LENGTH_SHORT).show();
                            return true;
                        }
                        return false;
                    }
                };
                groupLayer.layers.add(circle);
            }
            activity.mapView.getLayerManager().getLayers().add(groupLayer);
            activity.redrawLayers();
        }
    }
}
