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
package org.mapsforge.map.awt.graphics;

import org.mapsforge.core.graphics.HillshadingBitmap;
import org.mapsforge.core.model.BoundingBox;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;

public class AwtHillshadingBitmap extends AwtBitmap implements HillshadingBitmap {
    private final int padding;
    private final BoundingBox areaRect;

    public AwtHillshadingBitmap(BufferedImage bufferedImage, int padding, BoundingBox areaRect) {
        super(bufferedImage);

        this.padding = padding;
        this.areaRect = areaRect;
    }

    @Override
    public BoundingBox getAreaRect() {
        return areaRect;
    }

    @Override
    public int getPadding() {
        return padding;
    }

    @Override
    public long getSizeBytes() {
        long retVal = 0;

        final BufferedImage myBufferedImage = bufferedImage;

        if (myBufferedImage != null) {
            final DataBuffer dataBuffer = myBufferedImage
                    .getData()
                    .getDataBuffer();

            if (dataBuffer != null) {
                retVal = (long) dataBuffer.getSize() * DataBuffer.getDataTypeSize(dataBuffer.getDataType()) / 8;
            }
        }

        return retVal;
    }
}
