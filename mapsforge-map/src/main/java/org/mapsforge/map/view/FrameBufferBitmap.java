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
    private final Lock allowSwap = new Lock();

    private Bitmap bitmap = null;
    private BitmapRequest bitmapRequest = null;


    public Bitmap lock() {
        synchronized (frameLock) {
            createBitmapIfRequested();

            if (bitmap != null) {
                frameLock.enable();
            }
            return bitmap;
        }
    }

    private void createBitmapIfRequested() {
        if (bitmapRequest != null) {
            destroyBitmap();
            bitmap = bitmapRequest.create();
            bitmapRequest = null;
        }
    }

    public Bitmap lockWhenSwapped() {
        allowSwap.waitDisabled();
        return lock();
    }




    public void releaseAndAllowSwap() {
        frameLock.disable();
        synchronized(frameLock) {
            if (bitmap != null)
                allowSwap.enable();
        }
    }


    public void create(GraphicFactory factory, Dimension dimension, boolean isTransparent) {
        bitmapRequest = new BitmapRequest(factory, dimension, isTransparent);
    }


    public void destroy()  {
        synchronized(frameLock) {
            if (bitmap != null) {
                frameLock.waitDisabled();
                destroyBitmap();
            }
        }
        allowSwap.disable();
    }


    private void destroyBitmap() {
        if (bitmap != null) {
            bitmap.decrementRefCount();
            bitmap = null;
        }
    }


    public static boolean swap(FrameBufferBitmap a, FrameBufferBitmap b) {
        if (a.allowSwap.isEnabled() && b.allowSwap.isEnabled()) {
            Bitmap t = a.bitmap;
            a.bitmap = b.bitmap;
            b.bitmap = t;

            a.allowSwap.disable();
            b.allowSwap.disable();
            return true;

        }
        return false;
    }


    private static class Lock {
        private boolean enabled = false;


        public synchronized void disable() {
            enabled = false;
            notifyAll();
        }

        public synchronized void enable() {
            enabled = true;
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

        public boolean isEnabled() {
            return enabled;
        }
    }


    private static class BitmapRequest {
        private final GraphicFactory factory;
        private final Dimension dimension;
        private final boolean transparent;

        public BitmapRequest(GraphicFactory f, Dimension d, boolean t) {
            factory = f;
            dimension = d;
            transparent = t;
        }

        public Bitmap create() {
            if (dimension.width > 0 && dimension.height > 0)
                return factory.createBitmap(dimension.width, dimension.height, transparent);
            return null;
        }
    }
}
