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
package org.mapsforge.map.scalebar;

import org.mapsforge.core.model.MapPosition;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.model.MapViewPosition;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Typeface;

/**
 * A MapScaleBar displays the ratio of a distance on the map to the corresponding distance on the ground.
 */
public class MapScaleBar {
	private static final int BITMAP_HEIGHT = 50;
	private static final int BITMAP_WIDTH = 150;
	private static final double LATITUDE_REDRAW_THRESHOLD = 0.2;
	private static final int MARGIN_BOTTOM = 5;
	private static final int MARGIN_LEFT = 5;
	private static final Paint SCALE_BAR = createScaleBarPaint(Color.BLACK, 3);
	private static final Paint SCALE_BAR_STROKE = createScaleBarPaint(Color.WHITE, 5);
	private static final Paint SCALE_TEXT = createTextPaint(Color.BLACK, 0);
	private static final Paint SCALE_TEXT_STROKE = createTextPaint(Color.WHITE, 2);

	private static Paint createScaleBarPaint(int color, float strokeWidth) {
		Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setColor(color);
		paint.setStrokeWidth(strokeWidth);
		paint.setStyle(Style.STROKE);
		paint.setStrokeCap(Paint.Cap.SQUARE);
		return paint;
	}

	private static Paint createTextPaint(int color, float strokeWidth) {
		Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setColor(color);
		paint.setStrokeWidth(strokeWidth);
		paint.setStyle(Style.STROKE);
		paint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
		paint.setTextSize(20);
		return paint;
	}

	private Adapter adapter;
	private MapPosition mapPosition;
	private final Bitmap mapScaleBitmap;
	private final Canvas mapScaleCanvas;
	private final MapViewPosition mapViewPosition;
	private boolean redrawNeeded;
	private boolean visible;

	public MapScaleBar(MapViewPosition mapViewPosition) {
		this.mapViewPosition = mapViewPosition;
		this.mapScaleBitmap = Bitmap.createBitmap(BITMAP_WIDTH, BITMAP_HEIGHT, Bitmap.Config.ARGB_8888);
		this.mapScaleCanvas = new Canvas(this.mapScaleBitmap);

		this.adapter = Metric.INSTANCE;
	}

	public void draw(Canvas canvas) {
		if (!this.visible) {
			return;
		}

		redraw();

		int top = canvas.getHeight() - BITMAP_HEIGHT - MARGIN_BOTTOM;
		canvas.drawBitmap(this.mapScaleBitmap, MARGIN_LEFT, top, null);
	}

	public Adapter getAdapter() {
		return this.adapter;
	}

	public boolean isVisible() {
		return this.visible;
	}

	public void setAdapter(Adapter adapter) {
		if (adapter == null) {
			throw new IllegalArgumentException("adapter must not be null");
		}
		this.adapter = adapter;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	/**
	 * Redraws the map scale bitmap with the given parameters.
	 * 
	 * @param scaleBarLength
	 *            the length of the map scale bar in pixels.
	 * @param mapScaleValue
	 *            the map scale value in meters.
	 */
	private void draw(float scaleBarLength, int mapScaleValue) {
		this.mapScaleBitmap.eraseColor(Color.TRANSPARENT);

		drawScaleBar(scaleBarLength, SCALE_BAR_STROKE);
		drawScaleBar(scaleBarLength, SCALE_BAR);

		String scaleText = this.adapter.getScaleText(mapScaleValue);
		drawScaleText(scaleText, SCALE_TEXT_STROKE);
		drawScaleText(scaleText, SCALE_TEXT);
	}

	private void drawScaleBar(float scaleBarLength, Paint paint) {
		this.mapScaleCanvas.drawLine(7, 25, scaleBarLength + 3, 25, paint);
		this.mapScaleCanvas.drawLine(5, 10, 5, 40, paint);
		this.mapScaleCanvas.drawLine(scaleBarLength + 5, 10, scaleBarLength + 5, 40, paint);
	}

	private void drawScaleText(String scaleText, Paint paint) {
		this.mapScaleCanvas.drawText(scaleText, 12, 18, paint);
	}

	private boolean isRedrawNecessary() {
		if (this.redrawNeeded || this.mapPosition == null) {
			return true;
		}

		MapPosition currentMapPosition = this.mapViewPosition.getMapPosition();
		if (currentMapPosition.zoomLevel != this.mapPosition.zoomLevel) {
			return true;
		}

		double latitudeDiff = Math.abs(currentMapPosition.geoPoint.latitude - this.mapPosition.geoPoint.latitude);
		return latitudeDiff > LATITUDE_REDRAW_THRESHOLD;
	}

	private void redraw() {
		if (!isRedrawNecessary()) {
			return;
		}

		this.mapPosition = this.mapViewPosition.getMapPosition();
		double groundResolution = MercatorProjection.calculateGroundResolution(this.mapPosition.geoPoint.latitude,
				this.mapPosition.zoomLevel);

		groundResolution = groundResolution / this.adapter.getMeterRatio();
		int[] scaleBarValues = this.adapter.getScaleBarValues();

		float scaleBarLength = 0;
		int mapScaleValue = 0;

		for (int i = 0; i < scaleBarValues.length; ++i) {
			mapScaleValue = scaleBarValues[i];
			scaleBarLength = mapScaleValue / (float) groundResolution;
			if (scaleBarLength < (BITMAP_WIDTH - 10)) {
				break;
			}
		}

		draw(scaleBarLength, mapScaleValue);
		this.redrawNeeded = false;
	}
}
