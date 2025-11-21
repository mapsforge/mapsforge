/*
 * Copyright 2016-2019 devemux86
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
package org.mapsforge.map.rendertheme;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.map.datastore.PointOfInterest;
import org.mapsforge.map.layer.renderer.PolylineContainer;
import org.mapsforge.map.rendertheme.renderinstruction.RenderInstruction;

/**
 * Callback methods for render theme.
 */
public abstract class ThemeCallbackAdapter implements ThemeCallback {
    /**
     * @return the resource {@link Bitmap}
     */
    @Override
    public Bitmap getBitmap(Bitmap bitmap) {
        return bitmap;
    }

    /**
     * @return the color-int
     */
    @Override
    public int getColor(RenderInstruction origin, int color) {
        return color;
    }

    /**
     * @return the color-int
     */
    @Override
    public int getColor(PolylineContainer way, int color) {
        return color;
    }

    /**
     * @return the text
     */
    @Override
    public String getText(PointOfInterest poi, String text) {
        return text;
    }

    /**
     * @return the text
     */
    @Override
    public String getText(PolylineContainer way, String text) {
        return text;
    }
}
