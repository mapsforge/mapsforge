/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
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
package org.mapsforge.core.mapelements;

import org.mapsforge.core.graphics.Display;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Position;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Rectangle;
import org.mapsforge.core.model.Rotation;

public abstract class PointTextContainer extends MapElementContainer {

    public final double horizontalOffset;
    public final boolean isVisible;
    public final int maxTextWidth;
    public final Paint paintBack;
    public final Paint paintFront;
    public final Position position;
    public final SymbolContainer symbolContainer;
    public final String text;
    public final int textHeight;
    public final int textWidth;
    public final double verticalOffset;

    /**
     * Create a new point container, that holds the x-y coordinates of a point, a text variable, two paint objects, and
     * a reference on a symbolContainer, if the text is connected with a POI.
     */
    protected PointTextContainer(Point point, double horizontalOffset, double verticalOffset,
                                 Display display, int priority, String text, Paint paintFront, Paint paintBack,
                                 SymbolContainer symbolContainer, Position position, int maxTextWidth) {
        super(point, display, priority);

        this.maxTextWidth = maxTextWidth;
        this.text = text;
        this.symbolContainer = symbolContainer;
        this.paintFront = paintFront;
        this.paintBack = paintBack;
        this.position = position;
        if (paintBack != null) {
            this.textWidth = paintBack.getTextWidth(text);
            this.textHeight = paintBack.getTextHeight(text);
        } else {
            this.textWidth = paintFront.getTextWidth(text);
            this.textHeight = paintFront.getTextHeight(text);
        }
        this.horizontalOffset = horizontalOffset;
        this.verticalOffset = verticalOffset;
        this.isVisible = !this.paintFront.isTransparent() || (this.paintBack != null && !this.paintBack.isTransparent());
    }

    @Override
    public boolean clashesWith(MapElementContainer other, Rotation rotation) {
        if (super.clashesWith(other, rotation)) {
            return true;
        }
        if (!(other instanceof PointTextContainer)) {
            return false;
        }
        PointTextContainer ptc = (PointTextContainer) other;
        if (!Rotation.noRotation(rotation)) {
            Rotation mapRotation = new Rotation(rotation.degrees, (float) this.xy.x, (float) this.xy.y);
            Rectangle rotated = mapRotation.rotate(this.boundary.shift(xy));
            Rotation otherRotation = new Rotation(rotation.degrees, (float) ptc.xy.x, (float) ptc.xy.y);
            Rectangle otherRotated = otherRotation.rotate(ptc.boundary.shift(ptc.xy));
            if (rotated.intersects(otherRotated))
                return true;
        }
        if (this.text.equals(ptc.text) && this.xy.distance(ptc.xy) < 200) {
            return true;
        }
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof PointTextContainer)) {
            return false;
        }
        PointTextContainer other = (PointTextContainer) obj;
        if (!this.text.equals(other.text)) {
            return false;
        }
        return true;
    }

    /**
     * Gets the pixel absolute boundary for this element.
     *
     * @return Rectangle with absolute pixel coordinates.
     */
    protected Rectangle getBoundaryAbsolute() {
        Rectangle result = super.getBoundaryAbsolute();
        // we need to add the offset in this case as it is not applied automatically.
        return result.shift(new Point(this.horizontalOffset, this.verticalOffset));
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + text.hashCode();
        return result;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(super.toString());
        stringBuilder.append(", text=");
        stringBuilder.append(this.text);
        return stringBuilder.toString();
    }
}
