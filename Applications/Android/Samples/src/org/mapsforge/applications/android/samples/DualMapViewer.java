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
package org.mapsforge.applications.android.samples;

import java.io.File;

import org.mapsforge.android.maps.MapActivity;
import org.mapsforge.android.maps.MapScaleBar;
import org.mapsforge.android.maps.MapView;
import org.mapsforge.map.reader.header.FileOpenResult;

import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

/**
 * An application which demonstrates how to use two MapView instances at the same time.
 */
public class DualMapViewer extends MapActivity {
	private static final File MAP_FILE = new File(Environment.getExternalStorageDirectory().getPath(), "berlin.map");

	private MapView mapView1;
	private MapView mapView2;

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			finish();
			return true;
		}
		// forward the event to both MapViews for simultaneous movement
		return this.mapView1.onKeyDown(keyCode, event) | this.mapView2.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		// forward the event to both MapViews for simultaneous movement
		return this.mapView1.onKeyUp(keyCode, event) | this.mapView2.onKeyUp(keyCode, event);
	}

	@Override
	public boolean onTrackballEvent(MotionEvent event) {
		// forward the event to both MapViews for simultaneous movement
		return this.mapView1.onTrackballEvent(event) | this.mapView2.onTrackballEvent(event);
	}

	private MapView createMapView(boolean imperialUnits) {
		MapView mapView = new MapView(this);
		mapView.setClickable(true);
		mapView.setBuiltInZoomControls(true);
		mapView.getMapMover().setMoveSpeedFactor(1);

		MapScaleBar mapScaleBar = mapView.getMapScaleBar();
		mapScaleBar.setImperialUnits(imperialUnits);
		mapScaleBar.setShowMapScaleBar(true);

		FileOpenResult fileOpenResult = mapView.setMapFile(MAP_FILE);
		if (!fileOpenResult.isSuccess()) {
			Toast.makeText(this, fileOpenResult.getErrorMessage(), Toast.LENGTH_LONG).show();
			finish();
		}

		return mapView;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.mapView1 = createMapView(false);
		this.mapView2 = createMapView(true);

		// create a LineaLayout that contains both MapViews
		LinearLayout linearLayout = new LinearLayout(this);

		// if the device orientation is portrait, change the orientation to vertical
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			linearLayout.setOrientation(LinearLayout.VERTICAL);
		}

		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT, 1);
		this.mapView1.setLayoutParams(layoutParams);
		this.mapView2.setLayoutParams(layoutParams);

		// add both MapViews to the LinearLayout
		linearLayout.addView(this.mapView1);
		linearLayout.addView(this.mapView2);
		setContentView(linearLayout);
	}
}
