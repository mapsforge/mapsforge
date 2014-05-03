/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
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

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.map.android.AndroidPreferences;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.LayerManager;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.model.common.PreferencesFacade;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;

/**
 * A simple application which demonstrates how to use a MapView.
 */
public class BasicMapViewer extends Activity {
	protected MapView mapView;
	protected PreferencesFacade preferencesFacade;
	protected TileCache tileCache;

	protected void addLayers(LayerManager layerManager, TileCache tileCache, MapViewPosition mapViewPosition) {
		layerManager.getLayers().add(Utils.createTileRendererLayer(tileCache, mapViewPosition, getMapFile()));
	}

	protected TileCache createTileCache() {
		return Utils.createExternalStorageTileCache(this, getPersistableId());
	}

	protected MapPosition getInitialPosition() {
		return new MapPosition(new LatLong(52.517, 13.389), (byte) 14);
	}

	/**
	 * @return a map file
	 */
	protected File getMapFile() {
		return new File(Environment.getExternalStorageDirectory(), this.getMapFileName());
	}

	/**
	 * @return the map file name to be used
	 */
	protected String getMapFileName() {
		return "germany.map";
	}

	/**
	 * @return the mapview to be used
	 */
	protected MapView getMapView() {
		return new MapView(this);
	}

	/**
	 * @return the id that is used to save this mapview
	 */
	protected String getPersistableId() {
		return this.getClass().getSimpleName();
	}

	/**
	 * initializes the map view, here from source
	 */
	protected void init() {
		this.mapView = getMapView();

		initializeMapView(this.mapView, this.preferencesFacade);

		this.tileCache = createTileCache();

		MapViewPosition mapViewPosition = this.initializePosition(this.mapView.getModel().mapViewPosition);

		addLayers(this.mapView.getLayerManager(), this.tileCache, mapViewPosition);

		setContentView();
	}

	/**
	 * initializes the map view
	 * 
	 * @param mapView
	 *            the map view
	 */
	protected void initializeMapView(MapView mapView, PreferencesFacade preferences) {
		mapView.getModel().init(preferences);
		mapView.setClickable(true);
		mapView.getMapScaleBar().setVisible(true);
	}

	/**
	 * initializes the map view position
	 * 
	 * @param mapViewPosition
	 *            the map view position to be set
	 * @return the mapviewposition set
	 */
	protected MapViewPosition initializePosition(MapViewPosition mapViewPosition) {
		LatLong center = mapViewPosition.getCenter();

		if (center.equals(new LatLong(0, 0))) {
			mapViewPosition.setMapPosition(this.getInitialPosition());
		}
		return mapViewPosition;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		SharedPreferences sharedPreferences = this.getSharedPreferences(getPersistableId(), MODE_PRIVATE);
		this.preferencesFacade = new AndroidPreferences(sharedPreferences);

		init();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		this.mapView.destroy();
	}

	@Override
	protected void onPause() {
		super.onPause();
		this.mapView.getModel().save(this.preferencesFacade);
		this.preferencesFacade.save();
	}

	/**
	 * sets the content view if it has not been set already
	 */
	protected void setContentView() {
		setContentView(this.mapView);
	}
}
