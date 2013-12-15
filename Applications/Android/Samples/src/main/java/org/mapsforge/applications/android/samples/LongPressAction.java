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

import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.overlay.FixedPixelCircle;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import android.util.Log;


/**
 * Demonstrates how to enable a LongPress on a layer, the output of the long press is
 * just to the log.
 */
public class LongPressAction extends BasicMapViewerXml {

	private static final Paint green = Utils.createPaint(AndroidGraphicFactory.INSTANCE.createColor(Color.GREEN), 0, Style.FILL);
	private static final Paint red = Utils.createPaint(AndroidGraphicFactory.INSTANCE.createColor(Color.RED), 0, Style.FILL);
	private static float circleSize = 20 * AndroidGraphicFactory.INSTANCE.getScaleFactor();

	@Override
	protected void createLayers() {
		TileRendererLayer tileRendererLayer = new TileRendererLayer(this.tileCache, this.mapViewPosition,
				org.mapsforge.map.android.graphics.AndroidGraphicFactory.INSTANCE) {
			@Override
			public boolean onLongPress(LatLong tapLatLong, Point thisXY, Point tapXY) {
				Log.i("Tapping", "long press at position " + tapLatLong.toString());
				LongPressAction.this.onLongPress(tapLatLong);
				return true;
			}
		};
		tileRendererLayer.setMapFile(this.getMapFile());
		tileRendererLayer.setXmlRenderTheme(this.getRenderTheme());
		this.layerManagers.get(0).getLayers().add(tileRendererLayer);
	}


	protected void onLongPress(LatLong position) {
		FixedPixelCircle tappableCircle = new FixedPixelCircle(position, circleSize, green, null){
			@Override
			public boolean onTap(LatLong geoPoint, Point viewPosition, Point tapPoint) {
				if (this.contains(viewPosition, tapPoint)) {
					Log.w("Tapp", "The Circle was tapped at " + geoPoint.toString());
					toggleColor();
					this.requestRedraw();
					return true;
				}
				return false;
			}
			@Override
			public boolean onLongPress(LatLong geoPoint, Point viewPosition, Point tapPoint) {
				if (this.contains(viewPosition, tapPoint)) {
					Log.w("Tapp", "The Circle was long pressed at " + geoPoint.toString());
					LongPressAction.this.mapView.getLayerManager().getLayers().remove(this);
					LongPressAction.this.mapView.getLayerManager().redrawLayers();
					return true;
				}
				return false;
			}

			private void toggleColor() {
				if (this.getPaintFill().equals(LongPressAction.green)) {
					this.setPaintFill(LongPressAction.red);
				} else {
					this.setPaintFill(LongPressAction.green);
				}
			}
		};
		this.mapView.getLayerManager().getLayers().add(tappableCircle);
		tappableCircle.requestRedraw();

	}
}
