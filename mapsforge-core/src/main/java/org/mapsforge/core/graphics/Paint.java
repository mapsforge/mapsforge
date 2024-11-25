/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2016-2017 devemux86
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
package org.mapsforge.core.graphics;

import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Rectangle;

public interface Paint {
    /**
     * Factor to obtain the font padding value by multiplying it with the font height.
     */
    double FONT_PADDING_FACTOR = 0.2;

    int getColor();

    float getStrokeWidth();

    Rectangle getTextBounds(String text);

    int getTextHeight(String text);

    int getTextWidth(String text);

    boolean isTransparent();

    void setBitmapShader(Bitmap bitmap);

    void setBitmapShaderShift(Point origin);

    void setColor(Color color);

    /**
     * The default value is {@link Color#BLACK}.
     */
    void setColor(int color);

    void setDashPathEffect(float[] strokeDasharray);

    /**
     * The default value is {@link Cap#ROUND}.
     */
    void setStrokeCap(Cap cap);

    void setStrokeJoin(Join join);

    void setStrokeWidth(float strokeWidth);

    /**
     * The default value is {@link Style#FILL}.
     */
    void setStyle(Style style);

    void setTextAlign(Align align);

    void setTextSize(float textSize);

    void setTypeface(FontFamily fontFamily, FontStyle fontStyle);
}
