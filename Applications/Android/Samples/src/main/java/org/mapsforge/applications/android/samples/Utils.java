/*
 * Copyright 2013-2014 Ludwig M Brinckmann
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
package org.mapsforge.applications.android.samples;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.View.MeasureSpec;


import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Dimension;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.overlay.Marker;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.rendertheme.XmlRenderTheme;

import java.io.File;

/**
 * Utility functions that can be used across different mapsforge based activities
 */
public final class Utils {
    private Utils() {
        throw new IllegalStateException();
    }

    /**
     * Compatibility method
     *
     * @param a the current activity
     */
    @TargetApi(11)
    public static void enableHome(Activity a) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // Show the Up button in the action bar.
            a.getActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * Compatibility method
     *
     * @param view       the view to set the background on
     * @param background the background
     */
    @TargetApi(16)
    public static void setBackground(View view, Drawable background) {
        if (android.os.Build.VERSION.SDK_INT >= 16) {
            view.setBackground(background);
        } else {
            view.setBackgroundDrawable(background);
        }
    }


    /*
     * @param tileCache the cache
     * @param mapViewPosition the position
     * @param layerManager the layer manager
     * @param mapFile the map file
     * @param renderTheme the render theme to use
     * @return the layer
     */
    static TileRendererLayer createTileRendererLayer(TileCache tileCache, MapViewPosition mapViewPosition,
                                                     File mapFile, XmlRenderTheme renderTheme, boolean hasAlpha) {
        TileRendererLayer tileRendererLayer = new TileRendererLayer(tileCache, mapViewPosition, hasAlpha,
                AndroidGraphicFactory.INSTANCE);
        tileRendererLayer.setMapFile(mapFile);
        tileRendererLayer.setXmlRenderTheme(renderTheme);
        tileRendererLayer.setTextScale(1.5f);
        return tileRendererLayer;
    }

    static Marker createMarker(Context c, int resourceIdentifier, LatLong latLong) {
        Drawable drawable = c.getResources().getDrawable(resourceIdentifier);
        Bitmap bitmap = AndroidGraphicFactory.convertToBitmap(drawable);
        return new Marker(latLong, bitmap, 0, -bitmap.getHeight() / 2);
    }

    static Marker createTappableMarker(Context c, int resourceIdentifier, LatLong latLong) {
        Drawable drawable = c.getResources().getDrawable(resourceIdentifier);
        Bitmap bitmap = AndroidGraphicFactory.convertToBitmap(drawable);
        return new Marker(latLong, bitmap, 0, -bitmap.getHeight() / 2) {
	        @Override
            public boolean onTap(LatLong geoPoint, Point viewPosition, Point tapPoint) {
		        if (contains(viewPosition, tapPoint)) {
                    Log.w("Tapp", "The Marker was touched with onTap: " + this.getLatLong().toString());
		            return true;
		        }
		        return false;
            }
        };
    }

    static Paint createPaint(int color, int strokeWidth, Style style) {
        Paint paint = AndroidGraphicFactory.INSTANCE.createPaint();
        paint.setColor(color);
        paint.setStrokeWidth(strokeWidth);
        paint.setStyle(style);
        return paint;
    }

    static Bitmap viewToBitmap(Context c, View view) {
        view.measure(MeasureSpec.getSize(view.getMeasuredWidth()), MeasureSpec.getSize(view.getMeasuredHeight()));
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        view.setDrawingCacheEnabled(true);
        Drawable drawable = new BitmapDrawable(c.getResources(), android.graphics.Bitmap.createBitmap(view
                .getDrawingCache()));
        view.setDrawingCacheEnabled(false);
        return AndroidGraphicFactory.convertToBitmap(drawable);
    }

}
