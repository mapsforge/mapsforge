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

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import android.util.Log;


/**
 * Demonstrates how to enable a LongPress on a layer, the output of the long press is
 * just to the log.
 */
public class LongPressAction extends BasicMapViewerXml {

	@Override
	protected void createLayers() {
		TileRendererLayer tileRendererLayer = new TileRendererLayer(this.tileCache, this.mapViewPosition,
				org.mapsforge.map.android.graphics.AndroidGraphicFactory.INSTANCE) {
			@Override
			public boolean onLongPress(LatLong tapLatLong, Point thisXY, Point tapXY) {
				Log.i("Tapping", "long press at position " + tapLatLong.toString());
				return true;
			}
		};
		tileRendererLayer.setMapFile(this.getMapFile());
		tileRendererLayer.setXmlRenderTheme(this.getRenderTheme());
		this.layerManagers.get(0).getLayers().add(tileRendererLayer);
	}

}
