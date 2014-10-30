/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 * Copyright © 2014 Ludwig M Brinckmann
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

import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mapsforge.core.graphics.CorruptedInputStreamException;
import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.core.util.IOUtils;
import org.mapsforge.map.android.util.AndroidUtil;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/**
 * On Android, managing and recycling the memory for bitmaps is important, but varies significantly between versions:
 * before Honeycomb (3.0), bitmaps require a call to recycle() to clear the memory, for higher versions it is possible
 * to reuse the bitmap storage as long as it fits exactly. For older Android versions, bitmaps are recycled when no in
 * use any more. For new Android versions, the bitmap memory gets stored as a SoftReference, so that the memory can get
 * reclaimed if the GC so decides. For every new TileBitmap, it is attempted to reuse one of the older bitmaps from the
 * cache, only if that fails, a new bitmap is allocated.
 */

public class AndroidTileBitmap extends AndroidBitmap implements TileBitmap {
	private static final Logger LOGGER = Logger.getLogger(AndroidTileBitmap.class.getName());
	private static Map<Integer, Set<SoftReference<Bitmap>>> reusableTileBitmaps = new HashMap<Integer, Set<SoftReference<Bitmap>>>();

	private static AtomicInteger tileInstances;

	// For modern Android versions, bitmap storage can be recycled. To support different tile
	// sizes we have a hashmap that contains the caches by tileSize/alpha

	static {
		if (AndroidGraphicFactory.DEBUG_BITMAPS) {
			tileInstances = new AtomicInteger();
		}
	}

	private static int composeHash(int tileSize, boolean isTransparent) {
		if (isTransparent) {
			return tileSize + 0x10000000;
		}
		return tileSize;
	}

	private static android.graphics.Bitmap getTileBitmapFromReusableSet(int tileSize, boolean isTransparent) {
		int hash = composeHash(tileSize, isTransparent);
		Set<SoftReference<Bitmap>> subSet = reusableTileBitmaps.get(hash);

		if (subSet == null) {
			return null;
		}
		android.graphics.Bitmap bitmap = null;
		synchronized (subSet) {
			final Iterator<SoftReference<android.graphics.Bitmap>> iterator = subSet.iterator();
			android.graphics.Bitmap candidate;
			while (iterator.hasNext()) {
				candidate = iterator.next().get();
				if (null != candidate && candidate.isMutable()) {
					bitmap = candidate;
					if (isTransparent) {
						bitmap.eraseColor(android.graphics.Color.TRANSPARENT);
					}
					// Remove from reusable set so it can't be used again.
					iterator.remove();
					break;
				} else {
					// Remove from the set if the reference has been cleared.
					iterator.remove();
				}
			}
		}
		return bitmap;
	}

	private long expiration = 0;
	private long timestamp = System.currentTimeMillis();

	/*
	 * THIS CAN THROW AN IllegalArgumentException or SocketTimeoutException The inputStream can be corrupt for various
	 * reasons (slow download or slow access to file system) and will then raise an exception. This exception must be
	 * caught by client classes. We do not catch it here to allow proper handling in the higher levels (like redownload
	 * or reload from file storage).
	 */
	AndroidTileBitmap(InputStream inputStream, int tileSize, boolean isTransparent) {
		super();
		try {
			if (AndroidGraphicFactory.DEBUG_BITMAPS) {
				tileInstances.incrementAndGet();
			}
			this.bitmap = BitmapFactory.decodeStream(inputStream, null,
					createTileBitmapFactoryOptions(tileSize, isTransparent));
			// somehow on Android the decode stream can succeed, but the bitmap remains invalid.
			// Asking for the width forces the bitmap to be fully loaded and a NullPointerException
			// is triggered if the stream is not readable,
			// so that it can be handled at this point, rather than later
			// during bitmap painting
			int w = this.bitmap.getWidth(); //NOSONAR
		} catch (Exception e) {
			// this is really stupid, the runtime system actually throws a SocketTimeoutException,
			// but we cannot catch it, because it is not declared, so we needed to catch the base
			// class exception
			LOGGER.log(Level.INFO, "TILEBITMAP ERROR " + e.toString());
			this.bitmap = null; // need to null out to avoid recycling
			IOUtils.closeQuietly(inputStream); // seems to improve memory usage
			this.destroy();
			throw new CorruptedInputStreamException("Corrupted bitmap input stream", e);
		}
	}

	AndroidTileBitmap(int tileSize, boolean isTransparent) {
		super();
		if (AndroidUtil.HONEYCOMB_PLUS) {
			this.bitmap = getTileBitmapFromReusableSet(tileSize, isTransparent);
		}
		if (this.bitmap == null) {
			android.graphics.Bitmap.Config config = isTransparent ? AndroidGraphicFactory.TRANSPARENT_BITMAP
					: AndroidGraphicFactory.NON_TRANSPARENT_BITMAP;
			this.bitmap = AndroidBitmap.createAndroidBitmap(tileSize, tileSize, config);
		}
		if (AndroidGraphicFactory.DEBUG_BITMAPS) {
			tileInstances.incrementAndGet();
		}
	}

	@Override
	public long getTimestamp() {
		return timestamp;
	}

	@Override
	public boolean isExpired() {
		if (expiration == 0)
			return false;
		return (expiration >= System.currentTimeMillis());
	}

	@Override
	public void setExpiration(long expiration) {
		this.expiration = expiration;
	}

	@Override
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	@Override
	protected void destroy() {
		super.destroy();
		if (AndroidGraphicFactory.DEBUG_BITMAPS) {
			int i = tileInstances.decrementAndGet();
			LOGGER.log(Level.INFO, "TILEBITMAP COUNT " + Integer.toString(i));
		}
	}

	@Override
	protected void destroyBitmap() {
		if (this.bitmap != null) {
			// bitmap can be null if there is an error creating it
			if (AndroidUtil.HONEYCOMB_PLUS) {
				final int tileSize = this.getHeight();
				synchronized (reusableTileBitmaps) {
					int hash = composeHash(tileSize, this.bitmap.hasAlpha());
					if (!reusableTileBitmaps.containsKey(hash)) {
						// if the set specific to the tile size does not exist, create it. It will
						// never be destroyed, but the contained bitmaps will be recycled if memory
						// gets tight.
						reusableTileBitmaps.put(hash, new HashSet<SoftReference<Bitmap>>());
					}
					Set<SoftReference<Bitmap>> sizeSpecificSet = reusableTileBitmaps.get(hash);
					synchronized (sizeSpecificSet) {
						sizeSpecificSet.add(new SoftReference<Bitmap>(this.bitmap));
					}
				}
			} else {
				this.bitmap.recycle();
			}
			this.bitmap = null;
		}
	}

	@TargetApi(11)
	private BitmapFactory.Options createTileBitmapFactoryOptions(int tileSize, boolean isTransparent) {
		BitmapFactory.Options bitmapFactoryOptions = new BitmapFactory.Options();
		if (isTransparent) {
			bitmapFactoryOptions.inPreferredConfig = AndroidGraphicFactory.TRANSPARENT_BITMAP;
		} else {
			bitmapFactoryOptions.inPreferredConfig = AndroidGraphicFactory.NON_TRANSPARENT_BITMAP;
		}
		if (org.mapsforge.map.android.util.AndroidUtil.HONEYCOMB_PLUS) {
			android.graphics.Bitmap reusableBitmap = getTileBitmapFromReusableSet(tileSize, isTransparent);
			if (reusableBitmap != null) {
				bitmapFactoryOptions.inMutable = true;
				bitmapFactoryOptions.inSampleSize = 1; // not really sure why this is required, but otherwise decoding
														// fails
				bitmapFactoryOptions.inBitmap = getTileBitmapFromReusableSet(tileSize, isTransparent);
			}
		}
		return bitmapFactoryOptions;
	}
}
