/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
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
import java.io.OutputStream;

import org.mapsforge.core.graphics.Bitmap;

import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;

class AndroidBitmap implements Bitmap {
	private static final BitmapFactory.Options BITMAP_FACTORY_OPTIONS = createBitmapFactoryOptions();

	private static BitmapFactory.Options createBitmapFactoryOptions() {
		BitmapFactory.Options bitmapFactoryOptions = new BitmapFactory.Options();
		bitmapFactoryOptions.inPreferredConfig = Config.ARGB_8888;
		return bitmapFactoryOptions;
	}

	final android.graphics.Bitmap bitmap;

	AndroidBitmap(android.graphics.Bitmap bitmap) {
		if (bitmap.isRecycled()) {
			throw new IllegalArgumentException("bitmap is already recycled");
		}

		this.bitmap = bitmap;
	}

	AndroidBitmap(InputStream inputStream) {
		this.bitmap = BitmapFactory.decodeStream(inputStream, null, BITMAP_FACTORY_OPTIONS);
	}

	AndroidBitmap(int width, int height) {
		this.bitmap = android.graphics.Bitmap.createBitmap(width, height, Config.ARGB_8888);
	}

	@Override
	public void compress(OutputStream outputStream) {
		this.bitmap.compress(CompressFormat.PNG, 0, outputStream);
	}

	@Override
	public int getHeight() {
		return this.bitmap.getHeight();
	}

	@Override
	public int getWidth() {
		return this.bitmap.getWidth();
	}
}
