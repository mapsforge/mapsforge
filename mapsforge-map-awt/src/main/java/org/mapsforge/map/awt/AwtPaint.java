/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright © 2014 devemux86
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
package org.mapsforge.map.awt;

import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.TexturePaint;
import java.awt.image.BufferedImage;

import org.mapsforge.core.graphics.Align;
import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Cap;
import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.FontFamily;
import org.mapsforge.core.graphics.FontStyle;
import org.mapsforge.core.graphics.Join;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.Point;

class AwtPaint implements Paint {

	// needed to record size of bitmap shader to compute the shift
	private int shaderWidth;
	private int shaderHeight;

	private static int getCap(Cap cap) {
		switch (cap) {
			case BUTT:
				return BasicStroke.CAP_BUTT;
			case ROUND:
				return BasicStroke.CAP_ROUND;
			case SQUARE:
				return BasicStroke.CAP_SQUARE;
		}

		throw new IllegalArgumentException("unknown cap: " + cap);
	}

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
	private static int getJoin(Join join) {
		switch (join) {
			case ROUND:
				return BasicStroke.JOIN_ROUND;
			case BEVEL:
				return BasicStroke.JOIN_BEVEL;
			case MITER:
				return BasicStroke.JOIN_MITER;
		}

		throw new IllegalArgumentException("unknown cap: " + join);
	}


	java.awt.Color color;
	Font font;
	Stroke stroke;
	Style style;
	TexturePaint texturePaint;
	private int cap;
	private String fontName;
	private int fontStyle;
	private int join;
	private float[] strokeDasharray;
	private float strokeWidth;
	private float textSize;

	AwtPaint() {
		this.cap = getCap(Cap.ROUND);
		this.color = java.awt.Color.BLACK;
		this.style = Style.FILL;
		this.join = getJoin(Join.ROUND);
	}

	@Override
	public int getTextHeight(String text) {
		BufferedImage bufferedImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		FontMetrics fontMetrics = bufferedImage.getGraphics().getFontMetrics(this.font);
		return (int) this.font.createGlyphVector(fontMetrics.getFontRenderContext(), text).getVisualBounds().getHeight();
	}

	@Override
	public int getTextWidth(String text) {
		BufferedImage bufferedImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		FontMetrics fontMetrics = bufferedImage.getGraphics().getFontMetrics(this.font);
		return (int) this.font.createGlyphVector(fontMetrics.getFontRenderContext(), text).getVisualBounds().getWidth();
	}

	@Override
	public boolean isTransparent() {
		return this.texturePaint == null && this.color.getAlpha() == 0;
	}

	@Override
	public void setBitmapShader(Bitmap bitmap) {
		if (bitmap == null) {
			return;
		}
		this.shaderWidth = bitmap.getWidth();
		this.shaderHeight = bitmap.getHeight();
		Rectangle rectangle = new Rectangle(0, 0, bitmap.getWidth(), bitmap.getHeight());
		this.texturePaint = new TexturePaint(AwtGraphicFactory.getBufferedImage(bitmap), rectangle);
	}

	/**
	 * Shifts the bitmap pattern so that it will always start at a multiple of
	 * itself for any tile the pattern is used. This ensures that regardless of
	 * size of the pattern it tiles correctly.
	 * @param origin the reference point
	 */
	@Override
	public void setBitmapShaderShift(Point origin) {
		if (this.texturePaint != null) {
			int relativeDx = ((int) -origin.x) % this.shaderWidth;
			int relativeDy = ((int) -origin.y) % this.shaderHeight;

			Rectangle rectangle = new Rectangle(relativeDx, relativeDy, this.shaderWidth, this.shaderHeight);
			this.texturePaint = new TexturePaint(this.texturePaint.getImage(), rectangle);
		}
	}

	@Override
	public void setColor(Color color) {
		this.color = AwtGraphicFactory.getColor(color);
	}

	@Override
	public void setColor(int color) {
		this.color = new java.awt.Color(color, true);
	}

	@Override
	public void setDashPathEffect(float[] strokeDasharray) {
		this.strokeDasharray = strokeDasharray;
		createStroke();
	}

	@Override
	public void setStrokeCap(Cap cap) {
		this.cap = getCap(cap);
		createStroke();
	}

	@Override
	public void setStrokeJoin(Join join) {
		this.join = getJoin(join);
		createStroke();
	}

	@Override
	public void setStrokeWidth(float strokeWidth) {
		this.strokeWidth = strokeWidth;
		createStroke();
	}

	@Override
	public void setStyle(Style style) {
		this.style = style;
	}

	@Override
	public void setTextAlign(Align align) {
		// this.align = align; //never read
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

	private void createStroke() {
		if (this.strokeWidth <= 0) {
			return;
		}
		this.stroke = new BasicStroke(this.strokeWidth, this.cap, this.join, this.join == BasicStroke.JOIN_MITER ? 1.0f : 0, this.strokeDasharray, 0);
	}
}
