/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2014-2019 devemux86
 * Copyright 2017 usrusr
 * Copyright 2019 cpt1gl0
 * Copyright 2019 Adrian Batzill
 * Copyright 2019 mg4gh
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
package org.mapsforge.map.android.graphics;

import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.Build;
import org.mapsforge.core.graphics.*;
import org.mapsforge.core.model.Dimension;
import org.mapsforge.core.model.Rectangle;
import org.mapsforge.core.model.Rotation;

class AndroidCanvas implements Canvas {

    android.graphics.Canvas canvas;
    protected final android.graphics.Paint bitmapPaint = new android.graphics.Paint();
    protected final android.graphics.Paint shadePaint = new android.graphics.Paint();
    protected final android.graphics.Matrix tmpMatrix = new android.graphics.Matrix();

    /**
     * A set of reusable temporaries that is not needed when hillshading is inactive.
     */
    protected HillshadingTemps hillshadingTemps = null;

    AndroidCanvas() {
        this(new android.graphics.Canvas());
    }

    AndroidCanvas(android.graphics.Canvas canvas) {
        this.canvas = canvas;

        this.bitmapPaint.setAntiAlias(true);
        this.bitmapPaint.setFilterBitmap(true);

        shadePaint.setAntiAlias(true);
        shadePaint.setFilterBitmap(true);
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
    public void drawBitmap(Bitmap bitmap, int left, int top, float alpha) {
        int oldAlpha = this.bitmapPaint.getAlpha();
        if (alpha != 1) {
            this.bitmapPaint.setAlpha((int) (alpha * 255));
        }
        this.canvas.drawBitmap(AndroidGraphicFactory.getBitmap(bitmap), left, top, bitmapPaint);
        if (alpha != 1) {
            this.bitmapPaint.setAlpha(oldAlpha);
        }
    }

    @Override
    public void drawBitmap(Bitmap bitmap, Matrix matrix) {
        this.canvas.drawBitmap(AndroidGraphicFactory.getBitmap(bitmap), AndroidGraphicFactory.getMatrix(matrix), bitmapPaint);
    }

    @Override
    public void drawBitmap(Bitmap bitmap, Matrix matrix, float alpha) {
        int oldAlpha = this.bitmapPaint.getAlpha();
        if (alpha != 1) {
            this.bitmapPaint.setAlpha((int) (alpha * 255));
        }
        this.canvas.drawBitmap(AndroidGraphicFactory.getBitmap(bitmap), AndroidGraphicFactory.getMatrix(matrix), bitmapPaint);
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
                           int dstLeft, int dstTop, int dstRight, int dstBottom, float alpha) {
        int oldAlpha = this.bitmapPaint.getAlpha();
        if (alpha != 1) {
            this.bitmapPaint.setAlpha((int) (alpha * 255));
        }
        this.canvas.drawBitmap(AndroidGraphicFactory.getBitmap(bitmap),
                new Rect(srcLeft, srcTop, srcRight, srcBottom),
                new Rect(dstLeft, dstTop, dstRight, dstBottom),
                this.bitmapPaint);
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
    public void restore() {
        this.canvas.restore();
    }

    @Override
    public void rotate(float degrees, float px, float py) {
        if (degrees != 0) {
            this.canvas.rotate(degrees, px, py);
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
        this.canvas.save();
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
    public void setBitmap(Bitmap bitmap, float dx, float dy, float degrees, float px, float py) {
        setBitmap(bitmap);
        android.graphics.Matrix matrix = new android.graphics.Matrix();
        if (dx != 0 || dy != 0) {
            matrix.preTranslate(dx, dy);
        }
        if (degrees != 0) {
            matrix.preRotate(degrees, px, py);
        }
        this.canvas.setMatrix(matrix);
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
    public void shadeBitmap(Bitmap bitmap, Rectangle shadeRect, Rectangle tileRect, float magnitude) {
        this.canvas.save();

        shadePaint.setAlpha((int) (255 * magnitude));

        if (bitmap != null && tileRect != null) {
            final android.graphics.Bitmap hillsBitmap = AndroidGraphicFactory.getBitmap(bitmap);

            if (shadeRect.getWidth() != 0 && shadeRect.getHeight() != 0) {
                final double horizontalScale = tileRect.getWidth() / shadeRect.getWidth();
                final double verticalScale = tileRect.getHeight() / shadeRect.getHeight();

                final android.graphics.Matrix transform = tmpMatrix;
                transform.reset();

                // (2024-10) On some other systems (see AwtCanvas), a scaling transform with large factors would cause the entire hill shading bitmap to be upscaled,
                // thus wasting large amounts of memory and CPU time when only a small part of the bitmap is really needed. This is especially prominent on
                // larger zoom levels when horizontalScale and verticalScale become very large.
                // It turns out that Android is not susceptible to this problem, so sub-image paradigm was not implemented (at least not for Android Oreo+).
                transform.preTranslate((float) tileRect.left, (float) tileRect.top);
                transform.preScale((float) horizontalScale, (float) verticalScale);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    transform.preTranslate((float) -shadeRect.left, (float) -shadeRect.top);

                    this.canvas.clipRect((float) tileRect.left, (float) tileRect.top, (float) tileRect.right, (float) tileRect.bottom);
                    this.canvas.drawBitmap(hillsBitmap, transform, shadePaint);
                } else {
                    // Using a rectangle slightly larger than necessary to prevent resize artifacts
                    final int srcLeft = Math.max(0, (int) shadeRect.left - 1);
                    final int srcTop = Math.max(0, (int) shadeRect.top - 1);
                    final int srcWidth = Math.min(hillsBitmap.getWidth() - srcLeft, (int) shadeRect.getWidth() + 4);
                    final int srcHeight = Math.min(hillsBitmap.getHeight() - srcTop, (int) shadeRect.getHeight() + 4);

                    final android.graphics.Bitmap subImageArgb;
                    {
                        final android.graphics.Bitmap subImage = android.graphics.Bitmap.createBitmap(hillsBitmap, srcLeft, srcTop, srcWidth, srcHeight);

                        if (!android.graphics.Bitmap.Config.ARGB_8888.equals(subImage.getConfig())) {
                            // We need to copy the original bitmap to the ARGB configuration, otherwise the drawn bitmap will not be filtered
                            subImageArgb = subImage.copy(android.graphics.Bitmap.Config.ARGB_8888, false);
                        } else {
                            subImageArgb = subImage;
                        }
                    }

                    transform.preTranslate((float) -(shadeRect.left - srcLeft), (float) -(shadeRect.top - srcTop));

                    this.canvas.clipRect((float) tileRect.left, (float) tileRect.top, (float) tileRect.right, (float) tileRect.bottom, Region.Op.REPLACE);
                    this.canvas.drawBitmap(subImageArgb, transform, shadePaint);
                }
            }

            // (2024-10) An old workaround that doesn't seem to be needed anymore (tested on Sony Xperia).
//            final android.graphics.Bitmap sourceImage;
//            if (srcLeft == 0 && srcTop == 0) {
//                // special handling for an inconsistency in android where rect->rect drawImage upscaling is unfiltered if source top,left is 0,0
//                // (seems to be shortcutting to a different implementation, observed on sony)
//
//                android.graphics.Bitmap shiftedTemp = android.graphics.Bitmap.createBitmap(srcRight + 1, srcBottom, hillsBitmap.getConfig());
//                tempCanvas.setBitmap(shiftedTemp);
//                tempCanvas.drawBitmap(hillsBitmap, 1, 0, null);
//
//
//                sourceImage = shiftedTemp;
//
//                srcLeft += 1;
//                srcRight += 1;
//            } else {
//                sourceImage = hillsBitmap;
//            }

        } else {
            if (this.hillshadingTemps == null) {
                this.hillshadingTemps = new HillshadingTemps();
            }

            if (tileRect != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    this.canvas.clipRect((float) tileRect.left, (float) tileRect.top, (float) tileRect.right, (float) tileRect.bottom);
                } else {
                    this.canvas.clipRect((float) tileRect.left, (float) tileRect.top, (float) tileRect.right, (float) tileRect.bottom, Region.Op.REPLACE);
                }
            }

            // scale a dummy pixel over the canvas - just drawing a paint would probably be faster, but the resulting colors can be inconsistent with the bitmap draw (maybe only on some devices?)
            this.canvas.drawBitmap(hillshadingTemps.useNeutralShadingPixel(), hillshadingTemps.useAsr(0, 0, 1, 1), hillshadingTemps.useAdr(0, 0, canvas.getWidth(), canvas.getHeight()), shadePaint);
        }

        this.canvas.restore();
    }

    @Override
    public void translate(float dx, float dy) {
        this.canvas.translate(dx, dy);
    }

    protected static class HillshadingTemps {
        private final Rect asr = new Rect(0, 0, 0, 0);
        private final Rect adr = new Rect(0, 0, 0, 0);

        private final android.graphics.Bitmap neutralShadingPixel = AndroidGraphicFactory.INSTANCE.createMonoBitmap(1, 1, new byte[]{0}, 0, null).bitmap;

        private HillshadingTemps() {
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

        android.graphics.Bitmap useNeutralShadingPixel() {
            return neutralShadingPixel;
        }
    }
}
