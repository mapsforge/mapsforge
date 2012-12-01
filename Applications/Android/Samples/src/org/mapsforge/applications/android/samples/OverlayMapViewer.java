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
import java.util.Arrays;
import java.util.List;

import org.mapsforge.android.maps.MapActivity;
import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.overlay.Circle;
import org.mapsforge.android.maps.overlay.ListOverlay;
import org.mapsforge.android.maps.overlay.Marker;
import org.mapsforge.android.maps.overlay.OverlayItem;
import org.mapsforge.android.maps.overlay.Polygon;
import org.mapsforge.android.maps.overlay.PolygonalChain;
import org.mapsforge.android.maps.overlay.Polyline;
import org.mapsforge.core.model.GeoPoint;
import org.mapsforge.map.reader.header.FileOpenResult;

import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Toast;

/**
 * An application which demonstrates how to use overlays.
 */
public class OverlayMapViewer extends MapActivity {
	private static final GeoPoint BRANDENBURG_GATE = new GeoPoint(52.516273, 13.377725);
	private static final GeoPoint CENTRAL_STATION = new GeoPoint(52.52498, 13.36962);
	private static final File MAP_FILE = new File(Environment.getExternalStorageDirectory().getPath(), "berlin.map");
	private static final GeoPoint VICTORY_COLUMN = new GeoPoint(52.514505, 13.350111);

	private static Circle createCircle() {
		Paint paintFill = new Paint(Paint.ANTI_ALIAS_FLAG);
		paintFill.setStyle(Paint.Style.FILL);
		paintFill.setColor(Color.BLUE);
		paintFill.setAlpha(64);

		Paint paintStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
		paintStroke.setStyle(Paint.Style.STROKE);
		paintStroke.setColor(Color.BLUE);
		paintStroke.setAlpha(128);
		paintStroke.setStrokeWidth(3);

		return new Circle(CENTRAL_STATION, 200, paintFill, paintStroke);
	}

	private static Polygon createPolygon() {
		List<GeoPoint> geoPoints = Arrays.asList(VICTORY_COLUMN, CENTRAL_STATION, BRANDENBURG_GATE);
		PolygonalChain polygonalChain = new PolygonalChain(geoPoints);

		Paint paintFill = new Paint(Paint.ANTI_ALIAS_FLAG);
		paintFill.setStyle(Paint.Style.FILL);
		paintFill.setColor(Color.YELLOW);
		paintFill.setAlpha(96);
		paintFill.setStrokeCap(Cap.ROUND);
		paintFill.setStrokeJoin(Paint.Join.ROUND);

		Paint paintStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
		paintStroke.setStyle(Paint.Style.STROKE);
		paintStroke.setColor(Color.GRAY);
		paintStroke.setAlpha(192);
		paintStroke.setStrokeWidth(5);
		paintStroke.setStrokeCap(Cap.ROUND);
		paintStroke.setStrokeJoin(Paint.Join.ROUND);

		return new Polygon(Arrays.asList(polygonalChain), paintFill, paintStroke);
	}

	private static Polyline createPolyline() {
		List<GeoPoint> geoPoints = Arrays.asList(BRANDENBURG_GATE, VICTORY_COLUMN);
		PolygonalChain polygonalChain = new PolygonalChain(geoPoints);

		Paint paintStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
		paintStroke.setStyle(Paint.Style.STROKE);
		paintStroke.setColor(Color.MAGENTA);
		paintStroke.setAlpha(128);
		paintStroke.setStrokeWidth(7);
		paintStroke.setPathEffect(new DashPathEffect(new float[] { 25, 15 }, 0));

		return new Polyline(polygonalChain, paintStroke);
	}

	private Marker createMarker(int resourceIdentifier, GeoPoint geoPoint) {
		Drawable drawable = getResources().getDrawable(resourceIdentifier);
		return new Marker(geoPoint, Marker.boundCenterBottom(drawable));
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
		setContentView(mapView);

		Circle circle = createCircle();
		Polygon polygon = createPolygon();
		Polyline polyline = createPolyline();
		Marker marker1 = createMarker(R.drawable.marker_red, VICTORY_COLUMN);
		Marker marker2 = createMarker(R.drawable.marker_green, BRANDENBURG_GATE);

		ListOverlay listOverlay = new ListOverlay();
		List<OverlayItem> overlayItems = listOverlay.getOverlayItems();
		overlayItems.add(circle);
		overlayItems.add(polygon);
		overlayItems.add(polyline);
		overlayItems.add(marker1);
		overlayItems.add(marker2);
		mapView.getOverlays().add(listOverlay);
	}
}
