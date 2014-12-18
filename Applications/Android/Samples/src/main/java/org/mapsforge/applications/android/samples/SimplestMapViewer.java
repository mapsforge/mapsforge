/*
 * Copyright © 2014 Ludwig M Brinckmann
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

import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.android.util.MapViewerTemplate;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.rendertheme.InternalRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderTheme;

/**
 * The simplest form of creating a map viewer based on the MapViewerTemplate.
 */
public class SimplestMapViewer extends MapViewerTemplate {

	/**
	 * This MapViewer uses the deprecated built-in osmarender theme.
	 * @return the render theme to use
	 */
	@Override
	protected XmlRenderTheme getRenderTheme() {
		return InternalRenderTheme.OSMARENDER;
	}

	/**
	 * This MapViewer uses the standard xml layout in the Samples app.
	 * @return
	 */
	@Override
	protected int getLayoutId() {
		return R.layout.mapviewer;
	}

	/**
	 * The id of the mapview inside the layout.
	 * @return the id of the MapView inside the layout.
	 */
	@Override
	protected int getMapViewId() {
		return R.id.mapView;
	}

	/**
	 * The name of the map file.
	 * @return map file name
	 */
	@Override
	protected String getMapFileName() {
		return "germany.map";
	}

	/**
	 * Creates a simple tile renderer layer with the AndroidUtil helper.
	 */
	protected void createLayers() {
		TileRendererLayer tileRendererLayer = AndroidUtil.createTileRendererLayer(this.tileCaches.get(0),
				this.mapView.getModel().mapViewPosition, getMapFile(), getRenderTheme(), false, true);
		this.mapView.getLayerManager().getLayers().add(tileRendererLayer);
	}

	/**
	 * Creates the tile cache with the AndroidUtil helper
	 */
	protected void createTileCaches() {
		this.tileCaches.add(AndroidUtil.createTileCache(this, getPersistableId(),
				this.mapView.getModel().displayModel.getTileSize(), this.getScreenRatio(),
				this.mapView.getModel().frameBufferModel.getOverdrawFactor(),
				false, 0, false
		));
	}

}
