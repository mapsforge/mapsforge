/*
 * Copyright 2013-2014 Ludwig M Brinckmann
 * Copyright 2014, 2015 devemux86
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
package org.mapsforge.samples.android;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;
import android.view.View.MeasureSpec;
import android.widget.Toast;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.overlay.Marker;

/**
 * Utility functions that can be used across different mapsforge based
 * activities.
 */
public final class Utils {
    /**
     * Compatibility method.
     *
     * @param a the current activity
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static void enableHome(Activity a) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // Show the Up button in the action bar.
            a.getActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * Compatibility method.
     *
     * @param view       the view to set the background on
     * @param background the background
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @SuppressWarnings("deprecation")
    public static void setBackground(View view, Drawable background) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            view.setBackground(background);
        } else {
            view.setBackgroundDrawable(background);
        }
    }

    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    static Marker createMarker(Context c, int resourceIdentifier,
                               LatLong latLong) {
        Drawable drawable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? c.getDrawable(resourceIdentifier) : c.getResources().getDrawable(resourceIdentifier);
        Bitmap bitmap = AndroidGraphicFactory.convertToBitmap(drawable);
        return new Marker(latLong, bitmap, 0, -bitmap.getHeight() / 2);
    }

    static Paint createPaint(int color, int strokeWidth, Style style) {
        Paint paint = AndroidGraphicFactory.INSTANCE.createPaint();
        paint.setColor(color);
        paint.setStrokeWidth(strokeWidth);
        paint.setStyle(style);
        return paint;
    }

    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    static Marker createTappableMarker(final Context c, int resourceIdentifier,
                                       LatLong latLong) {
        Drawable drawable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? c.getDrawable(resourceIdentifier) : c.getResources().getDrawable(resourceIdentifier);
        Bitmap bitmap = AndroidGraphicFactory.convertToBitmap(drawable);
        bitmap.incrementRefCount();
        return new Marker(latLong, bitmap, 0, -bitmap.getHeight() / 2) {
            @Override
            public boolean onTap(LatLong geoPoint, Point viewPosition,
                                 Point tapPoint) {
                if (contains(viewPosition, tapPoint)) {
                    Toast.makeText(c,
                            "The Marker was tapped " + geoPoint.toString(),
                            Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            }
        };
    }

    public static Bitmap viewToBitmap(Context c, View view) {
        view.measure(MeasureSpec.getSize(view.getMeasuredWidth()),
                MeasureSpec.getSize(view.getMeasuredHeight()));
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        view.setDrawingCacheEnabled(true);
        Drawable drawable = new BitmapDrawable(c.getResources(),
                android.graphics.Bitmap.createBitmap(view.getDrawingCache()));
        view.setDrawingCacheEnabled(false);
        return AndroidGraphicFactory.convertToBitmap(drawable);
    }

    private Utils() {
        throw new IllegalStateException();
    }

}
