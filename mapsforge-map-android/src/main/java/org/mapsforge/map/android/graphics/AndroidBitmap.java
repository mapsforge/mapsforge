/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2014-2016 devemux86
 * Copyright 2024 Sublimis
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
package org.mapsforge.map.android.graphics;

import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.os.Build;

import org.mapsforge.core.graphics.BaseBitmap;
import org.mapsforge.core.graphics.Bitmap;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class AndroidBitmap extends BaseBitmap implements Bitmap {
    private static final List<AndroidBitmap> BITMAP_LIST;

    private static final AtomicInteger BITMAP_INSTANCES;
    private static final Logger LOGGER = Logger.getLogger(AndroidBitmap.class.getName());
    private static final Set<SoftReference<android.graphics.Bitmap>> REUSABLE_BITMAPS = new HashSet<>();

    static {
        if (AndroidGraphicFactory.DEBUG_BITMAPS) {
            BITMAP_INSTANCES = new AtomicInteger();
            BITMAP_LIST = new LinkedList<>();
        } else {
            BITMAP_LIST = null;
            BITMAP_INSTANCES = null;
        }
    }

    protected static android.graphics.Bitmap createAndroidBitmap(int width, int height, Config config) {
        return android.graphics.Bitmap.createBitmap(width, height, config);
    }

    protected static BitmapFactory.Options createBitmapFactoryOptions(Config config) {
        BitmapFactory.Options bitmapFactoryOptions = new BitmapFactory.Options();
        bitmapFactoryOptions.inPreferredConfig = config;
        return bitmapFactoryOptions;
    }

    protected volatile android.graphics.Bitmap bitmap;
    private final AtomicInteger refCount = new AtomicInteger();

    protected AndroidBitmap() {
        if (AndroidGraphicFactory.DEBUG_BITMAPS) {
            BITMAP_INSTANCES.incrementAndGet();
            synchronized (BITMAP_LIST) {
                BITMAP_LIST.add(this);
            }
        }
    }

    public AndroidBitmap(android.graphics.Bitmap bitmap) {
        this();
        if (bitmap.isRecycled()) {
            throw new IllegalArgumentException("bitmap is already recycled");
        }
        this.bitmap = bitmap;
    }

    AndroidBitmap(int width, int height, Config config) {
        this();
        this.bitmap = getBitmapFromReusableSet(width, height, config);
        if (this.bitmap == null) {
            this.bitmap = createAndroidBitmap(width, height, config);
        }
    }

    @Override
    public void compress(OutputStream outputStream) throws IOException {
        if (!this.bitmap.compress(CompressFormat.PNG, 100, outputStream)) {
            throw new IOException("Failed to write bitmap to output stream");
        }
    }

    @Override
    public void decrementRefCount() {
        if (this.refCount.decrementAndGet() < 0) {
            destroy();
        }
    }

    @Override
    public int getHeight() {
        return this.bitmap.getHeight();
    }

    @Override
    public int getWidth() {
        return this.bitmap.getWidth();
    }

    @Override
    public void incrementRefCount() {
        this.refCount.incrementAndGet();
    }

    @Override
    public boolean isDestroyed() {
        return this.bitmap == null;
    }

    @Override
    public void scaleTo(int width, int height) {
        if (getWidth() != width || getHeight() != height) {
            // The effect of the filter argument to createScaledBitmap is not well documented in the
            // official android docs, but according to
            // http://stackoverflow.com/questions/2895065/what-does-the-filter-parameter-to-createscaledbitmap-do
            // passing true results in smoother edges, less pixellation.
            // If smoother corners improve the readability of map labels is perhaps debatable.
            final android.graphics.Bitmap myBitmap = this.bitmap;
            android.graphics.Bitmap scaledBitmap = android.graphics.Bitmap.createScaledBitmap(myBitmap, width,
                    height, true);
            destroy();
            this.bitmap = scaledBitmap;
        }
    }

    @Override
    public void setBackgroundColor(int color) {
        this.bitmap.eraseColor(color);
    }

    @Override
    public String toString() {
        String info;
        final android.graphics.Bitmap myBitmap = this.bitmap;
        if (myBitmap != null) {
            if (myBitmap.hasAlpha()) {
                info = " has alpha";
            } else {
                info = " no alpha";
            }
        } else {
            info = " is recycled";
        }
        return super.toString() + " rC " + refCount.get() + info;

    }

    protected final boolean canUseBitmap(android.graphics.Bitmap candidate, int width, int height) {
        return candidate.getWidth() == width && candidate.getHeight() == height;
    }

    protected void destroy() {
        if (AndroidGraphicFactory.DEBUG_BITMAPS) {
            synchronized (BITMAP_LIST) {
                int i = BITMAP_INSTANCES.decrementAndGet();
                if (BITMAP_LIST.contains(this)) {
                    BITMAP_LIST.remove(this);
                } else {
                    LOGGER.severe("BITMAP ALREADY REMOVED " + this);
                }
                LOGGER.info("BITMAP COUNT " + i + " " + BITMAP_LIST.size());
            }
        }
        destroyBitmap();
    }

    protected void destroyBitmap() {
        final android.graphics.Bitmap myBitmap = this.bitmap;
        this.bitmap = null;

        if (myBitmap != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                synchronized (REUSABLE_BITMAPS) {
                    REUSABLE_BITMAPS.add(new SoftReference<>(myBitmap));
                }
            } else {
                myBitmap.recycle();
            }
        }
    }

    protected final android.graphics.Bitmap getBitmapFromReusableSet(int width, int height, Config config) {
        android.graphics.Bitmap result = null;

        if (!REUSABLE_BITMAPS.isEmpty()) {
            synchronized (REUSABLE_BITMAPS) {
                final Iterator<SoftReference<android.graphics.Bitmap>> iterator = REUSABLE_BITMAPS.iterator();
                android.graphics.Bitmap candidate;

                while (iterator.hasNext()) {
                    candidate = iterator.next().get();
                    if (null != candidate && candidate.isMutable()) {
                        // Check to see it the item can be used for inBitmap.
                        if (canUseBitmap(candidate, width, height)) {
                            result = candidate;
                            // Remove from reusable set so it can't be used again.
                            iterator.remove();
                            break;
                        }
                    } else {
                        // Remove from the set if the reference has been cleared.
                        iterator.remove();
                    }
                }
            }
        }
        return result;
    }

}
