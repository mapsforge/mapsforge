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

import org.mapsforge.map.layer.LayerManager;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.model.MapViewPosition;

/**
 * Basic map viewer with a few overlays added
 */
public class OverlayWithoutBaseMapViewer extends OverlayMapViewer {
	@Override
	protected void addLayers(LayerManager layerManager, TileCache tileCache, MapViewPosition mapViewPosition) {
		// just add few overlays to an empty map
		addOverlayLayers(layerManager.getLayers());
	}

	@Override
	protected TileCache createTileCache() {
		// do not need a tile cache here
		return null;
	}
}
