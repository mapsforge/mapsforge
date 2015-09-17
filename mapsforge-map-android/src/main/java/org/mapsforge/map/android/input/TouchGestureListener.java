/*
 * Copyright 2015 Andreas Schildbach
 * Copyright 2013 Ludwig M Brinckmann
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

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.util.MapViewProjection;

import android.os.Handler;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.Scroller;

public class TouchGestureListener
		implements GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener, ScaleGestureDetector.OnScaleGestureListener, Runnable {

	private final MapView mapView;
	private final MapViewProjection projection;
	private final Scroller flinger;
	private final Handler handler = new Handler();
	private float scaleFactorCumulative = 1f;
	private int lastFlingPositionX = 0, lastFlingPositionY = 0;

	public TouchGestureListener(final MapView mapView) {
		this.mapView = mapView;
		this.projection = new MapViewProjection(this.mapView);
		this.flinger = new Scroller(mapView.getContext());
	}

	@Override
	public boolean onDown(MotionEvent e) {
		flinger.abortAnimation();
		return true;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		flinger.fling(0, 0, (int) -velocityX, (int) -velocityY, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE);
		lastFlingPositionX = 0;
		lastFlingPositionY = 0;
		handler.removeCallbacksAndMessages(null);
		handler.post(this);
		return true;
	}

	@Override
	public void onLongPress(MotionEvent e) {
		for (int i = this.mapView.getLayerManager().getLayers().size() - 1; i >= 0; --i) {
			final Layer layer = this.mapView.getLayerManager().getLayers().get(i);
			final Point layerXY = projection.toPixels(layer.getPosition());
			final Point tapXY = new Point(e.getX(), e.getY());
			final LatLong latLong = projection.fromPixels(tapXY.x, tapXY.y);
			if (layer.onLongPress(latLong, layerXY, tapXY)) {
				break;
			}
		}
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		mapView.getModel().mapViewPosition.moveCenter(-distanceX, -distanceY, false);
		return true;
	}

	@Override
	public void onShowPress(MotionEvent e) {
		// ignore
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		return false;
	}

	@Override
	public boolean onDoubleTap(MotionEvent e) {
		final MapViewPosition mapViewPosition = this.mapView.getModel().mapViewPosition;
		if (mapViewPosition.getZoomLevel() < mapViewPosition.getZoomLevelMax()) {
			final Point center = this.mapView.getModel().mapViewDimension.getDimension().getCenter();
			final byte diffZoom = 1;
			final double diffX = (center.x - e.getX()) / Math.pow(2, diffZoom);
			final double diffY = (center.y - e.getY()) / Math.pow(2, diffZoom);
			mapViewPosition.setPivot(projection.fromPixels(e.getX(), e.getY()));
			mapViewPosition.moveCenterAndZoom(diffX, diffY, diffZoom);
		}
		return true;
	}

	@Override
	public boolean onSingleTapConfirmed(MotionEvent e) {
		return false;
	}

	@Override
	public boolean onDoubleTapEvent(MotionEvent e) {
		return false;
	}

	@Override
	public boolean onScaleBegin(ScaleGestureDetector detector) {
		this.scaleFactorCumulative = 1f;
		return true;
	}

	@Override
	public boolean onScale(ScaleGestureDetector detector) {
		final float focusX = detector.getFocusX();
		final float focusY = detector.getFocusY();
		final float scaleFactor = detector.getScaleFactor();
		this.scaleFactorCumulative *= scaleFactor;
		this.mapView.getModel().mapViewPosition.setPivot(projection.fromPixels(focusX, focusY));
		this.mapView.getModel().mapViewPosition.setScaleFactorAdjustment(scaleFactorCumulative);
		return true;
	}

	@Override
	public void onScaleEnd(ScaleGestureDetector detector) {
		double zoomLevelOffset = Math.log(this.scaleFactorCumulative) / Math.log(2);
		final byte zoomLevelDiff;
		if (Math.abs(zoomLevelOffset) > 1) {
			zoomLevelDiff = (byte) Math.round(zoomLevelOffset < 0 ? Math.floor(zoomLevelOffset) : Math.ceil(zoomLevelOffset));
		}
		else {
			zoomLevelDiff = (byte) Math.round(zoomLevelOffset);
		}
		this.mapView.getModel().mapViewPosition.zoom(zoomLevelDiff);
		this.scaleFactorCumulative = 1f;
	}

	@Override
	public void run() {
		final boolean flingerRunning = !flinger.isFinished() && flinger.computeScrollOffset();
		mapView.getModel().mapViewPosition.moveCenter(lastFlingPositionX - flinger.getCurrX(), lastFlingPositionY - flinger.getCurrY());
		lastFlingPositionX = flinger.getCurrX();
		lastFlingPositionY = flinger.getCurrY();
		if (flingerRunning) {
			handler.post(this);
		}
	}

	public void destroy() {
		handler.removeCallbacksAndMessages(null);
	}
}
