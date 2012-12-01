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
package org.mapsforge.graphics.android;

import java.io.InputStream;

import org.mapsforge.map.graphics.Bitmap;

import android.graphics.BitmapFactory;

class AndroidBitmap implements Bitmap {
	final android.graphics.Bitmap bitmap;

	AndroidBitmap(InputStream inputStream) {
		this.bitmap = BitmapFactory.decodeStream(inputStream);
	}

	@Override
	public void destroy() {
		this.bitmap.recycle();
	}

	@Override
	public int getHeight() {
		return this.bitmap.getHeight();
	}

	@Override
	public int[] getPixels() {
		int width = getWidth();
		int height = getHeight();
		int[] colors = new int[width * height];
		this.bitmap.getPixels(colors, 0, width, 0, 0, width, height);
		return colors;
	}

	@Override
	public int getWidth() {
		return this.bitmap.getWidth();
	}
}
