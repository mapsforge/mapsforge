/*
 * Copyright 2010, 2011, 2012 mapsforge.org
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
package org.mapsforge.android.maps.inputhandling;

import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.MapViewPosition;
import org.mapsforge.core.model.Point;

import android.content.Context;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ViewConfiguration;

/**
 * Implementation for multi-touch capable devices, requires Android API level 8 or higher.
 */
public class TouchEventHandler {
	private static final int INVALID_POINTER_ID = -1;

	/**
	 * @param motionEvent
	 *            the motion event whose action should be returned.
	 * @return the int value of the action as defined in the {@link MotionEvent} class.
	 */
	public static int getAction(MotionEvent motionEvent) {
		return motionEvent.getAction() & MotionEvent.ACTION_MASK;
	}

	private int activePointerId;
	private final float doubleTapDelta;
	private final int doubleTapTimeout;
	private long doubleTouchStart;
	private final float mapMoveDelta;
	private final MapViewPosition mapViewPosition;
	private boolean moveThresholdReached;
	private boolean previousEventTap;
	private Point previousPosition;
	private Point previousTapPosition;
	private long previousTapTime;
	private final ScaleGestureDetector scaleGestureDetector;

	/**
	 * @param context
	 *            a reference to the global application environment.
	 * @param mapView
	 *            the MapView from which the touch events are coming from.
	 */
	public TouchEventHandler(Context context, MapView mapView) {
		this.mapViewPosition = mapView.getMapViewPosition();
		ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
		this.mapMoveDelta = viewConfiguration.getScaledTouchSlop();
		this.doubleTapDelta = viewConfiguration.getScaledDoubleTapSlop();
		this.doubleTapTimeout = ViewConfiguration.getDoubleTapTimeout();
		this.scaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener(mapView));
	}

	/**
	 * Handles a {@code MotionEvent} from the touch screen.
	 * 
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

			case MotionEvent.ACTION_UP:
				return onActionUp(motionEvent);

			case MotionEvent.ACTION_CANCEL:
				return onActionCancel();

			case MotionEvent.ACTION_POINTER_DOWN:
				return onActionPointerDown(motionEvent);

			case MotionEvent.ACTION_POINTER_UP:
				return onActionPointerUp(motionEvent);
		}

		// the event was not handled
		return false;
	}

	private boolean onActionCancel() {
		this.activePointerId = INVALID_POINTER_ID;
		return true;
	}

	private boolean onActionDown(MotionEvent motionEvent) {
		this.activePointerId = motionEvent.getPointerId(0);
		this.previousPosition = new Point(motionEvent.getX(), motionEvent.getY());
		this.moveThresholdReached = false;
		return true;
	}

	private boolean onActionMove(MotionEvent motionEvent) {
		if (this.scaleGestureDetector.isInProgress()) {
			return true;
		}

		int pointerIndex = motionEvent.findPointerIndex(this.activePointerId);
		float moveX = (float) (motionEvent.getX(pointerIndex) - this.previousPosition.x);
		float moveY = (float) (motionEvent.getY(pointerIndex) - this.previousPosition.y);

		if (!this.moveThresholdReached) {
			if (Math.abs(moveX) > this.mapMoveDelta || Math.abs(moveY) > this.mapMoveDelta) {
				this.moveThresholdReached = true;
				this.previousPosition = new Point(motionEvent.getX(pointerIndex), motionEvent.getY(pointerIndex));
			}
			return true;
		}

		this.previousPosition = new Point(motionEvent.getX(pointerIndex), motionEvent.getY(pointerIndex));
		this.mapViewPosition.moveCenter(moveX, moveY);
		return true;
	}

	private boolean onActionPointerDown(MotionEvent motionEvent) {
		this.doubleTouchStart = motionEvent.getEventTime();
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
			this.previousPosition = new Point(motionEvent.getX(pointerIndex), motionEvent.getY(pointerIndex));
		}

		long doubleTouchTime = motionEvent.getEventTime() - this.doubleTouchStart;
		if (doubleTouchTime < this.doubleTapTimeout) {
			// multi-touch tap event, zoom out
			this.previousEventTap = false;
			this.mapViewPosition.zoomOut();
		}
		return true;
	}

	private boolean onActionUp(MotionEvent motionEvent) {
		int pointerIndex = motionEvent.findPointerIndex(this.activePointerId);
		this.activePointerId = INVALID_POINTER_ID;
		if (this.moveThresholdReached) {
			this.previousEventTap = false;
		} else {
			if (this.previousEventTap) {
				double diffX = Math.abs(motionEvent.getX(pointerIndex) - this.previousTapPosition.x);
				double diffY = Math.abs(motionEvent.getY(pointerIndex) - this.previousTapPosition.y);
				long doubleTapTime = motionEvent.getEventTime() - this.previousTapTime;

				if (diffX < this.doubleTapDelta && diffY < this.doubleTapDelta && doubleTapTime < this.doubleTapTimeout) {
					// double-tap event, zoom in
					this.previousEventTap = false;
					this.mapViewPosition.zoomIn();
					return true;
				}
			} else {
				this.previousEventTap = true;
			}

			this.previousTapPosition = new Point(motionEvent.getX(pointerIndex), motionEvent.getY(pointerIndex));
			this.previousTapTime = motionEvent.getEventTime();
		}
		return true;
	}
}
