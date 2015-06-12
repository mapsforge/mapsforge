/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2015 devemux86
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
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.util.MapViewProjection;

import android.view.ScaleGestureDetector;

public class ScaleListener implements ScaleGestureDetector.OnScaleGestureListener {
	private static float threshold = 0.05f;
	private float focusX, focusY;
	private final MapView mapView;
	private final MapViewProjection projection;
	private float scaleFactorApplied;
	private float scaleFactorCumulative;

	/**
	 * Creates a new ScaleListener for the given MapView.
	 * 
	 * @param mapView
	 *            the MapView which should be scaled.
	 */
	public ScaleListener(MapView mapView) {
		this.mapView = mapView;
		this.projection = new MapViewProjection(this.mapView);
	}

	@Override
	public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
		float scaleFactor = scaleGestureDetector.getScaleFactor();
		this.scaleFactorCumulative *= scaleFactor;
		if (this.scaleFactorCumulative < this.scaleFactorApplied - threshold
				|| this.scaleFactorCumulative > this.scaleFactorApplied + threshold) {
			// hysteresis to avoid flickering
			this.mapView.getModel().mapViewPosition.setPivot(projection.fromPixels(focusX, focusY));
			this.mapView.getModel().mapViewPosition.setScaleFactorAdjustment(scaleFactorCumulative);
			this.scaleFactorApplied = this.scaleFactorCumulative;
		}
		return true;
	}

	@Override
	public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
		this.focusX = scaleGestureDetector.getFocusX();
		this.focusY = scaleGestureDetector.getFocusY();
		this.scaleFactorCumulative = 1f;
		this.scaleFactorApplied = 1f;
		return true;
	}

	@Override
	public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {
		double zoomLevelOffset = Math.log(this.scaleFactorCumulative) / Math.log(2);
		byte zoomLevelDiff;
		if (Math.abs(zoomLevelOffset) > 1) {
			zoomLevelDiff = (byte) Math.round(zoomLevelOffset < 0 ? Math.floor(zoomLevelOffset) : Math.ceil(zoomLevelOffset));
		} else {
			zoomLevelDiff = (byte) Math.round(zoomLevelOffset);
		}

		if (zoomLevelDiff != 0) {
			double moveHorizontal = 0, moveVertical = 0;
			Point center = this.mapView.getModel().mapViewDimension.getDimension().getCenter();
			if (zoomLevelDiff > 0) {
				// Zoom in
				for (int i = 0; i < zoomLevelDiff; i++) {
					moveHorizontal += (center.x - focusX) / Math.pow(2, i + 1);
					moveVertical += (center.y - focusY) / Math.pow(2, i + 1);
				}
			} else {
				// Zoom out
				for (int i = 0; i > zoomLevelDiff; i--) {
					moveHorizontal += -(center.x - focusX) / Math.pow(2, i);
					moveVertical += -(center.y - focusY) / Math.pow(2, i);
				}
			}
			this.mapView.getModel().mapViewPosition.setPivot(projection.fromPixels(focusX, focusY));
			this.mapView.getModel().mapViewPosition.moveCenterAndZoom(moveHorizontal, moveVertical, zoomLevelDiff);
		} else {
			this.mapView.getModel().mapViewPosition.zoom(zoomLevelDiff);
		}
	}

}
