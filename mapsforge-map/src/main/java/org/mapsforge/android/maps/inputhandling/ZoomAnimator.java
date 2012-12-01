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
import org.mapsforge.android.maps.PausableThread;

import android.os.SystemClock;

/**
 * A ZoomAnimator handles the zoom-in and zoom-out animations of the corresponding MapView. It runs in a separate thread
 * to avoid blocking the UI thread.
 */
public class ZoomAnimator extends PausableThread {
	private static final int DEFAULT_DURATION = 250;
	private static final int FRAME_LENGTH_IN_MS = 15;
	private static final String THREAD_NAME = "ZoomAnimator";

	private boolean executeAnimation;
	private final MapView mapView;
	private float pivotX;
	private float pivotY;
	private float scaleFactorApplied;
	private long timeStart;
	private float zoomDifference;
	private float zoomStart;

	/**
	 * @param mapView
	 *            the MapView whose zoom level changes should be animated.
	 */
	public ZoomAnimator(MapView mapView) {
		super();
		this.mapView = mapView;
	}

	/**
	 * @return true if the ZoomAnimator is working, false otherwise.
	 */
	public boolean isExecuting() {
		return this.executeAnimation;
	}

	/**
	 * Starts a zoom animation with the current parameters.
	 * 
	 * @param scaleFactorStart
	 *            the scale factor at the begin of the animation.
	 * @param scaleFactorEnd
	 *            the scale factor at the end of the animation.
	 * @param focusX
	 *            the x coordinate of the animation center.
	 * @param focusY
	 *            the y coordinate of the animation center.
	 */
	public void startAnimation(float scaleFactorStart, float scaleFactorEnd, float focusX, float focusY) {
		this.zoomStart = scaleFactorStart;
		this.pivotX = focusX;
		this.pivotY = focusY;

		this.zoomDifference = scaleFactorEnd - scaleFactorStart;
		this.scaleFactorApplied = this.zoomStart;
		this.executeAnimation = true;
		this.timeStart = SystemClock.uptimeMillis();
		synchronized (this) {
			notify();
		}
	}

	@Override
	protected void doWork() throws InterruptedException {
		long timeElapsed = SystemClock.uptimeMillis() - this.timeStart;
		float timeElapsedPercent = Math.min(1, timeElapsed / (float) DEFAULT_DURATION);
		float currentZoom = this.zoomStart + timeElapsedPercent * this.zoomDifference;

		float scaleFactor = currentZoom / this.scaleFactorApplied;
		this.scaleFactorApplied *= scaleFactor;
		this.mapView.getFrameBuffer().matrixPostScale(scaleFactor, scaleFactor, this.pivotX, this.pivotY);

		if (timeElapsed >= DEFAULT_DURATION) {
			this.executeAnimation = false;
			this.mapView.redraw();
		} else {
			this.mapView.postInvalidate();
			sleep(FRAME_LENGTH_IN_MS);
		}
	}

	@Override
	protected String getThreadName() {
		return THREAD_NAME;
	}

	@Override
	protected ThreadPriority getThreadPriority() {
		return ThreadPriority.ABOVE_NORMAL;
	}

	@Override
	protected boolean hasWork() {
		return this.executeAnimation;
	}
}
