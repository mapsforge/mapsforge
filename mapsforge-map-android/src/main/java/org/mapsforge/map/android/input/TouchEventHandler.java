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

import org.mapsforge.core.model.Point;
import org.mapsforge.map.model.MapViewPosition;

import android.view.MotionEvent;
import android.view.ViewConfiguration;

public class TouchEventHandler {
	private static final String LISTENER_MUST_NOT_BE_NULL = "listener must not be null";

	private static int getAction(MotionEvent motionEvent) {
		return motionEvent.getAction() & MotionEvent.ACTION_MASK;
	}

	private int activePointerId;
	private Point lastPosition;
	private final float mapMoveDelta;
	private final MapViewPosition mapViewPosition;
	private boolean moveThresholdReached;
	private final List<TouchEventListener> touchEventListeners = new CopyOnWriteArrayList<TouchEventListener>();

	public TouchEventHandler(MapViewPosition mapViewPosition, ViewConfiguration viewConfiguration) {
		this.mapViewPosition = mapViewPosition;
		this.mapMoveDelta = viewConfiguration.getScaledTouchSlop();
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

	private boolean onActionDown(MotionEvent motionEvent) {
		this.activePointerId = motionEvent.getPointerId(0);
		this.lastPosition = new Point(motionEvent.getX(), motionEvent.getY());
		this.moveThresholdReached = false;

		return true;
	}

	private boolean onActionMove(MotionEvent motionEvent) {
		int pointerIndex = motionEvent.findPointerIndex(this.activePointerId);

		float moveX = (float) (motionEvent.getX(pointerIndex) - this.lastPosition.x);
		float moveY = (float) (motionEvent.getY(pointerIndex) - this.lastPosition.y);
		if (this.moveThresholdReached) {
			this.lastPosition = new Point(motionEvent.getX(pointerIndex), motionEvent.getY(pointerIndex));
			this.mapViewPosition.moveCenter(moveX, moveY);
		} else if (Math.abs(moveX) > this.mapMoveDelta || Math.abs(moveY) > this.mapMoveDelta) {
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
		int pointerIndex = motionEvent.findPointerIndex(this.activePointerId);
		Point point = new Point(motionEvent.getX(pointerIndex), motionEvent.getY(pointerIndex));
		long eventTime = motionEvent.getEventTime();

		for (TouchEventListener touchEventListener : this.touchEventListeners) {
			touchEventListener.onActionUp(point, eventTime, this.moveThresholdReached);
		}

		return true;
	}
}
