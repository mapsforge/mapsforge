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

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.GraphicContext;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.model.MapViewDimension;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.view.MapView;

/**
 * A MapScaleBar displays the ratio of a distance on the map to the corresponding distance on the ground.
 */
public abstract class MapScaleBar {
	private static final int MARGIN_BOTTOM = 0;
	private static final int MARGIN_LEFT = 5;
	private static final double LATITUDE_REDRAW_THRESHOLD = 0.2;

	private MapViewPosition mapViewPosition;
	private MapViewDimension mapViewDimension;
	private MapPosition prevMapPosition;
	protected DisplayModel displayModel;
	protected GraphicFactory graphicFactory;
	protected Bitmap mapScaleBitmap;
	protected final Canvas mapScaleCanvas;
	protected DistanceUnitAdapter distanceUnitAdapter;
	protected boolean visible;
	protected boolean redrawNeeded;

	/**
	 * Internal class used by calculateScaleBarLengthAndValue
	 */
	protected static class ScaleBarLengthAndValue {
		public int scaleBarLength;
		public int scaleBarValue;

		public ScaleBarLengthAndValue(int scaleBarLength, int scaleBarValue) {
			this.scaleBarLength = scaleBarLength;
			this.scaleBarValue = scaleBarValue;
		}
	}

	public MapScaleBar(MapViewPosition mapViewPosition, MapViewDimension mapViewDimension, DisplayModel displayModel,
			GraphicFactory graphicFactory, int width, int height) {
		this.mapViewPosition = mapViewPosition;
		this.mapViewDimension = mapViewDimension;
		this.displayModel = displayModel;
		this.graphicFactory = graphicFactory;
		this.mapScaleBitmap = graphicFactory.createBitmap((int) (width * this.displayModel.getScaleFactor()),
				(int) (height * this.displayModel.getScaleFactor()));

		this.mapScaleCanvas = graphicFactory.createCanvas();
		this.mapScaleCanvas.setBitmap(this.mapScaleBitmap);
		this.distanceUnitAdapter = MetricUnitAdapter.INSTANCE;
		this.visible = true;
		this.redrawNeeded = true;
	}

	/**
	 * Free all resources
	 */
	public void destroy() {
		this.mapScaleBitmap.decrementRefCount();
		this.mapScaleCanvas.destroy();
	}

	/**
	 * @return true if this {@link MapScaleBar} is visible
	 */
	public boolean isVisible() {
		return this.visible;
	}

	/**
	 * Set the visibility of this {@link MapScaleBar}
	 * 
	 * @param visible
	 *            true if the MapScaleBar should be visible, false otherwise
	 */
	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	/**
	 * @return the {@link DistanceUnitAdapter} in use by this MapScaleBar
	 */
	public DistanceUnitAdapter getDistanceUnitAdapter() {
		return this.distanceUnitAdapter;
	}

	/**
	 * Set the {@link DistanceUnitAdapter} for the MapScaleBar
	 * 
	 * @param distanceUnitAdapter
	 *            The {@link DistanceUnitAdapter} to be used by this {@link MapScaleBar}
	 */
	public void setDistanceUnitAdapter(DistanceUnitAdapter distanceUnitAdapter) {
		if (distanceUnitAdapter == null) {
			throw new IllegalArgumentException("adapter must not be null");
		}
		this.distanceUnitAdapter = distanceUnitAdapter;
		this.redrawNeeded = true;
	}

	/**
	 * Calculates the required length and value of the scalebar
	 * 
	 * @param unitAdapter
	 *            the DistanceUnitAdapter to calculate for
	 * @return a {@link ScaleBarLengthAndValue} object containing the required scaleBarLength and scaleBarValue
	 */
	protected ScaleBarLengthAndValue calculateScaleBarLengthAndValue(DistanceUnitAdapter unitAdapter) {
		this.prevMapPosition = this.mapViewPosition.getMapPosition();
		double groundResolution = MercatorProjection.calculateGroundResolution(this.prevMapPosition.latLong.latitude,
				MercatorProjection.getMapSize(this.prevMapPosition.zoomLevel, this.displayModel.getTileSize()));

		groundResolution = groundResolution / unitAdapter.getMeterRatio();
		int[] scaleBarValues = unitAdapter.getScaleBarValues();

		int scaleBarLength = 0;
		int mapScaleValue = 0;

		for (int i = 0; i < scaleBarValues.length; ++i) {
			mapScaleValue = scaleBarValues[i];
			scaleBarLength = (int) (mapScaleValue / groundResolution);
			if (scaleBarLength < (this.mapScaleBitmap.getWidth() - 10)) {
				break;
			}
		}

		return new ScaleBarLengthAndValue(scaleBarLength, mapScaleValue);
	}

	/**
	 * Calculates the required length and value of the scalebar using the current {@link DistanceUnitAdapter}
	 * 
	 * @return a {@link ScaleBarLengthAndValue} object containing the required scaleBarLength and scaleBarValue
	 */
	protected ScaleBarLengthAndValue calculateScaleBarLengthAndValue() {
		return calculateScaleBarLengthAndValue(this.distanceUnitAdapter);
	}

	/**
	 * Called from {@link MapView}
	 * 
	 * @param graphicContext
	 *            The graphicContext to use to draw the MapScaleBar
	 */
	public void draw(GraphicContext graphicContext) {
		if (!this.visible) {
			return;
		}

		if (this.mapViewDimension.getDimension() == null) {
			return;
		}

		if (this.isRedrawNecessary()) {
			redraw(this.mapScaleCanvas);
			this.redrawNeeded = false;
		}

		int top = this.mapViewDimension.getDimension().height - this.mapScaleBitmap.getHeight() - MARGIN_BOTTOM;
		graphicContext.drawBitmap(this.mapScaleBitmap, MARGIN_LEFT, top);
	}

	/**
	 * The scalebar will be redrawn on the next draw()
	 */
	public void redrawScaleBar() {
		this.redrawNeeded = true;
	}

	/**
	 * Determines if a redraw is necessary or not
	 * 
	 * @return true if redraw is necessary, false otherwise
	 */
	protected boolean isRedrawNecessary() {
		if (this.redrawNeeded || this.prevMapPosition == null) {
			return true;
		}

		MapPosition currentMapPosition = this.mapViewPosition.getMapPosition();
		if (currentMapPosition.zoomLevel != this.prevMapPosition.zoomLevel) {
			return true;
		}

		double latitudeDiff = Math.abs(currentMapPosition.latLong.latitude - this.prevMapPosition.latLong.latitude);
		return latitudeDiff > LATITUDE_REDRAW_THRESHOLD;
	}

	/**
	 * Redraw the mapScaleBar. Make sure you always apply this.displayModel.getScaleFactor() to all coordinates and
	 * dimensions.
	 * 
	 * @param canvas
	 *            The canvas to draw on
	 */
	protected abstract void redraw(Canvas canvas);
}
