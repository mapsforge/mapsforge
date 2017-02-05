/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2013 - 2014 Ludwig M Brinckmann
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
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.View;
import org.mapsforge.map.android.util.AndroidPreferences;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.model.common.PreferencesFacade;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.rendertheme.XmlRenderTheme;

import java.io.File;

/**
 * An activity with two independent MapViews.
 */
public class DualMapViewer extends DefaultTheme {

    protected MapView mapView2;
    protected PreferencesFacade preferencesFacade2;

    @Override
    protected void createLayers() {
        super.createLayers();
        createLayers2();
    }

    /**
     * creates the layers for the second map view.
     */
    protected void createLayers2() {
        this.mapView2.getLayerManager()
                .getLayers().add(AndroidUtil.createTileRendererLayer(this.tileCaches.get(1),
                this.mapView2.getModel().mapViewPosition, getMapFile2(),
                getRenderTheme2(), false, true, false));
    }

    @Override
    protected void createMapViews() {
        super.createMapViews();
        // second mapView is defined in layout
        this.mapView2 = (MapView) this.findViewById(R.id.mapView2);
        this.mapView2.getModel().init(this.preferencesFacade2);
        this.mapView2.setClickable(true);
        this.mapView2.getMapScaleBar().setVisible(true);
        this.mapView2.setBuiltInZoomControls(hasZoomControls());
        this.mapView2.getMapZoomControls().setZoomLevelMin(getZoomLevelMin());
        this.mapView2.getMapZoomControls().setZoomLevelMax(getZoomLevelMax());
        initializePosition(this.mapView2.getModel().mapViewPosition);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            boolean hardwareAcceleration = sharedPreferences.getBoolean(SamplesApplication.SETTING_HARDWARE_ACCELERATION, true);
            if (!hardwareAcceleration) {
                this.mapView2.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            }
        }
    }

    protected TileCache createTileCache2() {
        int tileSize = this.mapView2.getModel().displayModel.getTileSize();
        return AndroidUtil.createTileCache(this, getPersistableId2(), tileSize,
                getScreenRatio2(),
                this.mapView2.getModel().frameBufferModel.getOverdrawFactor());
    }

    @Override
    protected void createTileCaches() {
        super.createTileCaches();
        this.tileCaches.add(createTileCache2());
    }

    @Override
    protected void createSharedPreferences() {
        super.createSharedPreferences();
        this.preferencesFacade2 = new AndroidPreferences(this.getSharedPreferences(getPersistableId2(), MODE_PRIVATE));
    }

    @Override
    protected int getLayoutId() {
        // provides a layout with two mapViews
        return R.layout.dualmapviewer;
    }

    /**
     * @return the map file for the second view
     */
    protected MapFile getMapFile2() {
        return new MapFile(new File(Environment.getExternalStorageDirectory(), this.getMapFileName2()));
    }

    /**
     * @return the map file name for the second view
     */
    protected String getMapFileName2() {
        return getMapFileName();
    }

    protected String getPersistableId2() {
        return this.getPersistableId() + "-2";
    }

    /**
     * @return the rendertheme for the second view
     */
    protected XmlRenderTheme getRenderTheme2() {
        return getRenderTheme();
    }

    /**
     * @return the screen ratio that the mapview takes up (for cache
     * calculation)
     */
    @Override
    protected float getScreenRatio() {
        return 0.5f;
    }

    /**
     * @return the screen ratio that the mapview takes up (for cache
     * calculation)
     */
    protected float getScreenRatio2() {
        return 0.5f;
    }

    @Override
    protected void onDestroy() {
        this.mapView2.destroyAll();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        this.mapView2.getModel().save(this.preferencesFacade2);
        this.preferencesFacade2.save();
        super.onPause();
    }
}
