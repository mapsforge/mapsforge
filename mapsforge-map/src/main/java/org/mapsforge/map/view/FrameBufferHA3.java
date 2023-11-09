/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2015-2017 devemux86
 * Copyright 2017, 2020 Lukas Bai
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
package org.mapsforge.map.view;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.GraphicContext;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.Matrix;
import org.mapsforge.core.model.Dimension;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.model.FrameBufferModel;

public class FrameBufferHA3 extends FrameBuffer {

    private static final boolean IS_TRANSPARENT = false;

    /*
     *  lm: layer manager
     *      Layer manager draws the bitmap off-screen
     *  od: onDraw() -> draw()
     *      swaps the two bitmaps and puts one bitmap to the screen
     *      while the layer manager draws the next off-screen bitmap.
     */
    private final FrameBufferBitmapHA3 lmBitmap = new FrameBufferBitmapHA3();
    private final FrameBufferBitmapHA3 odBitmap = new FrameBufferBitmapHA3();

    private Dimension dimension;
    private final DisplayModel displayModel;
    private final FrameBufferModel frameBufferModel;
    private final GraphicFactory graphicFactory;

    /**
     * <pre>
     * {Scale X, Skew X, Transform X
     * Skew Y, Scale Y, Transform Y
     * Perspective 0, Perspective 1, Perspective 2}
     * </pre>
     * <p>
     * See https://stackoverflow.com/questions/13246415/clearly-understand-matrix-calculation/13246914
     */
    private final Matrix matrix;

    private final FrameBufferBitmapHA3.Lock framesLock = new FrameBufferBitmapHA3.Lock();
    private MapPosition lmMapPosition;

    public FrameBufferHA3(FrameBufferModel frameBufferModel, DisplayModel displayModel,
                          GraphicFactory graphicFactory) {
        this.frameBufferModel = frameBufferModel;
        this.displayModel = displayModel;
        this.graphicFactory = graphicFactory;

        this.matrix = graphicFactory.createMatrix();
        this.framesLock.unlock();
    }

    @Override
    public void adjustMatrix(float diffX, float diffY, float scaleFactor, Dimension mapViewDimension,
                             float pivotDistanceX, float pivotDistanceY) {
        synchronized (this.matrix) {
            if (this.dimension == null) {
                return;
            }
            this.matrix.reset();
            centerFrameBufferToMapView(mapViewDimension);
            if (pivotDistanceX == 0 && pivotDistanceY == 0) {
                // only translate the matrix if we are not zooming around a pivot,
                // the translation happens only once the zoom is finished.
                this.matrix.translate(diffX, diffY);
            }

            scale(scaleFactor, pivotDistanceX, pivotDistanceY);
        }
    }

    private void centerFrameBufferToMapView(Dimension mapViewDimension) {
        float dx = (this.dimension.width - mapViewDimension.width) / -2f;
        float dy = (this.dimension.height - mapViewDimension.height) / -2f;
        this.matrix.translate(dx, dy);
    }

    /**
     * This must be called from MapView after all drawing is done.
     */
    @Override
    public synchronized void destroy() {
        synchronized (this.framesLock) {
            this.odBitmap.destroy();
            this.lmBitmap.destroy();

            // This is to unlock LayerManager->getDrawingBitmap()
            // and to prevent allocation new frame bitmaps.
            framesLock.hardLock();
        }
    }

    /**
     * This is called from (Android) <code>MapView.onDraw</code>
     * and (Desktop) <code>MapView.paint</code>.
     */
    @Override
    public void draw(GraphicContext graphicContext) {
        /*
         * Swap bitmaps here (and only here).
         * Swapping is done when layer manager has finished. Else draw old bitmap again.
         * This draw() is always called when layer manager has finished. This ensures that the
         * last generated frame is always put on screen.
         */

        // FIXME: resetting the background color is redundant if the background color of the map view is already set
        graphicContext.fillColor(this.displayModel.getBackgroundColor());

        swapBitmaps();

        synchronized (this.matrix) {
            Bitmap b = this.odBitmap.lock();
            if (b != null) {
                graphicContext.drawBitmap(b, this.matrix);
            }
        }

        /*
         * Release here so destroy() can free resources
         */
        this.odBitmap.release();
    }

    /**
     * This is called from <code>LayerManager</code> when drawing is finished.
     */
    @Override
    public void frameFinished(MapPosition framePosition) {
        synchronized (this.framesLock) {
            this.lmMapPosition = framePosition;
            this.lmBitmap.release();
            this.framesLock.lock();
        }
    }

    @Override
    public synchronized Dimension getDimension() {
        return this.dimension;
    }

    /**
     * This is called from <code>LayerManager</code> when drawing starts.
     *
     * @return the bitmap of the second frame to draw on (may be null).
     */
    @Override
    public Bitmap getDrawingBitmap() {
        /*
         * Layer manager only starts drawing a new bitmap when the last one is swapped (taken to
         * the screen). This ensures that the layer manager draws not too many frames. (only as
         * much as can get displayed).
         */
        synchronized (this.framesLock) {
            this.framesLock.waitUntilUnlocked();
            Bitmap b = this.lmBitmap.lock();
            if (b != null) {
                b.setBackgroundColor(this.displayModel.getBackgroundColor());
            }
            return b;
        }
    }

    private void scale(float scaleFactor, float pivotDistanceX, float pivotDistanceY) {
        if (scaleFactor != 1) {
            final Point center = this.dimension.getCenter();
            float pivotX = (float) (pivotDistanceX + center.x);
            float pivotY = (float) (pivotDistanceY + center.y);
            this.matrix.scale(scaleFactor, scaleFactor, pivotX, pivotY);
        }
    }

    @Override
    public void setDimension(Dimension dimension) {
        synchronized (this.matrix) {
            if (this.dimension != null && this.dimension.equals(dimension)) {
                return;
            }
            this.dimension = dimension;
        }

        synchronized (this.framesLock) {
            this.odBitmap.create(this.graphicFactory, dimension, this.displayModel.getBackgroundColor(), IS_TRANSPARENT);
            this.lmBitmap.create(this.graphicFactory, dimension, this.displayModel.getBackgroundColor(), IS_TRANSPARENT);
        }
    }

    private void swapBitmaps() {
        /*
         *  Swap bitmaps only if the layerManager is currently not working and
         *  has drawn a new bitmap since the last swap
         */
        synchronized (this.framesLock) {
            if (this.framesLock.isSoftLocked()) {
                FrameBufferBitmapHA3.swap(this.odBitmap, this.lmBitmap);
                this.frameBufferModel.setMapPosition(this.lmMapPosition);
                this.framesLock.unlock();
            }
        }
    }
}
