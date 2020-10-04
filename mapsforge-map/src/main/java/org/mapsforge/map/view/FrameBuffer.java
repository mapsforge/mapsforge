/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2015-2017 devemux86
 * Copyright 2020 Lukas Bai
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
package org.mapsforge.map.view;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.GraphicContext;
import org.mapsforge.core.model.Dimension;
import org.mapsforge.core.model.MapPosition;

public abstract class FrameBuffer {

    public abstract void adjustMatrix(float diffX, float diffY, float scaleFactor, Dimension mapViewDimension,
                                      float pivotDistanceX, float pivotDistanceY);

    public abstract void destroy();

    public abstract void draw(GraphicContext graphicContext);

    public abstract void frameFinished(MapPosition frameMapPosition);

    public abstract Dimension getDimension();

    /**
     * @return the bitmap of the second frame to draw on (may be null).
     */
    public abstract Bitmap getDrawingBitmap();

    public abstract void setDimension(Dimension dimension);
}
