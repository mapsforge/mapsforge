/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014-2021 devemux86
 * Copyright 2017 usrusr
 * Copyright 2019 cpt1gl0
 * Copyright 2019 Adrian Batzill
 * Copyright 2019 Matthew Egeler
 * Copyright 2019 mg4gh
 * Copyright 2024-2025 Sublimis
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

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.*;
import org.mapsforge.core.model.Dimension;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Rectangle;
import org.mapsforge.core.model.Rotation;
import org.mapsforge.core.util.Parameters;

import java.awt.*;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.util.AbstractMap;
import java.util.Map;

class AwtCanvas implements Canvas {
    private static final String UNKNOWN_STYLE = "unknown style: ";

    private BufferedImage bufferedImage;
    private Graphics2D graphics2D;

    private static final java.awt.Color NEUTRAL_HILLS = AwtGraphicFactory.getColor(Color.TRANSPARENT);
    private static Map.Entry<Float, Composite> sizeOneShadingCompositeCache = null;
    private final AffineTransform transform = new AffineTransform();

    private static Composite getHillshadingComposite(float magnitude) {
        Map.Entry<Float, Composite> existing = sizeOneShadingCompositeCache;
        if (existing != null && existing.getKey() == magnitude) {
            // JMM says: "A thread-safe immutable object is seen as immutable by all threads, even
            // if a data race is used to pass references to the immutable object between threads"
            // worst case we construct more than strictly needed
            return existing.getValue();
        }

        Composite selected = AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, magnitude);

        if (sizeOneShadingCompositeCache == null) {
            // only cache the first magnitude value, in the rare instance that more than one
            // magnitude value would be used in a process lifecycle it would be better to create
            // new Composite instances than create new instances _and_ new cache entries
            sizeOneShadingCompositeCache = new AbstractMap.SimpleImmutableEntry<>(magnitude, selected);
        }

        return selected;
    }

    AwtCanvas() {
    }

    AwtCanvas(Graphics2D graphics2D) {
        this.graphics2D = graphics2D;
        setAntiAlias(Parameters.ANTI_ALIASING);
    }

    @Override
    public void destroy() {
        // do nothing
    }

    @Override
    public void drawBitmap(Bitmap bitmap, int left, int top) {
        final BufferedImage awtBitmap = AwtGraphicFactory.getBitmap(bitmap);
        if (awtBitmap.getColorModel() instanceof IndexColorModel) {
            // We need to clear the existing alpha to get a clean overwrite
            // (this is currently expected only for hill shading bitmaps)
            fillColor(Color.TRANSPARENT);
        }
        this.graphics2D.drawImage(awtBitmap, left, top, null);
    }

    @Override
    public void drawBitmap(Bitmap bitmap, int left, int top, float alpha) {
        Composite composite = this.graphics2D.getComposite();
        if (alpha != 1) {
            this.graphics2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        }
        this.graphics2D.drawImage(AwtGraphicFactory.getBitmap(bitmap), left, top, null);
        if (alpha != 1) {
            this.graphics2D.setComposite(composite);
        }
    }

    @Override
    public void drawBitmap(Bitmap bitmap, Matrix matrix) {
        this.graphics2D.drawRenderedImage(AwtGraphicFactory.getBitmap(bitmap),
                AwtGraphicFactory.getAffineTransform(matrix));
    }

    @Override
    public void drawBitmap(Bitmap bitmap, Matrix matrix, float alpha) {
        Composite composite = this.graphics2D.getComposite();
        if (alpha != 1) {
            this.graphics2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        }
        this.graphics2D.drawRenderedImage(AwtGraphicFactory.getBitmap(bitmap), AwtGraphicFactory.getAffineTransform(matrix));
        if (alpha != 1) {
            this.graphics2D.setComposite(composite);
        }
    }

    @Override
    public void drawBitmap(Bitmap bitmap, int srcLeft, int srcTop, int srcRight, int srcBottom,
                           int dstLeft, int dstTop, int dstRight, int dstBottom) {
        this.graphics2D.drawImage(AwtGraphicFactory.getBitmap(bitmap),
                dstLeft, dstTop, dstRight, dstBottom,
                srcLeft, srcTop, srcRight, srcBottom,
                null);
    }

    @Override
    public void drawBitmap(Bitmap bitmap, int srcLeft, int srcTop, int srcRight, int srcBottom,
                           int dstLeft, int dstTop, int dstRight, int dstBottom, float alpha) {
        Composite composite = this.graphics2D.getComposite();
        if (alpha != 1) {
            this.graphics2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        }
        this.graphics2D.drawImage(AwtGraphicFactory.getBitmap(bitmap),
                dstLeft, dstTop, dstRight, dstBottom,
                srcLeft, srcTop, srcRight, srcBottom,
                null);
        if (alpha != 1) {
            this.graphics2D.setComposite(composite);
        }
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
    public void drawLines(Point[][] coordinates, float dy, Paint paint) {
        // Not used
    }

    @Override
    public void drawPathText(String text, Path path, Paint paint) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        if (paint.isTransparent()) {
            return;
        }

        AwtPaint awtPaint = AwtGraphicFactory.getPaint(paint);
        AwtPath awtPath = AwtGraphicFactory.getPath(path);

        if (awtPaint.stroke == null) {
            this.graphics2D.setColor(awtPaint.color);
            this.graphics2D.setFont(awtPaint.font);
        } else {
            setColorAndStroke(awtPaint);
        }

        TextStroke textStroke = new TextStroke(text, awtPaint.font, this.graphics2D.getFontRenderContext(), false, false);
        Style style = awtPaint.style;
        switch (style) {
            case FILL:
                this.graphics2D.fill(textStroke.createStrokedShape(awtPath.path2D));
                return;

            case STROKE:
                this.graphics2D.draw(textStroke.createStrokedShape(awtPath.path2D));
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
        fillColor(new java.awt.Color(color, true));
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
    public boolean isAntiAlias() {
        return this.graphics2D.getRenderingHints().containsValue(RenderingHints.VALUE_ANTIALIAS_ON);
    }

    @Override
    public boolean isFilterBitmap() {
        return this.graphics2D.getRenderingHints().containsValue(RenderingHints.VALUE_INTERPOLATION_BICUBIC)
                || this.graphics2D.getRenderingHints().containsValue(RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    }

    @Override
    public void resetClip() {
        this.graphics2D.setClip(null);
    }

    @Override
    public void restore() {
        // no-op here
    }

    @Override
    public void rotate(float degrees, float px, float py) {
        if (degrees != 0) {
            this.graphics2D.rotate(degrees, px, py);
        }
    }

    @Override
    public void rotate(Rotation rotation) {
        if (!Rotation.noRotation(rotation)) {
            rotate(rotation.degrees, rotation.px, rotation.py);
        }
    }

    @Override
    public void save() {
        // no-op here
    }

    @Override
    public void setAntiAlias(boolean aa) {
        this.graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, aa ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
        this.graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, aa ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON : RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        this.graphics2D.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, aa ? RenderingHints.VALUE_FRACTIONALMETRICS_ON : RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
    }

    @Override
    public void setBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            this.bufferedImage = null;
            this.graphics2D = null;
        } else {
            this.bufferedImage = AwtGraphicFactory.getBitmap(bitmap);
            this.graphics2D = this.bufferedImage.createGraphics();
            setAntiAlias(Parameters.ANTI_ALIASING);
            this.graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            this.graphics2D.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        }
    }

    @Override
    public void setBitmap(Bitmap bitmap, float dx, float dy, float degrees, float px, float py) {
        translate(dx, dy);
        rotate(degrees, px, py);
        setBitmap(bitmap);
    }

    @Override
    public void setClip(int left, int top, int width, int height) {
        setClip(left, top, width, height, false);
    }

    @Override
    public void setClip(int left, int top, int width, int height, boolean intersect) {
        this.graphics2D.setClip(left, top, width, height);
    }

    @Override
    public void setClipDifference(float left, float top, float width, float height) {
        Area clip = new Area(new Rectangle2D.Double(0, 0, getWidth(), getHeight()));
        clip.subtract(new Area(new Rectangle2D.Double(left, top, width, height)));
        this.graphics2D.setClip(clip);
    }

    @Override
    public void setFilterBitmap(boolean filter) {
        this.graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, filter ? RenderingHints.VALUE_INTERPOLATION_BILINEAR : RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
    }

    @Override
    public void shadeBitmap(Bitmap bitmap, Rectangle shadeRect, Rectangle tileRect, float magnitude, int color) {
        Composite oldComposite = this.graphics2D.getComposite();
        Composite composite = getHillshadingComposite(magnitude);
        this.graphics2D.setComposite(composite);

        if (bitmap == null) {
            // apply flat shading
            if (tileRect != null) {
                this.graphics2D.setClip(
                        (int) Math.round(tileRect.left), (int) Math.round(tileRect.top),
                        (int) Math.round(tileRect.right - (int) tileRect.left), (int) Math.round(tileRect.bottom) - (int) Math.round(tileRect.top) // subtract in after rounding to get same error as on neighbor tile
                );
            }
            this.graphics2D.setColor(NEUTRAL_HILLS);
            this.graphics2D.fillRect(0, 0, getWidth(), getHeight());
            this.graphics2D.setComposite(oldComposite);
            this.graphics2D.setClip(null);
            return;
        }

        if (shadeRect.getWidth() != 0 && shadeRect.getHeight() != 0) {
            final double horizontalScale = tileRect.getWidth() / shadeRect.getWidth();
            final double verticalScale = tileRect.getHeight() / shadeRect.getHeight();

            // Using a rectangle slightly larger than necessary to prevent resize artifacts
            final int srcLeft = Math.max(0, (int) shadeRect.left - 1);
            final int srcTop = Math.max(0, (int) shadeRect.top - 1);
            final int srcWidth = Math.min(bitmap.getWidth() - srcLeft, (int) shadeRect.getWidth() + 4);
            final int srcHeight = Math.min(bitmap.getHeight() - srcTop, (int) shadeRect.getHeight() + 4);

            // (2024-10) Sub-image is extracted to prevent needless upscaling of the entire hill shading bitmap with the transform below.
            // This would waste large amounts of memory and CPU time when only a small part of the bitmap is really needed,
            // and could potentially cause an OOM exception, especially on larger zoom levels when horizontalScale and verticalScale become very large.
            // Note: It appears that Android is not susceptible to this problem, only AWT.
            final BufferedImage subImage = AwtGraphicFactory
                    .getBitmap(bitmap)
                    .getSubimage(srcLeft, srcTop, srcWidth, srcHeight);

            transform.setToIdentity();
            transform.translate(tileRect.left, tileRect.top);
            transform.scale(horizontalScale, verticalScale);
            transform.translate(-(shadeRect.left - srcLeft), -(shadeRect.top - srcTop));

            this.graphics2D.setClip(
                    (int) Math.round(tileRect.left), (int) Math.round(tileRect.top),
                    (int) Math.round(tileRect.right) - (int) Math.round(tileRect.left), (int) Math.round(tileRect.bottom) - (int) Math.round(tileRect.top) // subtract in after rounding to get same error as on neighbor tile
            );

            this.graphics2D.drawRenderedImage(subImage, transform);

            this.graphics2D.setClip(null);
        }

        this.graphics2D.setComposite(oldComposite);
    }

    @Override
    public void translate(float dx, float dy) {
        this.graphics2D.translate(dx, dy);
    }

    private void fillColor(java.awt.Color color) {
        final Composite originalComposite = this.graphics2D.getComposite();
        this.graphics2D.setComposite(AlphaComposite.getInstance(color.getAlpha() == 0 ? AlphaComposite.CLEAR : AlphaComposite.SRC_OVER));
        this.graphics2D.setColor(color);
        this.graphics2D.fillRect(0, 0, getWidth(), getHeight());
        this.graphics2D.setComposite(originalComposite);
    }

    void setColorAndStroke(AwtPaint awtPaint) {
        this.graphics2D.setColor(awtPaint.color);
        if (awtPaint.stroke != null) {
            this.graphics2D.setStroke(awtPaint.stroke);
        }
    }
}
