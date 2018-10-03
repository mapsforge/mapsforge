/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2015 devemux86
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
package org.mapsforge.map.awt.graphics;

import org.mapsforge.core.graphics.GraphicUtils;
import org.mapsforge.core.graphics.ResourceBitmap;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public class AwtResourceBitmap extends AwtBitmap implements ResourceBitmap {

    AwtResourceBitmap(InputStream inputStream, float scaleFactor, int width, int height, int percent) throws IOException {
        super(inputStream);
        float[] newSize = GraphicUtils.imageSize(getWidth(), getHeight(), scaleFactor, width, height, percent);
        scaleTo((int) newSize[0], (int) newSize[1]);
    }

    AwtResourceBitmap(BufferedImage bufferedImage) {
        super(bufferedImage);
    }

}
