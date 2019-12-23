/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2014-2019 devemux86
 * Copyright 2017 usrusr
 * Copyright 2019 cpt1gl0
 * Copyright 2019 Adrian Batzill
 * Copyright 2019 mg4gh
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

import android.graphics.*;
import android.os.Build;
import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.Matrix;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Path;
import org.mapsforge.core.graphics.*;
import org.mapsforge.core.model.Dimension;
import org.mapsforge.core.model.Rectangle;

class AndroidCanvas implements Canvas {
    private static final float[] INVERT_MATRIX = {
            -1, 0, 0, 0, 255,
            0, -1, 0, 0, 255,
            0, 0, -1, 0, 255,
            0, 0, 0, 1, 0
    };

    android.graphics.Canvas canvas;
    private final android.graphics.Paint bitmapPaint = new android.graphics.Paint();
    private ColorFilter grayscaleFilter, grayscaleInvertFilter, invertFilter;

    /**
     * A set of reusable temporaries that is not needed when hillshading is inactive.
     */
    private HilshadingTemps hillshadingTemps = null;

    AndroidCanvas() {
        this.canvas = new android.graphics.Canvas();

        this.bitmapPaint.setAntiAlias(true);
        this.bitmapPaint.setFilterBitmap(true);

        createFilters();
    }

    AndroidCanvas(android.graphics.Canvas canvas) {
        this.canvas = canvas;

        createFilters();
    }

    private void applyFilter(Filter filter) {
        if (filter == Filter.NONE) {
            return;
        }
        switch (filter) {
            case GRAYSCALE:
                bitmapPaint.setColorFilter(grayscaleFilter);
                break;
            case GRAYSCALE_INVERT:
                bitmapPaint.setColorFilter(grayscaleInvertFilter);
                break;
            case INVERT:
                bitmapPaint.setColorFilter(invertFilter);
                break;
        }
    }

    private void createFilters() {
        ColorMatrix grayscaleMatrix = new ColorMatrix();
        grayscaleMatrix.setSaturation(0);
        grayscaleFilter = new ColorMatrixColorFilter(grayscaleMatrix);

        ColorMatrix grayscaleInvertMatrix = new ColorMatrix();
        grayscaleInvertMatrix.setSaturation(0);
        grayscaleInvertMatrix.postConcat(new ColorMatrix(INVERT_MATRIX));
        grayscaleInvertFilter = new ColorMatrixColorFilter(grayscaleInvertMatrix);

        invertFilter = new ColorMatrixColorFilter(INVERT_MATRIX);
    }

    @Override
    public void destroy() {
        this.canvas = null;
    }

    @Override
    public void drawBitmap(Bitmap bitmap, int left, int top) {
        android.graphics.Bitmap androidBitmap = AndroidGraphicFactory.getBitmap(bitmap);
        if (AndroidGraphicFactory.MONO_ALPHA_BITMAP.equals(androidBitmap.getConfig())) {
            // we need to clear the existing alpha to get a clean overwrite
            canvas.drawColor(android.graphics.Color.argb(0, 0, 0, 0), PorterDuff.Mode.SRC);
        }
        this.canvas.drawBitmap(androidBitmap, left, top, bitmapPaint);
    }

    @Override
    public void drawBitmap(Bitmap bitmap, int left, int top, float alpha, Filter filter) {
        int oldAlpha = this.bitmapPaint.getAlpha();
        if (alpha != 1) {
            this.bitmapPaint.setAlpha((int) (alpha * 255));
        }
        applyFilter(filter);
        this.canvas.drawBitmap(AndroidGraphicFactory.getBitmap(bitmap), left, top, bitmapPaint);
        if (filter != Filter.NONE) {
            bitmapPaint.setColorFilter(null);
        }
        if (alpha != 1) {
            this.bitmapPaint.setAlpha(oldAlpha);
        }
    }

    @Override
    public void drawBitmap(Bitmap bitmap, Matrix matrix) {
        this.canvas.drawBitmap(AndroidGraphicFactory.getBitmap(bitmap), AndroidGraphicFactory.getMatrix(matrix), bitmapPaint);
    }

    @Override
    public void drawBitmap(Bitmap bitmap, Matrix matrix, float alpha, Filter filter) {
        int oldAlpha = this.bitmapPaint.getAlpha();
        if (alpha != 1) {
            this.bitmapPaint.setAlpha((int) (alpha * 255));
        }
        applyFilter(filter);
        this.canvas.drawBitmap(AndroidGraphicFactory.getBitmap(bitmap), AndroidGraphicFactory.getMatrix(matrix), bitmapPaint);
        if (filter != Filter.NONE) {
            bitmapPaint.setColorFilter(null);
        }
        if (alpha != 1) {
            this.bitmapPaint.setAlpha(oldAlpha);
        }
    }

    @Override
    public void drawBitmap(Bitmap bitmap, int srcLeft, int srcTop, int srcRight, int srcBottom,
                           int dstLeft, int dstTop, int dstRight, int dstBottom) {
        this.canvas.drawBitmap(AndroidGraphicFactory.getBitmap(bitmap),
                new Rect(srcLeft, srcTop, srcRight, srcBottom),
                new Rect(dstLeft, dstTop, dstRight, dstBottom),
                this.bitmapPaint);
    }

    @Override
    public void drawBitmap(Bitmap bitmap, int srcLeft, int srcTop, int srcRight, int srcBottom,
                           int dstLeft, int dstTop, int dstRight, int dstBottom, float alpha, Filter filter) {
        int oldAlpha = this.bitmapPaint.getAlpha();
        if (alpha != 1) {
            this.bitmapPaint.setAlpha((int) (alpha * 255));
        }
        applyFilter(filter);
        this.canvas.drawBitmap(AndroidGraphicFactory.getBitmap(bitmap),
                new Rect(srcLeft, srcTop, srcRight, srcBottom),
                new Rect(dstLeft, dstTop, dstRight, dstBottom),
                this.bitmapPaint);
        if (filter != Filter.NONE) {
            this.bitmapPaint.setColorFilter(null);
        }
        if (alpha != 1) {
            this.bitmapPaint.setAlpha(oldAlpha);
        }
    }

    @Override
    public void drawCircle(int x, int y, int radius, Paint paint) {
        if (paint.isTransparent()) {
            return;
        }
        this.canvas.drawCircle(x, y, radius, AndroidGraphicFactory.getPaint(paint));
    }

    @Override
    public void drawLine(int x1, int y1, int x2, int y2, Paint paint) {
        if (paint.isTransparent()) {
            return;
        }

        this.canvas.drawLine(x1, y1, x2, y2, AndroidGraphicFactory.getPaint(paint));
    }

    @Override
    public void drawPath(Path path, Paint paint) {
        if (paint.isTransparent()) {
            return;
        }
        this.canvas.drawPath(AndroidGraphicFactory.getPath(path), AndroidGraphicFactory.getPaint(paint));
    }

    @Override
    public void drawPathText(String text, Path path, Paint paint) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        if (paint.isTransparent()) {
            return;
        }

        android.graphics.Paint androidPaint = AndroidGraphicFactory.getPaint(paint);
        // Way text container was made larger by text height
        this.canvas.drawTextOnPath(text, AndroidGraphicFactory.getPath(path), 0, androidPaint.getTextSize() / 4, androidPaint);
    }

    @Override
    public void drawText(String text, int x, int y, Paint paint) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        if (paint.isTransparent()) {
            return;
        }
        this.canvas.drawText(text, x, y, AndroidGraphicFactory.getPaint(paint));
    }

    @Override
    public void drawTextRotated(String text, int x1, int y1, int x2, int y2, Paint paint) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        if (paint.isTransparent()) {
            return;
        }

        android.graphics.Path path = new android.graphics.Path();
        path.moveTo(x1, y1);
        path.lineTo(x2, y2);
        this.canvas.drawTextOnPath(text, path, 0, 3, AndroidGraphicFactory.getPaint(paint));
    }

    @Override
    public void fillColor(Color color) {
        fillColor(AndroidGraphicFactory.getColor(color));
    }

    @Override
    public void fillColor(int color) {
        int alpha = (color >> 24) & 0xff;
        this.canvas.drawColor(color, alpha == 0 ? PorterDuff.Mode.CLEAR : PorterDuff.Mode.SRC_OVER);
    }

    @Override
    public Dimension getDimension() {
        return new Dimension(getWidth(), getHeight());
    }

    @Override
    public int getHeight() {
        return this.canvas.getHeight();
    }

    @Override
    public int getWidth() {
        return this.canvas.getWidth();
    }

    @Override
    public boolean isAntiAlias() {
        return this.bitmapPaint.isAntiAlias();
    }

    @Override
    public boolean isFilterBitmap() {
        return this.bitmapPaint.isFilterBitmap();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void resetClip() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            this.canvas.clipRect(0, 0, getWidth(), getHeight(), Region.Op.REPLACE);
        }
    }

    @Override
    public void setAntiAlias(boolean aa) {
        this.bitmapPaint.setAntiAlias(aa);
    }

    @Override
    public void setBitmap(Bitmap bitmap) {
        this.canvas.setBitmap(AndroidGraphicFactory.getBitmap(bitmap));
    }

    @Override
    public void setClip(int left, int top, int width, int height) {
        setClip(left, top, width, height, false);
    }

    @Override
    public void setClip(int left, int top, int width, int height, boolean intersect) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (intersect) {
                this.canvas.clipRect(left, top, left + width, top + height);
            }
        } else {
            this.setClipInternal(left, top, width, height, Region.Op.REPLACE);
        }
    }

    @Override
    public void setClipDifference(int left, int top, int width, int height) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.canvas.clipOutRect(left, top, left + width, top + height);
        } else {
            this.setClipInternal(left, top, width, height, Region.Op.DIFFERENCE);
        }
    }

    @SuppressWarnings("deprecation")
    private void setClipInternal(int left, int top, int width, int height, Region.Op op) {
        this.canvas.clipRect(left, top, left + width, top + height, op);
    }

    @Override
    public void setFilterBitmap(boolean filter) {
        this.bitmapPaint.setFilterBitmap(filter);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void shadeBitmap(Bitmap bitmap, Rectangle hillRect, Rectangle tileRect, float magnitude) {
        this.canvas.save();
        final HilshadingTemps temps;
        if (this.hillshadingTemps == null) {
            this.hillshadingTemps = new HilshadingTemps();
        }
        temps = this.hillshadingTemps;

        android.graphics.Paint shadePaint = hillshadingTemps.useAlphaPaint((int) (255 * magnitude));

        if (bitmap == null) {
            if (tileRect != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    this.canvas.clipRect((float) tileRect.left, (float) tileRect.top, (float) tileRect.right, (float) tileRect.bottom);
                } else {
                    this.canvas.clipRect((float) tileRect.left, (float) tileRect.top, (float) tileRect.right, (float) tileRect.bottom, Region.Op.REPLACE);
                }
            }
            // scale a dummy pixel over the canvas - just drawing a paint would probably be faster, but the resulting colors can be inconsistent with the bitmap draw (maybe only on some devices?)
            this.canvas.drawBitmap(hillshadingTemps.useNeutralShadingPixel(), hillshadingTemps.useAsr(0, 0, 1, 1), hillshadingTemps.useAdr(0, 0, canvas.getWidth(), canvas.getHeight()), shadePaint);

            this.canvas.restore();
            return;
        }

        android.graphics.Bitmap hillsBitmap = AndroidGraphicFactory.getBitmap(bitmap);
        double horizontalScale = tileRect.getWidth() / hillRect.getWidth();
        double verticalScale = tileRect.getHeight() / hillRect.getHeight();

        if (horizontalScale < 1 && verticalScale < 1) {
            // fast path for wide zoom (downscaling)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                this.canvas.clipRect((float) tileRect.left, (float) tileRect.top, (float) tileRect.right, (float) tileRect.bottom);
            } else {
                this.canvas.clipRect((float) tileRect.left, (float) tileRect.top, (float) tileRect.right, (float) tileRect.bottom, Region.Op.REPLACE);
            }
            android.graphics.Matrix transform = temps.useMatrix();
            transform.preTranslate((float) tileRect.left, (float) tileRect.top);
            transform.preScale((float) horizontalScale, (float) verticalScale);
            transform.preTranslate((float) -hillRect.left, (float) -hillRect.top);
            this.canvas.drawBitmap(hillsBitmap, transform, shadePaint);
        } else {

            double leftRestUnlimited = 1 + (hillRect.left - Math.floor(hillRect.left));
            double leftRest = Math.min(hillRect.left, leftRestUnlimited);
            double leftExtra = horizontalScale * leftRest;

            double rightRestUnlimited = Math.floor(hillRect.right) + 2 - hillRect.right;
            double rightRest = Math.min(bitmap.getWidth() - hillRect.right, rightRestUnlimited);
            double rightExtra = horizontalScale * rightRest;

            double tempWidthDouble = rightExtra + leftExtra + (hillRect.right - hillRect.left) * horizontalScale;
            int tempWidth = (int) Math.ceil(tempWidthDouble);


            double topRestUnlimited = 1 + (hillRect.top - Math.floor(hillRect.top));
            double topRest = Math.min(hillRect.top, topRestUnlimited);
            double topExtra = verticalScale * topRest;

            double bottomRestUnlimited = Math.floor(hillRect.bottom) + 2 - hillRect.bottom;
            double bottomRest = Math.min(bitmap.getHeight() - hillRect.bottom, bottomRestUnlimited);
            double bottomExtra = verticalScale * bottomRest;

            double tempHeightDouble = bottomExtra + topExtra + (hillRect.bottom - hillRect.top) * verticalScale;
            int tempHeight = (int) Math.ceil(tempHeightDouble);

            int srcLeft = (int) Math.round(hillRect.left - leftRest);
            int srcTop = (int) Math.round(hillRect.top - topRest);
            int srcRight = (int) Math.round(hillRect.right + rightRest);
            int srcBottom = (int) Math.round(hillRect.bottom + bottomRest);

            android.graphics.Canvas tempCanvas = temps.useCanvas();

            final android.graphics.Bitmap sourceImage;
            if (srcLeft == 0 && srcTop == 0) {
                // special handling for an inconsistency in android where rect->rect drawImage upscaling is unfiltered if source top,left is 0,0
                // (seems to be shortcutting to a different implementation, observed on sony)

                android.graphics.Bitmap shiftedTemp = android.graphics.Bitmap.createBitmap(srcRight + 1, srcBottom, hillsBitmap.getConfig());
                tempCanvas.setBitmap(shiftedTemp);
                tempCanvas.drawBitmap(hillsBitmap, 1, 0, null);


                sourceImage = shiftedTemp;

                srcLeft += 1;
                srcRight += 1;
            } else {
                sourceImage = hillsBitmap;
            }


            Rect asr = temps.useAsr(
                    srcLeft,
                    srcTop,
                    srcRight,
                    srcBottom
            );
            Rect adr = temps.useAdr(
                    0,
                    0,
                    tempWidth,
                    tempHeight
            );

            android.graphics.Bitmap scaleTemp = temps.useScaleBitmap(tempWidth, tempHeight, hillsBitmap.getConfig());
            tempCanvas.setBitmap(scaleTemp);
            tempCanvas.drawBitmap(sourceImage, asr, adr, bitmapPaint);


            this.canvas.clipRect((float) tileRect.left, (float) tileRect.top, (float) tileRect.right, (float) tileRect.bottom);
            int drawOffsetLeft = (int) Math.round((tileRect.left - leftExtra));
            int drawOffsetTop = (int) Math.round((tileRect.top - topExtra));

            this.canvas.drawBitmap(scaleTemp, drawOffsetLeft, drawOffsetTop, shadePaint);
        }
        this.canvas.restore();
    }

    private static class HilshadingTemps {
        private final Rect asr = new Rect(0, 0, 0, 0);
        private final Rect adr = new Rect(0, 0, 0, 0);
        private final android.graphics.Canvas tmpCanvas = new android.graphics.Canvas();
        private android.graphics.Bitmap scaleTemp;
        private android.graphics.Bitmap shiftTemp;

        private final android.graphics.Paint shadePaint;
        private android.graphics.Bitmap neutralShadingPixel = AndroidGraphicFactory.INSTANCE.createMonoBitmap(1, 1, new byte[]{(byte) (127 & 0xFF)}, 0, null).bitmap;
        private android.graphics.Matrix tmpMatrix;

        private HilshadingTemps() {
            shadePaint = new android.graphics.Paint();

            shadePaint.setAntiAlias(true);
            shadePaint.setFilterBitmap(true);
        }

        Rect useAsr(int srcLeft, int srcTop, int srcRight, int srcBottom) {
            asr.left = srcLeft;
            asr.top = srcTop;
            asr.right = srcRight;
            asr.bottom = srcBottom;
            return asr;
        }

        Rect useAdr(int destLeft, int destTop, int destRight, int destBottom) {
            adr.left = destLeft;
            adr.top = destTop;
            adr.right = destRight;
            adr.bottom = destBottom;
            return adr;
        }

        /**
         * returns a temporary canvas that may be used in useScaleBitmap
         */
        android.graphics.Canvas useCanvas() {
            return tmpCanvas;
        }

        /**
         * returns a reuseable bitmap of size or larger and sets it for the temp canvas
         * (some internal operations use the canvas, setting it all the time makes this more uniform)
         */
        android.graphics.Bitmap useScaleBitmap(int tempWidth, int tempHeight, android.graphics.Bitmap.Config config) {
            scaleTemp = internalUseBitmap(scaleTemp, tempWidth, tempHeight, config);
            return scaleTemp;
        }

        /**
         * returns a reuseable bitmap of size or larger and sets it for the temp canvas
         * (some internal operations use the canvas, setting it all the time makes this more uniform)
         */
        android.graphics.Bitmap useShiftBitmap(int tempWidth, int tempHeight, android.graphics.Bitmap.Config config) {
            shiftTemp = internalUseBitmap(shiftTemp, tempWidth, tempHeight, config);
            return shiftTemp;
        }

        private android.graphics.Bitmap internalUseBitmap(android.graphics.Bitmap tmpBitmap, int tempWidth, int tempHeight, android.graphics.Bitmap.Config config) {
            if (tmpBitmap == null) {
                tmpBitmap = android.graphics.Bitmap.createBitmap(tempWidth, tempHeight, config);
                tmpCanvas.setBitmap(tmpBitmap);
            } else {
                if (tmpBitmap.getWidth() < tempWidth || tmpBitmap.getHeight() < tempHeight || !tmpBitmap.getConfig().equals(config)) {
                    tmpBitmap.recycle();
                    tmpBitmap = android.graphics.Bitmap.createBitmap(tempWidth, tempHeight, config);
                    tmpCanvas.setBitmap(tmpBitmap);
                } else {
                    tmpCanvas.setBitmap(tmpBitmap);
                    tmpCanvas.drawColor(android.graphics.Color.argb(0, 0, 0, 0), PorterDuff.Mode.SRC);
                }
            }
            return tmpBitmap;
        }

        android.graphics.Paint useAlphaPaint(int alpha) {
            shadePaint.setAlpha(alpha);
            return shadePaint;
        }

        android.graphics.Bitmap useNeutralShadingPixel() {
            return neutralShadingPixel;
        }

        android.graphics.Matrix useMatrix() {
            if (tmpMatrix == null) {
                tmpMatrix = new android.graphics.Matrix();
            }
            tmpMatrix.reset();
            return tmpMatrix;
        }
    }
}
