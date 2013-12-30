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

import android.annotation.TargetApi;
import android.graphics.BitmapFactory;

import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.core.graphics.CorruptedInputStream;
import org.mapsforge.core.util.IOUtils;

import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.Iterator;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;


public class AndroidTileBitmap extends AndroidBitmap implements TileBitmap {

	private static AtomicInteger tileInstances;
	static {
		if (AndroidGraphicFactory.debugBitmaps) {
			tileInstances = new AtomicInteger();
		}
	}

	private static final Logger LOGGER = Logger.getLogger(AndroidTileBitmap.class.getName());

	private static Set<SoftReference<android.graphics.Bitmap>> reusableTileBitmaps = new HashSet<>();

	static android.graphics.Bitmap getTileBitmapFromReusableSet() {
		android.graphics.Bitmap bitmap = null;

		if (reusableTileBitmaps != null && !reusableTileBitmaps.isEmpty()) {
			synchronized (reusableTileBitmaps) {
				final Iterator<SoftReference<android.graphics.Bitmap>> iterator = reusableTileBitmaps.iterator();
				android.graphics.Bitmap candidate;

				while (iterator.hasNext()) {
					candidate = iterator.next().get();
					if (null != candidate && candidate.isMutable()) {
						bitmap = candidate;
						bitmap.eraseColor(0);
						// Remove from reusable set so it can't be used again.
						iterator.remove();
						break;
					} else {
						// Remove from the set if the reference has been cleared.
						iterator.remove();
					}
				}
			}
		}
		return bitmap;
	}

	@TargetApi(11)
	private static final BitmapFactory.Options createTileBitmapFactoryOptions() {
		BitmapFactory.Options bitmapFactoryOptions = new BitmapFactory.Options();
		bitmapFactoryOptions.inPreferredConfig = AndroidGraphicFactory.bitmapConfig;
		if (org.mapsforge.map.android.util.AndroidUtil.honeyCombPlus) {
			bitmapFactoryOptions.inMutable = true;
			bitmapFactoryOptions.inSampleSize = 1; // not really sure why this is required, but otherwise decoding fails
			bitmapFactoryOptions.inBitmap = getTileBitmapFromReusableSet();
		}
		return bitmapFactoryOptions;
	}
	
	AndroidTileBitmap() {
		this.bitmap = getTileBitmapFromReusableSet();
		if (this.bitmap == null) {
			this.bitmap = AndroidBitmap.createAndroidBitmap(GraphicFactory.getTileSize(), GraphicFactory.getTileSize());
		}
        if (AndroidGraphicFactory.debugBitmaps) {
		    tileInstances.incrementAndGet();
        }
	}

    /*
        THIS CAN THROW AN IllegalArgumentException or SocketTimeoutException
        The inputStream can be corrupt for various reasons (slow download or slow access
        to file system) and will then raise an exception. This exception must be caught
        by client classes. We do not catch it here to allow proper handling in the higher
        levels (like redownload or reload from file storage).
     */
	AndroidTileBitmap(InputStream inputStream) {
        try {
            if (AndroidGraphicFactory.debugBitmaps) {
                tileInstances.incrementAndGet();
            }
            this.bitmap = BitmapFactory.decodeStream(inputStream, null, createTileBitmapFactoryOptions());
	        // somehow on Android the decode stream can succeed, but the bitmap remains invalid.
	        // Asking for the width forces the bitmap to be fully loaded and a NullPointerException
	        // is triggered if the stream is not readable,
	        // so that it can be handled at this point, rather than later
	        // during bitmap painting
	        int w = this.bitmap.getWidth();
        } catch (Exception e) {
            // this is really stupid, the runtime system actually throws a SocketTimeoutException,
            // but we cannot catch it, because it is not declared, so we needed to catch the base
	        // class exception
            LOGGER.log(Level.INFO, "TILEBITMAP ERROR " + e.toString());
	        this.bitmap = null; // need to null out to avoid recycling
	        IOUtils.closeQuietly(inputStream); // seems to improve memory usage
            this.destroy();
            throw new CorruptedInputStream("Corrupted bitmap input stream", e);
        }
	}

	@Override
	protected void destroy() {
		super.destroy();
        if (AndroidGraphicFactory.debugBitmaps) {
    		int i = tileInstances.decrementAndGet();
    		LOGGER.log(Level.INFO, "TILEBITMAP COUNT " + Integer.toString(i));
        }
	}

    @Override
    protected void destroyBitmap() {
	    if (this.bitmap != null) {
		    // bitmap can be null if there is an error creating it
            if (org.mapsforge.map.android.util.AndroidUtil.honeyCombPlus) {
                synchronized (reusableTileBitmaps) {
                    reusableTileBitmaps.add(new SoftReference<android.graphics.Bitmap>(this.bitmap));
                }
            } else {
                this.bitmap.recycle();
            }
		    this.bitmap = null;
	    }
    }
}
