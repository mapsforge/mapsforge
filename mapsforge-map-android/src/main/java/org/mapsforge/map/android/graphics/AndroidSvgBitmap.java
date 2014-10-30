/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 * Copyright 2013-2014 Ludwig M Brinckmann
 * Copyright 2014 devemux86
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

import java.io.IOException;
import java.io.InputStream;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Picture;
import android.graphics.RectF;
import android.util.Pair;

import com.caverock.androidsvg.SVG;

class AndroidSvgBitmap extends AndroidResourceBitmap {
	static final float DEFAULT_SIZE = 400f;

	private static android.graphics.Bitmap getResourceBitmap(InputStream inputStream, int hash, float scaleFactor, int width, int height, int percent)
			throws IOException {
		synchronized (RESOURCE_BITMAPS) {
			Pair<Bitmap, Integer> data = RESOURCE_BITMAPS.get(hash);
			if (data != null) {
				Pair<android.graphics.Bitmap, Integer> updated = new Pair<android.graphics.Bitmap, Integer>(data.first,
						data.second + 1);
				RESOURCE_BITMAPS.put(hash, updated);
				return data.first;
			}

			android.graphics.Bitmap bitmap = AndroidSvgBitmapStore.get(hash);

			if (bitmap == null) {
				try {
					// not in any cache, so need to render svg
					SVG svg = SVG.getFromInputStream(inputStream);
					Picture picture = svg.renderToPicture();

					double scale = scaleFactor / Math.sqrt((picture.getHeight() * picture.getWidth()) / DEFAULT_SIZE);

					float bitmapWidth = (float) (picture.getWidth() * scale);
					float bitmapHeight = (float) (picture.getHeight() * scale);

					float aspectRatio = (1f * picture.getWidth()) / picture.getHeight();

					if (width != 0 && height != 0) {
						// both width and height set, override any other setting
						bitmapWidth = width;
						bitmapHeight = height;
					} else if (width == 0 && height != 0) {
						// only width set, calculate from aspect ratio
						bitmapWidth = height * aspectRatio;
						bitmapHeight = height;
					} else if (width != 0 && height == 0) {
						// only height set, calculate from aspect ratio
						bitmapHeight = width / aspectRatio;
						bitmapWidth = width;
					}

					if (percent != 100) {
						bitmapWidth *= percent / 100f;
						bitmapHeight *= percent / 100f;
					}

					bitmap = android.graphics.Bitmap.createBitmap((int) Math.ceil(bitmapWidth),
							(int) Math.ceil(bitmapHeight), AndroidGraphicFactory.TRANSPARENT_BITMAP);
					Canvas canvas = new Canvas(bitmap);
					canvas.drawPicture(picture, new RectF(0, 0, bitmapWidth, bitmapHeight));

					// save to disk for faster future retrieval
					AndroidSvgBitmapStore.put(hash, bitmap);
				} catch (Exception e) {
					throw new IOException(e);
				}
			}

			// save in in-memory cache
			Pair<android.graphics.Bitmap, Integer> updated = new Pair<android.graphics.Bitmap, Integer>(bitmap,
					Integer.valueOf(1));
			RESOURCE_BITMAPS.put(hash, updated);

			if (AndroidGraphicFactory.DEBUG_BITMAPS) {
				rInstances.incrementAndGet();
				synchronized (rBitmaps) {
					rBitmaps.add(hash);
				}
			}
			return bitmap;
		}
	}

	AndroidSvgBitmap(InputStream inputStream, int hash, float scaleFactor, int width, int height, int percent) throws IOException {
		super(hash);
		this.bitmap = getResourceBitmap(inputStream, hash, scaleFactor, width, height, percent);
	}

}
