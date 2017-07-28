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

public class FrameBufferBitmap {
    private static final Logger LOGGER = Logger.getLogger(FrameBufferBitmap.class.getName());

    private final Lock frameLock = new Lock();

    private Bitmap bitmap = null;

    private BitmapRequest bitmapRequest = null;
    private final Object bitmapRequestSync = new Object();


    public void create(GraphicFactory factory, Dimension dimension, int color, boolean isTransparent) {
        synchronized(bitmapRequestSync) {
            bitmapRequest = new BitmapRequest(factory, dimension, color, isTransparent);
        }
    }


    private void createBitmapIfRequested() {
        synchronized(bitmapRequestSync) {
            if (bitmapRequest != null) {

                destroyBitmap();
                bitmap = bitmapRequest.create();

                bitmapRequest = null;
            }
        }
    }


    public void destroy()  {
        synchronized(frameLock) {
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


    public Bitmap lock() {
        synchronized (frameLock) {
            createBitmapIfRequested();

            if (bitmap != null) {
                frameLock.enable();
            }
            return bitmap;
        }
    }


    public void release() {
        synchronized(frameLock) {
            frameLock.disable();
        }
    }


    public static void swap(FrameBufferBitmap a, FrameBufferBitmap b) {
        Bitmap t = a.bitmap;
        a.bitmap = b.bitmap;
        b.bitmap = t;

        BitmapRequest r = a.bitmapRequest;
        a.bitmapRequest = b.bitmapRequest;
        b.bitmapRequest = r;
    }


    public static class Lock {
        private boolean enabled = false;


        public synchronized void disable() {
            enabled = false;
            notifyAll();
        }


        public synchronized void enable() {
            enabled = true;
        }


        public boolean isEnabled() {
            return enabled;
        }


        public synchronized void waitDisabled() {
            try {
                while (enabled) {
                    wait();
                }
            } catch (InterruptedException e) {
                LOGGER.warning("FrameBufferHA interrupted");
            }
        }
    }


    private static class BitmapRequest {
        private final GraphicFactory factory;
        private final Dimension dimension;
        private final boolean transparent;
        private final int color;


        public BitmapRequest(GraphicFactory f, Dimension d, int c, boolean t) {
            factory = f;
            dimension = d;
            transparent = t;
            color = c;
        }


        public Bitmap create() {
            if (dimension.width > 0 && dimension.height > 0) {
                Bitmap b = factory.createBitmap(
                        dimension.width,
                        dimension.height,
                        transparent);

                b.setBackgroundColor(color);
                return b;
            }
            return null;
        }
    }
}
