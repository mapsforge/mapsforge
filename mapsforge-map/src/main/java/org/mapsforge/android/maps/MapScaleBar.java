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
package org.mapsforge.android.maps;

import java.util.HashMap;
import java.util.Map;

import org.mapsforge.core.model.MapPosition;
import org.mapsforge.core.util.MercatorProjection;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;

/**
 * A MapScaleBar displays the ratio of a distance on the map to the corresponding distance on the ground.
 */
public class MapScaleBar {
	/**
	 * Enumeration of all text fields.
	 */
	public enum TextField {
		/**
		 * Unit symbol for one foot.
		 */
		FOOT,

		/**
		 * Unit symbol for one kilometer.
		 */
		KILOMETER,

		/**
		 * Unit symbol for one meter.
		 */
		METER,

		/**
		 * Unit symbol for one mile.
		 */
		MILE;
	}

	private static final int BITMAP_HEIGHT = 50;
	private static final int BITMAP_WIDTH = 150;
	private static final double LATITUDE_REDRAW_THRESHOLD = 0.2;
	private static final int MARGIN_BOTTOM = 5;
	private static final int MARGIN_LEFT = 5;
	private static final double METER_FOOT_RATIO = 0.3048;
	private static final int ONE_KILOMETER = 1000;
	private static final int ONE_MILE = 5280;
	private static final Paint SCALE_BAR = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint SCALE_BAR_STROKE = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final int[] SCALE_BAR_VALUES_IMPERIAL = { 26400000, 10560000, 5280000, 2640000, 1056000, 528000,
			264000, 105600, 52800, 26400, 10560, 5280, 2000, 1000, 500, 200, 100, 50, 20, 10, 5, 2, 1 };
	private static final int[] SCALE_BAR_VALUES_METRIC = { 10000000, 5000000, 2000000, 1000000, 500000, 200000, 100000,
			50000, 20000, 10000, 5000, 2000, 1000, 500, 200, 100, 50, 20, 10, 5, 2, 1 };
	private static final Paint SCALE_TEXT = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint SCALE_TEXT_STROKE = new Paint(Paint.ANTI_ALIAS_FLAG);

	private static void configurePaints() {
		SCALE_BAR.setStrokeWidth(2);
		SCALE_BAR.setStrokeCap(Paint.Cap.SQUARE);
		SCALE_BAR.setColor(Color.BLACK);
		SCALE_BAR_STROKE.setStrokeWidth(5);
		SCALE_BAR_STROKE.setStrokeCap(Paint.Cap.SQUARE);
		SCALE_BAR_STROKE.setColor(Color.WHITE);

		SCALE_TEXT.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
		SCALE_TEXT.setTextSize(17);
		SCALE_TEXT.setColor(Color.BLACK);
		SCALE_TEXT_STROKE.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
		SCALE_TEXT_STROKE.setStyle(Paint.Style.STROKE);
		SCALE_TEXT_STROKE.setColor(Color.WHITE);
		SCALE_TEXT_STROKE.setStrokeWidth(2);
		SCALE_TEXT_STROKE.setTextSize(17);
	}

	private boolean imperialUnits;
	private MapPosition mapPosition;
	private final Bitmap mapScaleBitmap;
	private final Canvas mapScaleCanvas;
	private final MapView mapView;
	private boolean redrawNeeded;
	private boolean showMapScaleBar;
	private final Map<TextField, String> textFields;

	MapScaleBar(MapView mapView) {
		this.mapView = mapView;
		this.mapScaleBitmap = Bitmap.createBitmap(BITMAP_WIDTH, BITMAP_HEIGHT, Bitmap.Config.ARGB_4444);
		this.mapScaleCanvas = new Canvas(this.mapScaleBitmap);
		this.textFields = new HashMap<TextField, String>();
		setDefaultTexts();
		configurePaints();
	}

	/**
	 * @return true if imperial units are used, false otherwise.
	 */
	public boolean isImperialUnits() {
		return this.imperialUnits;
	}

	/**
	 * @return true if this map scale bar is visible, false otherwise.
	 */
	public boolean isShowMapScaleBar() {
		return this.showMapScaleBar;
	}

	/**
	 * @param imperialUnits
	 *            true if imperial units should be used rather than metric units.
	 */
	public void setImperialUnits(boolean imperialUnits) {
		this.imperialUnits = imperialUnits;
		this.redrawNeeded = true;
	}

	/**
	 * @param showMapScaleBar
	 *            true if the map scale bar should be drawn, false otherwise.
	 */
	public void setShowMapScaleBar(boolean showMapScaleBar) {
		this.showMapScaleBar = showMapScaleBar;
	}

	/**
	 * Overrides the specified text field with the given string.
	 * 
	 * @param textField
	 *            the text field to override.
	 * @param value
	 *            the new value of the text field.
	 */
	public void setText(TextField textField, String value) {
		this.textFields.put(textField, value);
		this.redrawNeeded = true;
	}

	private void drawScaleBar(float scaleBarLength, Paint paint) {
		this.mapScaleCanvas.drawLine(7, 25, scaleBarLength + 3, 25, paint);
		this.mapScaleCanvas.drawLine(5, 10, 5, 40, paint);
		this.mapScaleCanvas.drawLine(scaleBarLength + 5, 10, scaleBarLength + 5, 40, paint);
	}

	private void drawScaleText(int scaleValue, String unitSymbol, Paint paint) {
		this.mapScaleCanvas.drawText(scaleValue + unitSymbol, 12, 18, paint);
	}

	private boolean isRedrawNecessary() {
		if (this.redrawNeeded || this.mapPosition == null) {
			return true;
		}

		MapPosition currentMapPosition = this.mapView.getMapViewPosition().getMapPosition();

		if (currentMapPosition.zoomLevel != this.mapPosition.zoomLevel) {
			return true;
		}

		double latitudeDiff = Math.abs(currentMapPosition.geoPoint.latitude - this.mapPosition.geoPoint.latitude);
		if (latitudeDiff > LATITUDE_REDRAW_THRESHOLD) {
			return true;
		}

		return false;
	}

	/**
	 * Redraws the map scale bitmap with the given parameters.
	 * 
	 * @param scaleBarLength
	 *            the length of the map scale bar in pixels.
	 * @param mapScaleValue
	 *            the map scale value in meters.
	 */
	private void redrawMapScaleBitmap(float scaleBarLength, int mapScaleValue) {
		this.mapScaleBitmap.eraseColor(Color.TRANSPARENT);

		// draw the scale bar
		drawScaleBar(scaleBarLength, SCALE_BAR_STROKE);
		drawScaleBar(scaleBarLength, SCALE_BAR);

		int scaleValue;
		String unitSymbol;
		if (this.imperialUnits) {
			if (mapScaleValue < ONE_MILE) {
				scaleValue = mapScaleValue;
				unitSymbol = this.textFields.get(TextField.FOOT);
			} else {
				scaleValue = mapScaleValue / ONE_MILE;
				unitSymbol = this.textFields.get(TextField.MILE);
			}
		} else {
			if (mapScaleValue < ONE_KILOMETER) {
				scaleValue = mapScaleValue;
				unitSymbol = this.textFields.get(TextField.METER);
			} else {
				scaleValue = mapScaleValue / ONE_KILOMETER;
				unitSymbol = this.textFields.get(TextField.KILOMETER);
			}
		}

		// draw the scale text
		drawScaleText(scaleValue, unitSymbol, SCALE_TEXT_STROKE);
		drawScaleText(scaleValue, unitSymbol, SCALE_TEXT);
	}

	private void setDefaultTexts() {
		this.textFields.put(TextField.FOOT, " ft");
		this.textFields.put(TextField.MILE, " mi");

		this.textFields.put(TextField.METER, " m");
		this.textFields.put(TextField.KILOMETER, " km");
	}

	void destroy() {
		this.mapScaleBitmap.recycle();
	}

	void draw(Canvas canvas) {
		int top = this.mapView.getHeight() - BITMAP_HEIGHT - MARGIN_BOTTOM;
		canvas.drawBitmap(this.mapScaleBitmap, MARGIN_LEFT, top, null);
	}

	void redrawScaleBar() {
		if (!isRedrawNecessary()) {
			return;
		}

		this.mapPosition = this.mapView.getMapViewPosition().getMapPosition();
		double groundResolution = MercatorProjection.calculateGroundResolution(this.mapPosition.geoPoint.latitude,
				this.mapPosition.zoomLevel);

		int[] scaleBarValues;
		if (this.imperialUnits) {
			groundResolution = groundResolution / METER_FOOT_RATIO;
			scaleBarValues = SCALE_BAR_VALUES_IMPERIAL;
		} else {
			scaleBarValues = SCALE_BAR_VALUES_METRIC;
		}

		float scaleBarLength = 0;
		int mapScaleValue = 0;

		for (int i = 0; i < scaleBarValues.length; ++i) {
			mapScaleValue = scaleBarValues[i];
			scaleBarLength = mapScaleValue / (float) groundResolution;
			if (scaleBarLength < (BITMAP_WIDTH - 10)) {
				break;
			}
		}

		redrawMapScaleBitmap(scaleBarLength, mapScaleValue);
		this.redrawNeeded = false;
	}
}
