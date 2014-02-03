/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 * Copyright 2013-2014 Ludwig M Brinckmann
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

import com.applantation.android.svg.SVG;
import com.applantation.android.svg.SVGParser;

class AndroidSvgBitmap extends AndroidResourceBitmap {
	static final float DEFAULT_SIZE = 400f;

	private static android.graphics.Bitmap getResourceBitmap(InputStream inputStream, int hash, float scaleFactor)
			throws IOException {
		synchronized (RESOURCE_BITMAPS) {
			Pair<Bitmap, Integer> data = RESOURCE_BITMAPS.get(hash);
			if (data != null) {
				Pair<android.graphics.Bitmap, Integer> updated = new Pair<android.graphics.Bitmap, Integer>(data.first,
						data.second + 1);
				RESOURCE_BITMAPS.put(hash, updated);
				return data.first;
			} else {
				SVG svg = SVGParser.getSVGFromInputStream(inputStream);
				Picture picture = svg.getPicture();

				double scale = scaleFactor / Math.sqrt((picture.getHeight() * picture.getWidth()) / DEFAULT_SIZE);

				float bitmapWidth = (float) (picture.getWidth() * scale);
				float bitmapHeight = (float) (picture.getHeight() * scale);

				android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap((int) Math.ceil(bitmapWidth),
						(int) Math.ceil(bitmapHeight), android.graphics.Bitmap.Config.ARGB_8888);
				Canvas canvas = new Canvas(bitmap);
				canvas.drawPicture(picture, new RectF(0, 0, bitmapWidth, bitmapHeight));
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
	}

	AndroidSvgBitmap(InputStream inputStream, int hash, float scaleFactor) throws IOException {
		super(hash);
		this.bitmap = getResourceBitmap(inputStream, hash, scaleFactor);
	}

}
