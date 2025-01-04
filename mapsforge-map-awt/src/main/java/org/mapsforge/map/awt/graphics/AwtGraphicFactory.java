/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2014 Christian Pesch
 * Copyright 2014 Develar
 * Copyright 2015-2017 devemux86
 * Copyright 2017 usrusr
 * Copyright 2018 Adrian Batzill
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

import com.kitfox.svg.SVGCache;
import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.*;
import org.mapsforge.core.mapelements.PointTextContainer;
import org.mapsforge.core.mapelements.SymbolContainer;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Point;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

public class AwtGraphicFactory implements GraphicFactory {
    public static final GraphicFactory INSTANCE = new AwtGraphicFactory();
    public static final java.awt.Color TRANSPARENT = new java.awt.Color(0, 0, 0, 0);

    protected ColorModel monoColorModel = null;
    protected final AtomicInteger hillshadingColor = new AtomicInteger(Integer.MAX_VALUE);

    private AwtGraphicFactory() {
    }

    public static Canvas createGraphicContext(Graphics graphics) {
        return new org.mapsforge.map.awt.graphics.AwtCanvas((Graphics2D) graphics);
    }

    static AffineTransform getAffineTransform(Matrix matrix) {
        return ((AwtMatrix) matrix).affineTransform;
    }

    public static Graphics2D getGraphics(Canvas canvas) {
        return ((AwtCanvas) canvas).getGraphicObject();
    }

    public static AwtPaint getPaint(Paint paint) {
        return (AwtPaint) paint;
    }

    static AwtPath getPath(Path path) {
        return (AwtPath) path;
    }

    static java.awt.Color getColor(Color color) {
        switch (color) {
            case BLACK:
                return java.awt.Color.BLACK;
            case BLUE:
                return java.awt.Color.BLUE;
            case GREEN:
                return java.awt.Color.GREEN;
            case RED:
                return java.awt.Color.RED;
            case TRANSPARENT:
                return TRANSPARENT;
            case WHITE:
                return java.awt.Color.WHITE;
        }

        throw new IllegalArgumentException("unknown color: " + color);
    }

    public static void clearResourceFileCache() {
        // We don't use a resource file cache
    }

    public static void clearResourceMemoryCache() {
        SVGCache.getSVGUniverse().clear();
    }

    @Override
    public Bitmap createBitmap(int width, int height) {
        return new AwtBitmap(width, height);
    }

    @Override
    public Bitmap createBitmap(int width, int height, boolean isTransparent) {
        if (isTransparent) {
            throw new UnsupportedOperationException("No transparencies in AWT implementation");
        }
        return new AwtBitmap(width, height);
    }

    /**
     * Returns the internal image representation.
     *
     * @param bitmap Mapsforge Bitmap
     * @return platform specific image.
     */
    public static BufferedImage getBitmap(Bitmap bitmap) {
        return ((AwtBitmap) bitmap).bufferedImage;
    }

    @Override
    public Canvas createCanvas() {
        return new org.mapsforge.map.awt.graphics.AwtCanvas();
    }

    @Override
    public int createColor(Color color) {
        return getColor(color).getRGB();
    }

    @Override
    public int createColor(int alpha, int red, int green, int blue) {
        return new java.awt.Color(red, green, blue, alpha).getRGB();
    }

    @Override
    public Matrix createMatrix() {
        return new AwtMatrix();
    }

    @Override
    public AwtHillshadingBitmap createMonoBitmap(int width, int height, byte[] buffer, int padding, BoundingBox area, int color) {
        synchronized (this.hillshadingColor) {
            if (color != hillshadingColor.getAndSet(color) || monoColorModel == null) {
                final java.awt.Color awtColor = new java.awt.Color(color, true);

                final int alpha = awtColor.getAlpha();

                byte[] reds = new byte[256];
                byte[] greens = new byte[256];
                byte[] blues = new byte[256];
                byte[] alphas = new byte[256];

                Arrays.fill(reds, (byte) awtColor.getRed());
                Arrays.fill(greens, (byte) awtColor.getGreen());
                Arrays.fill(blues, (byte) awtColor.getBlue());

                // use a lookup color model on the AWT side so that the android implementation can take the bytes without any twiddling
                // (the only 8 bit bitmaps android knows are alpha masks, so we have to define our mono bitmap bytes in a way that are easy for android to understand)
                for (int i = 0; i < 256; i++) {
                    alphas[i] = (byte) (i * alpha / 255);
                }

                monoColorModel = new IndexColorModel(8, 256, reds, greens, blues, alphas);
            }
        }

        DataBuffer dataBuffer = new DataBufferByte(buffer, buffer.length);
        SampleModel singleByteSampleModel = monoColorModel.createCompatibleSampleModel(width + 2 * padding, height + 2 * padding);
        WritableRaster writableRaster = Raster.createWritableRaster(singleByteSampleModel, dataBuffer, null);
        BufferedImage bufferedImage = new BufferedImage(monoColorModel, writableRaster, false, null);

        return new AwtHillshadingBitmap(bufferedImage, padding, area);
    }

    @Override
    public Paint createPaint() {
        return new AwtPaint();
    }

    @Override
    public Paint createPaint(Paint paint) {
        return new AwtPaint(paint);
    }


    @Override
    public Path createPath() {
        return new AwtPath();
    }

    @Override
    public PointTextContainer createPointTextContainer(Point xy, double horizontalOffset, double verticalOffset,
                                                       Display display, int priority, String text, Paint paintFront, Paint paintBack,
                                                       SymbolContainer symbolContainer, Position position, int maxTextWidth) {
        return new AwtPointTextContainer(xy, horizontalOffset, verticalOffset, display,
                priority, text, paintFront, paintBack, symbolContainer, position, maxTextWidth);
    }

    @Override
    public ResourceBitmap createResourceBitmap(InputStream inputStream, float scaleFactor, int width, int height, int percent, int hash) throws IOException {
        return new AwtResourceBitmap(inputStream, scaleFactor, width, height, percent);
    }

    @Override
    public TileBitmap createTileBitmap(InputStream inputStream, int tileSize, boolean hasAlpha) throws IOException {
        return new AwtTileBitmap(inputStream);
    }

    @Override
    public TileBitmap createTileBitmap(int tileSize, boolean hasAlpha) {
        return new AwtTileBitmap(tileSize, hasAlpha);
    }

    @Override
    public InputStream platformSpecificSources(String relativePathPrefix, String src) throws IOException {
        return null;
    }

    @Override
    public ResourceBitmap renderSvg(InputStream inputStream, float scaleFactor, int width, int height, int percent, int hash) throws IOException {
        return new AwtSvgBitmap(inputStream, hash, scaleFactor, width, height, percent);
    }

}
