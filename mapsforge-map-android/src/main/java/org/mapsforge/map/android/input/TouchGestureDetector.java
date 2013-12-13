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
package org.mapsforge.map.android.input;

import android.view.ViewConfiguration;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.util.MapViewProjection;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.Layer;


public class TouchGestureDetector implements TouchEventListener {
	private final float doubleTapSlop;

	private final int gestureTimeout;
	private Point lastActionUpPoint;
	private long lastEventTime;
	private final MapView mapView;
	private final MapViewProjection projection;

	public TouchGestureDetector(MapView mapView, ViewConfiguration viewConfiguration) {
		this.mapView = mapView;
		this.doubleTapSlop = viewConfiguration.getScaledDoubleTapSlop();
		this.gestureTimeout = ViewConfiguration.getDoubleTapTimeout();
		this.projection = new MapViewProjection(this.mapView);
	}

	@Override
	public void onActionUp(LatLong latLong, Point xy, long eventTime, boolean moveThresholdReached) {
		if (moveThresholdReached) {
			this.lastActionUpPoint = null;
			return;
		} else if (this.lastActionUpPoint != null) {
			long eventTimeDiff = eventTime - this.lastEventTime;

			if (eventTimeDiff < this.gestureTimeout && this.lastActionUpPoint.distance(xy) < this.doubleTapSlop) {

				// handle a double tap, changes the mapview position
				// so that the tap position remains stable within the view
				Point center = this.mapView.getModel().mapViewDimension.getDimension().getCenter();
				final byte zoomLevelDiff = 1;
				double moveHorizontal = (center.x - xy.x) / Math.pow(2, zoomLevelDiff);
				double moveVertical = (center.y - xy.y) / Math.pow(2, zoomLevelDiff);
				this.mapView.getModel().mapViewPosition.setPivot(latLong);
				this.mapView.getModel().mapViewPosition.moveCenterAndZoom(moveHorizontal, moveVertical, zoomLevelDiff);

				this.lastActionUpPoint = null;
				return;
			}
		}
		this.lastActionUpPoint = xy;
		this.lastEventTime = eventTime;

		for (int i = this.mapView.getLayerManager().getLayers().size() - 1; i >= 0; --i) {
			final Layer ovl = this.mapView.getLayerManager().getLayers().get(i);
			final Point layerXY = projection.toPixels(ovl.getPosition());
			if (ovl.onTap(latLong, layerXY, xy)) {
				break;
			}
		}
	}

	@Override
	public void onPointerDown(long eventTime) {
		this.lastActionUpPoint = null;
		this.lastEventTime = eventTime;
	}

	@Override
	public void onPointerUp(long eventTime) {
		long doubleTouchTime = eventTime - this.lastEventTime;
		if (doubleTouchTime < this.gestureTimeout) {
			this.lastActionUpPoint = null;
			this.mapView.getModel().mapViewPosition.zoomOut();
		}
	}

	@Override
	public void onLongPress(LatLong latLong, Point xy){
		for (int i = this.mapView.getLayerManager().getLayers().size() - 1; i >= 0; --i) {
			final Layer ovl = this.mapView.getLayerManager().getLayers().get(i);
			final Point layerXY = projection.toPixels(ovl.getPosition());
			if (ovl.onLongPress(latLong, layerXY, xy)) {
				break;
			}
		}
	}

}
