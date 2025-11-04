/*
 * Copyright 2025 Sublimis
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
package org.mapsforge.map.elevation;

import org.mapsforge.core.graphics.BaseBitmap;
import org.mapsforge.core.graphics.HillshadingBitmap;
import org.mapsforge.core.model.BoundingBox;

import java.io.IOException;
import java.io.OutputStream;

public class ElevationBitmap extends BaseBitmap implements HillshadingBitmap {

    public byte[] buffer;
    public int width;
    public int height;

    // ***************************
    // Methods below are not used.
    // ***************************

    @Override
    public BoundingBox getAreaRect() {
        return null;
    }

    @Override
    public int getPadding() {
        return 0;
    }

    @Override
    public long getSizeBytes() {
        return 0;
    }

    @Override
    public void compress(OutputStream outputStream) throws IOException {
    }

    @Override
    public void decrementRefCount() {
    }

    @Override
    public int getHeight() {
        return 0;
    }

    @Override
    public int getWidth() {
        return 0;
    }

    @Override
    public void incrementRefCount() {
    }

    @Override
    public boolean isDestroyed() {
        return false;
    }

    @Override
    public void scaleTo(int width, int height) {
    }

    @Override
    public void setBackgroundColor(int color) {
    }
}
