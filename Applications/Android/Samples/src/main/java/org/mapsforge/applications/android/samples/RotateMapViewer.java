/*
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
package org.mapsforge.applications.android.samples;

import org.mapsforge.map.android.util.AndroidUtil;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

/**
 * Demonstration of map rotation via a {@link RotateView}.
 */
public class RotateMapViewer extends OverlayMapViewer {

	@Override
	protected void createMapViews() {
		mapView = getMapView();
		mapView.getModel().frameBufferModel.setOverdrawFactor(1.0d);
		// mapView.getModel().init(this.preferencesFacade);
		mapView.setClickable(true);
		mapView.getMapScaleBar().setVisible(false);
		mapView.setBuiltInZoomControls(hasZoomControls());
		mapView.getMapZoomControls().setZoomLevelMin(getZoomLevelMin());
		mapView.getMapZoomControls().setZoomLevelMax(getZoomLevelMax());
		initializePosition(mapView.getModel().mapViewPosition);

		// Rotate button
		Button rotateButton = (Button) findViewById(R.id.rotateButton);
		rotateButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				RotateView rotateView = (RotateView) findViewById(R.id.rotateView);
				rotateView.setHeading(rotateView.getHeading() - 45f);
				rotateView.postInvalidate();
			}
		});
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
	@SuppressWarnings("deprecation")
	@Override
	protected void createTileCaches() {
		boolean threaded = sharedPreferences.getBoolean(
				SamplesApplication.SETTING_TILECACHE_THREADING, true);
		int queueSize = Integer.parseInt(sharedPreferences.getString(
				SamplesApplication.SETTING_TILECACHE_QUEUESIZE, "4"));
		boolean persistent = sharedPreferences.getBoolean(
				SamplesApplication.SETTING_TILECACHE_PERSISTENCE, true);

		Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
				.getDefaultDisplay();
		final int hypot;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
			android.graphics.Point point = new android.graphics.Point();
			display.getSize(point);
			hypot = (int) Math.hypot(point.x, point.y);
		} else {
			hypot = (int) Math.hypot(display.getWidth(), display.getHeight());
		}

		this.tileCaches.add(AndroidUtil.createTileCache(this,
				getPersistableId(),
				this.mapView.getModel().displayModel.getTileSize(), hypot,
				hypot,
				this.mapView.getModel().frameBufferModel.getOverdrawFactor(),
				threaded, queueSize, persistent));
	}

	@Override
	protected int getLayoutId() {
		return R.layout.rotatemapviewer;
	}

	@Override
	protected boolean hasZoomControls() {
		return false;
	}
}
