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

import org.mapsforge.map.model.MapViewPosition;

import android.view.ScaleGestureDetector;

public class ScaleListener implements ScaleGestureDetector.OnScaleGestureListener {
	private static float threshold = 0.05f;
	private final MapViewPosition mapViewPosition;
	private float scaleFactorApplied;
	private float scaleFactorCumulative;

	/**
	 * Creates a new ScaleListener for the given MapView.
	 * 
	 * @param mapViewPosition
	 *            the MapViewPosition which should be scaled.
	 */
	public ScaleListener(MapViewPosition mapViewPosition) {
		this.mapViewPosition = mapViewPosition;
	}

	@Override
	public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
		float scaleFactor = scaleGestureDetector.getScaleFactor();
		this.scaleFactorCumulative *= scaleFactor;
		if (this.scaleFactorCumulative < this.scaleFactorApplied - threshold
				|| this.scaleFactorCumulative > this.scaleFactorApplied + threshold) {
			// hysteresis to avoid flickering
			this.mapViewPosition.setScaleFactorAdjustment(scaleFactorCumulative);
			this.scaleFactorApplied = this.scaleFactorCumulative;
		}
		return true;
	}

	@Override
	public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
		this.scaleFactorCumulative = 1f;
		this.scaleFactorApplied = 1f;
		return true;
	}

	@Override
	public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {
		byte zoomLevelDiff = (byte) Math.round(Math.log(this.scaleFactorCumulative) / Math.log(2));
		this.mapViewPosition.zoom(zoomLevelDiff);
	}

}
