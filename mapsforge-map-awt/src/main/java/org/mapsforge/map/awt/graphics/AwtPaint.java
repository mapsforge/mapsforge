/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014-2017 devemux86
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2024 Sublimis
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
package org.mapsforge.map.awt.graphics;

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

import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.TexturePaint;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.text.AttributedCharacterIterator;
import java.util.HashMap;
import java.util.Map;

public class AwtPaint implements Paint {
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

    private static final Map<AttributedCharacterIterator.Attribute, Object> TEXT_ATTRIBUTES = new HashMap<>();

    static {
        TEXT_ATTRIBUTES.put(TextAttribute.KERNING, TextAttribute.KERNING_ON);
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

    // needed to record size of bitmap shader to compute the shift
    private int shaderWidth;
    private int shaderHeight;

    // Avoid creating unnecessary objects
    private final BufferedImage bufferedImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

    AwtPaint() {
        this.cap = getCap(Cap.ROUND);
        this.color = java.awt.Color.BLACK;
        this.style = Style.FILL;
        this.join = getJoin(Join.ROUND);
    }

    AwtPaint(Paint paint) {
        AwtPaint ap = (AwtPaint) paint;
        this.cap = ap.cap;
        this.color = ap.color;
        this.style = ap.style;
        this.join = ap.join;
        this.stroke = ap.stroke;
        this.fontStyle = ap.fontStyle;
        this.font = ap.font;
        this.fontName = ap.fontName;
        this.strokeWidth = ap.strokeWidth;
        this.textSize = ap.textSize;
        this.strokeDasharray = ap.strokeDasharray;
    }

    @Override
    public int getColor() {
        return this.color.getRGB();
    }

    @Override
    public float getStrokeWidth() {
        return this.strokeWidth;
    }

    @Override
    public int getTextHeight(String text) {
        Graphics2D graphics2d = bufferedImage.createGraphics();
        FontMetrics fontMetrics = graphics2d.getFontMetrics(this.font);
        graphics2d.dispose();
        return (int) this.font.createGlyphVector(fontMetrics.getFontRenderContext(), text).getVisualBounds().getHeight();
    }

    @Override
    public int getTextHeight(final String text, boolean includePadding) {
        Graphics2D graphics2d = bufferedImage.createGraphics();
        FontMetrics fontMetrics = graphics2d.getFontMetrics(this.font);
        graphics2d.dispose();
        int retVal = (int) this.font.createGlyphVector(fontMetrics.getFontRenderContext(), text).getVisualBounds().getHeight();

        if (includePadding && retVal > 0) {
            retVal += getFontPadding(fontMetrics);
        }

        return retVal;
    }

    @Override
    public int getTextWidth(String text) {
        Graphics2D graphics2d = bufferedImage.createGraphics();
        FontMetrics fontMetrics = graphics2d.getFontMetrics(this.font);
        graphics2d.dispose();
        return fontMetrics.stringWidth(text);
    }

    @Override
    public int getTextWidth(String text, boolean includePadding) {
        Graphics2D graphics2d = bufferedImage.createGraphics();
        FontMetrics fontMetrics = graphics2d.getFontMetrics(this.font);
        graphics2d.dispose();
        int retVal = fontMetrics.stringWidth(text);

        if (includePadding && retVal > 0) {
            retVal += getFontPadding(fontMetrics);
        }

        return retVal;
    }

    /**
     * Returns total vertical font padding (top+bottom). The value can also be used for horizontal padding.
     */
    @Override
    public int getFontPadding() {
        Graphics2D graphics2d = bufferedImage.createGraphics();
        FontMetrics fontMetrics = graphics2d.getFontMetrics(this.font);
        graphics2d.dispose();

        return getFontPadding(fontMetrics);
    }

    /**
     * <p>
     * Returns total vertical font padding (top+bottom). The value can also be used for horizontal padding.
     * The value is computed as 12.5% of {@code fontMetrics.getHeight()}.
     * <p>
     * Why 12.5%, why not 10%?
     * <p>
     * 0.125 is a power-of-two number so has an exact binary representation, but that aside, it also has a
     * more practical property for us. On standard screens (dot pitch values cca 0.25-0.27mm) and with AWT
     * sample app defaults, the calculated padding with 10% constant is only 1 pixel for many labels, while
     * with the 12.5% it results in a padding of 2 (our padding is defined as top+bottom, so this is appealing
     * because each side gets 1 pixel of padding).
     */
    public static int getFontPadding(FontMetrics fontMetrics) {
        return (int) (0.125 * fontMetrics.getHeight());
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
        this.texturePaint = new TexturePaint(AwtGraphicFactory.getBitmap(bitmap), rectangle);
    }

    /**
     * Shifts the bitmap pattern so that it will always start at a multiple of
     * itself for any tile the pattern is used. This ensures that regardless of
     * size of the pattern it tiles correctly.
     *
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
            this.font = new Font(this.fontName, this.fontStyle, (int) this.textSize).deriveFont(TEXT_ATTRIBUTES);
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

    public void setStroke(Stroke stroke) {
        this.stroke = stroke;
    }
}
