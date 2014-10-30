/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright © 2013-2014 Ludwig M Brinckmann
 * Copyright © 2014 devemux86
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
import org.mapsforge.map.layer.renderer.TileRendererLayer;

/**
 * A map viewer that draws the labels onto a single separate layer. The LabelLayer remains
 * experimental code and has some notable speed issues. Its use in production is currently not
 * recommended.
 */
public class LabelLayerMapViewer extends RenderTheme4 {

	protected void createLayers() {
		TileRendererLayer tileRendererLayer = AndroidUtil.createTileRendererLayer(this.tileCaches.get(0),
				this.mapView.getModel().mapViewPosition, getMapFile(), getRenderTheme(), false, false);
		mapView.getLayerManager().getLayers().add(tileRendererLayer);
		org.mapsforge.map.layer.labels.LabelLayer labelLayer = new org.mapsforge.map.layer.labels.LabelLayer(AndroidGraphicFactory.INSTANCE, tileRendererLayer.getLabelStore());
		mapView.getLayerManager().getLayers().add(labelLayer);
	}

}
