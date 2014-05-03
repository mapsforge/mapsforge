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

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.Matrix;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Path;

import android.graphics.Bitmap.Config;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

public final class AndroidGraphicFactory implements GraphicFactory {
	public static final GraphicFactory INSTANCE = new AndroidGraphicFactory();

	public static Bitmap convertToBitmap(Drawable drawable) {
		android.graphics.Bitmap bitmap;
		if (drawable instanceof BitmapDrawable) {
			bitmap = ((BitmapDrawable) drawable).getBitmap();
		} else {
			int width = drawable.getIntrinsicWidth();
			int height = drawable.getIntrinsicHeight();
			bitmap = android.graphics.Bitmap.createBitmap(width, height, Config.ARGB_8888);
			android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);

			Rect rect = drawable.getBounds();
			drawable.setBounds(0, 0, width, height);
			drawable.draw(canvas);
			drawable.setBounds(rect);
		}

		return new AndroidBitmap(bitmap);
	}

	public static Canvas createGraphicContext(android.graphics.Canvas canvas) {
		return new AndroidCanvas(canvas);
	}

	public static android.graphics.Bitmap getBitmap(Bitmap bitmap) {
		return ((AndroidBitmap) bitmap).bitmap;
	}

	public static android.graphics.Canvas getCanvas(Canvas canvas) {
		return ((AndroidCanvas) canvas).canvas;
	}

	static int getColor(Color color) {
		switch (color) {
			case BLACK:
				return android.graphics.Color.BLACK;
			case BLUE:
				return android.graphics.Color.BLUE;
			case GREEN:
				return android.graphics.Color.GREEN;
			case RED:
				return android.graphics.Color.RED;
			case TRANSPARENT:
				return android.graphics.Color.TRANSPARENT;
			case WHITE:
				return android.graphics.Color.WHITE;
		}

		throw new IllegalArgumentException("unknown color: " + color);
	}

	static android.graphics.Matrix getMatrix(Matrix matrix) {
		return ((AndroidMatrix) matrix).matrix;
	}

	static android.graphics.Paint getPaint(Paint paint) {
		return ((AndroidPaint) paint).paint;
	}

	static android.graphics.Path getPath(Path path) {
		return ((AndroidPath) path).path;
	}

	private AndroidGraphicFactory() {
		// do nothing
	}

	@Override
	public Bitmap createBitmap(InputStream inputStream) {
		return new AndroidBitmap(inputStream);
	}

	@Override
	public Bitmap createBitmap(int width, int height) {
		return new AndroidBitmap(width, height);
	}

	@Override
	public Canvas createCanvas() {
		return new AndroidCanvas();
	}

	@Override
	public int createColor(Color color) {
		return getColor(color);
	}

	@Override
	public int createColor(int alpha, int red, int green, int blue) {
		return android.graphics.Color.argb(alpha, red, green, blue);
	}

	@Override
	public Matrix createMatrix() {
		return new AndroidMatrix();
	}

	@Override
	public Paint createPaint() {
		return new AndroidPaint();
	}

	@Override
	public Path createPath() {
		return new AndroidPath();
	}
}
