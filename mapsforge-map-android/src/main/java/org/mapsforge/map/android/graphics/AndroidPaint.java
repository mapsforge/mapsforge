/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
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

import org.mapsforge.core.graphics.Align;
import org.mapsforge.core.graphics.Cap;
import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.FontFamily;
import org.mapsforge.core.graphics.FontStyle;
import org.mapsforge.core.graphics.Join;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.Point;

import android.graphics.BitmapShader;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.PathEffect;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;
import android.graphics.Typeface;
import android.os.Build;


class AndroidPaint implements Paint {

	// needed to record size of bitmap shader to compute the shift
	private int shaderWidth;
	private int shaderHeight;

	private static android.graphics.Paint.Align getAndroidAlign(Align align) {
		switch (align) {
			case CENTER:
				return android.graphics.Paint.Align.CENTER;
			case LEFT:
				return android.graphics.Paint.Align.LEFT;
			case RIGHT:
				return android.graphics.Paint.Align.RIGHT;
		}

		throw new IllegalArgumentException("unknown align: " + align);
	}

	private static android.graphics.Paint.Cap getAndroidCap(Cap cap) {
		switch (cap) {
			case BUTT:
				return android.graphics.Paint.Cap.BUTT;
			case ROUND:
				return android.graphics.Paint.Cap.ROUND;
			case SQUARE:
				return android.graphics.Paint.Cap.SQUARE;
		}

		throw new IllegalArgumentException("unknown cap: " + cap);
	}

	private static android.graphics.Paint.Join getAndroidJoin(Join join) {
		switch (join) {
			case BEVEL:
				return android.graphics.Paint.Join.BEVEL;
			case ROUND:
				return android.graphics.Paint.Join.ROUND;
			case MITER:
				return android.graphics.Paint.Join.MITER;
		}

		throw new IllegalArgumentException("unknown join: " + join);
	}


	private static android.graphics.Paint.Style getAndroidStyle(Style style) {
		switch (style) {
			case FILL:
				return android.graphics.Paint.Style.FILL;
			case STROKE:
				return android.graphics.Paint.Style.STROKE;
		}

		throw new IllegalArgumentException("unknown style: " + style);
	}

	private static int getFontStyle(FontStyle fontStyle) {
		switch (fontStyle) {
			case BOLD:
				return Typeface.BOLD;
			case BOLD_ITALIC:
				return Typeface.BOLD_ITALIC;
			case ITALIC:
				return Typeface.ITALIC;
			case NORMAL:
				return Typeface.NORMAL;
		}

		throw new IllegalArgumentException("unknown font style: " + fontStyle);
	}

	private static Typeface getTypeface(org.mapsforge.core.graphics.FontFamily fontFamily) {
		switch (fontFamily) {
			case DEFAULT:
				return Typeface.DEFAULT;
			case MONOSPACE:
				return Typeface.MONOSPACE;
			case SANS_SERIF:
				return Typeface.SANS_SERIF;
			case SERIF:
				return Typeface.SERIF;
		}

		throw new IllegalArgumentException("unknown font family: " + fontFamily);
	}

	final android.graphics.Paint paint = new android.graphics.Paint();

	AndroidPaint() {
		this.paint.setAntiAlias(true);
		this.paint.setStrokeCap(getAndroidCap(Cap.ROUND));
		this.paint.setStrokeJoin(android.graphics.Paint.Join.ROUND);
		this.paint.setStyle(getAndroidStyle(Style.FILL));
	}

	@Override
	public int getTextHeight(String text) {
		Rect rect = new Rect();
		this.paint.getTextBounds(text, 0, text.length(), rect);
		return rect.height();
	}

	@Override
	public int getTextWidth(String text) {
		Rect rect = new Rect();
		this.paint.getTextBounds(text, 0, text.length(), rect);
		return rect.width();
	}

	@Override
	public boolean isTransparent() {
		return this.paint.getShader() == null && this.paint.getAlpha() == 0;
	}

	@Override
	public void setBitmapShader(org.mapsforge.core.graphics.Bitmap bitmap) {
		if (bitmap == null) {
			return;
		}
		this.shaderWidth = bitmap.getWidth();
		this.shaderHeight = bitmap.getHeight();
		if (!AndroidGraphicFactory.KEEP_RESOURCE_BITMAPS && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			// there is an problem when bitmaps are recycled too early on honeycomb and up,
			// where shaders are corrupted. This problem does of course not arise if
			// the bitmaps are cached for future use.
			// incrementing the refcount stops the recycling, but leaks the bitmap.
			bitmap.incrementRefCount();
		}
		this.paint.setColor(AndroidGraphicFactory.getColor(Color.WHITE));
		this.paint
				.setShader(new BitmapShader(AndroidGraphicFactory.getBitmap(bitmap), TileMode.REPEAT, TileMode.REPEAT));

	}

	/**
	 * Shifts the bitmap pattern so that it will always start at a multiple of
	 * itself for any tile the pattern is used. This ensures that regardless of
	 * size of the pattern it tiles correctly.
	 * @param origin the reference point
	 */
	@Override
	public void setBitmapShaderShift(Point origin) {
		Shader shader = this.paint.getShader();
		if (shader != null) {
			int relativeDx = ((int) -origin.x) % this.shaderWidth;
			int relativeDy = ((int) -origin.y) % this.shaderHeight;

			Matrix localMatrix = new Matrix();
			localMatrix.setTranslate(relativeDx, relativeDy);
			shader.setLocalMatrix(localMatrix);
		}
	}

	@Override
	public void setColor(Color color) {
		this.paint.setColor(AndroidGraphicFactory.getColor(color));
	}

	@Override
	public void setColor(int color) {
		this.paint.setColor(color);
	}

	@Override
	public void setDashPathEffect(float[] strokeDasharray) {
		PathEffect pathEffect = new DashPathEffect(strokeDasharray, 0);
		this.paint.setPathEffect(pathEffect);
	}

	@Override
	public void setStrokeCap(Cap cap) {
		this.paint.setStrokeCap(getAndroidCap(cap));
	}

	@Override
	public void setStrokeJoin(org.mapsforge.core.graphics.Join join) {
		this.paint.setStrokeJoin(getAndroidJoin(join));
	}

	@Override
	public void setStrokeWidth(float strokeWidth) {
		this.paint.setStrokeWidth(strokeWidth);
	}

	@Override
	public void setStyle(Style style) {
		this.paint.setStyle(getAndroidStyle(style));
	}

	@Override
	public void setTextAlign(Align align) {
		this.paint.setTextAlign(getAndroidAlign(align));
	}

	@Override
	public void setTextSize(float textSize) {
		this.paint.setTextSize(textSize);
	}

	@Override
	public void setTypeface(FontFamily fontFamily, FontStyle fontStyle) {
		this.paint.setTypeface(Typeface.create(getTypeface(fontFamily), getFontStyle(fontStyle)));
	}
}
