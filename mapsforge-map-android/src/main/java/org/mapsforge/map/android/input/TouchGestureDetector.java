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
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.model.MapViewPosition;

public class TouchGestureDetector implements TouchEventListener {
	private final float doubleTapSlop;

	private final int gestureTimeout;
	private Point lastActionUpPoint;
	private long lastEventTime;
	private final MapView mapView;

	public TouchGestureDetector(MapView mapView, ViewConfiguration viewConfiguration) {
		this.mapView = mapView;
		this.doubleTapSlop = viewConfiguration.getScaledDoubleTapSlop();
		this.gestureTimeout = ViewConfiguration.getDoubleTapTimeout();
	}

	@Override
	public void onActionUp(LatLong latLong, Point xy, long eventTime, boolean moveThresholdReached) {
		if (moveThresholdReached) {
			this.lastActionUpPoint = null;
			return;
		} else if (this.lastActionUpPoint != null) {
			long eventTimeDiff = eventTime - this.lastEventTime;

			if (eventTimeDiff < this.gestureTimeout && this.lastActionUpPoint.distance(xy) < this.doubleTapSlop) {
				this.mapView.getModel().mapViewPosition.zoomIn();
				this.lastActionUpPoint = null;
				return;
			}
		}
		this.lastActionUpPoint = xy;
		this.lastEventTime = eventTime;

		for (int i = this.mapView.getLayerManager().getLayers().size() - 1; i >= 0; --i) {
			final Layer ovl = this.mapView.getLayerManager().getLayers().get(i);
			final Point layerXY = toPixels(ovl.getPosition());
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
		for (int i = mapView.getLayerManager().getLayers().size() - 1; i >= 0; --i) {
			final Layer ovl = mapView.getLayerManager().getLayers().get(i);
			final Point layerXY = toPixels(ovl.getPosition());
			if (ovl.onLongPress(latLong, layerXY, xy)) {
				break;
			}
		}
	}

	private Point toPixels(LatLong in) {
		if (in == null || this.mapView.getWidth() <= 0 || this.mapView.getHeight() <= 0) {
			return null;
		}

		MapViewPosition mapPosition = this.mapView.getModel().mapViewPosition;

		// calculate the pixel coordinates of the top left corner
		LatLong geoPoint = mapPosition.getMapPosition().latLong;
		double pixelX = MercatorProjection.longitudeToPixelX(geoPoint.longitude, mapPosition.getZoomLevel());
		double pixelY = MercatorProjection.latitudeToPixelY(geoPoint.latitude, mapPosition.getZoomLevel());
		pixelX -= this.mapView.getWidth() >> 1;
		pixelY -= this.mapView.getHeight() >> 1;
		return new Point((int) (MercatorProjection.longitudeToPixelX(in.longitude, mapPosition.getZoomLevel()) - pixelX),
				(int) (MercatorProjection.latitudeToPixelY(in.latitude, mapPosition.getZoomLevel()) - pixelY));
	}

}
