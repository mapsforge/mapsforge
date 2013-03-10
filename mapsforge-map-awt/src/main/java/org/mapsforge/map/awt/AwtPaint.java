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

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.image.BufferedImage;

import org.mapsforge.core.graphics.Align;
import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Cap;
import org.mapsforge.core.graphics.FontFamily;
import org.mapsforge.core.graphics.FontStyle;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;

// TODO check default values
class AwtPaint implements Paint {
	private static String getFontName(FontFamily fontFamily) {
		switch (fontFamily) {
			case MONOSPACE:
				return Font.MONOSPACED;

			case DEFAULT:
				return null;

			case SANS_SERIF:
				return Font.SANS_SERIF;

			case SERIF:
				return Font.SERIF;
		}

		throw new IllegalArgumentException("unknown fontFamily: " + fontFamily);
	}

	private static int getFontStyle(FontStyle fontStyle) {
		switch (fontStyle) {
			case BOLD:
				return Font.BOLD;

			case BOLD_ITALIC:
				return Font.BOLD | Font.ITALIC;

			case ITALIC:
				return Font.ITALIC;

			case NORMAL:
				return Font.PLAIN;
		}

		throw new IllegalArgumentException("unknown fontStyle: " + fontStyle);
	}

	Bitmap bitmap;
	Font font;
	Style style = Style.FILL;
	private Align align = Align.LEFT;
	private Cap cap = Cap.BUTT;
	private int color = 0;
	private String fontName;
	private int fontStyle;
	private float[] strokeDasharray;
	private float strokeWidth;
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
		BufferedImage bufferedImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		FontMetrics fontMetrics = bufferedImage.getGraphics().getFontMetrics(this.font);
		return fontMetrics.getHeight();
	}

	@Override
	public int getTextWidth(String text) {
		BufferedImage bufferedImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		FontMetrics fontMetrics = bufferedImage.getGraphics().getFontMetrics(this.font);
		return fontMetrics.stringWidth(text);
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
		createFont();
	}

	@Override
	public void setTypeface(FontFamily fontFamily, FontStyle fontStyle) {
		this.fontName = getFontName(fontFamily);
		this.fontStyle = getFontStyle(fontStyle);
		createFont();
	}

	private void createFont() {
		if (this.textSize > 0) {
			this.font = new Font(this.fontName, this.fontStyle, (int) this.textSize);
		} else {
			this.font = null;
		}
	}
}
