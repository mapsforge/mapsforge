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
package org.mapsforge.map.awt;

import org.mapsforge.core.graphics.Align;
import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Cap;
import org.mapsforge.core.graphics.FontFamily;
import org.mapsforge.core.graphics.FontStyle;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;

// TODO check default values
class AwtPaint implements Paint {
	private Align align = Align.LEFT;
	private Bitmap bitmap;
	private Cap cap = Cap.BUTT;
	private int color = 0;
	private FontFamily fontFamily;
	private FontStyle fontStyle;
	private float[] strokeDasharray;
	private float strokeWidth;
	private Style style = Style.FILL;
	private float textSize;

	@Override
	public int getColor() {
		return this.color;
	}

	@Override
	public Cap getStrokeCap() {
		return this.cap;
	}

	@Override
	public float getStrokeWidth() {
		return this.strokeWidth;
	}

	@Override
	public int getTextHeight(String text) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getTextWidth(String text) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setAlpha(int alpha) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setBitmapShader(Bitmap bitmap) {
		this.bitmap = bitmap;
	}

	@Override
	public void setColor(int color) {
		this.color = color;
	}

	@Override
	public void setDashPathEffect(float[] strokeDasharray) {
		this.strokeDasharray = strokeDasharray;
	}

	@Override
	public void setStrokeCap(Cap cap) {
		this.cap = cap;
	}

	@Override
	public void setStrokeWidth(float strokeWidth) {
		this.strokeWidth = strokeWidth;
	}

	@Override
	public void setStyle(Style style) {
		this.style = style;
	}

	@Override
	public void setTextAlign(Align align) {
		this.align = align;
	}

	@Override
	public void setTextSize(float textSize) {
		this.textSize = textSize;
	}

	@Override
	public void setTypeface(FontFamily fontFamily, FontStyle fontStyle) {
		this.fontFamily = fontFamily;
		this.fontStyle = fontStyle;
	}
}
