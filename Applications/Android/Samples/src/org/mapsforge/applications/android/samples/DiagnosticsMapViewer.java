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

import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.LayerManager;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.debug.TileCoordinatesLayer;
import org.mapsforge.map.layer.debug.TileGridLayer;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.model.common.PreferencesFacade;

/**
 * Viewer with tile grid and coordinates visible and frame counter displayed
 */
public class DiagnosticsMapViewer extends BasicMapViewerXml {
	@Override
	protected void addLayers(LayerManager layerManager, TileCache tileCache, MapViewPosition mapViewPosition) {
		super.addLayers(layerManager, tileCache, mapViewPosition);
		layerManager.getLayers().add(new TileGridLayer(AndroidGraphicFactory.INSTANCE));
		layerManager.getLayers().add(new TileCoordinatesLayer(AndroidGraphicFactory.INSTANCE));
	}

	@Override
	protected void initializeMapView(MapView mapView, PreferencesFacade preferences) {
		super.initializeMapView(mapView, preferences);
		// turn on the frame counter display
		mapView.getFpsCounter().setVisible(true);
	}
}
