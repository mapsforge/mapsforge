/*
 * Copyright 2017 usrusr
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
package org.mapsforge.map.layer.renderer;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.HillshadingBitmap;
import org.mapsforge.core.model.Rectangle;
import org.mapsforge.core.util.Constants;

public class HillshadingContainer implements ShapeContainer {

    public final float magnitude;
    public final int color;
    public final Bitmap bitmap;
    public final Rectangle hillsRect;
    public final Rectangle tileRect;

    public HillshadingContainer(HillshadingBitmap bitmap, float magnitude, int color, Rectangle hillsRect, Rectangle tileRect) {
        this.magnitude = magnitude;
        this.color = color;
        this.bitmap = bitmap;
        this.hillsRect = hillsRect;
        this.tileRect = tileRect;
    }

    public HillshadingContainer(HillshadingBitmap bitmap, float magnitude, Rectangle hillsRect, Rectangle tileRect) {
        this(bitmap, magnitude, Constants.HILLSHADING_COLOR_DEFAULT, hillsRect, tileRect);
    }

    @Override
    public ShapeType getShapeType() {
        return ShapeType.HILLSHADING;
    }

    @Override
    public String toString() {
        return "[Hillshading:" + magnitude + "#" + System.identityHashCode(bitmap) + "\n @# " + hillsRect + "\n -> " + tileRect + "\n]";
    }
}
