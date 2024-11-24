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

    /**
     * This will draw clash rectangles around labels (for debugging purposes).
     */
    protected static final boolean DEBUG_CLASH_BOUNDS = false;

    public final double horizontalOffset;
    public final boolean isVisible;
    public final int maxTextWidth;
    public final Paint paintBack;
    public final Paint paintFront;
    public final Position position;
    public final SymbolContainer symbolContainer;
    public final String text;
    public final double verticalOffset;
    public volatile double clashRotationDegrees;
    public volatile Rectangle clashRect;

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

        if (!Rotation.noRotation(rotation) || DEBUG_CLASH_BOUNDS) {
            Rectangle rect1 = getClashRect(this, rotation);
            Rectangle rect2 = getClashRect(ptc, rotation);

            if (rect1 != null && rect2 != null && rect1.intersects(rect2)) {
                return true;
            }
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

    /**
     * @return New rotated x,y position of the container relative to the origin point
     */
    protected Point getRotatedRelativePosition(double originX, double originY, Rotation rotation) {
        return getRotatedRelativePosition(this, originX, originY, rotation);
    }

    /**
     * @return New rotated x,y position of the container relative to the origin point
     */
    public static Point getRotatedRelativePosition(PointTextContainer pointTextContainer, double originX, double originY, Rotation rotation) {
        double x = pointTextContainer.xy.x - originX;
        double y = pointTextContainer.xy.y - originY;

        if (!Rotation.noRotation(rotation)) {
            Point rotated = rotation.rotate(x, y, true);
            x = rotated.x;
            y = rotated.y;
        }

        // The offsets can only be applied after rotation
        x += pointTextContainer.horizontalOffset + pointTextContainer.getBoundary().left;
        y += pointTextContainer.verticalOffset + pointTextContainer.getBoundary().top;

        return new Point(x, y);
    }

    /**
     * See org.mapsforge.map.rendertheme.renderinstruction.Caption#Caption,
     * org.mapsforge.map.android.graphics.AndroidPointTextContainer#AndroidPointTextContainer,
     * or org.mapsforge.map.awt.graphics.AwtPointTextContainer#AwtPointTextContainer.
     *
     * @return Clash rectangle in absolute coordinate space
     */
    public static Rectangle getClashRect(PointTextContainer pointTextContainer, Rotation rotation) {
        if (!pointTextContainer.isVisible) {
            return null;
        }

        // All other fields used are final (read only)
        if (rotation.degrees == pointTextContainer.clashRotationDegrees && pointTextContainer.clashRect != null) {
            return pointTextContainer.clashRect;
        }

        final Rotation newRotation = new Rotation(rotation.degrees, 0, 0);

        // We work in the absolute coordinate space of the map (intentionally)
        double x = pointTextContainer.xy.x;
        double y = pointTextContainer.xy.y;

        if (!Rotation.noRotation(newRotation)) {
            Point rotated = newRotation.rotate(x, y, true);
            x = rotated.x;
            y = rotated.y;
        }

        // The offsets can only be applied after rotation
        x += pointTextContainer.horizontalOffset;
        y += pointTextContainer.verticalOffset;

        double textOffsetX, textOffsetY;

        // Implementation of the constructors mentioned above allows such use of boundary dimensions
        textOffsetX = pointTextContainer.getBoundary().getWidth();
        textOffsetY = pointTextContainer.getBoundary().getHeight();

        final Rectangle output;

        switch (pointTextContainer.position) {
            case CENTER:
            default:
                // Text is positioned centered on x,y
                textOffsetX /= 2;
                textOffsetY /= 2;
                output = new Rectangle(x - textOffsetX, y - textOffsetY, x + textOffsetX, y + textOffsetY);
                break;
            case BELOW:
                // Text is positioned centered-below x,y
                textOffsetX /= 2;
                output = new Rectangle(x - textOffsetX, y, x + textOffsetX, y + textOffsetY);
                break;
            case ABOVE:
                // Text is positioned centered-above x,y
                textOffsetX /= 2;
                output = new Rectangle(x - textOffsetX, y - textOffsetY, x + textOffsetX, y);
                break;
            case BELOW_LEFT:
                // Text is positioned below-left of x,y
                output = new Rectangle(x - textOffsetX, y, x, y + textOffsetY);
                break;
            case ABOVE_LEFT:
                // Text is positioned above-left of x,y
                output = new Rectangle(x - textOffsetX, y - textOffsetY, x, y);
                break;
            case LEFT:
                // Text is positioned centered-left of x,y
                textOffsetY /= 2;
                output = new Rectangle(x - textOffsetX, y - textOffsetY, x, y + textOffsetY);
                break;
            case BELOW_RIGHT:
                // Text is positioned below-right of x,y
                output = new Rectangle(x, y, x + textOffsetX, y + textOffsetY);
                break;
            case ABOVE_RIGHT:
                // Text is positioned above-right of x,y
                output = new Rectangle(x, y - textOffsetY, x + textOffsetX, y);
                break;
            case RIGHT:
                // Text is positioned centered-right of x,y
                textOffsetY /= 2;
                output = new Rectangle(x, y - textOffsetY, x + textOffsetX, y + textOffsetY);
                break;
        }

        pointTextContainer.clashRect = output;
        pointTextContainer.clashRotationDegrees = newRotation.degrees;

        return output;
    }

    /**
     * Mainly used for debugging purposes.
     *
     * @return Clash rectangle transformed back to label space
     */
    public static Rectangle getClashRectangleTransformed(PointTextContainer pointTextContainer, double originX, double originY, Rotation rotation) {
        Rectangle output = null;

        final Rectangle clashBounds = pointTextContainer.clashRect;

        if (clashBounds != null) {
            final Rotation myRotation = new Rotation(rotation.degrees, 0, 0);

            // "Undo" clash rectangle offset
            Rectangle rect1 = clashBounds.shift(new Point(-pointTextContainer.horizontalOffset, -pointTextContainer.verticalOffset));

            // "Undo" clash rectangle rotation
            Point lt = myRotation.rotate(rect1.left, rect1.top, false);
            Point rb = myRotation.rotate(rect1.right, rect1.bottom, false);

            // Imitate drawing clash rectangle as a label
            Point lt2 = lt.offset(-originX, -originY);
            Point rb2 = rb.offset(-originX, -originY);
            Point lt3 = rotation.rotate(lt2, true);
            Point rb3 = rotation.rotate(rb2, true);
            Rectangle rect2 = new Rectangle(lt3.x, lt3.y, rb3.x, rb3.y);
            output = rect2.shift(new Point(pointTextContainer.horizontalOffset, pointTextContainer.verticalOffset));
        }

        return output;
    }
}
