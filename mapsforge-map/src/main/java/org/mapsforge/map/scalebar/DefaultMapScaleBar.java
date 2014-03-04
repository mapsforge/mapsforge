/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright © 2014 Ludwig M Brinckmann
 * Copyright © 2014 devemux86
 * Copyright © 2014 Erik Duisters
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

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.Cap;
import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.FontFamily;
import org.mapsforge.core.graphics.FontStyle;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.model.MapViewDimension;
import org.mapsforge.map.model.MapViewPosition;

/**
 * Displays the default mapsforge MapScaleBar
 */
public class DefaultMapScaleBar extends MapScaleBar {
	private static final int BITMAP_HEIGHT = 50;
	private static final int BITMAP_WIDTH = 150;
	private static final int TEXT_MARGIN = 5;
	private static final float STROKE_EXTERNAL = 4;
	private static final float STROKE_INTERNAL = 2;

	private boolean displayMetricAndImperialScale;
	private DistanceUnitAdapter secondaryDistanceUnitAdapter;

	private final Paint paintScaleBar;
	private final Paint paintScaleBarStroke;
	private final Paint paintScaleText;
	private final Paint paintScaleTextStroke;

	public DefaultMapScaleBar(MapViewPosition mapViewPosition, MapViewDimension mapViewDimension,
			GraphicFactory graphicFactory, DisplayModel displayModel) {
		super(mapViewPosition, mapViewDimension, displayModel, graphicFactory, BITMAP_WIDTH, BITMAP_HEIGHT);

		this.displayMetricAndImperialScale = false;
		this.secondaryDistanceUnitAdapter = null;

		this.paintScaleBar = createScaleBarPaint(Color.BLACK, STROKE_INTERNAL, Style.FILL);
		this.paintScaleBarStroke = createScaleBarPaint(Color.WHITE, STROKE_EXTERNAL, Style.STROKE);
		this.paintScaleText = createTextPaint(Color.BLACK, 0, Style.FILL);
		this.paintScaleTextStroke = createTextPaint(Color.WHITE, 2, Style.STROKE);
	}

	/**
	 * Makes sure the secondaryDistanceUnitAdapter is set to the opposite of distanceUnitAdapter.
	 */
	private void setSecondaryDistanceUnitAdapter() {
		if (this.distanceUnitAdapter.getMeterRatio() == 1.0f) {
			this.secondaryDistanceUnitAdapter = ImperialUnitAdapter.INSTANCE;
		} else {
			this.secondaryDistanceUnitAdapter = MetricUnitAdapter.INSTANCE;
		}
		this.redrawNeeded = true;
	}

	/**
	 * request the display of both Metric and Imperial scales
	 * 
	 * @param display
	 *            true if both scales should be displayed, false otherwise
	 */
	public void displayMetricAndImperialScale(boolean display) {
		this.displayMetricAndImperialScale = display;

		if (this.displayMetricAndImperialScale) {
			if (this.secondaryDistanceUnitAdapter != null) {
				if (this.secondaryDistanceUnitAdapter.getMeterRatio() == this.distanceUnitAdapter.getMeterRatio()) {
					setSecondaryDistanceUnitAdapter();
				}
			} else {
				setSecondaryDistanceUnitAdapter();
			}
		} else {
			if (this.secondaryDistanceUnitAdapter != null) {
				this.secondaryDistanceUnitAdapter = null;
				this.redrawNeeded = true;
			}
		}
	}

	@Override
	public void setDistanceUnitAdapter(DistanceUnitAdapter distanceUnitAdapter) {
		super.setDistanceUnitAdapter(distanceUnitAdapter);

		this.displayMetricAndImperialScale(this.displayMetricAndImperialScale);
	}

	private Paint createScaleBarPaint(Color color, float strokeWidth, Style style) {
		Paint paint = this.graphicFactory.createPaint();
		paint.setColor(color);
		paint.setStrokeWidth(strokeWidth * this.displayModel.getScaleFactor());
		paint.setStyle(style);
		paint.setStrokeCap(Cap.SQUARE);
		return paint;
	}

	private Paint createTextPaint(Color color, float strokeWidth, Style style) {
		Paint paint = this.graphicFactory.createPaint();
		paint.setColor(color);
		paint.setStrokeWidth(strokeWidth * this.displayModel.getScaleFactor());
		paint.setStyle(style);
		paint.setTypeface(FontFamily.DEFAULT, FontStyle.BOLD);
		paint.setTextSize(16 * this.displayModel.getScaleFactor());

		return paint;
	}

	@Override
	protected void redraw(Canvas canvas) {
		canvas.fillColor(Color.TRANSPARENT);

		float scale = this.displayModel.getScaleFactor();
		ScaleBarLengthAndValue lengthAndValue = this.calculateScaleBarLengthAndValue();
		ScaleBarLengthAndValue lengthAndValue2;

		if (this.displayMetricAndImperialScale) {
			lengthAndValue2 = this.calculateScaleBarLengthAndValue(this.secondaryDistanceUnitAdapter);
		} else {
			lengthAndValue2 = new ScaleBarLengthAndValue(0, 0);
		}

		drawScaleBar(canvas, lengthAndValue.scaleBarLength, lengthAndValue2.scaleBarLength, this.paintScaleBarStroke,
				scale);
		drawScaleBar(canvas, lengthAndValue.scaleBarLength, lengthAndValue2.scaleBarLength, this.paintScaleBar, scale);

		String scaleText1 = this.distanceUnitAdapter.getScaleText(lengthAndValue.scaleBarValue);
		String scaleText2 = (this.displayMetricAndImperialScale) ? this.secondaryDistanceUnitAdapter
				.getScaleText(lengthAndValue2.scaleBarValue) : "";

		drawScaleText(canvas, scaleText1, scaleText2, this.paintScaleTextStroke, scale);
		drawScaleText(canvas, scaleText1, scaleText2, this.paintScaleText, scale);
	}

	private void drawScaleBar(Canvas canvas, int scaleBarLength1, int scaleBarLength2, Paint paint, float scale) {
		final int maxScaleBarLength = (scaleBarLength1 <= scaleBarLength2) ? scaleBarLength2 : scaleBarLength1;
		final float startX = (STROKE_EXTERNAL * scale - STROKE_INTERNAL * scale) * 0.5f + STROKE_INTERNAL * scale;
		int stopX;

		canvas.drawLine(Math.round(startX), Math.round(canvas.getHeight() * 0.5f),
				Math.round(startX + maxScaleBarLength), Math.round(canvas.getHeight() * 0.5f), paint);

		final float startY = 10 * scale;
		canvas.drawLine(Math.round(STROKE_EXTERNAL * scale * 0.5f), Math.round(startY),
				Math.round(STROKE_EXTERNAL * scale * 0.5f), Math.round(canvas.getHeight() - startY), paint);

		if (scaleBarLength2 == 0) {
			stopX = Math.round(startX + maxScaleBarLength + STROKE_INTERNAL * scale * 0.5f);

			canvas.drawLine(stopX, Math.round(startY), stopX, Math.round(canvas.getHeight() - startY), paint);
		} else {
			stopX = Math.round(startX + scaleBarLength1 + STROKE_INTERNAL * scale * 0.5f);
			canvas.drawLine(stopX, Math.round(startY), stopX, Math.round(canvas.getHeight() * 0.5f), paint);
			stopX = Math.round(startX + scaleBarLength2 + STROKE_INTERNAL * scale * 0.5f);
			canvas.drawLine(stopX, Math.round(canvas.getHeight() * 0.5f), stopX,
					Math.round(canvas.getHeight() - startY), paint);
		}
	}

	private void drawScaleText(Canvas canvas, String scaleText1, String scaleText2, Paint paint, float scale) {
		canvas.drawText(scaleText1, Math.round(STROKE_EXTERNAL * scale + TEXT_MARGIN),
				Math.round(canvas.getHeight() * 0.5f - STROKE_EXTERNAL * scale * 0.5f - TEXT_MARGIN), paint);
		canvas.drawText(scaleText2, Math.round(STROKE_EXTERNAL * scale + TEXT_MARGIN),
				Math.round(canvas.getHeight() * 0.5f + STROKE_EXTERNAL * scale * 0.5f  + TEXT_MARGIN + this.paintScaleTextStroke.getTextHeight(scaleText2)), paint);
	}
}
