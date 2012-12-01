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
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.mapsforge.android.maps.MapActivity;
import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.overlay.Circle;
import org.mapsforge.android.maps.overlay.ListOverlay;
import org.mapsforge.android.maps.overlay.OverlayItem;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.GeoPoint;
import org.mapsforge.map.reader.header.FileOpenResult;

import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Toast;

/**
 * An application which demonstrates how to use overlays.
 */
public class OverlayBenchmark extends MapActivity {
	private static final File MAP_FILE = new File(Environment.getExternalStorageDirectory().getPath(), "berlin.map");
	private static final int NUMBER_OF_CIRCLES = 10000;

	private static List<OverlayItem> createCircles() {
		List<OverlayItem> overlayItems = new ArrayList<OverlayItem>(NUMBER_OF_CIRCLES);

		Paint paintFill = new Paint(Paint.ANTI_ALIAS_FLAG);
		paintFill.setStyle(Paint.Style.FILL);
		paintFill.setColor(Color.BLUE);
		paintFill.setAlpha(64);

		Paint paintStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
		paintStroke.setStyle(Paint.Style.STROKE);
		paintStroke.setColor(Color.BLUE);
		paintStroke.setAlpha(96);
		paintStroke.setStrokeWidth(3);

		Random random = new Random(0);
		BoundingBox boundingBox = new BoundingBox(52.33446, 13.08283, 52.6783, 13.76136);
		double latitudeSpan = boundingBox.getLatitudeSpan();
		double longitudeSpan = boundingBox.getLongitudeSpan();

		for (int i = 0; i < NUMBER_OF_CIRCLES; ++i) {
			double latitude = boundingBox.minLatitude + random.nextDouble() * latitudeSpan;
			double longitude = boundingBox.minLongitude + random.nextDouble() * longitudeSpan;
			GeoPoint geoPoint = new GeoPoint(latitude, longitude);
			overlayItems.add(new Circle(geoPoint, 50, paintFill, paintStroke));
		}

		return overlayItems;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		MapView mapView = new MapView(this);
		mapView.setClickable(true);
		mapView.setBuiltInZoomControls(true);
		FileOpenResult fileOpenResult = mapView.setMapFile(MAP_FILE);
		if (!fileOpenResult.isSuccess()) {
			Toast.makeText(this, fileOpenResult.getErrorMessage(), Toast.LENGTH_LONG).show();
			finish();
		}
		mapView.getFpsCounter().setFpsCounter(true);
		setContentView(mapView);

		List<OverlayItem> overlayItems = createCircles();
		ListOverlay listOverlay = new ListOverlay();
		listOverlay.getOverlayItems().addAll(overlayItems);
		mapView.getOverlays().add(listOverlay);
	}
}
