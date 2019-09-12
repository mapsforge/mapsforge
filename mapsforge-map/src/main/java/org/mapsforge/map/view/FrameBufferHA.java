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
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.model.FrameBufferModel;

import java.util.logging.Logger;

public class FrameBufferHA extends FrameBuffer {

    private static final Logger LOGGER = Logger.getLogger(FrameBufferHA.class.getName());

    private boolean allowBitmapSwap = true;
    private final Object lmBitmapLock = new Object();
    private MapPosition lmMapPosition;

    public FrameBufferHA(FrameBufferModel frameBufferModel, DisplayModel displayModel,
                         GraphicFactory graphicFactory) {
        super(frameBufferModel, displayModel, graphicFactory);
    }

    /**
     * This is called from (Android) <code>MapView.onDraw()</code>.
     */
    @Override
    public void draw(GraphicContext graphicContext) {
        graphicContext.fillColor(this.displayModel.getBackgroundColor());

        // Swap bitmaps before the Canvas.drawBitmap to prevent flickering as much as possible
        swapBitmaps();

        if (this.odBitmap != null) {
            graphicContext.drawBitmap(this.odBitmap, this.matrix);
        }
    }

    /**
     * This is called from <code>LayerManager</code> when drawing is finished.
     */
    @Override
    public void frameFinished(MapPosition frameMapPosition) {
        freeLmBitmap(frameMapPosition);
    }

    private void freeLmBitmap(MapPosition frameMapPosition) {
        synchronized (this.lmBitmapLock) {
            this.lmMapPosition = frameMapPosition;
            this.allowBitmapSwap = true;
        }
    }

    /**
     * This is called from <code>LayerManager</code> when drawing starts.
     */
    @Override
    public Bitmap getDrawingBitmap() {
        lockLmBitmap();

        return super.getDrawingBitmap();
    }

    private void lockLmBitmap() {
        synchronized (this.lmBitmapLock) {
            if (this.lmBitmap != null) {
                if (this.allowBitmapSwap) { // not yet swapped by onDraw()
                    try {
                        this.lmBitmapLock.wait(); // wait until swapped
                    } catch (InterruptedException e) {
                        LOGGER.fine("FrameBufferHA interrupted");
                    }
                }
            }
            this.allowBitmapSwap = false;
        }
    }

    private void swapBitmaps() {
        synchronized (this.lmBitmapLock) {
            // Swap bitmaps only if the LayerManager is currently not working and has drawn a new
            // bitmap since the last swap
            if (allowBitmapSwap) {
                Bitmap bitmapTemp = this.odBitmap;
                this.odBitmap = this.lmBitmap;
                this.lmBitmap = bitmapTemp;

                this.frameBufferModel.setMapPosition(this.lmMapPosition);

                this.allowBitmapSwap = false;
                this.lmBitmapLock.notify();
            }
        }
    }
}
