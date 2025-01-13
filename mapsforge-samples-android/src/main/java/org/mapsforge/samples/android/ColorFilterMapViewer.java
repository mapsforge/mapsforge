/*
 * Copyright 2025 devemux86
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

import android.graphics.*;
import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.map.android.graphics.AndroidBitmap;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.rendertheme.ThemeCallback;
import org.mapsforge.map.rendertheme.renderinstruction.RenderInstruction;

/**
 * Standard map view with a grayscale color filter.
 */
public class ColorFilterMapViewer extends DefaultTheme {

    @Override
    protected void createMapViews() {
        super.createMapViews();

        ColorMatrix colorMatrix = new ColorMatrix();
        // Grayscale
        colorMatrix.setSaturation(0);
        ColorFilter colorFilter = new ColorMatrixColorFilter(colorMatrix);
        Paint paint = new Paint();
        paint.setColorFilter(colorFilter);

        mapView.getModel().displayModel.setThemeCallback(new ThemeCallback() {
            @Override
            public Bitmap getBitmap(Bitmap bitmap) {
                android.graphics.Bitmap immutable = AndroidGraphicFactory.getBitmap(bitmap);
                android.graphics.Bitmap mutable = immutable.copy(android.graphics.Bitmap.Config.ARGB_8888, true);
                Canvas androidCanvas = new Canvas(mutable);
                androidCanvas.drawBitmap(mutable, 0, 0, paint);
                return new AndroidBitmap(mutable);
            }

            @Override
            public int getColor(RenderInstruction origin, int color) {
                int a = color >>> 24;
                int r = (color >> 16) & 0xFF;
                int g = (color >> 8) & 0xFF;
                int b = color & 0xFF;
                // Grayscale
                r = g = b = (int) (0.213f * r + 0.715f * g + 0.072f * b);
                return (a << 24) | (r << 16) | (g << 8) | b;
            }
        });
    }
}
