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

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.layer.MyLocationOverlay;
import org.mapsforge.map.layer.LayerManager;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.model.MapViewPosition;

import android.graphics.drawable.Drawable;

/**
 * MapViewer that shows current position. In the data directory of the Samples project is the file berlin.gpx that can
 * be loaded in the Android Monitor to simulate location data in the center of Berlin.
 */
public class LocationOverlayMapViewer extends BasicMapViewerXml {
	private MyLocationOverlay myLocationOverlay;

	@Override
	public void onPause() {
		super.onPause();
		// stop receiving location updates
		this.myLocationOverlay.disableMyLocation();
	}

	@Override
	public void onResume() {
		super.onResume();
		// register for location updates
		this.myLocationOverlay.enableMyLocation(true);
	}

	@Override
	protected void addLayers(LayerManager layerManager, TileCache tileCache, MapViewPosition mapViewPosition) {
		super.addLayers(layerManager, tileCache, mapViewPosition);

		// a marker to show at the position
		Drawable drawable = getResources().getDrawable(R.drawable.marker_red);
		Bitmap bitmap = AndroidGraphicFactory.convertToBitmap(drawable);

		// create the overlay and tell it to follow the location
		this.myLocationOverlay = new MyLocationOverlay(this, mapViewPosition, bitmap);
		this.myLocationOverlay.setSnapToLocationEnabled(true);

		layerManager.getLayers().add(this.myLocationOverlay);
	}
}
