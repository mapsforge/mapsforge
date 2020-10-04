/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2015-2016 devemux86
 * Copyright 2020 Adrian Batzill
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
package org.mapsforge.core.mapelements;

import org.mapsforge.core.graphics.*;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Rectangle;

public class SymbolContainer extends MapElementContainer {
    public Bitmap symbol;
    public final float theta;

    public SymbolContainer(Point point, Display display, int priority, Rectangle boundary, Bitmap symbol) {
        this(point, display, priority, boundary, symbol, 0);
    }

    public SymbolContainer(Point point, Display display, int priority, Rectangle boundary, Bitmap symbol, float theta) {
        super(point, display, priority);
        this.symbol = symbol;
        this.theta = theta;
        this.boundary = boundary;
        if (this.boundary == null) {
            double halfWidth = this.symbol.getWidth() / 2d;
            double halfHeight = this.symbol.getHeight() / 2d;
            this.boundary = new Rectangle(-halfWidth, -halfHeight, halfWidth, halfHeight);
        }

        this.symbol.incrementRefCount();
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof SymbolContainer)) {
            return false;
        }
        SymbolContainer other = (SymbolContainer) obj;
        if (this.symbol != other.symbol) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + symbol.hashCode();
        return result;
    }

    @Override
    public void draw(Canvas canvas, Point origin, Matrix matrix, Filter filter) {
        matrix.reset();
        // We cast to int for pixel perfect positioning
        matrix.translate((int) (this.xy.x - origin.x + boundary.left), (int) (this.xy.y - origin.y + boundary.top));
        if (theta != 0) {
            matrix.rotate(theta, (float) -boundary.left, (float) -boundary.top);
        }
        canvas.drawBitmap(this.symbol, matrix, 1, filter);
    }
}
