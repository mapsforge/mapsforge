/*
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2016 devemux86
 * Copyright 2024-2025 Sublimis
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

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.Display;
import org.mapsforge.core.graphics.Matrix;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Rectangle;
import org.mapsforge.core.model.Rotation;

/**
 * The MapElementContainer is the abstract base class for annotations that can be placed on the
 * map, e.g. labels and icons.
 * <p/>
 * A MapElementContainer has a central pivot point, which denotes the geographic point for the entity
 * translated into absolute map pixels. The boundary denotes the space that the item requires
 * around this central point.
 * <p/>
 * A MapElementContainer has a priority (higher value means higher priority) that should be used to determine
 * the drawing order, i.e. elements with higher priority should be drawn before elements with lower
 * priority. If there is not enough space on the map, elements with lower priority should then not be
 * drawn.
 */
public abstract class MapElementContainer implements Comparable<MapElementContainer> {
    protected Rectangle boundaryAbsolute;
    protected final Display display;
    protected final int priority;
    protected final Point xy;
    protected volatile double clashRotationDegrees;
    protected volatile Rectangle clashRect;

    protected MapElementContainer(Point xy, Display display, int priority) {
        this.xy = xy;
        this.display = display;
        this.priority = priority;
    }

    protected abstract Rectangle getBoundary();

    /**
     * Compares elements according to their priority, then display, then position.
     * The compare is consistent with equals.
     */
    @Override
    public int compareTo(MapElementContainer other) {
        if (this.priority < other.priority) {
            return -1;
        }
        if (this.priority > other.priority) {
            return 1;
        }

        if (this.display != other.display) {
            if (this.display != Display.ALWAYS && other.display == Display.ALWAYS) {
                return -1;
            }
            if (this.display == Display.ALWAYS) {
                return 1;
            }

            if (this.display != Display.ORDER && other.display == Display.ORDER) {
                return -1;
            }
            if (this.display == Display.ORDER) {
                return 1;
            }

            switch (this.display) {
                case NEVER:
                    return -1;
                case IFSPACE:
                    return 1;
                default:
                    throw new IllegalArgumentException("Unknown Display state for MapElementContainer.compareTo()");
            }
        }

        // If the priorities and display are the same, make a more detailed ordering.
        // Basically we don't want to allow two elements to be arbitrarily ordered,
        // because that makes drawing the elements non-deterministic.
        // This also makes the natural ordering of elements consistent with equals,
        // as it should be.
        return xy.compareTo(other.xy);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof MapElementContainer)) {
            return false;
        }
        MapElementContainer other = (MapElementContainer) obj;
        if (this.priority != other.priority) {
            return false;
        } else if (this.display != other.display) {
            return false;
        } else if (!this.xy.equals(other.xy)) {
            return false;
        }
        return true;
    }

    /**
     * Drawing method: element will draw itself on canvas shifted by origin point of canvas and
     * using the matrix if rotation is required.
     */
    public abstract void draw(Canvas canvas, Point origin, Matrix matrix, Rotation rotation);

    /**
     * Gets the pixel absolute boundary for this element.
     *
     * @return Rectangle with absolute pixel coordinates.
     */
    protected Rectangle getBoundaryAbsolute() {
        if (boundaryAbsolute == null) {
            boundaryAbsolute = this.getBoundary().shift(xy);
        }
        return boundaryAbsolute;
    }

    public boolean intersects(Rectangle rectangle, Rotation rotation) {
        Rectangle rect = this.getClashRect(rotation);

        return rect != null && rect.intersects(rectangle);
    }

    /**
     * Returns if MapElementContainers clash with each other
     *
     * @param other element to test against
     * @return true if they overlap
     */
    public boolean clashesWith(MapElementContainer other, Rotation rotation) {
        // If any element is always drawn, the elements do not clash, otherwise do more checks
        if (Display.ALWAYS == this.display || Display.ALWAYS == other.display) {
            return false;
        }

        // If exactly one of the elements is order drawn, the elements do not clash, otherwise do more checks
        if (Display.ORDER == this.display ^ Display.ORDER == other.display) {
            return false;
        }

        Rectangle rect1 = this.getClashRect(rotation);
        Rectangle rect2 = other.getClashRect(rotation);

        if (rect1 != null && rect1.intersects(rect2)) {
            return true;
        }

        return false;
    }

    public Rectangle getClashRect(Rotation rotation) {
        final MapElementContainer pointTextContainer = this;

        Rectangle output = null;

        if (getBoundary() != null) {
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

            output = new Rectangle(x + getBoundary().left, y + getBoundary().top, x + getBoundary().right, y + getBoundary().bottom);

            pointTextContainer.clashRect = output;
            pointTextContainer.clashRotationDegrees = newRotation.degrees;
        }

        return output;
    }

    @Override
    public int hashCode() {
        int result = 7;
        result = 31 * result + xy.hashCode();
        result = 31 * result + priority;
        return result;
    }

    /**
     * Gets the center point of this element.
     *
     * @return Point with absolute center pixel coordinates.
     */
    public Point getPoint() {
        return this.xy;
    }

    public int getPriority() {
        return priority;
    }

    /**
     * @return {@code true} if this is definitely not visible; {@code false} if it may or may not be visible
     */
    public boolean isNotVisible() {
        return Display.NEVER == this.display;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("xy=");
        stringBuilder.append(this.xy);
        stringBuilder.append(", priority=");
        stringBuilder.append(this.priority);
        return stringBuilder.toString();
    }
}
