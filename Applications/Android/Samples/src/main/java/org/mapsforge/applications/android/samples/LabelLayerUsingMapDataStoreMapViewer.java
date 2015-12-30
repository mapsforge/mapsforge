/*
 * Copyright 2015 Ludwig M Brinckmann
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
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.layer.labels.MapDataStoreLabelStore;
import org.mapsforge.map.layer.renderer.TileRendererLayer;

/**
 * A map viewer that draws the labels onto a single separate layer. The LabelLayer used in this example
 * caches the read results per tile to combine the results for the whole screen. 
 */
public class LabelLayerUsingMapDataStoreMapViewer extends RenderTheme4 {

	@Override
	protected void createLayers() {
		TileRendererLayer tileRendererLayer = AndroidUtil.createTileRendererLayer(this.tileCaches.get(0),
				this.mapView.getModel().mapViewPosition, getMapFile(), getRenderTheme(), false, false, false);
		mapView.getLayerManager().getLayers().add(tileRendererLayer);
		MapDataStoreLabelStore labelStore = new MapDataStoreLabelStore(getMapFile(), tileRendererLayer.getRenderThemeFuture(),
				tileRendererLayer.getTextScale(), tileRendererLayer.getDisplayModel(), AndroidGraphicFactory.INSTANCE);
		org.mapsforge.map.layer.labels.LabelLayer labelLayer =
				new org.mapsforge.map.layer.labels.LabelLayer(AndroidGraphicFactory.INSTANCE, labelStore);
		mapView.getLayerManager().getLayers().add(labelLayer);
	}

}
