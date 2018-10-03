/*
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2016 devemux86
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
package org.mapsforge.core.graphics;

/**
 * Utility class for graphics operations.
 */
public final class GraphicUtils {
    /**
     * Color filtering.
     *
     * @param color  color value in layout 0xAARRGGBB.
     * @param filter filter to apply on the color.
     * @return the filtered color.
     */
    public static int filterColor(int color, Filter filter) {
        if (filter == Filter.NONE) {
            return color;
        }
        int a = color >>> 24;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        switch (filter) {
            case GRAYSCALE:
                r = g = b = (int) (0.213f * r + 0.715f * g + 0.072f * b);
                break;
            case GRAYSCALE_INVERT:
                r = g = b = 255 - (int) (0.213f * r + 0.715f * g + 0.072f * b);
                break;
            case INVERT:
                r = 255 - r;
                g = 255 - g;
                b = 255 - b;
                break;
        }
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * @param color color value in layout 0xAARRGGBB.
     * @return the alpha value for the color.
     */
    public static int getAlpha(int color) {
        return (color >> 24) & 0xff;
    }

    /**
     * Given the original image size, as well as width, height, percent parameters,
     * can compute the final image size.
     *
     * @param picWidth    original image width
     * @param picHeight   original image height
     * @param scaleFactor scale factor to screen DPI
     * @param width       requested width (0: no change)
     * @param height      requested height (0: no change)
     * @param percent     requested scale percent (100: no change)
     */
    public static float[] imageSize(float picWidth, float picHeight, float scaleFactor, int width, int height, int percent) {
        float bitmapWidth = picWidth * scaleFactor;
        float bitmapHeight = picHeight * scaleFactor;

        float aspectRatio = picWidth / picHeight;

        if (width != 0 && height != 0) {
            // both width and height set, override any other setting
            bitmapWidth = width;
            bitmapHeight = height;
        } else if (width == 0 && height != 0) {
            // only width set, calculate from aspect ratio
            bitmapWidth = height * aspectRatio;
            bitmapHeight = height;
        } else if (width != 0 && height == 0) {
            // only height set, calculate from aspect ratio
            bitmapHeight = width / aspectRatio;
            bitmapWidth = width;
        }

        if (percent != 100) {
            bitmapWidth *= percent / 100f;
            bitmapHeight *= percent / 100f;
        }

        return new float[]{bitmapWidth, bitmapHeight};
    }

    private GraphicUtils() {
        // noop, just to make tools happy.
    }
}
