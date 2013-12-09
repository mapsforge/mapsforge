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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Timer;

import org.mapsforge.core.model.Point;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.core.util.MercatorProjection;

import org.mapsforge.core.model.LatLong;

import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.Layers;
import org.mapsforge.map.layer.LayerManager;

import android.content.Context;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.ScaleGestureDetector;
import android.os.Handler;


public class TouchEventHandler {

	private final String TAG = TouchEventHandler.class.getCanonicalName();
	private static final String LISTENER_MUST_NOT_BE_NULL = "listener must not be null";
	private MapView mapView = null;
	private LayerManager layerManager = null;
	private Layers layers = null;
	private final ScaleGestureDetector scaleGestureDetector;

	protected Timer singleTapActionTimer;
	private Point previousTapPosition;
	private long previousTapTime;
	private long doubleTapTimeout = 25;

	private static int getAction(MotionEvent motionEvent) {
		return motionEvent.getAction() & MotionEvent.ACTION_MASK;
	}

	private int activePointerId;
	private Handler longPressHandler;
	private boolean longPressInProgress;
	private Runnable onLongPress;
	private Point lastPosition;
	private final float mapMoveDelta;
	private final MapViewPosition mapViewPosition;
	private boolean moveThresholdReached;
	private final List<TouchEventListener> touchEventListeners = new CopyOnWriteArrayList<TouchEventListener>();

	public TouchEventHandler(Context context, MapView mapView, ViewConfiguration viewConfiguration) {
		this.longPressHandler = new Handler();
		this.mapView = mapView;
		this.layerManager = this.mapView.getLayerManager();
		this.layers = this.layerManager.getLayers();
		this.mapViewPosition = this.mapView.getModel().mapViewPosition;
		this.mapMoveDelta = viewConfiguration.getScaledTouchSlop();
		this.scaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener(mapView.getModel().mapViewPosition));

	}

	public void addListener(TouchEventListener touchEventListener) {
		if (touchEventListener == null) {
			throw new IllegalArgumentException(LISTENER_MUST_NOT_BE_NULL);
		} else if (this.touchEventListeners.contains(touchEventListener)) {
			throw new IllegalArgumentException("listener is already registered: " + touchEventListener);
		}
		this.touchEventListeners.add(touchEventListener);
	}

	/**
	 * @param motionEvent
	 *            the event to be handled.
	 * @return true if the event was handled, false otherwise.
	 */
	public boolean onTouchEvent(MotionEvent motionEvent) {
		int action = getAction(motionEvent);

		// workaround for a bug in the ScaleGestureDetector, see Android issue #12976
		if (action != MotionEvent.ACTION_MOVE || motionEvent.getPointerCount() > 1) {
			this.scaleGestureDetector.onTouchEvent(motionEvent);
		}

		switch (action) {
			case MotionEvent.ACTION_DOWN:
				return onActionDown(motionEvent);
			case MotionEvent.ACTION_MOVE:
				return onActionMove(motionEvent);
			case MotionEvent.ACTION_POINTER_DOWN:
				return onActionPointerDown(motionEvent);
			case MotionEvent.ACTION_POINTER_UP:
				return onActionPointerUp(motionEvent);
			case MotionEvent.ACTION_UP:
				return onActionUp(motionEvent);
			case MotionEvent.ACTION_CANCEL:
				return true;
		}

		// the event was not handled
		return false;
	}

	public void removeListener(TouchEventListener touchEventListener) {
		if (touchEventListener == null) {
			throw new IllegalArgumentException(LISTENER_MUST_NOT_BE_NULL);
		} else if (!this.touchEventListeners.contains(touchEventListener)) {
			throw new IllegalArgumentException("listener is not registered: " + touchEventListener);
		}
		this.touchEventListeners.remove(touchEventListener);
	}


	private LatLong fromPixels(Point p) {
		MapViewPosition mapPosition = this.mapView.getModel().mapViewPosition;
		LatLong geoPoint = mapPosition.getMapPosition().latLong;

		double pixelX = MercatorProjection.longitudeToPixelX(geoPoint.longitude, mapPosition.getZoomLevel());
		double pixelY = MercatorProjection.latitudeToPixelY(geoPoint.latitude, mapPosition.getZoomLevel());

		pixelX -= this.mapView.getWidth() >> 1;
		pixelY -= this.mapView.getHeight() >> 1;

		LatLong l = new LatLong(MercatorProjection.pixelYToLatitude(pixelY + p.y, mapPosition.getZoomLevel()),
				MercatorProjection.pixelXToLongitude(pixelX + p.x, mapPosition.getZoomLevel()));

		return l;
	}

	public Point toPixels(LatLong in) {
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

	private LatLong getPosition(Layer ovl) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, NullPointerException, ExceptionInInitializerError {
		final Method getPosition = ovl.getClass().getMethod("getPosition");
		return (LatLong)getPosition.invoke(ovl);
	}

	private boolean onActionDown(MotionEvent motionEvent) {
		this.activePointerId = motionEvent.getPointerId(0);
		this.lastPosition = new Point(motionEvent.getX(), motionEvent.getY());
		final LatLong tapPoint = fromPixels(lastPosition);
		this.moveThresholdReached = false;
		this.longPressInProgress = true;

		onLongPress = new Runnable() {
			@Override
			public void run() {
				if (TouchEventHandler.this.longPressInProgress) {
					synchronized (TouchEventHandler.this.layerManager.getLayers()) {
						for (int i = TouchEventHandler.this.layers.size() - 1; i >= 0; --i) {
							final Layer ovl = TouchEventHandler.this.layers.get(i);
							Point layerXY = toPixels(ovl.getPosition());
							if (ovl.onLongPress(tapPoint, layerXY, TouchEventHandler.this.lastPosition)) {
								break;
							}
						}
						TouchEventHandler.this.longPressInProgress = false;
					}
				}
			}
		};

		this.longPressHandler.postDelayed(onLongPress, ViewConfiguration.getLongPressTimeout());
		synchronized (this.layerManager.getLayers()) {
			for (int i = this.layers.size() - 1; i >= 0; --i) {
				final Layer ovl = this.layers.get(i);
				Point layerXY = toPixels(ovl.getPosition());
				if (ovl.onTap(tapPoint, layerXY, this.lastPosition)) {
					return true;
				}
			}
		}
		return true;
	}

	private boolean onActionMove(MotionEvent motionEvent) {
		if (this.scaleGestureDetector.isInProgress()) {
			cancelLongPress();
			return true;
		}

		int pointerIndex = motionEvent.findPointerIndex(this.activePointerId);

		float moveX = (float) (motionEvent.getX(pointerIndex) - this.lastPosition.x);
		float moveY = (float) (motionEvent.getY(pointerIndex) - this.lastPosition.y);
		if (this.moveThresholdReached) {
			this.lastPosition = new Point(motionEvent.getX(pointerIndex), motionEvent.getY(pointerIndex));
			this.mapViewPosition.moveCenter(moveX, moveY);
		} else if (Math.abs(moveX) > this.mapMoveDelta || Math.abs(moveY) > this.mapMoveDelta) {
			cancelLongPress();
			this.moveThresholdReached = true;
			this.lastPosition = new Point(motionEvent.getX(pointerIndex), motionEvent.getY(pointerIndex));
		}

		return true;
	}

	private boolean onActionPointerDown(MotionEvent motionEvent) {
		long eventTime = motionEvent.getEventTime();

		for (TouchEventListener touchEventListener : this.touchEventListeners) {
			touchEventListener.onPointerDown(eventTime);
		}

		return true;
	}

	private boolean onActionPointerUp(MotionEvent motionEvent) {
		// extract the index of the pointer that left the touch sensor
		int pointerIndex = (motionEvent.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;

		if (motionEvent.getPointerId(pointerIndex) == this.activePointerId) {
			// the active pointer has gone up, choose a new one
			if (pointerIndex == 0) {
				pointerIndex = 1;
			} else {
				pointerIndex = 0;
			}
			this.activePointerId = motionEvent.getPointerId(pointerIndex);
			this.lastPosition = new Point(motionEvent.getX(pointerIndex), motionEvent.getY(pointerIndex));
		}

		long eventTime = motionEvent.getEventTime();

		for (TouchEventListener touchEventListener : this.touchEventListeners) {
			touchEventListener.onPointerUp(eventTime);
		}

		return true;
	}

	private boolean onActionUp(MotionEvent motionEvent) {
		cancelLongPress();
		int pointerIndex = motionEvent.findPointerIndex(this.activePointerId);
		Point point = new Point(motionEvent.getX(pointerIndex), motionEvent.getY(pointerIndex));
		long eventTime = motionEvent.getEventTime();

		for (TouchEventListener touchEventListener : this.touchEventListeners) {
			touchEventListener.onActionUp(point, eventTime, this.moveThresholdReached);
		}

		return true;
	}

	private void cancelLongPress() {
		this.longPressInProgress = false;
		this.longPressHandler.removeCallbacks(onLongPress);
	}
}
