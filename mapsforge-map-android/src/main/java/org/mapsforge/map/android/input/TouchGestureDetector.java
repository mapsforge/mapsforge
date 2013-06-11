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

import org.mapsforge.core.model.Point;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.model.Model;

import android.view.ViewConfiguration;

public class TouchGestureDetector implements TouchEventListener {
	private final float doubleTapSlop;

	private final int gestureTimeout;
	private Point lastActionUpPoint;
	private long lastEventTime;
	private final MapViewPosition mapViewPosition;

	public TouchGestureDetector(Model model, ViewConfiguration viewConfiguration) {
		this.mapViewPosition = model.mapViewPosition;
		this.doubleTapSlop = viewConfiguration.getScaledDoubleTapSlop();
		this.gestureTimeout = ViewConfiguration.getDoubleTapTimeout();
	}

	@Override
	public void onActionUp(Point point, long eventTime, boolean moveThresholdReached) {
		if (moveThresholdReached) {
			this.lastActionUpPoint = null;
			return;
		} else if (this.lastActionUpPoint != null) {
			long eventTimeDiff = eventTime - this.lastEventTime;

			if (eventTimeDiff < this.gestureTimeout && this.lastActionUpPoint.distance(point) < this.doubleTapSlop) {
				this.mapViewPosition.zoomIn();
				this.lastActionUpPoint = null;
				return;
			}
		}

		this.lastActionUpPoint = point;
		this.lastEventTime = eventTime;
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
			this.mapViewPosition.zoomOut();
		}
	}
}
