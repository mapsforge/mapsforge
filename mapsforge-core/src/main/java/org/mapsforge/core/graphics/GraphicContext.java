/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2016-2019 devemux86
 * Copyright 2017 usrusr
 * Copyright 2019 cpt1gl0
 * Copyright 2019 Adrian Batzill
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

import org.mapsforge.core.model.Rectangle;

public interface GraphicContext {
    void drawBitmap(Bitmap bitmap, int left, int top);

    void drawBitmap(Bitmap bitmap, int left, int top, Filter filter);

    void drawBitmap(Bitmap bitmap, Matrix matrix);

    void drawBitmap(Bitmap bitmap, Matrix matrix, Filter filter);

    void drawBitmap(Bitmap bitmap, int srcLeft, int srcTop, int srcRight, int srcBottom,
                    int dstLeft, int dstTop, int dstRight, int dstBottom);

    void drawBitmap(Bitmap bitmap, int srcLeft, int srcTop, int srcRight, int srcBottom,
                    int dstLeft, int dstTop, int dstRight, int dstBottom, Filter filter);

    void drawCircle(int x, int y, int radius, Paint paint);

    void drawLine(int x1, int y1, int x2, int y2, Paint paint);

    void drawPath(Path path, Paint paint);

    void drawPathText(String text, Path path, Paint paint);

    void drawText(String text, int x, int y, Paint paint);

    void drawTextRotated(String text, int x1, int y1, int x2, int y2, Paint paint);

    void fillColor(Color color);

    void fillColor(int color);

    boolean isAntiAlias();

    boolean isFilterBitmap();

    void resetClip();

    void setAntiAlias(boolean aa);

    void setClip(int left, int top, int width, int height);

    void setClip(int left, int top, int width, int height, boolean intersect);

    void setClipDifference(int left, int top, int width, int height);

    void setFilterBitmap(boolean filter);

    /**
     * Shade whole map tile when tileRect is null (and bitmap, shadeRect are null).
     * Shade tileRect neutral if bitmap is null (and shadeRect).
     * Shade tileRect with bitmap otherwise.
     */
    void shadeBitmap(Bitmap bitmap, Rectangle shadeRect, Rectangle tileRect, float magnitude);
}
