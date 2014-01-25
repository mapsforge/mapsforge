/*
 * Copyright 2010, 2011, 2012 mapsforge.org
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


import android.graphics.BitmapFactory;
import android.util.Pair;

import org.mapsforge.core.graphics.ResourceBitmap;

import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AndroidResourceBitmap extends AndroidBitmap implements ResourceBitmap {

	// used for debug bitmap accounting
    protected static AtomicInteger rInstances;
    protected static HashSet<Integer> rBitmaps;
	static {
		if (AndroidGraphicFactory.DEBUG_BITMAPS) {
			rInstances = new AtomicInteger();
			rBitmaps = new HashSet<>();
		}
	}

    protected static final Logger LOGGER = Logger.getLogger(AndroidResourceBitmap.class.getName());
    protected static final HashMap<Integer, Pair<android.graphics.Bitmap, Integer>> RESOURCE_BITMAPS =
            new HashMap<>();


	// if AndroidGraphicFactory.KEEP_RESOURCE_BITMAPS is set, the bitmaps are kept in
	// a dictionary for faster retrieval and are not deleted or recycled until
	// clearBitmaps is called

    private static android.graphics.Bitmap getResourceBitmap(InputStream inputStream, int hash) {
        synchronized (RESOURCE_BITMAPS) {
            Pair<android.graphics.Bitmap, Integer> data = RESOURCE_BITMAPS.get(hash);
            if (data != null) {
                Pair<android.graphics.Bitmap, Integer> updated = new Pair<android.graphics.Bitmap, Integer>(data.first, data.second + 1);
                RESOURCE_BITMAPS.put(hash, updated);
                return data.first;
            } else {
                android.graphics.Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, createBitmapFactoryOptions(AndroidGraphicFactory.TRANSPARENT_BITMAP));
                Pair<android.graphics.Bitmap, Integer> updated = new Pair<android.graphics.Bitmap, Integer>(bitmap, Integer.valueOf(1));
                RESOURCE_BITMAPS.put(hash, updated);
	            if (AndroidGraphicFactory.DEBUG_BITMAPS) {
		            LOGGER.log(Level.INFO, "RESOURCE BITMAP CREATE " + hash);
		            rInstances.incrementAndGet();
		            synchronized (rBitmaps) {
			            rBitmaps.add(hash);
		            }
		            LOGGER.log(Level.INFO, "RESOURCE BITMAP ACC COUNT " + rInstances.get() + " " + rBitmaps.size());
		            LOGGER.log(Level.INFO, "RESOURCE BITMAP COUNT " + RESOURCE_BITMAPS.size());
	            }
                return bitmap;
            }
        }
    }

    private static boolean removeBitmap(int hash) {
        if (AndroidGraphicFactory.KEEP_RESOURCE_BITMAPS) {
            return false;
        }
        synchronized (RESOURCE_BITMAPS) {
            Pair<android.graphics.Bitmap, Integer> data = RESOURCE_BITMAPS.get(hash);
            if (data != null) {
                if (data.second.intValue() > 1) {
                    Pair<android.graphics.Bitmap, Integer> updated = new Pair<android.graphics.Bitmap, Integer>(data.first, data.second - 1);
                    RESOURCE_BITMAPS.put(hash, updated);
                    return false;
                }
                RESOURCE_BITMAPS.remove(hash);
	            if (AndroidGraphicFactory.DEBUG_BITMAPS) {
		            synchronized (rBitmaps) {
			            LOGGER.log(Level.INFO, "RESOURCE BITMAP DELETE " + hash);
			            int i = rInstances.decrementAndGet();
			            if (rBitmaps.contains(hash)) {
				            rBitmaps.remove(hash);
			            } else {
				            LOGGER.log(Level.SEVERE, "RESOURCE BITMAP ALREADY REMOVED " + hash);
			            }
			            LOGGER.log(Level.INFO, "RESOURCE BITMAP ACC COUNT " + i + " " + rBitmaps.size());
		            }
		            LOGGER.log(Level.INFO, "RESOURCE BITMAP COUNT " + RESOURCE_BITMAPS.size());
	            }
	            return true;
            }
        }
	    throw new IllegalStateException("Bitmap should have been here " + hash);
    }

    public static void clearResourceBitmaps() {
	    if (!AndroidGraphicFactory.KEEP_RESOURCE_BITMAPS) {
		    return;
	    }
        synchronized (RESOURCE_BITMAPS) {
            for (Pair<android.graphics.Bitmap, Integer> p : RESOURCE_BITMAPS.values()) {
                p.first.recycle();
	            if (AndroidGraphicFactory.DEBUG_BITMAPS) {
		            rInstances.decrementAndGet();
	            }
            }
	        if (AndroidGraphicFactory.DEBUG_BITMAPS) {
				rBitmaps.clear();
	        }
            RESOURCE_BITMAPS.clear();
        }
    }

    private final int hash; // the hash value is used to avoid multiple loading of the same resource

	protected AndroidResourceBitmap(int hash) {
		super();
		this.hash = hash;
	}

    AndroidResourceBitmap(InputStream inputStream, int hash) {
        this(hash);
        this.bitmap = getResourceBitmap(inputStream, hash);
    }

	// destroy is the super method here, which will take care of bitmap accounting
	// and call down into destroyBitmap when the resource bitmap needs to be destroyed

	protected void destroyBitmap() {
		if (this.bitmap != null) {
			if (removeBitmap(this.hash)) {
				this.bitmap.recycle();
			}
			this.bitmap = null;
		}
	}
}
