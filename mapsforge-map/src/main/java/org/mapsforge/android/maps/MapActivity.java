/*
 * Copyright 2010, 2011, 2012 mapsforge.org
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
package org.mapsforge.android.maps;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.mapsforge.core.model.GeoPoint;
import org.mapsforge.core.model.MapPosition;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

/**
 * MapActivity is the abstract base class which must be extended in order to use a {@link MapView}. There are no
 * abstract methods in this implementation which subclasses need to override and no API key or registration is required.
 * <p>
 * A subclass may create a MapView either via one of the MapView constructors or by inflating an XML layout file. It is
 * possible to use more than one MapView at the same time.
 * <p>
 * When the MapActivity is shut down, the current center position, zoom level and map file of the MapView are saved in a
 * preferences file and restored in the next startup process.
 */
public abstract class MapActivity extends Activity {
	private static final String KEY_LATITUDE = "latitude";
	private static final String KEY_LONGITUDE = "longitude";
	private static final String KEY_MAP_FILE = "mapFile";
	private static final String KEY_ZOOM_LEVEL = "zoomLevel";
	private static final String PREFERENCES_FILE = "MapActivity";

	private static boolean containsMapViewPosition(SharedPreferences sharedPreferences) {
		return sharedPreferences.contains(KEY_LATITUDE) && sharedPreferences.contains(KEY_LONGITUDE)
				&& sharedPreferences.contains(KEY_ZOOM_LEVEL);
	}

	/**
	 * Counter to store the last ID given to a MapView.
	 */
	private int lastMapViewId;

	/**
	 * Internal list which contains references to all running MapView objects.
	 */
	private final List<MapView> mapViews = new ArrayList<MapView>(2);

	private void destroyMapViews() {
		while (!this.mapViews.isEmpty()) {
			MapView mapView = this.mapViews.remove(0);
			mapView.destroy();
		}
	}

	private void restoreMapView(MapView mapView) {
		SharedPreferences sharedPreferences = getSharedPreferences(PREFERENCES_FILE, MODE_PRIVATE);
		if (containsMapViewPosition(sharedPreferences)) {
			if (sharedPreferences.contains(KEY_MAP_FILE)) {
				// get and set the map file
				mapView.setMapFile(new File(sharedPreferences.getString(KEY_MAP_FILE, null)));
			}

			// get and set the map position and zoom level
			float latitude = sharedPreferences.getFloat(KEY_LATITUDE, 0);
			float longitude = sharedPreferences.getFloat(KEY_LONGITUDE, 0);
			int zoomLevel = sharedPreferences.getInt(KEY_ZOOM_LEVEL, -1);

			GeoPoint geoPoint = new GeoPoint(latitude, longitude);
			MapPosition mapPosition = new MapPosition(geoPoint, (byte) zoomLevel);
			mapView.getMapViewPosition().setMapPosition(mapPosition);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		destroyMapViews();
	}

	@Override
	protected void onPause() {
		super.onPause();

		if (this.mapViews.isEmpty()) {
			return;
		}

		for (int i = 0, n = this.mapViews.size(); i < n; ++i) {
			this.mapViews.get(i).onPause();
		}

		Editor editor = getSharedPreferences(PREFERENCES_FILE, MODE_PRIVATE).edit();
		editor.clear();

		MapView mapView = this.mapViews.get(0);

		// save the map position and zoom level
		MapPosition mapPosition = mapView.getMapViewPosition().getMapPosition();
		GeoPoint geoPoint = mapPosition.geoPoint;
		editor.putFloat(KEY_LATITUDE, (float) geoPoint.latitude);
		editor.putFloat(KEY_LONGITUDE, (float) geoPoint.longitude);
		editor.putInt(KEY_ZOOM_LEVEL, mapPosition.zoomLevel);

		if (mapView.getMapFile() != null) {
			// save the map file
			editor.putString(KEY_MAP_FILE, mapView.getMapFile().getAbsolutePath());
		}

		editor.commit();
	}

	@Override
	protected void onResume() {
		super.onResume();
		for (int i = 0, n = this.mapViews.size(); i < n; ++i) {
			this.mapViews.get(i).onResume();
		}
	}

	/**
	 * @return a unique MapView ID on each call.
	 */
	final int getMapViewId() {
		return ++this.lastMapViewId;
	}

	/**
	 * This method is called once by each MapView during its setup process.
	 * 
	 * @param mapView
	 *            the calling MapView.
	 */
	final void registerMapView(MapView mapView) {
		this.mapViews.add(mapView);
		restoreMapView(mapView);
	}
}
