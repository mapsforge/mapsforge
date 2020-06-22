/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2014-2020 devemux86
 * Copyright 2017 usrusr
 * Copyright 2018 Adrian Batzill
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

import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap.Config;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.TextUtils;
import org.mapsforge.core.graphics.*;
import org.mapsforge.core.mapelements.PointTextContainer;
import org.mapsforge.core.mapelements.SymbolContainer;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.model.DisplayModel;

import java.io.*;
import java.nio.Buffer;
import java.nio.ByteBuffer;

public final class AndroidGraphicFactory implements GraphicFactory {

    // turn on for bitmap accounting
    public static final boolean DEBUG_BITMAPS = false;

    /**
     * Constructor simply for IDE layout designers!
     */
    public static AndroidGraphicFactory INSTANCE = new AndroidGraphicFactory(null);

    // if true RESOURCE_BITMAPS will be kept in the cache to avoid
    // multiple loading
    public static final boolean KEEP_RESOURCE_BITMAPS = true;
    public static final Config NON_TRANSPARENT_BITMAP = Config.RGB_565;

    // determines size of bitmaps used, RGB_565 is 2 bytes per pixel
    // while ARGB_8888 uses 4 bytes per pixel (with severe impact
    // on memory use) and allows transparencies. Use ARGB_8888 whenever
    // you have transparencies in any of the bitmaps. ARGB_4444 is deprecated
    // and is much slower despite smaller size that ARGB_8888 as it
    // passes through unoptimized path in the skia library.
    public static final Config TRANSPARENT_BITMAP = Config.ARGB_8888;


    public static final Config MONO_ALPHA_BITMAP = Config.ALPHA_8;

    public static android.graphics.Bitmap convertToAndroidBitmap(Drawable drawable) {
        android.graphics.Bitmap bitmap;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P && drawable instanceof BitmapDrawable) {
            bitmap = ((BitmapDrawable) drawable).getBitmap();
        } else {
            int width = drawable.getIntrinsicWidth();
            int height = drawable.getIntrinsicHeight();
            bitmap = android.graphics.Bitmap.createBitmap(width, height, AndroidGraphicFactory.TRANSPARENT_BITMAP);
            android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);

            Rect rect = drawable.getBounds();
            drawable.setBounds(0, 0, width, height);
            drawable.draw(canvas);
            drawable.setBounds(rect);
        }

        return bitmap;
    }

    public static Bitmap convertToBitmap(Drawable drawable) {
        return new AndroidBitmap(AndroidGraphicFactory.convertToAndroidBitmap(drawable));
    }

    public static Bitmap convertToBitmap(Drawable drawable, android.graphics.Paint paint) {
        android.graphics.Bitmap immutable = AndroidGraphicFactory.convertToAndroidBitmap(drawable);
        android.graphics.Bitmap mutable = immutable.copy(AndroidGraphicFactory.TRANSPARENT_BITMAP, true);
        android.graphics.Canvas canvas = new android.graphics.Canvas(mutable);

        canvas.drawBitmap(mutable, 0, 0, paint);

        return new AndroidBitmap(mutable);
    }

    public static Canvas createGraphicContext(android.graphics.Canvas canvas) {
        return new AndroidCanvas(canvas);
    }

    public static void createInstance(Application app) {
        INSTANCE = new AndroidGraphicFactory(app);
    }

    /**
     * return the byte usage per pixel of a bitmap based on its configuration.
     */
    public static int getBytesPerPixel(Config config) {
        if (config == Config.ARGB_8888) {
            return 4;
        } else if (config == Config.RGB_565) {
            return 2;
        } else if (config == Config.ARGB_4444) {
            return 2;
        } else if (config == Config.ALPHA_8) {
            return 1;
        }
        return 1;
    }

    public static android.graphics.Canvas getCanvas(Canvas canvas) {
        return ((AndroidCanvas) canvas).canvas;
    }

    public static android.graphics.Paint getPaint(Paint paint) {
        return ((AndroidPaint) paint).paint;
    }

    public static android.graphics.Bitmap getBitmap(Bitmap bitmap) {
        return ((AndroidBitmap) bitmap).bitmap;
    }

    static int getColor(Color color) {
        switch (color) {
            case BLACK:
                return android.graphics.Color.BLACK;
            case BLUE:
                return android.graphics.Color.BLUE;
            case GREEN:
                return android.graphics.Color.GREEN;
            case RED:
                return android.graphics.Color.RED;
            case TRANSPARENT:
                return android.graphics.Color.TRANSPARENT;
            case WHITE:
                return android.graphics.Color.WHITE;
        }

        throw new IllegalArgumentException("unknown color: " + color);
    }

    static android.graphics.Matrix getMatrix(Matrix matrix) {
        return ((AndroidMatrix) matrix).matrix;
    }

    static android.graphics.Path getPath(Path path) {
        return ((AndroidPath) path).path;
    }

    private final Application application;
    private File svgCacheDir;

    private AndroidGraphicFactory(Application app) {
        this.application = app;
        if (app != null) {
            // the density is an approximate scale factor for the device
            DisplayModel.setDeviceScaleFactor(app.getResources().getDisplayMetrics().density);
        }
    }

    public static void clearResourceFileCache() {
        AndroidSvgBitmapStore.clear();
    }

    public static void clearResourceMemoryCache() {
        AndroidResourceBitmap.clearResourceBitmaps();
    }

    @Override
    public Bitmap createBitmap(int width, int height) {
        return new AndroidBitmap(width, height, TRANSPARENT_BITMAP);
    }

    @Override
    public Bitmap createBitmap(int width, int height, boolean isTransparent) {
        if (isTransparent) {
            return new AndroidBitmap(width, height, TRANSPARENT_BITMAP);
        }
        return new AndroidBitmap(width, height, NON_TRANSPARENT_BITMAP);
    }

    @Override
    public Canvas createCanvas() {
        return new AndroidCanvas();
    }

    @Override
    public int createColor(Color color) {
        return getColor(color);
    }

    @Override
    public int createColor(int alpha, int red, int green, int blue) {
        return android.graphics.Color.argb(alpha, red, green, blue);
    }

    @Override
    public Matrix createMatrix() {
        return new AndroidMatrix();
    }

    @Override
    public AndroidHillshadingBitmap createMonoBitmap(int width, int height, byte[] buffer, int padding, BoundingBox area) {
        AndroidHillshadingBitmap androidBitmap = new AndroidHillshadingBitmap(width + 2 * padding, height + 2 * padding, padding, area);
        if (buffer != null) {
            Buffer b = ByteBuffer.wrap(buffer);
            androidBitmap.bitmap.copyPixelsFromBuffer(b);
        }
        return androidBitmap;
    }

    @Override
    public Paint createPaint() {
        return new AndroidPaint();
    }

    @Override
    public Paint createPaint(Paint paint) {
        return new AndroidPaint(paint);
    }


    @Override
    public Path createPath() {
        return new AndroidPath();
    }

    @Override
    public PointTextContainer createPointTextContainer(Point xy, Display display, int priority, String text, Paint paintFront, Paint paintBack,
                                                       SymbolContainer symbolContainer, Position position, int maxTextWidth) {
        return new AndroidPointTextContainer(xy, display, priority, text, paintFront, paintBack, symbolContainer, position, maxTextWidth);
    }

    @Override
    public ResourceBitmap createResourceBitmap(InputStream inputStream, float scaleFactor, int width, int height, int percent, int hash) throws IOException {
        return new AndroidResourceBitmap(inputStream, scaleFactor, width, height, percent, hash);
    }

    @Override
    public TileBitmap createTileBitmap(InputStream inputStream, int tileSize, boolean isTransparent) {
        return new AndroidTileBitmap(inputStream, tileSize, isTransparent);
    }

    @Override
    public TileBitmap createTileBitmap(int tileSize, boolean isTransparent) {
        return new AndroidTileBitmap(tileSize, isTransparent);
    }


    /*
     * Android method accessible only via Context.
     */
    public boolean deleteFile(String name) {
        if (this.svgCacheDir != null) {
            return new File(this.svgCacheDir, name).delete();
        }
        return this.application.deleteFile(name);
    }

    /*
     * Android method accessible only via Context.
     */
    public String[] fileList() {
        if (this.svgCacheDir != null) {
            return this.svgCacheDir.list();
        }
        return this.application.fileList();
    }

    /*
     * Android method accessible only via Context.
     */
    public FileInputStream openFileInput(String name) throws FileNotFoundException {
        if (this.svgCacheDir != null) {
            return new FileInputStream(new File(this.svgCacheDir, name));
        }
        return this.application.openFileInput(name);
    }

    /*
     * Android method accessible only via Context.
     */
    public FileOutputStream openFileOutput(String name, int mode) throws FileNotFoundException {
        if (this.svgCacheDir != null) {
            return new FileOutputStream(new File(this.svgCacheDir, name), mode == Context.MODE_APPEND);
        }
        return this.application.openFileOutput(name, mode);
    }

    @Override
    public InputStream platformSpecificSources(String relativePathPrefix, String src) throws IOException {
        // this allows loading of resource bitmaps from the Android assets folder
        String pathName = (TextUtils.isEmpty(relativePathPrefix) ? "" : relativePathPrefix) + src;
        try {
            return this.application.getAssets().open(pathName);
        } catch (IOException e) {
            throw new FileNotFoundException("invalid resource: " + pathName);
        }
    }

    @Override
    public ResourceBitmap renderSvg(InputStream inputStream, float scaleFactor, int width, int height, int percent, int hash) throws IOException {
        return new AndroidSvgBitmap(inputStream, hash, scaleFactor, width, height, percent);
    }

    public void setSvgCacheDir(File svgCacheDir) {
        this.svgCacheDir = svgCacheDir;
    }
}
