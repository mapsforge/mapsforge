/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
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
package org.mapsforge.map.android.graphics;

import android.content.Context;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.Build;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.Matrix4f;
import android.renderscript.RSRuntimeException;
import android.renderscript.RenderScript;
import android.renderscript.Sampler;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.Filter;
import org.mapsforge.core.graphics.Matrix;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Path;
import org.mapsforge.core.model.Dimension;
import org.mapsforge.core.model.Rectangle;
import org.mapsforge.map.android.ScriptC_hillshading;

import java.util.HashMap;
import java.util.Map;

class AndroidCanvas implements Canvas {
    private static final float[] INVERT_MATRIX = {
            -1, 0, 0, 0, 255,
            0, -1, 0, 0, 255,
            0, 0, -1, 0, 255,
            0, 0, 0, 1, 0
    };
    private Context context;
    private RenderScript rs;
    private Access565_ScriptC_hillshading hillshade;
    private android.graphics.Bitmap paintingOn;
    private Allocation paintingOnAllocation;
    private android.graphics.Bitmap paintingOnTempBitmap;


    android.graphics.Canvas canvas;
    private final android.graphics.Paint bitmapPaint = new android.graphics.Paint();
    private final android.graphics.Paint shadePaint = new android.graphics.Paint();
    private ColorFilter grayscaleFilter, grayscaleInvertFilter, invertFilter;

    AndroidCanvas(Context context) {
        this.context = context;
        this.canvas = new android.graphics.Canvas();

        this.bitmapPaint.setAntiAlias(true);
        this.bitmapPaint.setFilterBitmap(true);

        this.shadePaint.setAntiAlias(true);
        this.shadePaint.setFilterBitmap(true);

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
        if(rs!=null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                if(hillshade!=null) hillshade.destroy();
                rs.destroy();
            }
            rs=null;
        }
    }

    @Override
    public void drawBitmap(Bitmap bitmap, int left, int top) {
        this.canvas.drawBitmap(AndroidGraphicFactory.getBitmap(bitmap), left, top, bitmapPaint);
    }

    @Override
    public void drawBitmap(Bitmap bitmap, int left, int top, Filter filter) {
        applyFilter(filter);
        this.canvas.drawBitmap(AndroidGraphicFactory.getBitmap(bitmap), left, top, bitmapPaint);
        if (filter != Filter.NONE) {
            bitmapPaint.setColorFilter(null);
        }
    }

    @Override
    public void drawBitmap(Bitmap bitmap, Matrix matrix) {
        this.canvas.drawBitmap(AndroidGraphicFactory.getBitmap(bitmap), AndroidGraphicFactory.getMatrix(matrix), bitmapPaint);
    }

    @Override
    public void shadeBitmap(Bitmap bitmap, Rectangle hillRect, Rectangle tileRect, float magnitude) {
        shadePaint.setAlpha((int) (255 * magnitude));
        Rect atr = new Rect((int)hillRect.left, (int)hillRect.top, (int)hillRect.right, (int)hillRect.bottom);
        Rect asr = new Rect((int)tileRect.left, (int)tileRect.top, (int)tileRect.right, (int)tileRect.bottom);
        this.canvas.drawBitmap(AndroidGraphicFactory.getBitmap(bitmap), atr, asr, shadePaint);
    }

    @Override
    public void shadeBitmap(Bitmap bitmap, Matrix matrix, float magnitude) {
        if (
//                false &&
                Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
            if(paintingOn!=null) {
                if (rs == null) {
                    rs = RenderScript.create(context);
                    hillshade = new Access565_ScriptC_hillshading(rs);
                }
                if(paintingOnAllocation ==null) {
                    android.graphics.Bitmap.Config config = paintingOn.getConfig();

                    if(
//                            true||
                            config== android.graphics.Bitmap.Config.ARGB_8888){
                        paintingOnAllocation = Allocation.createFromBitmap(rs, paintingOn);
                    }else{
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                            paintingOnAllocation = Allocation.createFromBitmap(rs, paintingOn, Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SHARED);
                        }else{

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                                paintingOnAllocation = Allocation.createFromBitmap(rs, paintingOn);
                            }
                        }
                    }
                }

                android.graphics.Bitmap bitmap1 = AndroidGraphicFactory.getBitmap(bitmap);
                Allocation texture = Allocation.createFromBitmap(rs, bitmap1, Allocation.MipmapControl.MIPMAP_ON_SYNC_TO_TEXTURE, Allocation.USAGE_GRAPHICS_TEXTURE);
                Sampler sampler = Sampler.CLAMP_LINEAR_MIP_LINEAR(rs);

                hillshade.set_sampler(sampler);
                //matrix.scale(1.f/(bitmap.getWidth()*paintingOn.getWidth()), 1.f/(bitmap.getHeight()*paintingOn.getHeight()));

                android.graphics.Matrix androidMatrix = AndroidGraphicFactory.getMatrix(matrix);
                Matrix4f mx4f = new Matrix4f();
//                mx3f.scale(1.f/(bitmap.getWidth()*paintingOn.getWidth()), 1.f/(bitmap.getHeight()*paintingOn.getHeight()));
//                androidMatrix.getValues(mx4f.getArray());

                androidMatrix.postScale(1.f/(bitmap.getWidth()*paintingOn.getWidth()), 1.f/(bitmap.getHeight()*paintingOn.getHeight()));
//                androidMatrix.postScale(1.f/(bitmap.getWidth()*paintingOn.getWidth()), 1.f/(bitmap.getHeight()*paintingOn.getHeight()));
//                androidMatrix.preScale(1.f/(paintingOn.getWidth()), 1.f/(paintingOn.getHeight()));
                androidMatrix.preScale((paintingOn.getWidth()), (paintingOn.getHeight()));

                float[] array4x4 = mx4f.getArray();
                float[] array3x3 = new float[9];
                androidMatrix.getValues(array3x3);
//                androidMatrix.getValues(array3x3);
//                for(int x=0;x<3;x++){
//                    for(int y=0;y<3;y++){
//                        array4x4[(y*4)+x] = array3x3[y*3+x];
//                    }
//                }
//                array4x4[(y*4)+x] = array3x3[y*3+x];
                array4x4[0] = array3x3[0];
                array4x4[1] = array3x3[1];
                array4x4[3] = array3x3[2];
                array4x4[4] = array3x3[3];
                array4x4[5] = array3x3[4];
                array4x4[7] = array3x3[5];
                array4x4[10] = 1f;
                array4x4[12] = array3x3[6];
                array4x4[13] = array3x3[7];
                array4x4[15] = array3x3[8];

                hillshade.invoke_update(magnitude, mx4f, texture);
                if(paintingOnAllocation.getType().getElement().getBytesSize()==2){
                    hillshade.forEach_shade565_RGB_565(paintingOnAllocation, paintingOnAllocation);
                    rs.finish();
//                    paintingOnAllocation.syncAll(Allocation.USAGE_SCRIPT);
                    paintingOnAllocation.copyTo(paintingOn); // this is supposed to be a NOOP from API 18 on (because of USAGE_SHARED)

                }

                return;
            }
        }
        shadePaint.setAlpha((int) (255 * magnitude));
        this.canvas.drawBitmap(AndroidGraphicFactory.getBitmap(bitmap), AndroidGraphicFactory.getMatrix(matrix), shadePaint);
    }

    @Override
    public void drawBitmap(Bitmap bitmap, Matrix matrix, Filter filter) {
        applyFilter(filter);
        this.canvas.drawBitmap(AndroidGraphicFactory.getBitmap(bitmap), AndroidGraphicFactory.getMatrix(matrix), bitmapPaint);
        if (filter != Filter.NONE) {
            bitmapPaint.setColorFilter(null);
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
        this.canvas.drawColor(AndroidGraphicFactory.getColor(color), PorterDuff.Mode.CLEAR);
    }

    @Override
    public void fillColor(int color) {
        this.canvas.drawColor(color);
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
    public void resetClip() {
        this.canvas.clipRect(0, 0, getWidth(), getHeight(), Region.Op.REPLACE);
    }

    @Override
    public void setBitmap(Bitmap bitmap) {
        android.graphics.Bitmap androidBitmap = AndroidGraphicFactory.getBitmap(bitmap);
        this.paintingOn = androidBitmap;
        this.canvas.setBitmap(androidBitmap);
    }

    @Override
    public void setClip(int left, int top, int width, int height) {
        this.setClipInternal(left, top, width, height, Region.Op.REPLACE);
    }

    @Override
    public void setClipDifference(int left, int top, int width, int height) {
        this.setClipInternal(left, top, width, height, Region.Op.DIFFERENCE);
    }

    public void setClipInternal(int left, int top, int width, int height, Region.Op op) {
        this.canvas.clipRect(left, top, left + width, top + height, op);
    }

    /**
     * wrap the generated reflection class to make RGB_565 allocations pass checks
     */
    private static class Access565_ScriptC_hillshading extends ScriptC_hillshading {
        private final Element elementRgb565;

        public Access565_ScriptC_hillshading(RenderScript rs) {
            super(rs);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                this.elementRgb565 = Element.RGB_565(rs);
            }else{
                this.elementRgb565 = null;
            }
        }
        private Map<KernelID, Integer> knownIds = new HashMap<>();
        private Integer slotShade565 = null;
        public void forEach_shade565_RGB_565(Allocation ain, Allocation aout) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                Element inElement = ain.getElement();
                if( ! inElement.isCompatible(elementRgb565)) {
                    throw new RSRuntimeException("Type mismatch, !");
                }
                if(ain != aout) {
                    Element outElement = aout.getElement();
                    if( ! outElement.isCompatible(elementRgb565)) {
                        throw new RuntimeException();
                    }
                }
                if(slotShade565 ==null) {
                    KernelID kernelID = getKernelID_shade565();
                    slotShade565 = knownIds.get(kernelID);
                }

                super.forEach(slotShade565, ain, aout, null);
            }
        }

        @Override
        protected KernelID createKernelID(int slot, int sig, Element ein, Element eout) {
            KernelID kernelID = super.createKernelID(slot, sig, ein, eout);
            knownIds.put(kernelID, slot);
            return kernelID;
        }
    }

}
