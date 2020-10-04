/*
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
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.model.Dimension;

import java.util.logging.Logger;

class FrameBufferBitmapHA3 {

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
        private static final int HARD_LOCKED = -1;
        private static final int SOFT_LOCKED = 1;
        private static final int UNLOCKED = 0;

        private int state = UNLOCKED;

        synchronized void hardLock() {
            state = HARD_LOCKED;
            notifyAll();
        }

        synchronized boolean isHardLocked() {
            return (state == HARD_LOCKED);
        }

        synchronized boolean isLocked() {
            return (state == SOFT_LOCKED || state == HARD_LOCKED);
        }

        synchronized boolean isSoftLocked() {
            return (state == SOFT_LOCKED);
        }

        synchronized boolean isUnlocked() {
            return (state == UNLOCKED);
        }

        synchronized void lock() {
            if (state == UNLOCKED) {
                state = SOFT_LOCKED;
            }
        }

        synchronized void unlock() {
            if (state == SOFT_LOCKED) {
                state = UNLOCKED;
            }
            notifyAll();
        }

        synchronized void waitUntilUnlocked() {
            try {
                while (state == SOFT_LOCKED) {
                    wait(); // leaves synchronised block here
                }
            } catch (InterruptedException e) {
                LOGGER.fine("Frame buffer interrupted");
            }
        }
    }

    private static final Logger LOGGER = Logger.getLogger(FrameBufferBitmapHA3.class.getName());

    private Bitmap bitmap = null;
    private BitmapRequest bitmapRequest = null;
    private final Object bitmapRequestSync = new Object();
    private final Lock frameLock = new Lock();

    void create(GraphicFactory factory, Dimension dimension, int color, boolean isTransparent) {
        synchronized (frameLock) {
            if (!frameLock.isHardLocked()) {
                synchronized (bitmapRequestSync) {
                    bitmapRequest = new BitmapRequest(factory, dimension, color, isTransparent);
                }
            }
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
                destroyBitmap();
                frameLock.hardLock();
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
            if (frameLock.isUnlocked()) {
                createBitmapIfRequested();
                if (bitmap != null) {
                    frameLock.lock();
                }
            }
            return bitmap;
        }
    }

    void release() {
        synchronized (frameLock) {
            frameLock.unlock();
        }
    }

    static void swap(FrameBufferBitmapHA3 a, FrameBufferBitmapHA3 b) {
        Bitmap t = a.bitmap;
        a.bitmap = b.bitmap;
        b.bitmap = t;

        BitmapRequest r = a.bitmapRequest;
        a.bitmapRequest = b.bitmapRequest;
        b.bitmapRequest = r;
    }
}
