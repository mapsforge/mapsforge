/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2013-2014 Ludwig M Brinckmann
 * Copyright 2014, 2015 devemux86
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
package org.mapsforge.map.android.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.model.common.PreferencesFacade;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.rendertheme.XmlRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderThemeStyleMenu;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;

/**
 * A abstract template map viewer activity that provides a standard life cycle and
 * modification points for mapsforge-based map activities.
 */
public abstract class MapViewerTemplate extends Activity  {

	protected MapView mapView;
	protected PreferencesFacade preferencesFacade;
	protected XmlRenderThemeStyleMenu renderThemeStyleMenu;
	protected List<TileCache> tileCaches = new ArrayList<TileCache>();

	/*
	 * Abstract methods that must be implemented.
	 */

	/**
	 * @return the layout to be used,
	 */
	protected abstract int getLayoutId();

	/**
	 * @return the id of the mapview inside the layout.
	 */
	protected abstract int getMapViewId();

	/**
	 * Gets the name of the map file.
	 * The directory for the file is supplied by getMapFileDirectory()
	 *
	 * @return the map file name to be used
	 */
	protected abstract String getMapFileName();

	/**
	 * @return the rendertheme for this viewer
	 */
	protected abstract XmlRenderTheme getRenderTheme();

	/**
	 * Hook to create map layers. You will need to create at least one layer to
	 * have something visible on the map.
	 */
	protected abstract void createLayers();

	/**
	 * Hook to create tile caches. For most map viewers you will need a tile cache
	 * for the renderer. If you do not need tilecaches just provide an empty implementation.
	 */
	protected abstract void createTileCaches();

	/**
	 * Hook to create controls, such as scale bars.
	 * You can add more controls.
	 */
	protected void createControls() {
		initializePosition(mapView.getModel().mapViewPosition);
	}

	/**
	 * The MaxTextWidthFactor determines how long a text may be before it is line broken. The
	 * default setting should be good enough for most apps.
	 * @return the maximum text width factor for line breaking captions
	 */
	protected float getMaxTextWidthFactor() {
		return 0.7f;
	}

	/**
	 * @return the default starting zoom level if nothing is encoded in the map file.
	 */
	protected byte getZoomLevelDefault() {
		return (byte) 12;
	}

	/**
	 * @return the minimum zoom level of the map view.
	 */
	protected byte getZoomLevelMin() {
		return (byte) 0;
	}

	/**
	 * @return the maximum zoom level of the map view.
	 */
	protected byte getZoomLevelMax() {
		return (byte) 24;
	}

	/**
	 * Template method to create the map views.
	 */
	protected void createMapViews() {
		mapView = getMapView();
		mapView.getModel().init(this.preferencesFacade);
		mapView.setClickable(true);
		mapView.getMapScaleBar().setVisible(true);
		mapView.setBuiltInZoomControls(hasZoomControls());
		mapView.getMapZoomControls().setAutoHide(isZoomControlsAutoHide());
		mapView.getMapZoomControls().setZoomLevelMin(getZoomLevelMin());
		mapView.getMapZoomControls().setZoomLevelMax(getZoomLevelMax());
	}

	/**
	 * Creates the shared preferences that are being used to store map view data over
	 * activity restarts.
	 */
	protected void createSharedPreferences() {
		this.preferencesFacade = new AndroidPreferences(this.getSharedPreferences(getPersistableId(), MODE_PRIVATE));
	}

	/**
	 * Gets the default initial position of a map view if nothing is set in the map file. This
	 * operation is used as a fallback only. Override this if you are not sure if your map file
	 * will always have an initial position.
	 * @return the fallback initial position of the mapview.
	 */
	protected MapPosition getDefaultInitialPosition() {
		return new MapPosition(new LatLong(0, 0), getZoomLevelDefault());
	}

	/**
	 * Extracts the initial position from the map file, falling back onto the value supplied
	 * by getDefaultInitialPosition if there is no initial position coded into the map file.
	 * You will only need to override this method if you do not want the initial position extracted
	 * from the map file.
	 * @return the initial position encoded in the map file or a fallback value.
	 */
	protected MapPosition getInitialPosition() {
		MapDataStore mapFile = getMapFile();

		if (mapFile.startPosition() != null) {
			Byte startZoomLevel = mapFile.startZoomLevel();
			if (startZoomLevel == null) {
				// it is actually possible to have no start zoom level in the file
				startZoomLevel = new Byte((byte) 12);
			}
			return new MapPosition(mapFile.startPosition(), startZoomLevel);
		} else {
			return getDefaultInitialPosition();
		}
	}

	/**
	 * Provides the directory of the map file, by default the Android external storage
	 * directory (e.g. sdcard).
	 * @return
	 */
	protected File getMapFileDirectory() {
		return Environment.getExternalStorageDirectory();
	}

	/**
	 * Combines map file directory and map file to a map file.
	 * This method usually will not need to be changed.
	 * @return a map file interface
	 */
	protected MapDataStore getMapFile() {
		return new MapFile(new File(getMapFileDirectory(), this.getMapFileName()));
	}

	/**
	 * The persistable ID is used to store settings information, like the center of the last view
	 * and the zoomlevel. By default the simple name of the class is used. The value is not user
	 * visibile.
	 * @return the id that is used to save this mapview.
	 */
	protected String getPersistableId() {
		return this.getClass().getSimpleName();
	}

	/**
	 * Returns the relative size of a map view in relation to the screen size of the device. This
	 * is used for cache size calculations.
	 * By default this returns 1.0, for a full size map view.
	 * @return the screen ratio of the mapview
	 */
	protected float getScreenRatio() {
		return 1.0f;
	}

	/**
	 * Configuration method to set if a map view activity will have zoom controls.
	 * @return true if the map has standard zoom controls.
	 */
	protected boolean hasZoomControls() {
		return true;
	}

	/**
	 * Configuration method to set if map view activity's zoom controls hide automatically.
	 * @return true if zoom controls hide automatically.
	 */
	protected boolean isZoomControlsAutoHide() {
		return true;
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
		mvp.setZoomLevelMax(getZoomLevelMax());
		mvp.setZoomLevelMin(getZoomLevelMin());
		return mvp;
	}

	/**
	 * Hook to check for Android Runtime Permissions. There is no check here, as
	 * see the @MapViewerTemplateRuntimePermissions for an implementation that works with
	 * Runtime Permissions.
	 */
	protected void checkPermissionsAndCreateLayersAndControls() {
		createLayers();
		createControls();
	}

	/**
	 * Android Activity life cycle method.
	 * @param savedInstanceState
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		createSharedPreferences();
		createMapViews();
		createTileCaches();
		checkPermissionsAndCreateLayersAndControls();
	}


	/**
	 * Android Activity life cycle method.
	 */
	@Override
	protected void onPause() {
		mapView.getModel().save(this.preferencesFacade);
		this.preferencesFacade.save();
		super.onPause();
	}

	/**
	 * Android Activity life cycle method.
	 */
	@Override
	protected void onDestroy() {
		mapView.destroyAll();
		tileCaches.clear();
		super.onDestroy();
	}

	/**
	 * Hook to purge tile caches.
	 * By default we purge every tile cache that has been added to the tileCaches list.
	 */
	protected void purgeTileCaches() {
		for (TileCache tileCache : tileCaches) {
			tileCache.purge();
		}
		tileCaches.clear();
	}

	protected void redrawLayers() {
		mapView.getLayerManager().redrawLayers();
	}

	/**
	 * sets the content view if it has not been set already.
	 */
	protected void setContentView() {
		setContentView(mapView);
	}

	/**
	 * Creates a map view using an XML layout file supplied by getLayoutId() and finds
	 * the map view component inside it with getMapViewId().
	 * @return the Android MapView for this activity.
	 */
	protected MapView getMapView() {
		setContentView(getLayoutId());
		return (MapView) findViewById(getMapViewId());
	}
}
