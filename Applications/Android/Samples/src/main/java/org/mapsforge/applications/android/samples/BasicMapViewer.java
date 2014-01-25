/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright © 2013-2014 Ludwig M Brinckmann
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

import java.io.File;
import java.util.ArrayList;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.map.android.AndroidPreferences;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.android.util.AndroidUtil;

import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.LayerManager;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.model.common.PreferencesFacade;
import org.mapsforge.map.reader.MapDatabase;
import org.mapsforge.map.reader.header.FileOpenResult;
import org.mapsforge.map.reader.header.MapFileInfo;
import org.mapsforge.map.rendertheme.InternalRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderTheme;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * A simple application which demonstrates how to use a MapView.
 */
public class BasicMapViewer extends Activity implements OnSharedPreferenceChangeListener {
	protected ArrayList<MapView> mapViews = new ArrayList<>();
	protected ArrayList<MapViewPosition> mapViewPositions = new ArrayList<>();
	protected TileCache tileCache;
	protected ArrayList<LayerManager> layerManagers = new ArrayList<LayerManager>();
	protected PreferencesFacade preferencesFacade;
	protected SharedPreferences sharedPreferences;

	protected static final int DIALOG_ENTER_COORDINATES = 2923878;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		this.sharedPreferences.registerOnSharedPreferenceChangeListener(this);

		createSharedPreferences();
		createMapViews();
		createMapViewPositions();
		createLayerManagers();
		createTileCaches();
		createControls();
	}

	@Override
	protected void onStart() {
		super.onStart();
		createLayers();
	}

	@Override
	protected void onPause() {
		super.onPause();
		for (MapView mapView : mapViews) {
			mapView.getModel().save(this.preferencesFacade);
		}
		this.preferencesFacade.save();
	}

	@Override
	protected void onStop() {
		super.onStop();
        destroyLayers();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		destroyTileCaches();
		destroyMapViewPositions();
		destroyMapViews();
		this.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        org.mapsforge.map.android.graphics.AndroidResourceBitmap.clearResourceBitmaps();
	}

	protected void createSharedPreferences() {
		SharedPreferences sp = this.getSharedPreferences(getPersistableId(), MODE_PRIVATE);
		this.preferencesFacade = new AndroidPreferences(sp);
	}

	protected void createMapViewPositions() {
		for (MapView mapView : mapViews) {
			this.mapViewPositions.add(initializePosition(mapView.getModel().mapViewPosition));
		}
	}

	protected void destroyMapViewPositions() {
		for (MapViewPosition mapViewPosition : mapViewPositions) {
			mapViewPosition.destroy();
		}
	}

	protected void createLayerManagers() {
		for (MapView mapView : mapViews) {
			this.layerManagers.add(mapView.getLayerManager());
		}
	}

	protected void createLayers() {
		TileRendererLayer tileRendererLayer =
				Utils.createTileRendererLayer(this.tileCache, this.mapViewPositions.get(0), getMapFile(), getRenderTheme(), false);
		this.layerManagers.get(0).getLayers().add(tileRendererLayer);
	}

	protected void destroyLayers() {
		for (LayerManager layerManager : this.layerManagers) {
			for (Layer layer : layerManager.getLayers()) {
				layerManager.getLayers().remove(layer);
				layer.onDestroy();
			}
		}
	}

	protected void createTileCaches() {
		this.tileCache = AndroidUtil.createTileCache(this, getPersistableId(), this.mapViews.get(0).getModel().displayModel.getTileSize(), this.getScreenRatio(), this.mapViews.get(0).getModel().frameBufferModel.getOverdrawFactor());
	}

	protected void destroyTileCaches() {
		this.tileCache.destroy();
	}

	protected void createControls() {
		// time to create control elements
	}

	protected void createMapViews() {
		MapView mapView = getMapView();
		mapView.getModel().init(this.preferencesFacade);
		mapView.setClickable(true);
		mapView.getMapScaleBar().setVisible(true);
		mapView.setBuiltInZoomControls(hasZoomControls());
		mapView.getMapZoomControls().setZoomLevelMin((byte) 10);
		mapView.getMapZoomControls().setZoomLevelMax((byte) 20);
		this.mapViews.add(mapView);
	}

	protected void destroyMapViews() {
		for (MapView mapView : mapViews) {
			mapView.destroy();
		}
	}

	protected void redrawLayers() {
		for (LayerManager layerManager : this.layerManagers) {
			layerManager.redrawLayers();
		}
	}

	protected MapPosition getInitialPosition() {
		MapDatabase mapDatabase = new MapDatabase();
		final FileOpenResult result = mapDatabase.openFile(getMapFile());
		if (result.isSuccess()) {
			final MapFileInfo mapFileInfo = mapDatabase.getMapFileInfo();
			if  (mapFileInfo != null && mapFileInfo.startPosition != null) {
				return new MapPosition(mapFileInfo.startPosition, (byte) mapFileInfo.startZoomLevel);
			} else {
				return new MapPosition(new LatLong(52.517037, 13.38886), (byte) 12);
			}
		}
		throw new IllegalArgumentException("Invalid Map File " + getMapFileName());
	}

	/**
	 * @return a map file
	 */
	protected File getMapFile() {
        File file = new File(Environment.getExternalStorageDirectory(), this.getMapFileName());
		Log.i(SamplesApplication.TAG, "Map file is " + file.getAbsolutePath());
		return file;
	}

	/**
	 * @return the map file name to be used
	 */
	protected String getMapFileName() {
		return "germany.map";
	}

	/**
	 * @return the id that is used to save this mapview
	 */
	protected String getPersistableId() {
		return this.getClass().getSimpleName();
	}

	/**
	 * @return the rendertheme for this viewer
	 */
	protected XmlRenderTheme getRenderTheme() {
		return InternalRenderTheme.OSMARENDER;
	}

	/**
	 * @return the screen ratio that the mapview takes up (for cache calculation)
	 */
	protected float getScreenRatio() {
		return 1.0f;
	}

	protected boolean hasZoomControls() {
		return true;
	}

	protected MapView getMapView() {
		MapView mv = new MapView(this);
		setContentView(mv);
		return mv;
	}

	/**
	 * initializes the map view position.
	 * 
	 * @param mvp
	 *            the map view position to be set
	 * @return the mapviewposition set
	 */
	protected MapViewPosition initializePosition(MapViewPosition mvp) {
		LatLong center = mvp.getCenter();

		if (center.equals(new LatLong(0, 0))) {
			mvp.setMapPosition(this.getInitialPosition());
		}
		mvp.setZoomLevelMax((byte) 24);
		mvp.setZoomLevelMin((byte) 7);
		return mvp;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.options_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent;
		switch (item.getItemId()) {
			case R.id.menu_preferences:
				intent = new Intent(this, Settings.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
				startActivity(intent);
				return true;
			case R.id.menu_position_enter_coordinates:
				showDialog(DIALOG_ENTER_COORDINATES);
				break;
		}
		return false;
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
		if (SamplesApplication.SETTING_SCALE.equals(key)) {
			destroyTileCaches();
			for (MapView mapView : mapViews) {
				mapView.getModel().displayModel.setUserScaleFactor(DisplayModel.getDefaultUserScaleFactor());
			}
			Log.d(SamplesApplication.TAG, "Tilesize now " + mapViews.get(0).getModel().displayModel.getTileSize());
			createTileCaches();
			redrawLayers();
		}
	}

	@Deprecated
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
						byte zoomLevel = (byte) ((((SeekBar) view.findViewById(R.id.zoomlevel)).getProgress()) + BasicMapViewer.this.mapViewPositions.get(0)
								.getZoomLevelMin());

						BasicMapViewer.this.mapViewPositions.get(0).setMapPosition(new MapPosition(new LatLong(lat, lon),
								zoomLevel));
					}
				});
				builder.setNegativeButton(R.string.cancelbutton, null);
				return builder.create();
		}
		return null;
	}

	@Deprecated
	@Override
	protected void onPrepareDialog(int id, final Dialog dialog) {
		if (id == this.DIALOG_ENTER_COORDINATES) {
			MapViewPosition currentPosition = BasicMapViewer.this.mapViewPositions.get(0);
			LatLong currentCenter = currentPosition.getCenter();
			EditText editText = (EditText) dialog.findViewById(R.id.latitude);
			editText.setText(Double.toString(currentCenter.latitude));
			editText = (EditText) dialog.findViewById(R.id.longitude);
			editText.setText(Double.toString(currentCenter.longitude));
			SeekBar zoomlevel = (SeekBar) dialog.findViewById(R.id.zoomlevel);
			zoomlevel.setMax(currentPosition.getZoomLevelMax() - currentPosition.getZoomLevelMin());
			zoomlevel.setProgress(BasicMapViewer.this.mapViewPositions.get(0).getZoomLevel()
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

	/**
	 * sets the content view if it has not been set already.
	 */
	protected void setContentView() {
		setContentView(this.mapViews.get(0));
	}
}
