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

import org.mapsforge.map.android.AndroidPreferences;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.LayerManager;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.model.common.PreferencesFacade;

import android.content.SharedPreferences;

/**
 * An activity with two independent MapViews
 */
public class DualMapViewer extends BasicMapViewerXml {
	protected PreferencesFacade preferencesFacade2;
	MapView mapView2;

	protected void addSecondMapLayer(LayerManager layerManager, TileCache tileCache, MapViewPosition mapViewPosition) {
		layerManager.getLayers().add(Utils.createTileRendererLayer(tileCache, mapViewPosition, getMapFile()));
	}

	/**
	 * @return tilecache to use for second mapView
	 */
	protected TileCache createTileCache2() {
		// no extra tile cache needed in this instance as map source is the same
		return this.tileCache;
	}

	@Override
	protected int getLayoutId() {
		// provides a layout with two mapViews
		return R.layout.dualmapviewer;
	}

	protected String getPersistableId2() {
		return this.getPersistableId() + "-2";
	}

	@Override
	protected void init() {
		super.init();

		SharedPreferences sharedPreferences = this.getSharedPreferences(getPersistableId2(), MODE_PRIVATE);
		this.preferencesFacade2 = new AndroidPreferences(sharedPreferences);

		// second mapView
		this.mapView2 = (MapView) this.findViewById(R.id.mapView2);
		initializeMapView(this.mapView2, this.preferencesFacade2);

		TileCache tileCache2 = createTileCache2();
		MapViewPosition mapViewPosition2 = this.initializePosition(this.mapView2.getModel().mapViewPosition);

		addSecondMapLayer(this.mapView2.getLayerManager(), tileCache2, mapViewPosition2);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		this.mapView2.destroy();
	}

	@Override
	protected void onPause() {
		super.onPause();
		this.mapView2.getModel().save(this.preferencesFacade2);
		this.preferencesFacade2.save();
	}
}
