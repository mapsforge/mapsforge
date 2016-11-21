/*
 * Copyright 2014 Ludwig M Brinckmann
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

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.preference.PreferenceManager;
import android.view.View;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.map.android.graphics.AndroidBitmap;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.android.util.MapViewerTemplate;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.layer.overlay.ChildMarker;
import org.mapsforge.map.layer.overlay.GroupMarker;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.rendertheme.InternalRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderTheme;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * The simplest form of creating a map viewer based on the MapViewerTemplate.
 * It also demonstrates the use simplified cleanup operation at activity exit.
 */
public class GroupMarkerExample extends MapViewerTemplate {

    private static final GraphicFactory GRAPHIC_FACTORY = AndroidGraphicFactory.INSTANCE;

    /**
     * This MapViewer uses the deprecated built-in osmarender theme.
     *
     * @return the render theme to use
     */
    @Override
    protected XmlRenderTheme getRenderTheme() {
        return InternalRenderTheme.OSMARENDER;
    }

    /**
     * This MapViewer uses the standard xml layout in the Samples app.
     */
    @Override
    protected int getLayoutId() {
        return R.layout.mapviewer;
    }

    /**
     * The id of the mapview inside the layout.
     *
     * @return the id of the MapView inside the layout.
     */
    @Override
    protected int getMapViewId() {
        return R.id.mapView;
    }

    /**
     * The name of the map file.
     *
     * @return map file name
     */
    @Override
    protected String getMapFileName() {
        return "germany.map";
    }

    /**
     * Creates a simple tile renderer layer with the AndroidUtil helper.
     */
    @Override
    protected void createLayers() {
        TileRendererLayer tileRendererLayer = AndroidUtil.createTileRendererLayer(this.tileCaches.get(0),
                this.mapView.getModel().mapViewPosition,  getMapFile(), getRenderTheme(), false, true, false);
        this.mapView.getLayerManager().getLayers().add(tileRendererLayer);

        addGroupMarker();
    }

    @Override
    protected void createMapViews() {
        super.createMapViews();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            boolean hardwareAcceleration = sharedPreferences.getBoolean(SamplesApplication.SETTING_HARDWARE_ACCELERATION, true);
            if (!hardwareAcceleration) {
                mapView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            }
        }
    }

    /**
     * Creates the tile cache with the AndroidUtil helper
     */
    @Override
    protected void createTileCaches() {
        this.tileCaches.add(AndroidUtil.createTileCache(this, getPersistableId(),
                this.mapView.getModel().displayModel.getTileSize(), this.getScreenRatio(),
                this.mapView.getModel().frameBufferModel.getOverdrawFactor()));
    }

    /**
     * Add group marker
     *
     */
    private void addGroupMarker() {

            Drawable drawable;
            // small icon for maximum single item
            drawable = getResources().getDrawable(R.drawable.marker_green);
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setColorFilter(new PorterDuffColorFilter(Color.GREEN, PorterDuff.Mode.MULTIPLY));

            Bitmap bitmap = AndroidGraphicFactory.convertToBitmap(drawable, paint);

            final GroupMarker groupMarker = new GroupMarker(new LatLong(56.072035, 39.433590), bitmap, 0, 0,
                    mapView.getLayerManager().getLayers(), GRAPHIC_FACTORY.createPaint());
            groupMarker.getChildren()
                    .add(new ChildMarker(bitmap, 15, 0, groupMarker, GRAPHIC_FACTORY.createPaint()));
            groupMarker.getChildren()
                    .add(new ChildMarker(bitmap, 15, 0, groupMarker, GRAPHIC_FACTORY.createPaint()));
            groupMarker.getChildren()
                    .add(new ChildMarker(bitmap, 15, 0, groupMarker, GRAPHIC_FACTORY.createPaint()));
            groupMarker.getChildren()
                    .add(new ChildMarker(bitmap, 15, 0, groupMarker, GRAPHIC_FACTORY.createPaint()));
            groupMarker.getChildren()
                    .add(new ChildMarker(bitmap, 15, 0, groupMarker, GRAPHIC_FACTORY.createPaint()));
            groupMarker.getChildren()
                    .add(new ChildMarker(bitmap, 15, 0, groupMarker, GRAPHIC_FACTORY.createPaint()));
            groupMarker.getChildren()
                    .add(new ChildMarker(bitmap, 15, 0, groupMarker, GRAPHIC_FACTORY.createPaint()));
            groupMarker.getChildren()
                    .add(new ChildMarker(bitmap, 15, 0, groupMarker, GRAPHIC_FACTORY.createPaint()));
            groupMarker.getChildren()
                    .add(new ChildMarker(bitmap, 15, 0, groupMarker, GRAPHIC_FACTORY.createPaint()));

            mapView.addLayer(groupMarker);



    }

}
