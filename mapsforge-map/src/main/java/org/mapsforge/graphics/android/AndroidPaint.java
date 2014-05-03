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

import org.mapsforge.map.graphics.Align;
import org.mapsforge.map.graphics.Cap;
import org.mapsforge.map.graphics.FontFamily;
import org.mapsforge.map.graphics.FontStyle;
import org.mapsforge.map.graphics.Paint;
import org.mapsforge.map.graphics.Style;

import android.graphics.Bitmap.Config;
import android.graphics.BitmapShader;
import android.graphics.DashPathEffect;
import android.graphics.PathEffect;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;
import android.graphics.Typeface;

class AndroidPaint implements Paint {
	private static int getStyle(org.mapsforge.map.graphics.FontStyle fontStyle) {
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

	private static Typeface getTypeface(org.mapsforge.map.graphics.FontFamily fontFamily) {
		switch (fontFamily) {
			case DEFAULT:
				return Typeface.DEFAULT;
			case DEFAULT_BOLD:
				return Typeface.DEFAULT_BOLD;
			case MONOSPACE:
				return Typeface.MONOSPACE;
			case SANS_SERIF:
				return Typeface.SANS_SERIF;
			case SERIF:
				return Typeface.SERIF;
		}

		throw new IllegalArgumentException("unknown font family: " + fontFamily);
	}

	final android.graphics.Paint paint;

	AndroidPaint() {
		this.paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
	}

	@Override
	public int getColor() {
		return this.paint.getColor();
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
	public void setBitmapShader(org.mapsforge.map.graphics.Bitmap bitmap) {
		if (bitmap == null) {
			return;
		}

		android.graphics.Bitmap androidBitmap = android.graphics.Bitmap.createBitmap(bitmap.getPixels(),
				bitmap.getWidth(), bitmap.getHeight(), Config.ARGB_8888);
		Shader shader = new BitmapShader(androidBitmap, TileMode.REPEAT, TileMode.REPEAT);
		this.paint.setShader(shader);
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
		android.graphics.Paint.Cap androidCap = android.graphics.Paint.Cap.valueOf(cap.name());
		this.paint.setStrokeCap(androidCap);
	}

	@Override
	public void setStrokeWidth(float width) {
		this.paint.setStrokeWidth(width);
	}

	@Override
	public void setStyle(Style style) {
		android.graphics.Paint.Style androidStyle = android.graphics.Paint.Style.valueOf(style.name());
		this.paint.setStyle(androidStyle);
	}

	@Override
	public void setTextAlign(Align align) {
		android.graphics.Paint.Align androidAlign = android.graphics.Paint.Align.valueOf(align.name());
		this.paint.setTextAlign(androidAlign);
	}

	@Override
	public void setTextSize(float textSize) {
		this.paint.setTextSize(textSize);
	}

	@Override
	public void setTypeface(FontFamily fontFamily, FontStyle fontStyle) {
		Typeface typeface = Typeface.create(getTypeface(fontFamily), getStyle(fontStyle));
		this.paint.setTypeface(typeface);
	}
}
