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
import org.mapsforge.map.graphics.Paint;
import org.mapsforge.map.rendertheme.GraphicAdapter;

public final class AndroidGraphics implements GraphicAdapter {
	public static final AndroidGraphics INSTANCE = new AndroidGraphics();

	public static android.graphics.Bitmap getAndroidBitmap(Bitmap bitmap) {
		return ((AndroidBitmap) bitmap).bitmap;
	}

	public static android.graphics.Paint getAndroidPaint(Paint paint) {
		return ((AndroidPaint) paint).paint;
	}

	private AndroidGraphics() {
		// do nothing
	}

	@Override
	public Bitmap decodeStream(InputStream inputStream) {
		return new AndroidBitmap(inputStream);
	}

	@Override
	public int getColor(Color color) {
		switch (color) {
			case BLACK:
				return android.graphics.Color.BLACK;

			case CYAN:
				return android.graphics.Color.CYAN;

			case TRANSPARENT:
				return android.graphics.Color.TRANSPARENT;

			case WHITE:
				return android.graphics.Color.WHITE;
		}

		throw new IllegalArgumentException("unknown color value: " + color);
	}

	@Override
	public Paint getPaint() {
		return new AndroidPaint();
	}

	@Override
	public int parseColor(String colorString) {
		return android.graphics.Color.parseColor(colorString);
	}
}
