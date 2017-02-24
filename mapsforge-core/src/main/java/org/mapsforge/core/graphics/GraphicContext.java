/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2016 devemux86
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
    void shadeBitmap(Bitmap bitmap, Rectangle shadeRect, Rectangle tileRect, float magnitude);
    
    void drawBitmap(Bitmap bitmap, int left, int top);

    void drawBitmap(Bitmap bitmap, int left, int top, Filter filter);

    void drawBitmap(Bitmap bitmap, Matrix matrix);

    void drawBitmap(Bitmap bitmap, Matrix matrix, Filter filter);

    void drawCircle(int x, int y, int radius, Paint paint);

    void drawLine(int x1, int y1, int x2, int y2, Paint paint);

    void drawPath(Path path, Paint paint);

    void drawText(String text, int x, int y, Paint paint);

    void drawTextRotated(String text, int x1, int y1, int x2, int y2, Paint paint);

    void fillColor(Color color);

    void fillColor(int color);

    void resetClip();

    void setClip(int left, int top, int width, int height);

    void setClipDifference(int left, int top, int width, int height);
}
