/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2015-2017 devemux86
 * Copyright 2019 Alexander Schmidt
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
package org.mapsforge.map.controller;

import org.mapsforge.core.model.*;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.core.util.Parameters;
import org.mapsforge.map.model.Model;
import org.mapsforge.map.model.common.Observer;
import org.mapsforge.map.view.FrameBuffer;

public final class FrameBufferController implements Observer {

    public static FrameBufferController create(FrameBuffer frameBuffer, Model model) {
        FrameBufferController frameBufferController = new FrameBufferController(frameBuffer, model);

        model.frameBufferModel.addObserver(frameBufferController);
        model.mapViewDimension.addObserver(frameBufferController);
        model.mapViewPosition.addObserver(frameBufferController);
        model.displayModel.addObserver(frameBufferController);

        return frameBufferController;
    }

    public static Dimension calculateFrameBufferDimension(Dimension mapViewDimension, double overdrawFactor) {
        int width = (int) (mapViewDimension.width * overdrawFactor);
        int height = (int) (mapViewDimension.height * overdrawFactor);
        if (Parameters.SQUARE_FRAME_BUFFER) {
            width = (int) Math.hypot(width, height);
            height = width;
        }
        return new Dimension(width, height);
    }

    private final FrameBuffer frameBuffer;
    private Dimension lastMapViewDimension;
    private double lastOverdrawFactor;
    private final Model model;

    private FrameBufferController(FrameBuffer frameBuffer, Model model) {
        this.frameBuffer = frameBuffer;
        this.model = model;
    }

    public void destroy() {
        this.model.displayModel.removeObserver(this);
        this.model.mapViewPosition.removeObserver(this);
        this.model.mapViewDimension.removeObserver(this);
        this.model.frameBufferModel.removeObserver(this);
    }

    @Override
    public void onChange() {
        Dimension mapViewDimension = this.model.mapViewDimension.getDimension();
        if (mapViewDimension == null) {
            // at this point map view not visible
            return;
        }

        double overdrawFactor = this.model.frameBufferModel.getOverdrawFactor();
        if (dimensionChangeNeeded(mapViewDimension, overdrawFactor)) {
            Dimension newDimension = calculateFrameBufferDimension(mapViewDimension, overdrawFactor);
            if (!Parameters.SQUARE_FRAME_BUFFER || frameBuffer.getDimension() == null
                    || newDimension.width > frameBuffer.getDimension().width
                    || newDimension.height > frameBuffer.getDimension().height) {
                // new dimensions if we either always reallocate on config change or if new dimension
                // is larger than the old
                this.frameBuffer.setDimension(newDimension);
            }
            this.lastMapViewDimension = mapViewDimension;
            this.lastOverdrawFactor = overdrawFactor;
        }

        synchronized (this.model.mapViewPosition) {
            synchronized (this.frameBuffer) {
                // we need resource ordering here to avoid deadlock
                MapPosition mapPositionFrameBuffer = this.model.frameBufferModel.getMapPosition();
                if (mapPositionFrameBuffer != null) {
                    double scaleFactor = this.model.mapViewPosition.getScaleFactor();
                    LatLong pivot = this.model.mapViewPosition.getPivot();
                    float mapViewCenterX = this.model.mapViewPosition.getMapViewCenterX();
                    float mapViewCenterY = this.model.mapViewPosition.getMapViewCenterY();
                    adjustFrameBufferMatrix(mapPositionFrameBuffer, mapViewDimension, scaleFactor, pivot,
                            mapViewCenterX, mapViewCenterY);
                }
            }
        }
    }

    private void adjustFrameBufferMatrix(MapPosition mapPositionFrameBuffer, Dimension mapViewDimension,
                                         double scaleFactor, LatLong pivot,
                                         float mapViewCenterX, float mapViewCenterY) {

        MapPosition mapViewPosition = this.model.mapViewPosition.getMapPosition();

        long mapSize = MercatorProjection.getMapSize(mapPositionFrameBuffer.zoomLevel, model.displayModel.getTileSize());

        Point pointFrameBuffer = MercatorProjection.getPixel(mapPositionFrameBuffer.latLong, mapSize);
        Point pointMapPosition = MercatorProjection.getPixel(mapViewPosition.latLong, mapSize);

        double diffX = pointFrameBuffer.x - pointMapPosition.x;
        double diffY = pointFrameBuffer.y - pointMapPosition.y;
        if (!Parameters.ROTATION_MATRIX) {
            if ((diffX != 0 || diffY != 0) && !Rotation.noRotation(mapPositionFrameBuffer.rotation)) {
                Rotation mapRotation = new Rotation(mapPositionFrameBuffer.rotation.degrees, 0, 0);
                Point rotated = mapRotation.rotate(diffX, diffY, true);
                diffX = rotated.x;
                diffY = rotated.y;
            }
        }

        // we need to compute the pivot distance from the map center
        // as we will need to find the pivot point for the
        // frame buffer (which generally has not the same size as the
        // map view).
        double pivotDistanceX = 0d;
        double pivotDistanceY = 0d;
        if (pivot != null) {
            Point pivotXY = MercatorProjection.getPixel(pivot, mapSize);
            pivotDistanceX = pivotXY.x - pointFrameBuffer.x;
            pivotDistanceY = pivotXY.y - pointFrameBuffer.y;
        }

        float currentScaleFactor = this.model.frameBufferModel.isScaleEnabled() ? (float) (scaleFactor / Math.pow(2, mapPositionFrameBuffer.zoomLevel)) : 1;

        this.frameBuffer.adjustMatrix((float) diffX, (float) diffY, currentScaleFactor, mapViewDimension,
                (float) pivotDistanceX, (float) pivotDistanceY,
                mapPositionFrameBuffer.rotation, mapViewCenterX, mapViewCenterY);
    }

    private boolean dimensionChangeNeeded(Dimension mapViewDimension, double overdrawFactor) {
        if (Double.compare(overdrawFactor, this.lastOverdrawFactor) != 0) {
            return true;
        } else if (!mapViewDimension.equals(this.lastMapViewDimension)) {
            return true;
        }
        return false;
    }
}
