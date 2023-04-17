/*
 * Copyright 2014-2015 Ludwig M Brinckmann
 * Copyright 2015-2019 devemux86
 * Copyright 2015 Andreas Schildbach
 * Copyright 2017 usrusr
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

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import org.mapsforge.core.model.Dimension;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.core.util.LatLongUtils;
import org.mapsforge.core.util.Parameters;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.input.MapZoomControls.Orientation;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.android.util.MapViewerTemplate;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.renderer.MapWorkerPool;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.model.IMapViewPosition;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.scalebar.ImperialUnitAdapter;
import org.mapsforge.map.scalebar.MetricUnitAdapter;
import org.mapsforge.map.scalebar.NauticalUnitAdapter;

import java.io.File;

/**
 * Code common to most activities in the Samples app.
 */
@SuppressWarnings("deprecation")
public abstract class SamplesBaseActivity extends MapViewerTemplate implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String SETTING_SCALEBAR = "scalebar";
    public static final String SETTING_SCALEBAR_METRIC = "metric";
    public static final String SETTING_SCALEBAR_IMPERIAL = "imperial";
    public static final String SETTING_SCALEBAR_NAUTICAL = "nautical";
    public static final String SETTING_SCALEBAR_BOTH = "both";
    public static final String SETTING_SCALEBAR_NONE = "none";

    protected static final int DIALOG_ENTER_COORDINATES = 2923878;
    protected SharedPreferences sharedPreferences;

    @Override
    protected int getLayoutId() {
        return R.layout.mapviewer;
    }

    @Override
    protected int getMapViewId() {
        return R.id.mapView;
    }

    @Override
    protected MapPosition getInitialPosition() {
        int tileSize = this.mapView.getModel().displayModel.getTileSize();
        byte zoomLevel = LatLongUtils.zoomForBounds(new Dimension(tileSize * 4, tileSize * 4), getMapFile().boundingBox(), tileSize);
        return new MapPosition(getMapFile().boundingBox().getCenterPoint(), zoomLevel);
    }

    @Override
    protected void createLayers() {
        TileRendererLayer tileRendererLayer = AndroidUtil.createTileRendererLayer(this.tileCaches.get(0),
                mapView.getModel().mapViewPosition, getMapFile(), getRenderTheme(), false, true, false,
                getHillsRenderConfig());
        this.mapView.getLayerManager().getLayers().add(tileRendererLayer);

        // needed only for samples to hook into Settings.
        setMaxTextWidthFactor();
    }

    @Override
    protected void createControls() {
        super.createControls();
        setMapScaleBar();
    }

    @Override
    protected void createMapViews() {
        super.createMapViews();

        mapView.getMapZoomControls().setZoomControlsOrientation(Orientation.VERTICAL_IN_OUT);
        mapView.getMapZoomControls().setZoomInResource(R.drawable.zoom_control_in);
        mapView.getMapZoomControls().setZoomOutResource(R.drawable.zoom_control_out);
        mapView.getMapZoomControls().setMarginHorizontal(getResources().getDimensionPixelOffset(R.dimen.controls_margin));
        mapView.getMapZoomControls().setMarginVertical(getResources().getDimensionPixelOffset(R.dimen.controls_margin));
    }

    @Override
    protected void createTileCaches() {
        boolean persistent = sharedPreferences.getBoolean(SamplesApplication.SETTING_TILECACHE_PERSISTENCE, true);

        this.tileCaches.add(AndroidUtil.createTileCache(this, getPersistableId(),
                this.mapView.getModel().displayModel.getTileSize(), this.getScreenRatio(),
                this.mapView.getModel().frameBufferModel.getOverdrawFactor(), persistent));
    }

    @Override
    protected String getMapFileName() {
        String mapfile = (Samples.launchUrl == null) ? null : Samples.launchUrl.getQueryParameter("mapfile");
        if (mapfile != null) {
            return mapfile;
        }
        return "berlin.map";
    }

    @Override
    protected File getMapFileDirectory() {
        String mapdir = (Samples.launchUrl == null) ? null : Samples.launchUrl.getQueryParameter("mapdir");
        if (mapdir != null) {
            File file = new File(mapdir);
            if (file.exists() && file.isDirectory()) {
                return file;
            }
            throw new RuntimeException(file + " does not exist or is not a directory (configured in launch URI " + Samples.launchUrl + " )");
        }
        return super.getMapFileDirectory();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getClass().getSimpleName());
    }

    @Override
    protected void onDestroy() {
        this.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

    /*
     * Settings related methods.
     */

    @Override
    protected void createSharedPreferences() {
        super.createSharedPreferences();

        this.sharedPreferences = android.preference.PreferenceManager.getDefaultSharedPreferences(this);

        // problem that the first call to getAll() returns nothing, apparently the
        // following two calls have to be made to read all the values correctly
        // http://stackoverflow.com/questions/9310479/how-to-iterate-through-all-keys-of-shared-preferences
        this.sharedPreferences.edit().clear();
        android.preference.PreferenceManager.setDefaultValues(this, R.xml.preferences, true);

        this.sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @SuppressLint("InflateParams")
    @Override
    protected Dialog onCreateDialog(int id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater factory = LayoutInflater.from(this);
        switch (id) {
            case DIALOG_ENTER_COORDINATES:
                builder.setIcon(android.R.drawable.ic_menu_mylocation);
                builder.setTitle(R.string.dialog_location_title);
                final View view = factory.inflate(R.layout.dialog_enter_coordinates, null);
                builder.setView(view);
                builder.setPositiveButton(R.string.okbutton, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        double lat = Double.parseDouble(((EditText) view.findViewById(R.id.latitude)).getText()
                                .toString());
                        double lon = Double.parseDouble(((EditText) view.findViewById(R.id.longitude)).getText()
                                .toString());
                        byte zoomLevel = (byte) ((((SeekBar) view.findViewById(R.id.zoomlevel)).getProgress()) +
                                SamplesBaseActivity.this.mapView.getModel().mapViewPosition.getZoomLevelMin());

                        SamplesBaseActivity.this.mapView.getModel().mapViewPosition.setMapPosition(
                                new MapPosition(new LatLong(lat, lon), zoomLevel));
                    }
                });
                builder.setNegativeButton(R.string.cancelbutton, null);
                return builder.create();
        }
        return null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_preferences) {
            Intent intent = new Intent(this, Settings.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            if (renderThemeStyleMenu != null) {
                intent.putExtra(Settings.RENDERTHEME_MENU, renderThemeStyleMenu);
            }
            startActivity(intent);
            return true;
        } else if (itemId == R.id.menu_position_enter_coordinates) {
            showDialog(DIALOG_ENTER_COORDINATES);
        } else if (itemId == R.id.menu_svgclear) {
            AndroidGraphicFactory.clearResourceFileCache();
        }
        return false;
    }

    @Override
    protected void onPrepareDialog(int id, final Dialog dialog) {
        if (id == DIALOG_ENTER_COORDINATES) {
            IMapViewPosition currentPosition = SamplesBaseActivity.this.mapView.getModel().mapViewPosition;
            LatLong currentCenter = currentPosition.getCenter();
            EditText editText = (EditText) dialog.findViewById(R.id.latitude);
            editText.setText(Double.toString(currentCenter.latitude));
            editText = (EditText) dialog.findViewById(R.id.longitude);
            editText.setText(Double.toString(currentCenter.longitude));
            SeekBar zoomlevel = (SeekBar) dialog.findViewById(R.id.zoomlevel);
            zoomlevel.setMax(currentPosition.getZoomLevelMax() - currentPosition.getZoomLevelMin());
            zoomlevel.setProgress(SamplesBaseActivity.this.mapView.getModel().mapViewPosition.getZoomLevel()
                    - currentPosition.getZoomLevelMin());
            final TextView textView = (TextView) dialog.findViewById(R.id.zoomlevelValue);
            textView.setText(String.valueOf(zoomlevel.getProgress()));
            zoomlevel.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    textView.setText(String.valueOf(progress));
                }

                @Override
                public void onStartTrackingTouch(SeekBar arg0) {
                    // nothing
                }

                @Override
                public void onStopTrackingTouch(SeekBar arg0) {
                    // nothing
                }
            });
        } else {
            super.onPrepareDialog(id, dialog);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
        if (SamplesApplication.SETTING_SCALE.equals(key)) {
            this.mapView.getModel().displayModel.setUserScaleFactor(DisplayModel.getDefaultUserScaleFactor());
            Log.d(SamplesApplication.TAG, "Tilesize now " + this.mapView.getModel().displayModel.getTileSize());
            AndroidUtil.restartActivity(this);
        }
        if (SamplesApplication.SETTING_PREFERRED_LANGUAGE.equals(key)) {
            String language = preferences.getString(SamplesApplication.SETTING_PREFERRED_LANGUAGE, null);
            Log.d(SamplesApplication.TAG, "Preferred language now " + language);
            AndroidUtil.restartActivity(this);
        }
        if (SamplesApplication.SETTING_TILECACHE_PERSISTENCE.equals(key)) {
            if (!preferences.getBoolean(SamplesApplication.SETTING_TILECACHE_PERSISTENCE, false)) {
                Log.d(SamplesApplication.TAG, "Purging tile caches");
                for (TileCache tileCache : this.tileCaches) {
                    tileCache.purge();
                }
            }
            AndroidUtil.restartActivity(this);
        }
        if (SamplesApplication.SETTING_TEXTWIDTH.equals(key)) {
            AndroidUtil.restartActivity(this);
        }
        if (SETTING_SCALEBAR.equals(key)) {
            setMapScaleBar();
        }
        if (SamplesApplication.SETTING_DEBUG_TIMING.equals(key)) {
            MapWorkerPool.DEBUG_TIMING = preferences.getBoolean(SamplesApplication.SETTING_DEBUG_TIMING, false);
        }
        if (SamplesApplication.SETTING_RENDERING_THREADS.equals(key)) {
            Parameters.NUMBER_OF_THREADS = preferences.getInt(SamplesApplication.SETTING_RENDERING_THREADS, 1);
            AndroidUtil.restartActivity(this);
        }
        if (SamplesApplication.SETTING_WAYFILTERING_DISTANCE.equals(key) ||
                SamplesApplication.SETTING_WAYFILTERING.equals(key)) {
            MapFile.wayFilterEnabled = preferences.getBoolean(SamplesApplication.SETTING_WAYFILTERING, true);
            if (MapFile.wayFilterEnabled) {
                MapFile.wayFilterDistance = preferences.getInt(SamplesApplication.SETTING_WAYFILTERING_DISTANCE, 20);
            }
        }
    }

    /**
     * Sets the scale bar from preferences.
     */
    protected void setMapScaleBar() {
        String value = this.sharedPreferences.getString(SETTING_SCALEBAR, SETTING_SCALEBAR_BOTH);

        if (SETTING_SCALEBAR_NONE.equals(value)) {
            AndroidUtil.setMapScaleBar(this.mapView, null, null);
        } else {
            if (SETTING_SCALEBAR_BOTH.equals(value)) {
                AndroidUtil.setMapScaleBar(this.mapView, MetricUnitAdapter.INSTANCE, ImperialUnitAdapter.INSTANCE);
            } else if (SETTING_SCALEBAR_METRIC.equals(value)) {
                AndroidUtil.setMapScaleBar(this.mapView, MetricUnitAdapter.INSTANCE, null);
            } else if (SETTING_SCALEBAR_IMPERIAL.equals(value)) {
                AndroidUtil.setMapScaleBar(this.mapView, ImperialUnitAdapter.INSTANCE, null);
            } else if (SETTING_SCALEBAR_NAUTICAL.equals(value)) {
                AndroidUtil.setMapScaleBar(this.mapView, NauticalUnitAdapter.INSTANCE, null);
            }
        }
    }

    /**
     * sets the value for breaking line text in labels.
     */
    protected void setMaxTextWidthFactor() {
        mapView.getModel().displayModel.setMaxTextWidthFactor(Float.valueOf(sharedPreferences.getString(SamplesApplication.SETTING_TEXTWIDTH, "0.7")));
    }

}
