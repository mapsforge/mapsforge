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

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.util.MapViewProjection;

import android.os.Handler;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ViewConfiguration;

public class TouchEventHandler {
	private static final String LISTENER_MUST_NOT_BE_NULL = "listener must not be null";

	private static int getAction(MotionEvent motionEvent) {
		return motionEvent.getAction() & MotionEvent.ACTION_MASK;
	}

	private int activePointerId;
	private LatLong lastLatLong;
	private int lastNumberOfPointers;
	private Point lastPosition;
	private boolean longPressConsumed;
	private Handler longPressHandler;
	private boolean longPressInProgress;
	private final float mapMoveDelta;
	private final MapView mapView;
	private boolean moveThresholdReached;
	private Runnable onLongPress;
	private final MapViewProjection projection;
	private final ScaleGestureDetector scaleGestureDetector;

	private final List<TouchEventListener> touchEventListeners = new CopyOnWriteArrayList<TouchEventListener>();

	public TouchEventHandler(MapView mapView, ViewConfiguration viewConfiguration, ScaleGestureDetector sgd) {
		this.longPressHandler = new Handler();
		this.mapView = mapView;
		this.mapMoveDelta = viewConfiguration.getScaledTouchSlop();
		this.scaleGestureDetector = sgd;
		this.projection = new MapViewProjection(this.mapView);
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
				cancelLongPress();
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

	private void cancelLongPress() {
		this.longPressInProgress = false;
		if (this.onLongPress != null) {
			this.longPressHandler.removeCallbacks(onLongPress);
			this.onLongPress = null;
		}
	}

	private boolean onActionDown(MotionEvent motionEvent) {
		this.activePointerId = motionEvent.getPointerId(0);
		this.lastPosition = new Point(motionEvent.getX(), motionEvent.getY());
		this.lastLatLong = projection.fromPixels(this.lastPosition.x, this.lastPosition.y);
		this.lastNumberOfPointers = motionEvent.getPointerCount();
		this.moveThresholdReached = false;

		if (this.lastNumberOfPointers == 1) {
			// set up a handler that will run after the long press interval expires,
			// unless the operations is cancelled first
			this.longPressInProgress = true;
			this.longPressConsumed = false;
			onLongPress = new Runnable() {
				@Override
				public void run() {
					TouchEventHandler.this.longPressConsumed = true;
					if (TouchEventHandler.this.longPressInProgress) {
						if (TouchEventHandler.this.lastNumberOfPointers == 1) {
							for (TouchEventListener touchEventListener : TouchEventHandler.this.touchEventListeners) {
								touchEventListener.onLongPress(TouchEventHandler.this.lastLatLong,
										TouchEventHandler.this.lastPosition);
							}
						}
						TouchEventHandler.this.longPressInProgress = false;
					}
				}
			};
			this.longPressHandler.postDelayed(onLongPress, ViewConfiguration.getLongPressTimeout());
		}

		return true;
	}

	private boolean onActionMove(MotionEvent motionEvent) {
		if (this.scaleGestureDetector.isInProgress()) {
			cancelLongPress();
			return true;
		}

		this.lastNumberOfPointers = motionEvent.getPointerCount();
		int pointerIndex = motionEvent.findPointerIndex(this.activePointerId);

		float moveX = (float) (motionEvent.getX(pointerIndex) - this.lastPosition.x);
		float moveY = (float) (motionEvent.getY(pointerIndex) - this.lastPosition.y);
		if (this.moveThresholdReached) {
			this.lastPosition = new Point(motionEvent.getX(pointerIndex), motionEvent.getY(pointerIndex));
			this.mapView.getModel().mapViewPosition.moveCenter(moveX, moveY);
		} else if (Math.abs(moveX) > this.mapMoveDelta || Math.abs(moveY) > this.mapMoveDelta) {
			cancelLongPress();
			this.moveThresholdReached = true;
			this.lastPosition = new Point(motionEvent.getX(pointerIndex), motionEvent.getY(pointerIndex));
		}

		return true;
	}

	private boolean onActionPointerDown(MotionEvent motionEvent) {

		this.lastNumberOfPointers = motionEvent.getPointerCount();

		long eventTime = motionEvent.getEventTime();

		for (TouchEventListener touchEventListener : this.touchEventListeners) {
			touchEventListener.onPointerDown(eventTime);
		}

		return true;
	}

	private boolean onActionPointerUp(MotionEvent motionEvent) {

		this.lastNumberOfPointers = motionEvent.getPointerCount();
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

		this.lastNumberOfPointers = motionEvent.getPointerCount();
		if (longPressConsumed) {
			// the press was consumed by a long press action, and the up must
			// not be handled anymore.
			return true;
		}

		cancelLongPress();

		int pointerIndex = motionEvent.findPointerIndex(this.activePointerId);
		Point point = new Point(motionEvent.getX(pointerIndex), motionEvent.getY(pointerIndex));
		long eventTime = motionEvent.getEventTime();

		for (TouchEventListener touchEventListener : this.touchEventListeners) {
			touchEventListener.onActionUp(this.lastLatLong, point, eventTime, this.moveThresholdReached);
		}

		return true;
	}
}
