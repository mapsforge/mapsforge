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

import java.util.List;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Dimension;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.LayerManager;
import org.mapsforge.map.layer.Layers;
import org.mapsforge.map.layer.overlay.Polyline;


/**
 * Basic map viewer with a few overlays added
 */
public class ZoomToBounds extends OverlayMapViewer {

	@Override
	public void onWindowFocusChanged (boolean hasFocus) {
	    super.onWindowFocusChanged(hasFocus);
	    if (hasFocus) {
		BoundingBox bb = new BoundingBox(latLong2.latitude, latLong3.longitude, latLong3.latitude, latLong2.longitude);
		Dimension dimension = this.mapView.getModel().mapViewDimension.getDimension();
		this.mapView.getModel().mapViewPosition.setMapPosition(new MapPosition(bb.getCenterPoint(), Utils.zoomForBounds(dimension, bb)));
	    }
	}

	protected void addOverlayLayers(Layers layers) {
		Polyline polyline = new Polyline(Utils.createPaint(AndroidGraphicFactory.INSTANCE.createColor(Color.BLUE), 8,
				Style.STROKE), AndroidGraphicFactory.INSTANCE);
		List<LatLong> latLongs = polyline.getLatLongs();
		latLongs.add(latLong2);
		latLongs.add(latLong3);
		layers.add(polyline);
	}

}
