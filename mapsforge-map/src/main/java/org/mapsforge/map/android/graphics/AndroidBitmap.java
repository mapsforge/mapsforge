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

import java.io.InputStream;
import java.nio.ByteBuffer;

import org.mapsforge.map.graphics.Bitmap;

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

	private AndroidBitmap(android.graphics.Bitmap bitmap) {
		this.bitmap = bitmap;
	}

	AndroidBitmap(InputStream inputStream) {
		this.bitmap = BitmapFactory.decodeStream(inputStream, null, BITMAP_FACTORY_OPTIONS);
	}

	AndroidBitmap(int width, int height) {
		this.bitmap = android.graphics.Bitmap.createBitmap(width, height, Config.ARGB_8888);
	}

	@Override
	public Bitmap copy() {
		return new AndroidBitmap(this.bitmap.copy(this.bitmap.getConfig(), false));
	}

	@Override
	public void copyPixelsFromBuffer(ByteBuffer byteBuffer) {
		this.bitmap.copyPixelsFromBuffer(byteBuffer);
	}

	@Override
	public void copyPixelsToBuffer(ByteBuffer byteBuffer) {
		this.bitmap.copyPixelsToBuffer(byteBuffer);
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
