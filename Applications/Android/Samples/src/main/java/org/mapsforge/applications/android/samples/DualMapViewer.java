/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2013 - 2014 Ludwig M Brinckmann
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

import org.mapsforge.map.android.AndroidPreferences;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.model.common.PreferencesFacade;
import org.mapsforge.map.rendertheme.XmlRenderTheme;

import android.content.SharedPreferences;
import android.os.Environment;

/**
 * An activity with two independent MapViews
 */
public class DualMapViewer extends BasicMapViewerXml {

	protected PreferencesFacade preferencesFacade2;
	protected TileCache tileCache2;

    @Override
	protected void createMapViews() {
		super.createMapViews();
		// second mapView is defined in layout
		MapView mapView = (MapView) this.findViewById(R.id.mapView2);
		mapView.getModel().init(this.preferencesFacade2);
		mapView.setClickable(true);
	    mapViews.add(mapView);
	}

	@Override
	protected void createSharedPreferences() {
		super.createSharedPreferences();
		SharedPreferences sp = this.getSharedPreferences(getPersistableId2(), MODE_PRIVATE);
		this.preferencesFacade2 = new AndroidPreferences(sp);
	}

	@Override
	protected void createTileCaches() {
		super.createTileCaches();
		this.tileCache2 = createTileCache2();
	}


	@Override
	protected void createLayers() {
		super.createLayers();
		createLayers2();
	}


	@Override
	protected int getLayoutId() {
		// provides a layout with two mapViews
		return R.layout.dualmapviewer;
	}

	protected String getPersistableId2() {
		return this.getPersistableId() + "-2";
	}

	/**
	 * @return the screen ratio that the mapview takes up (for cache calculation)
	 */
	protected float getScreenRatio() {
		return 0.5f;
	}

	/**
	 * @return the screen ratio that the mapview takes up (for cache calculation)
	 */
	protected float getScreenRatio2() {
		return 0.5f;
	}


	@Override
	protected void onPause() {
		super.onPause();
		this.preferencesFacade2.save();
	}

	/**
	 * creates the layers for the second map view
	 */
	protected void createLayers2() {
		this.layerManagers
				.get(1)
				.getLayers()
				.add(Utils.createTileRendererLayer(this.tileCache2, this.mapViewPositions.get(1),
						getMapFile2(), getRenderTheme2(), false));
	}

	/**
	 * @return the map file for the second view
	 */
	protected File getMapFile2() {
		return new File(Environment.getExternalStorageDirectory(), this.getMapFileName2());
	}

	/**
	 * @return the map file name for the second view
	 */
	protected String getMapFileName2() {
		return getMapFileName();
	}

	/**
	 * @return the rendertheme for the second view
	 */
	protected XmlRenderTheme getRenderTheme2() {
		return getRenderTheme();
	}

	/**
	 * @return tilecache for second map view
	 */
	protected TileCache createTileCache2() {
		// no extra tile cache needed in this instance as map source is the same
		return this.tileCache;
	}

}
