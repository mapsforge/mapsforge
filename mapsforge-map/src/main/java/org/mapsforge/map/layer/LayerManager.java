/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2014 devemux86
 * Copyright 2016 ksaihtam
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
package org.mapsforge.map.layer;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.model.*;
import org.mapsforge.core.util.Parameters;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.util.MapPositionUtil;
import org.mapsforge.map.util.PausableThread;
import org.mapsforge.map.view.FrameBuffer;
import org.mapsforge.map.view.MapView;

public class LayerManager extends PausableThread implements Redrawer {
    private static final int MILLISECONDS_PER_FRAME = 30;

    private final Canvas drawingCanvas;
    private final Layers layers;
    private final MapView mapView;
    private final MapViewPosition mapViewPosition;
    private boolean redrawNeeded;


    public LayerManager(MapView mapView, MapViewPosition mapViewPosition, GraphicFactory graphicFactory) {
        super();

        this.mapView = mapView;
        this.mapViewPosition = mapViewPosition;

        this.drawingCanvas = graphicFactory.createCanvas();
        this.layers = new Layers(this, mapView.getModel().displayModel);
        this.waitForWork = true;
    }

    public Layers getLayers() {
        return this.layers;
    }

    @Override
    public void redrawLayers() {
        this.redrawNeeded = true;
        synchronized (this) {
            notify();
        }
    }

    @Override
    protected void afterRun() {
        synchronized (this.layers) {
            for (Layer layer : this.layers) {
                layer.onDestroy();
            }
        }
        this.drawingCanvas.destroy();
    }

    @Override
    protected void doWork() throws InterruptedException {
        long startTime = System.nanoTime();
        this.redrawNeeded = false;

        FrameBuffer frameBuffer = this.mapView.getFrameBuffer();
        Bitmap bitmap = frameBuffer.getDrawingBitmap();
        if (bitmap != null) {
            this.drawingCanvas.setBitmap(bitmap);

            MapPosition mapPosition = this.mapViewPosition.getMapPosition();
            Rotation rotation = this.mapView.getMapRotation();
            int tileSize = this.mapView.getModel().displayModel.getTileSize();
            Dimension canvasDimension = this.drawingCanvas.getDimension();
            float mapViewCenterX = this.mapView.getMapViewCenterX();
            float mapViewCenterY = this.mapView.getMapViewCenterY();
            BoundingBox boundingBox = MapPositionUtil.getBoundingBox(mapPosition, rotation, tileSize, canvasDimension, mapViewCenterX, mapViewCenterY);
            Point topLeftPoint = MapPositionUtil.getTopLeftPoint(mapPosition, canvasDimension, tileSize);

            float dx = this.mapView.getOffsetX();
            float dy = this.mapView.getOffsetY();
            float px = rotation.px + (canvasDimension.width - this.mapView.getWidth()) * 0.5f;
            float py = rotation.py + (canvasDimension.height - this.mapView.getHeight()) * 0.5f;
            if (!Parameters.ROTATION_MATRIX) {
                if (dx != 0 || dy != 0) {
                    this.drawingCanvas.translate(dx, dy);
                }
                if (!Rotation.noRotation(rotation)) {
                    this.drawingCanvas.rotate(rotation.degrees, px, py);
                }
            }
            synchronized (this.layers) {
                for (Layer layer : this.layers) {
                    if (layer.isVisible()) {
                        layer.draw(boundingBox, mapPosition.zoomLevel, this.drawingCanvas, topLeftPoint, rotation);
                    }
                }
            }
            if (!Parameters.ROTATION_MATRIX) {
                if (!Rotation.noRotation(rotation)) {
                    this.drawingCanvas.rotate(-rotation.degrees, px, py);
                }
                if (dx != 0 || dy != 0) {
                    this.drawingCanvas.translate(-dx, -dy);
                }
            }

            if (!mapViewPosition.animationInProgress()) {
                // this causes a lot of flickering when an animation
                // is in progress
                frameBuffer.frameFinished(mapPosition);
                this.mapView.repaint();
            } else {
                // make sure that we redraw at the end
                this.redrawNeeded = true;
            }
        }

        long elapsedMilliseconds = (System.nanoTime() - startTime) / 1000000;
        long timeSleep = MILLISECONDS_PER_FRAME - elapsedMilliseconds;

        if (timeSleep > 1 && !isInterrupted()) {
            sleep(timeSleep);
        }
    }

    @Override
    protected ThreadPriority getThreadPriority() {
        return ThreadPriority.NORMAL;
    }

    @Override
    protected boolean hasWork() {
        return this.redrawNeeded;
    }
}
