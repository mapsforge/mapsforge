/*
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
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.model.Dimension;

import java.util.logging.Logger;

class FrameBufferBitmap {

    private static class BitmapRequest {
        private final GraphicFactory factory;
        private final Dimension dimension;
        private final int color;
        private final boolean isTransparent;

        BitmapRequest(GraphicFactory factory, Dimension dimension, int color, boolean isTransparent) {
            this.factory = factory;
            this.dimension = dimension;
            this.color = color;
            this.isTransparent = isTransparent;
        }

        Bitmap create() {
            if (dimension.width > 0 && dimension.height > 0) {
                Bitmap bitmap = factory.createBitmap(dimension.width, dimension.height, isTransparent);
                bitmap.setBackgroundColor(color);
                return bitmap;
            }
            return null;
        }
    }

    static class Lock {
        private boolean enabled = false;

        synchronized void disable() {
            enabled = false;
            notifyAll();
        }

        synchronized void enable() {
            enabled = true;
        }

        boolean isEnabled() {
            return enabled;
        }

        synchronized void waitDisabled() {
            try {
                while (enabled) {
                    wait();
                }
            } catch (InterruptedException e) {
                LOGGER.fine("Frame buffer interrupted");
            }
        }
    }

    private static final Logger LOGGER = Logger.getLogger(FrameBufferBitmap.class.getName());

    private Bitmap bitmap = null;
    private BitmapRequest bitmapRequest = null;
    private final Object bitmapRequestSync = new Object();
    private final Lock frameLock = new Lock();

    void create(GraphicFactory factory, Dimension dimension, int color, boolean isTransparent) {
        synchronized (bitmapRequestSync) {
            bitmapRequest = new BitmapRequest(factory, dimension, color, isTransparent);
        }
    }

    private void createBitmapIfRequested() {
        synchronized (bitmapRequestSync) {
            if (bitmapRequest != null) {
                destroyBitmap();
                bitmap = bitmapRequest.create();
                bitmapRequest = null;
            }
        }
    }

    void destroy() {
        synchronized (frameLock) {
            if (bitmap != null) {
                frameLock.waitDisabled();
                destroyBitmap();
            }
        }
    }

    private void destroyBitmap() {
        if (bitmap != null) {
            bitmap.decrementRefCount();
            bitmap = null;
        }
    }

    Bitmap lock() {
        synchronized (frameLock) {
            createBitmapIfRequested();
            if (bitmap != null) {
                frameLock.enable();
            }
            return bitmap;
        }
    }

    void release() {
        synchronized (frameLock) {
            frameLock.disable();
        }
    }

    static void swap(FrameBufferBitmap a, FrameBufferBitmap b) {
        Bitmap t = a.bitmap;
        a.bitmap = b.bitmap;
        b.bitmap = t;

        BitmapRequest r = a.bitmapRequest;
        a.bitmapRequest = b.bitmapRequest;
        b.bitmapRequest = r;
    }
}
