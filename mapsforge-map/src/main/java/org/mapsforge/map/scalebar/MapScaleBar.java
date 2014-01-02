/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright © 2014 Ludwig M Brinckmann
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

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.Cap;
import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.FontFamily;
import org.mapsforge.core.graphics.FontStyle;
import org.mapsforge.core.graphics.GraphicContext;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.model.MapViewDimension;
import org.mapsforge.map.model.MapViewPosition;

/**
 * A MapScaleBar displays the ratio of a distance on the map to the corresponding distance on the ground.
 */
public class MapScaleBar {
	private static final int BITMAP_HEIGHT = 50;
	private static final int BITMAP_WIDTH = 150;
	private static final double LATITUDE_REDRAW_THRESHOLD = 0.2;
	private static final int MARGIN_BOTTOM = 5;
	private static final int MARGIN_LEFT = 5;

	private Adapter adapter;
	private final DisplayModel displayModel;
	private GraphicFactory graphicFactory;
	private MapPosition mapPosition;
	private final Bitmap mapScaleBitmap;
	private final Canvas mapScaleCanvas;
	private final MapViewDimension mapViewDimension;
	private final MapViewPosition mapViewPosition;
	private final Paint paintScaleBar;
	private final Paint paintScaleBarStroke;
	private final Paint paintScaleText;
	private final Paint paintScaleTextStroke;
	private boolean redrawNeeded;
	private boolean visible;

	public MapScaleBar(MapViewPosition mapViewPosition, MapViewDimension mapViewDimension, GraphicFactory graphicFactory, DisplayModel displayModel) {
		this.mapViewPosition = mapViewPosition;
		this.mapViewDimension = mapViewDimension;
		this.displayModel = displayModel;
		this.graphicFactory = graphicFactory;

		this.mapScaleBitmap = graphicFactory.createBitmap(BITMAP_WIDTH, BITMAP_HEIGHT);
		this.mapScaleCanvas = graphicFactory.createCanvas();
		this.mapScaleCanvas.setBitmap(this.mapScaleBitmap);
		this.adapter = Metric.INSTANCE;

		this.paintScaleBar = createScaleBarPaint(Color.BLACK, 3, Style.FILL);
		this.paintScaleBarStroke = createScaleBarPaint(Color.WHITE, 5, Style.STROKE);
		this.paintScaleText = createTextPaint(Color.BLACK, 0, Style.FILL);
		this.paintScaleTextStroke = createTextPaint(Color.WHITE, 2, Style.STROKE);
	}

	public void destroy() {
		this.mapScaleBitmap.decrementRefCount();
		this.mapScaleCanvas.destroy();
	}

	public void draw(GraphicContext graphicContext) {
		if (!this.visible) {
			return;
		}

		if (this.mapViewDimension.getDimension() == null) {
			return;
		}

		redraw();

		int top = this.mapViewDimension.getDimension().height - BITMAP_HEIGHT - MARGIN_BOTTOM;
		graphicContext.drawBitmap(this.mapScaleBitmap, MARGIN_LEFT, top);
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
		this.redrawNeeded = true;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	private Paint createScaleBarPaint(Color color, float strokeWidth, Style style) {
		Paint paint = this.graphicFactory.createPaint();
		paint.setColor(color);
		paint.setStrokeWidth(strokeWidth);
		paint.setStyle(style);
		paint.setStrokeCap(Cap.SQUARE);
		return paint;
	}

	private Paint createTextPaint(Color color, float strokeWidth, Style style) {
		Paint paint = this.graphicFactory.createPaint();
		paint.setColor(color);
		paint.setStrokeWidth(strokeWidth);
		paint.setStyle(style);
		paint.setTypeface(FontFamily.DEFAULT, FontStyle.BOLD);
		paint.setTextSize(20);
		return paint;
	}

	/**
	 * Redraws the map scale bitmap with the given parameters.
	 * 
	 * @param scaleBarLength
	 *            the length of the map scale bar in pixels.
	 * @param mapScaleValue
	 *            the map scale value in meters.
	 */
	private void draw(int scaleBarLength, int mapScaleValue) {
		this.mapScaleCanvas.fillColor(Color.TRANSPARENT);

		drawScaleBar(scaleBarLength, this.paintScaleBarStroke);
		drawScaleBar(scaleBarLength, this.paintScaleBar);

		String scaleText = this.adapter.getScaleText(mapScaleValue);
		drawScaleText(scaleText, this.paintScaleTextStroke);
		drawScaleText(scaleText, this.paintScaleText);
	}

	private void drawScaleBar(int scaleBarLength, Paint paint) {
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

		double latitudeDiff = Math.abs(currentMapPosition.latLong.latitude - this.mapPosition.latLong.latitude);
		return latitudeDiff > LATITUDE_REDRAW_THRESHOLD;
	}

	private void redraw() {
		if (!isRedrawNecessary()) {
			return;
		}

		this.mapPosition = this.mapViewPosition.getMapPosition();
		double groundResolution = MercatorProjection.calculateGroundResolution(this.mapPosition.latLong.latitude,
				this.mapPosition.zoomLevel, this.displayModel.getTileSize());

		groundResolution = groundResolution / this.adapter.getMeterRatio();
		int[] scaleBarValues = this.adapter.getScaleBarValues();

		int scaleBarLength = 0;
		int mapScaleValue = 0;

		for (int i = 0; i < scaleBarValues.length; ++i) {
			mapScaleValue = scaleBarValues[i];
			scaleBarLength = (int) (mapScaleValue / groundResolution);
			if (scaleBarLength < (BITMAP_WIDTH - 10)) {
				break;
			}
		}

		draw(scaleBarLength, mapScaleValue);
		this.redrawNeeded = false;
	}
}
