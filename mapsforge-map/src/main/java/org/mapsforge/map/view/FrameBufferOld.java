/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2015-2017 devemux86
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

public class FrameBufferOld extends FrameBuffer {

    static final boolean IS_TRANSPARENT = false;

    /**
     * lmBitmap: layerManager bitmap - the bitmap that gets drawn by the LayerManager.doWork()
     * thread
     */
    Bitmap lmBitmap;
    /**
     * odBitmap: onDraw bitmap - the bitmap that gets displayed by the MapView.onDraw() function
     * from the UI thread
     */
    Bitmap odBitmap;

    Dimension dimension;
    final DisplayModel displayModel;
    final FrameBufferModel frameBufferModel;
    final GraphicFactory graphicFactory;

    /**
     * <pre>
     * {Scale X, Skew X, Transform X
     * Skew Y, Scale Y, Transform Y
     * Perspective 0, Perspective 1, Perspective 2}
     * </pre>
     * <p>
     * See https://stackoverflow.com/questions/13246415/clearly-understand-matrix-calculation/13246914
     */
    final Matrix matrix;

    public FrameBufferOld(FrameBufferModel frameBufferModel, DisplayModel displayModel, GraphicFactory graphicFactory) {
        this.frameBufferModel = frameBufferModel;
        this.displayModel = displayModel;
        this.graphicFactory = graphicFactory;
        this.matrix = graphicFactory.createMatrix();
    }

    @Override
    public synchronized void adjustMatrix(float diffX, float diffY, float scaleFactor, Dimension mapViewDimension,
                                          float pivotDistanceX, float pivotDistanceY) {
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

    void centerFrameBufferToMapView(Dimension mapViewDimension) {
        float dx = (this.dimension.width - mapViewDimension.width) / -2f;
        float dy = (this.dimension.height - mapViewDimension.height) / -2f;
        this.matrix.translate(dx, dy);
    }

    @Override
    public synchronized void destroy() {
        destroyBitmaps();
    }

    private void destroyBitmaps() {
        if (this.odBitmap != null) {
            this.odBitmap.decrementRefCount();
            this.odBitmap = null;
        }
        if (this.lmBitmap != null) {
            this.lmBitmap.decrementRefCount();
            this.lmBitmap = null;
        }
    }

    @Override
    public synchronized void draw(GraphicContext graphicContext) {
        graphicContext.fillColor(this.displayModel.getBackgroundColor());
        if (this.odBitmap != null) {
            graphicContext.drawBitmap(this.odBitmap, this.matrix);
        }
    }

    @Override
    public void frameFinished(MapPosition frameMapPosition) {
        synchronized (this) {
            // swap both bitmap references
            Bitmap bitmapTemp = this.odBitmap;
            this.odBitmap = this.lmBitmap;
            this.lmBitmap = bitmapTemp;
        }
        // taking this out of the synchronized region removes a deadlock potential
        // at the small risk of an inconsistent zoom
        this.frameBufferModel.setMapPosition(frameMapPosition);
    }

    @Override
    public synchronized Dimension getDimension() {
        return this.dimension;
    }

    /**
     * @return the bitmap of the second frame to draw on (may be null).
     */
    @Override
    public synchronized Bitmap getDrawingBitmap() {
        if (this.lmBitmap != null) {
            this.lmBitmap.setBackgroundColor(this.displayModel.getBackgroundColor());
        }
        return this.lmBitmap;
    }

    void scale(float scaleFactor, float pivotDistanceX, float pivotDistanceY) {
        if (scaleFactor != 1) {
            final Point center = this.dimension.getCenter();
            float pivotX = (float) (pivotDistanceX + center.x);
            float pivotY = (float) (pivotDistanceY + center.y);
            this.matrix.scale(scaleFactor, scaleFactor, pivotX, pivotY);
        }
    }

    @Override
    public synchronized void setDimension(Dimension dimension) {
        if (this.dimension != null && this.dimension.equals(dimension)) {
            return;
        }
        this.dimension = dimension;

        destroyBitmaps();

        if (dimension.width > 0 && dimension.height > 0) {
            this.odBitmap = this.graphicFactory.createBitmap(dimension.width, dimension.height, IS_TRANSPARENT);
            this.lmBitmap = this.graphicFactory.createBitmap(dimension.width, dimension.height, IS_TRANSPARENT);
        }
    }
}
