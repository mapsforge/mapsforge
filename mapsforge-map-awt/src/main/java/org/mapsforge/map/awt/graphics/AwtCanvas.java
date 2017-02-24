/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014-2016 devemux86
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

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.Filter;
import org.mapsforge.core.graphics.Matrix;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Path;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.Dimension;
import org.mapsforge.core.model.Rectangle;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.color.ColorSpace;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorConvertOp;
import java.awt.image.IndexColorModel;
import java.awt.image.LookupOp;
import java.awt.image.ShortLookupTable;
import java.util.AbstractMap;
import java.util.Map;

class AwtCanvas implements Canvas {
    private static final String UNKNOWN_STYLE = "unknown style: ";

    private BufferedImage bufferedImage;
    private Graphics2D graphics2D;
    private BufferedImageOp grayscaleOp, invertOp, invertOp4;

    private static Map.Entry<Float, Composite> sizeOneShadingCompositeCache = null;
    private static Composite getHillshadingComposite(float magnitude){
        Map.Entry<Float, Composite> existing = sizeOneShadingCompositeCache;
        if(existing!=null && existing.getKey()==magnitude){
            // JMM says: "A thread-safe immutable object is seen as immutable by all threads, even if a data race is used to pass references to the immutable object between threads"
            // worst case we construct more than strictly needed
            return existing.getValue();
        }

        Composite selected = selectHillShadingComposite(magnitude);

        if(sizeOneShadingCompositeCache==null) {
            // only cache the first magnitude value, in the rare instance that more than one magnitude value would be used
            // in a process lifecycle it would be better to create new Composite instances than create new instances _and_ new cache entries
            sizeOneShadingCompositeCache = new AbstractMap.SimpleImmutableEntry(magnitude, selected);
        }

        return selected;
    }
    /** composite selection,
     * select between {@link AlphaComposite} (fast, squashes saturation at high magnitude)
     * and {@link AwtLuminanceShadingComposite} (per-pixel rgb->hsv->rgb conversion to keep saturation at high magnitude, might be a bit slow and/or inconsistent with the android implementation) */
    private static Composite selectHillShadingComposite(float magnitude) {
        return new AwtLuminanceShadingComposite(magnitude);
        // return AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, magnitude);
    }



    AwtCanvas() {
        createFilters();
    }

    AwtCanvas(Graphics2D graphics2D) {
        this.graphics2D = graphics2D;
        enableAntiAliasing();

        createFilters();
    }

    private BufferedImage applyFilter(BufferedImage src, Filter filter) {
        if (filter == Filter.NONE) {
            return src;
        }
        BufferedImage dest = null;
        switch (filter) {
            case GRAYSCALE:
                dest = new BufferedImage(src.getWidth(), src.getHeight(), src.getType());
                this.grayscaleOp.filter(src, dest);
                break;
            case GRAYSCALE_INVERT:
                dest = new BufferedImage(src.getWidth(), src.getHeight(), src.getType());
                this.grayscaleOp.filter(src, dest);
                dest = applyInvertFilter(dest);
                break;
            case INVERT:
                dest = applyInvertFilter(src);
                break;
        }
        return dest;
    }

    private BufferedImage applyInvertFilter(BufferedImage src) {
        final BufferedImage newSrc;
        if (src.getColorModel() instanceof IndexColorModel) {
            newSrc = new BufferedImage(src.getWidth(), src.getHeight(), src.getColorModel().getNumComponents() == 3 ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = newSrc.createGraphics();
            g2.drawImage(src, 0, 0, null);
            g2.dispose();
        } else {
            newSrc = src;
        }
        BufferedImage dest = new BufferedImage(newSrc.getWidth(), newSrc.getHeight(), newSrc.getType());
        switch (newSrc.getColorModel().getNumComponents()) {
            case 3:
                this.invertOp.filter(newSrc, dest);
                break;
            case 4:
                this.invertOp4.filter(newSrc, dest);
                break;
        }
        return dest;
    }

    private void createFilters() {
        this.grayscaleOp = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null);

        short[] invert = new short[256];
        short[] straight = new short[256];
        for (int i = 0; i < 256; i++) {
            invert[i] = (short) (255 - i);
            straight[i] = (short) i;
        }
        this.invertOp = new LookupOp(new ShortLookupTable(0, invert), null);
        this.invertOp4 = new LookupOp(new ShortLookupTable(0, new short[][]{invert, invert, invert, straight}), null);
    }

    @Override
    public void destroy() {
        // do nothing
    }

    @Override
    public void drawBitmap(Bitmap bitmap, int left, int top) {
        this.graphics2D.drawImage(AwtGraphicFactory.getBufferedImage(bitmap), left, top, null);
    }

    @Override
    public void drawBitmap(Bitmap bitmap, int left, int top, Filter filter) {
        this.graphics2D.drawImage(applyFilter(AwtGraphicFactory.getBufferedImage(bitmap), filter), left, top, null);
    }

    @Override
    public void drawBitmap(Bitmap bitmap, Matrix matrix) {
        this.graphics2D.drawRenderedImage(AwtGraphicFactory.getBufferedImage(bitmap),
                AwtGraphicFactory.getAffineTransform(matrix));
    }

    @Override
    public void shadeBitmap(Bitmap bitmap, Rectangle shadeRect, Rectangle tileRect, float magnitude) {
        Composite oldComposite = this.graphics2D.getComposite();
        Composite composite = getHillshadingComposite(magnitude);

        this.graphics2D.setComposite(composite);
        BufferedImage bufferedImage = AwtGraphicFactory.getBufferedImage(bitmap);

        this.graphics2D.drawImage(bufferedImage
                , (int)tileRect.left, (int)tileRect.top, (int)tileRect.right, (int)tileRect.bottom
                , (int)shadeRect.left, (int)shadeRect.top, (int)shadeRect.right, (int)shadeRect.bottom
                , null);


        this.graphics2D.setComposite(oldComposite); 
    }


    @Override
    public void drawBitmap(Bitmap bitmap, Matrix matrix, Filter filter) {
        this.graphics2D.drawRenderedImage(applyFilter(AwtGraphicFactory.getBufferedImage(bitmap), filter), AwtGraphicFactory.getAffineTransform(matrix));
    }

    @Override
    public void drawCircle(int x, int y, int radius, Paint paint) {
        if (paint.isTransparent()) {
            return;
        }

        AwtPaint awtPaint = AwtGraphicFactory.getPaint(paint);
        setColorAndStroke(awtPaint);
        int doubleRadius = radius * 2;

        Style style = awtPaint.style;
        switch (style) {
            case FILL:
                this.graphics2D.fillOval(x - radius, y - radius, doubleRadius, doubleRadius);
                return;

            case STROKE:
                this.graphics2D.drawOval(x - radius, y - radius, doubleRadius, doubleRadius);
                return;
        }

        throw new IllegalArgumentException(UNKNOWN_STYLE + style);
    }

    @Override
    public void drawLine(int x1, int y1, int x2, int y2, Paint paint) {
        if (paint.isTransparent()) {
            return;
        }

        setColorAndStroke(AwtGraphicFactory.getPaint(paint));
        this.graphics2D.drawLine(x1, y1, x2, y2);
    }

    @Override
    public void drawPath(Path path, Paint paint) {
        if (paint.isTransparent()) {
            return;
        }

        AwtPaint awtPaint = AwtGraphicFactory.getPaint(paint);
        AwtPath awtPath = AwtGraphicFactory.getPath(path);

        setColorAndStroke(awtPaint);
        this.graphics2D.setPaint(awtPaint.texturePaint);

        Style style = awtPaint.style;
        switch (style) {
            case FILL:
                this.graphics2D.fill(awtPath.path2D);
                return;

            case STROKE:
                this.graphics2D.draw(awtPath.path2D);
                return;
        }

        throw new IllegalArgumentException(UNKNOWN_STYLE + style);
    }

    @Override
    public void drawText(String text, int x, int y, Paint paint) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        if (paint.isTransparent()) {
            return;
        }

        AwtPaint awtPaint = AwtGraphicFactory.getPaint(paint);

        if (awtPaint.stroke == null) {
            this.graphics2D.setColor(awtPaint.color);
            this.graphics2D.setFont(awtPaint.font);
            this.graphics2D.drawString(text, x, y);
        } else {
            setColorAndStroke(awtPaint);
            TextLayout textLayout = new TextLayout(text, awtPaint.font, this.graphics2D.getFontRenderContext());
            AffineTransform affineTransform = new AffineTransform();
            affineTransform.translate(x, y);
            this.graphics2D.draw(textLayout.getOutline(affineTransform));
        }
    }

    @Override
    public void drawTextRotated(String text, int x1, int y1, int x2, int y2, Paint paint) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        if (paint.isTransparent()) {
            return;
        }

        AffineTransform affineTransform = this.graphics2D.getTransform();

        double theta = Math.atan2(y2 - y1, x2 - x1);
        this.graphics2D.rotate(theta, x1, y1);

        double lineLength = Math.hypot(x2 - x1, y2 - y1);
        int textWidth = paint.getTextWidth(text);
        int dx = (int) (lineLength - textWidth) / 2;
        int xy = paint.getTextHeight(text) / 3;
        drawText(text, x1 + dx, y1 + xy, paint);

        this.graphics2D.setTransform(affineTransform);
    }

    @Override
    public void fillColor(Color color) {
        fillColor(AwtGraphicFactory.getColor(color));
    }

    @Override
    public void fillColor(int color) {
        fillColor(new java.awt.Color(color));
    }

    @Override
    public Dimension getDimension() {
        return new Dimension(getWidth(), getHeight());
    }

    Graphics2D getGraphicObject() {
        return graphics2D;
    }

    @Override
    public int getHeight() {
        return this.bufferedImage != null ? this.bufferedImage.getHeight() : 0;
    }

    @Override
    public int getWidth() {
        return this.bufferedImage != null ? this.bufferedImage.getWidth() : 0;
    }

    @Override
    public void resetClip() {
        this.graphics2D.setClip(null);
    }

    @Override
    public void setBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            this.bufferedImage = null;
            this.graphics2D = null;
        } else {
            this.bufferedImage = AwtGraphicFactory.getBufferedImage(bitmap);
            this.graphics2D = this.bufferedImage.createGraphics();
            enableAntiAliasing();
        }
    }

    @Override
    public void setClip(int left, int top, int width, int height) {
        this.graphics2D.setClip(left, top, width, height);
    }

    @Override
    public void setClipDifference(int left, int top, int width, int height) {
        Area clip = new Area(new Rectangle2D.Double(0, 0, getWidth(), getHeight()));
        clip.subtract(new Area(new Rectangle2D.Double(left, top, width, height)));
        this.graphics2D.setClip(clip);
    }

    private void enableAntiAliasing() {
        this.graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        this.graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        this.graphics2D.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        this.graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        this.graphics2D.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    private void fillColor(java.awt.Color color) {
        final Composite originalComposite = this.graphics2D.getComposite();
        this.graphics2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC));
        this.graphics2D.setColor(color);
        this.graphics2D.fillRect(0, 0, getWidth(), getHeight());
        this.graphics2D.setComposite(originalComposite);
    }

    public void setColorAndStroke(AwtPaint awtPaint) {
        this.graphics2D.setColor(awtPaint.color);
        if (awtPaint.stroke != null) {
            this.graphics2D.setStroke(awtPaint.stroke);
        }
    }

}
