/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2015-2017 devemux86
 * Copyright 2017 bailuk
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
import org.mapsforge.core.model.Dimension;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.model.FrameBufferModel;

public class FrameBufferHA2 extends FrameBuffer {

    /*
     *  lm: layer manager
     *      Layer manager draws the bitmap off-screen
     *  od: onDraw() -> draw()
     *      swaps the two bitmaps and puts one bitmap to the screen
     *      while the layer manager draws the next off-screen bitmap.
     */
    private final FrameBufferBitmap lmBitmap = new FrameBufferBitmap();
    private final FrameBufferBitmap odBitmap = new FrameBufferBitmap();

    private final Object dimLock = new Object();
    private MapPosition lmMapPosition;

    public FrameBufferHA2(FrameBufferModel frameBufferModel, DisplayModel displayModel,
                          GraphicFactory graphicFactory) {
        super(frameBufferModel, displayModel, graphicFactory);
    }

    public void adjustMatrix(float diffX, float diffY, float scaleFactor, Dimension mapViewDimension,
                             float pivotDistanceX, float pivotDistanceY) {
        synchronized (dimLock) {
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

    public synchronized void destroy() {
        odBitmap.destroy();
        lmBitmap.destroy();
    }

    /**
     * This is called from (Android) <code>MapView.onDraw()</code>.
     */
    public void draw(GraphicContext graphicContext) {
        graphicContext.fillColor(this.displayModel.getBackgroundColor());

        /*
         * Swap bitmaps here (and only here).
         * Swapping is done when layer manager has finished. Else draw old bitmap again.
         * This onDraw() is always called when layer manager has finished. This ensures that the
         * last generated frame is always put on screen.
         */
        swapBitmaps();

        Bitmap b = odBitmap.lock();
        if (b != null) {
            synchronized (dimLock) {
                graphicContext.drawBitmap(b, this.matrix);
            }
        }

        /*
         * Release here so destroy() can free resources
         */
        odBitmap.releaseAndAllowSwap();
    }

    /**
     * This is called from <code>LayerManager</code> when drawing is finished.
     */
    public void frameFinished(MapPosition framePosition) {
        lmBitmap.releaseAndAllowSwap();
        lmMapPosition = framePosition;
    }

    /**
     * This is called from <code>LayerManager</code> when drawing starts.
     *
     * @return the bitmap of the second frame to draw on (may be null).
     */
    public Bitmap getDrawingBitmap() {
        /*
         * Layer manager only starts drawing a new bitmap when the last one is swapped (taken to
         * the screen). This ensures that the layer manager draws not too many frames. (only as
         * much as can get displayed).
         */
        Bitmap b = lmBitmap.lockWhenSwapped();

        if (b != null) {
            b.setBackgroundColor(this.displayModel.getBackgroundColor());
        }
        return b;
    }

    public void setDimension(Dimension dimension) {
        synchronized (dimLock) {
            if (this.dimension != null && this.dimension.equals(dimension)) {
                return;
            }
            this.dimension = dimension;

            odBitmap.create(graphicFactory, dimension, IS_TRANSPARENT);
            lmBitmap.create(graphicFactory, dimension, IS_TRANSPARENT);
        }
    }

    private void swapBitmaps() {
        /*
         *  Swap bitmaps only if the layerManager is currently not working and
         *  has drawn a new bitmap since the last swap
         */
        if (FrameBufferBitmap.swap(odBitmap, lmBitmap)) {
            frameBufferModel.setMapPosition(lmMapPosition);
        }
    }
}
