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

import android.view.ScaleGestureDetector;

class ScaleListener implements ScaleGestureDetector.OnScaleGestureListener {
	private final MapView mapView;
	private float pivotX;
	private float pivotY;
	private float scaleFactorApplied;

	/**
	 * Creates a new ScaleListener for the given MapView.
	 * 
	 * @param mapView
	 *            the MapView which should be scaled.
	 */
	ScaleListener(MapView mapView) {
		this.mapView = mapView;
	}

	@Override
	public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
		float scaleFactor = scaleGestureDetector.getScaleFactor();
		this.scaleFactorApplied *= scaleFactor;
		this.mapView.getFrameBuffer().matrixPostScale(scaleFactor, scaleFactor, this.pivotX, this.pivotY);
		this.mapView.invalidateOnUiThread();
		return true;
	}

	@Override
	public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
		this.scaleFactorApplied = 1;
		this.pivotX = this.mapView.getWidth() >> 1;
		this.pivotY = this.mapView.getHeight() >> 1;
		return true;
	}

	@Override
	public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {
		byte zoomLevelDiff = (byte) Math.round(Math.log(this.scaleFactorApplied) / Math.log(2));
		this.mapView.getMapViewPosition().zoom(zoomLevelDiff, this.scaleFactorApplied);
	}
}
