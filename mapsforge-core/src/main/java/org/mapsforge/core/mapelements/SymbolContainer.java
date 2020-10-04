/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2015-2016 devemux86
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
    public Position position;

    public SymbolContainer(Point point, Display display, int priority, Position position, Bitmap symbol) {
        this(point, display, priority, position, symbol, 0);
    }

    public SymbolContainer(Point point, Display display, int priority, Position position, Bitmap symbol, float theta) {
        super(point, display, priority);
        this.symbol = symbol;
        this.theta = theta;
        this.position = position;
        computeBoundary();
        this.symbol.incrementRefCount();
    }

    private void computeBoundary() {
        // Center by default
        double xfactor = -0.5, yfactor = -0.5;

        if (position == Position.ABOVE_LEFT || position == Position.LEFT || position == Position.BELOW_LEFT)
            xfactor = -1;
        else if (position == Position.ABOVE_RIGHT || position == Position.RIGHT || position == Position.BELOW_RIGHT)
            xfactor = 0;

        if (position == Position.ABOVE_LEFT || position == Position.ABOVE || position == Position.ABOVE_RIGHT)
            yfactor = -1;
        else if (position == Position.BELOW_LEFT || position == Position.BELOW || position == Position.BELOW_RIGHT)
            yfactor = 0;

        int width = this.symbol.getWidth();
        int height = this.symbol.getHeight();
        double left = xfactor * width;
        double top = yfactor * width;
        this.boundary = new Rectangle(left, top, left + width, top + height);
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
