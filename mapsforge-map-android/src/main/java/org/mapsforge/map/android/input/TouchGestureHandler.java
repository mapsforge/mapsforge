/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2013-2014 Ludwig M Brinckmann
 * Copyright 2014, 2015 devemux86
 * Copyright 2014 Jordan Black
 * Copyright 2015 Andreas Schildbach
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

/**
 * Central handling of touch gestures.
 * <ul>
 * <li>Scroll (pan)</li>
 * <li>Scale</li>
 * <li>Scale with focus</li>
 * <li>Quick scale (double tap + swipe)</li>
 * <li>Fling</li>
 * <li>Double tap (zoom with focus)</li>
 * <li>Tap (overlay)</li>
 * <li>Long press (overlay)</li>
 * </ul>
 */
public class TouchGestureHandler extends GestureDetector.SimpleOnGestureListener implements ScaleGestureDetector.OnScaleGestureListener, Runnable {

	private final Scroller flinger;
	private int flingLastX, flingLastY;
	private float focusX, focusY;
	private final Handler handler = new Handler();
	private boolean isInDoubleTap, isInScale;
	private final MapView mapView;
	private LatLong pivot;
	private final MapViewProjection projection;
	private float scaleFactorCumulative;

	public TouchGestureHandler(MapView mapView) {
		this.mapView = mapView;
		this.projection = new MapViewProjection(this.mapView);
		this.flinger = new Scroller(mapView.getContext());
	}

	public void destroy() {
		this.handler.removeCallbacksAndMessages(null);
	}

	@Override
	public boolean onDoubleTapEvent(MotionEvent e) {
		int action = e.getActionMasked();

		switch (action) {
			case MotionEvent.ACTION_DOWN:
				this.isInDoubleTap = true;
				break;
			case MotionEvent.ACTION_UP:
				// Quick scale in between (cancel double tap)
				if (this.isInDoubleTap) {
					MapViewPosition mapViewPosition = this.mapView.getModel().mapViewPosition;
					if (mapViewPosition.getZoomLevel() < mapViewPosition.getZoomLevelMax()) {
						Point center = this.mapView.getModel().mapViewDimension.getDimension().getCenter();
						byte zoomLevelDiff = 1;
						double moveHorizontal = (center.x - e.getX()) / Math.pow(2, zoomLevelDiff);
						double moveVertical = (center.y - e.getY()) / Math.pow(2, zoomLevelDiff);
						LatLong pivot = projection.fromPixels(e.getX(), e.getY());
						if (pivot != null) {
							mapViewPosition.setPivot(pivot);
							mapViewPosition.moveCenterAndZoom(moveHorizontal, moveVertical, zoomLevelDiff);
						}
					}
					this.isInDoubleTap = false;
					return true;
				}
				break;
		}

		return false;
	}

	@Override
	public boolean onDown(MotionEvent e) {
		this.isInScale = false;
		this.flinger.forceFinished(true);
		return true;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		if (!this.isInScale && e1.getPointerCount() == 1 && e2.getPointerCount() == 1) {
			this.flinger.fling(0, 0, (int) -velocityX, (int) -velocityY, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE);
			this.flingLastX = this.flingLastY = 0;
			this.handler.removeCallbacksAndMessages(null);
			this.handler.post(this);
			return true;
		}
		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {
		// Normal or quick scale (no long press)
		if (!this.isInScale && !this.isInDoubleTap) {
			Point tapXY = new Point(e.getX(), e.getY());
			LatLong tapLatLong = projection.fromPixels(tapXY.x, tapXY.y);
			if (tapLatLong != null) {
				for (int i = this.mapView.getLayerManager().getLayers().size() - 1; i >= 0; --i) {
					Layer layer = this.mapView.getLayerManager().getLayers().get(i);
					Point layerXY = projection.toPixels(layer.getPosition());
					if (layer.onLongPress(tapLatLong, layerXY, tapXY)) {
						break;
					}
				}
			}
		}
	}

	@Override
	public boolean onScale(ScaleGestureDetector detector) {
		this.scaleFactorCumulative *= detector.getScaleFactor();
		this.mapView.getModel().mapViewPosition.setPivot(pivot);
		this.mapView.getModel().mapViewPosition.setScaleFactorAdjustment(scaleFactorCumulative);
		return true;
	}

	@Override
	public boolean onScaleBegin(ScaleGestureDetector detector) {
		this.isInScale = true;
		this.scaleFactorCumulative = 1f;

		// Quick scale (no pivot)
		if (this.isInDoubleTap) {
			this.pivot = null;
		} else {
			this.focusX = detector.getFocusX();
			this.focusY = detector.getFocusY();
			this.pivot = projection.fromPixels(focusX, focusY);
		}
		return true;
	}

	@Override
	public void onScaleEnd(ScaleGestureDetector detector) {
		double zoomLevelOffset = Math.log(this.scaleFactorCumulative) / Math.log(2);
		byte zoomLevelDiff;
		if (Math.abs(zoomLevelOffset) > 1) {
			// Complete large zooms towards gesture direction
			zoomLevelDiff = (byte) Math.round(zoomLevelOffset < 0 ? Math.floor(zoomLevelOffset) : Math.ceil(zoomLevelOffset));
		} else {
			zoomLevelDiff = (byte) Math.round(zoomLevelOffset);
		}

		MapViewPosition mapViewPosition = this.mapView.getModel().mapViewPosition;
		if (zoomLevelDiff != 0 && pivot != null) {
			// Zoom with focus
			double moveHorizontal = 0, moveVertical = 0;
			Point center = this.mapView.getModel().mapViewDimension.getDimension().getCenter();
			if (zoomLevelDiff > 0) {
				// Zoom in
				for (int i = 1; i <= zoomLevelDiff; i++) {
					if (mapViewPosition.getZoomLevel() + i > mapViewPosition.getZoomLevelMax()) {
						break;
					}
					moveHorizontal += (center.x - focusX) / Math.pow(2, i);
					moveVertical += (center.y - focusY) / Math.pow(2, i);
				}
			} else {
				// Zoom out
				for (int i = -1; i >= zoomLevelDiff; i--) {
					if (mapViewPosition.getZoomLevel() + i < mapViewPosition.getZoomLevelMin()) {
						break;
					}
					moveHorizontal -= (center.x - focusX) / Math.pow(2, i + 1);
					moveVertical -= (center.y - focusY) / Math.pow(2, i + 1);
				}
			}
			mapViewPosition.setPivot(pivot);
			mapViewPosition.moveCenterAndZoom(moveHorizontal, moveVertical, zoomLevelDiff);
		} else {
			// Zoom without focus
			mapViewPosition.zoom(zoomLevelDiff);
		}

		this.isInDoubleTap = false;
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		if (!this.isInScale && e1.getPointerCount() == 1 && e2.getPointerCount() == 1) {
			this.mapView.getModel().mapViewPosition.moveCenter(-distanceX, -distanceY, false);
			return true;
		}
		return false;
	}

	@Override
	public boolean onSingleTapConfirmed(MotionEvent e) {
		Point tapXY = new Point(e.getX(), e.getY());
		LatLong tapLatLong = projection.fromPixels(tapXY.x, tapXY.y);
		if (tapLatLong != null) {
			for (int i = this.mapView.getLayerManager().getLayers().size() - 1; i >= 0; --i) {
				Layer layer = this.mapView.getLayerManager().getLayers().get(i);
				Point layerXY = projection.toPixels(layer.getPosition());
				if (layer.onTap(tapLatLong, layerXY, tapXY)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public void run() {
		boolean flingerRunning = !this.flinger.isFinished() && this.flinger.computeScrollOffset();
		this.mapView.getModel().mapViewPosition.moveCenter(this.flingLastX - this.flinger.getCurrX(), this.flingLastY - this.flinger.getCurrY());
		this.flingLastX = this.flinger.getCurrX();
		this.flingLastY = this.flinger.getCurrY();
		if (flingerRunning) {
			this.handler.post(this);
		}
	}
}
