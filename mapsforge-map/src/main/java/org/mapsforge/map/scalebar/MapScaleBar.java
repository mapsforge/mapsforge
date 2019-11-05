/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2014-2019 devemux86
 * Copyright 2014 Erik Duisters
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
import org.mapsforge.map.model.IMapViewPosition;
import org.mapsforge.map.model.MapViewDimension;
import org.mapsforge.map.view.MapView;

/**
 * A MapScaleBar displays the ratio of a distance on the map to the corresponding distance on the ground.
 */
public abstract class MapScaleBar {
    public enum ScaleBarPosition {BOTTOM_CENTER, BOTTOM_LEFT, BOTTOM_RIGHT, TOP_CENTER, TOP_LEFT, TOP_RIGHT}

    /**
     * Default position of the scale bar.
     */
    private static final ScaleBarPosition DEFAULT_SCALE_BAR_POSITION = ScaleBarPosition.BOTTOM_LEFT;

    private static final double LATITUDE_REDRAW_THRESHOLD = 0.2;

    protected final DisplayModel displayModel;
    protected DistanceUnitAdapter distanceUnitAdapter;
    protected final GraphicFactory graphicFactory;
    protected final Bitmap mapScaleBitmap;
    protected final Canvas mapScaleCanvas;
    private final MapViewDimension mapViewDimension;
    private final IMapViewPosition mapViewPosition;
    private int marginHorizontal;
    private int marginVertical;
    protected MapPosition prevMapPosition;
    protected boolean redrawNeeded;
    protected final float scale;
    protected ScaleBarPosition scaleBarPosition;
    private boolean visible;

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

    public MapScaleBar(IMapViewPosition mapViewPosition, MapViewDimension mapViewDimension, DisplayModel displayModel,
                       GraphicFactory graphicFactory, int width, int height, float scale) {
        this.mapViewPosition = mapViewPosition;
        this.mapViewDimension = mapViewDimension;
        this.displayModel = displayModel;
        this.graphicFactory = graphicFactory;
        this.mapScaleBitmap = graphicFactory.createBitmap(width, height);
        this.scale = scale;

        this.scaleBarPosition = DEFAULT_SCALE_BAR_POSITION;

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
     * @param visible true if the MapScaleBar should be visible, false otherwise
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
     * @param distanceUnitAdapter The {@link DistanceUnitAdapter} to be used by this {@link MapScaleBar}
     */
    public void setDistanceUnitAdapter(DistanceUnitAdapter distanceUnitAdapter) {
        if (distanceUnitAdapter == null) {
            throw new IllegalArgumentException("adapter must not be null");
        }
        this.distanceUnitAdapter = distanceUnitAdapter;
        this.redrawNeeded = true;
    }

    public int getMarginHorizontal() {
        return marginHorizontal;
    }

    public void setMarginHorizontal(int marginHorizontal) {
        if (this.marginHorizontal != marginHorizontal) {
            this.marginHorizontal = marginHorizontal;
            this.redrawNeeded = true;
        }
    }

    public int getMarginVertical() {
        return marginVertical;
    }

    public void setMarginVertical(int marginVertical) {
        if (this.marginVertical != marginVertical) {
            this.marginVertical = marginVertical;
            this.redrawNeeded = true;
        }
    }

    public ScaleBarPosition getScaleBarPosition() {
        return scaleBarPosition;
    }

    public void setScaleBarPosition(ScaleBarPosition scaleBarPosition) {
        if (this.scaleBarPosition != scaleBarPosition) {
            this.scaleBarPosition = scaleBarPosition;
            this.redrawNeeded = true;
        }
    }

    private int calculatePositionLeft(int left, int right, int width) {
        switch (scaleBarPosition) {
            case BOTTOM_LEFT:
            case TOP_LEFT:
                return marginHorizontal;

            case BOTTOM_CENTER:
            case TOP_CENTER:
                return (right - left - width) / 2;

            case BOTTOM_RIGHT:
            case TOP_RIGHT:
                return right - left - width - marginHorizontal;
        }

        throw new IllegalArgumentException("unknown horizontal position: " + scaleBarPosition);
    }

    private int calculatePositionTop(int top, int bottom, int height) {
        switch (scaleBarPosition) {
            case TOP_CENTER:
            case TOP_LEFT:
            case TOP_RIGHT:
                return marginVertical;

            case BOTTOM_CENTER:
            case BOTTOM_LEFT:
            case BOTTOM_RIGHT:
                return bottom - top - height - marginVertical;
        }

        throw new IllegalArgumentException("unknown vertical position: " + scaleBarPosition);
    }

    /**
     * Calculates the required length and value of the scalebar
     *
     * @param unitAdapter the DistanceUnitAdapter to calculate for
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

        for (int scaleBarValue : scaleBarValues) {
            mapScaleValue = scaleBarValue;
            scaleBarLength = (int) (mapScaleValue / groundResolution);
            if (scaleBarLength < (this.mapScaleBitmap.getWidth() - 10 * this.scale)) {
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
     * @param graphicContext The graphicContext to use to draw the MapScaleBar
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

        int positionLeft = calculatePositionLeft(0, this.mapViewDimension.getDimension().width, this.mapScaleBitmap.getWidth());
        int positionTop = calculatePositionTop(0, this.mapViewDimension.getDimension().height, this.mapScaleBitmap.getHeight());

        graphicContext.drawBitmap(this.mapScaleBitmap, positionLeft, positionTop);
    }

    /**
     * The scalebar is redrawn now.
     */
    public void drawScaleBar() {
        draw(mapScaleCanvas);
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
     * Redraw the map scale bar.
     * Make sure you always apply scale factor to all coordinates and dimensions.
     *
     * @param canvas The canvas to draw on
     */
    protected abstract void redraw(Canvas canvas);
}
