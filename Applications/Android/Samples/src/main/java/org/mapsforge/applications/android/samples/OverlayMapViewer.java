/*
 * Copyright 2013-2014 Ludwig M Brinckmann
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

import android.util.Log;

import java.util.List;

import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.LayerManager;
import org.mapsforge.map.layer.Layers;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.overlay.Circle;
import org.mapsforge.map.layer.overlay.FixedPixelCircle;
import org.mapsforge.map.layer.overlay.Marker;
import org.mapsforge.map.layer.overlay.Polygon;
import org.mapsforge.map.layer.overlay.Polyline;

/**
 * Basic map viewer with a few overlays added.
 */
public class OverlayMapViewer extends BasicMapViewerXml {

    protected LatLong latLong1 = new LatLong(52.5, 13.4);
	protected LatLong latLong2 = new LatLong(52.499, 13.402);
	protected LatLong latLong3 = new LatLong(52.503, 13.399);
	protected LatLong latLong4 = new LatLong(52.51, 13.401);
	protected LatLong latLong5 = new LatLong(52.508, 13.408);
	protected LatLong latLong6 = new LatLong(52.515, 13.420);

	@Override
	protected void createLayers() {
		super.createLayers();

		// we just add a few more overlays
		addOverlayLayers(layerManagers.get(0).getLayers());
	}

	protected void addOverlayLayers(Layers layers) {

		Polyline polyline = new Polyline(Utils.createPaint(AndroidGraphicFactory.INSTANCE.createColor(Color.BLUE), 8,
				Style.STROKE), AndroidGraphicFactory.INSTANCE);
		List<LatLong> latLongs = polyline.getLatLongs();
		latLongs.add(latLong1);
		latLongs.add(latLong2);
		latLongs.add(latLong3);

		Paint paintFill = Utils.createPaint(AndroidGraphicFactory.INSTANCE.createColor(Color.GREEN), 2, Style.STROKE);
		Paint paintStroke = Utils.createPaint(AndroidGraphicFactory.INSTANCE.createColor(Color.BLACK), 2, Style.STROKE);
		Polygon polygon = new Polygon(paintFill, paintStroke, AndroidGraphicFactory.INSTANCE);
		latLongs = polygon.getLatLongs();
		latLongs.add(latLong2);
		latLongs.add(latLong3);
		latLongs.add(latLong4);
		latLongs.add(latLong5);

		Marker marker1 = Utils.createTappableMarker(this, R.drawable.marker_red, latLong1);

		Circle circle = new Circle(latLong3, 300, Utils.createPaint(
				AndroidGraphicFactory.INSTANCE.createColor(Color.WHITE), 0, Style.FILL), null);


		FixedPixelCircle tappableCircle = new FixedPixelCircle(latLong6, 70, Utils.createPaint(
				AndroidGraphicFactory.INSTANCE.createColor(Color.GREEN), 0, Style.FILL), null) {
			@Override
			public boolean onTap(LatLong geoPoint, Point viewPosition, Point tapPoint) {
				if (this.contains(viewPosition, tapPoint)) {
					Log.w(SamplesApplication.TAG, "The Circle was tapped at " + geoPoint.toString());
					return true;
				}
				return false;
			}

			@Override
			public boolean onLongPress(LatLong geoPoint, Point viewPosition, Point tapPoint) {
				if (this.contains(viewPosition, tapPoint)) {
					Log.w(SamplesApplication.TAG, "The Circle was long pressed at " + geoPoint.toString());
					return true;
				}
				return false;
			}
		};


		layers.add(polyline);
		layers.add(polygon);
		layers.add(circle);
		layers.add(marker1);
		layers.add(tappableCircle);
	}
}
