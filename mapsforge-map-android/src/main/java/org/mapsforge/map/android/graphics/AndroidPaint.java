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

import org.mapsforge.core.graphics.Align;
import org.mapsforge.core.graphics.Cap;
import org.mapsforge.core.graphics.FontFamily;
import org.mapsforge.core.graphics.FontStyle;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;

import android.graphics.BitmapShader;
import android.graphics.DashPathEffect;
import android.graphics.PathEffect;
import android.graphics.Rect;
import android.graphics.Shader.TileMode;
import android.graphics.Typeface;

class AndroidPaint implements Paint {
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

	private static int getFontStyle(org.mapsforge.core.graphics.FontStyle fontStyle) {
		switch (fontStyle) {
			case BOLD:
				return 1;
			case BOLD_ITALIC:
				return 3;
			case ITALIC:
				return 2;
			case NORMAL:
				return 0;
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

	final android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);

	@Override
	public int getColor() {
		return this.paint.getColor();
	}

	@Override
	public Cap getStrokeCap() {
		android.graphics.Paint.Cap cap = this.paint.getStrokeCap();
		switch (cap) {
			case BUTT:
				return Cap.BUTT;
			case ROUND:
				return Cap.ROUND;
			case SQUARE:
				return Cap.SQUARE;
		}

		throw new IllegalStateException("unknown cap: " + cap);
	}

	@Override
	public float getStrokeWidth() {
		return this.paint.getStrokeWidth();
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
	public void setAlpha(int alpha) {
		this.paint.setAlpha(alpha);
	}

	@Override
	public void setBitmapShader(org.mapsforge.core.graphics.Bitmap bitmap) {
		if (bitmap == null) {
			return;
		}

		this.paint.setShader(new BitmapShader(AndroidGraphics.getBitmap(bitmap), TileMode.REPEAT, TileMode.REPEAT));
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
	public void setStrokeWidth(float strokeWidth) {
		this.paint.setStrokeWidth(strokeWidth);
	}

	@Override
	public void setStyle(Style style) {
		this.paint.setStyle(android.graphics.Paint.Style.valueOf(style.name()));
	}

	@Override
	public void setTextAlign(Align align) {
		this.paint.setTextAlign(android.graphics.Paint.Align.valueOf(align.name()));
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
