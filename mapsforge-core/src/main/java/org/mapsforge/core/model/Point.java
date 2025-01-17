/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
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
package org.mapsforge.core.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * A Point represents an immutable pair of double coordinates.
 */
public class Point implements Comparable<Point>, Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * The x coordinate of this point.
     */
    public final double x;

    /**
     * The y coordinate of this point.
     */
    public final double y;

    /**
     * @param x the x coordinate of this point.
     * @param y the y coordinate of this point.
     */
    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public int compareTo(Point point) {
        return compareCoord(this.x, this.y, point.x, point.y);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Point)) return false;
        Point point = (Point) o;
        return Double.compare(x, point.x) == 0 && Double.compare(y, point.y) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    /**
     * @return the euclidean distance from this point to the given point.
     */
    public double distance(Point point) {
        return Math.hypot(this.x - point.x, this.y - point.y);
    }

    public Point offset(double dx, double dy) {
        if (0 == dx && 0 == dy) {
            return this;
        }
        return new Point(this.x + dx, this.y + dy);
    }

    /**
     * Rotates the point with {@link Rotation}. This method can be chained to apply multiple
     * rotations to one point.
     *
     * @param rotation the rotation
     * @return a new rotated point.
     */
    public Point rotate(Rotation rotation) {
        return rotation.rotate(this);
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("x=");
        stringBuilder.append(this.x);
        stringBuilder.append(", y=");
        stringBuilder.append(this.y);
        return stringBuilder.toString();
    }

    public static int compareCoord(double x1, double y1, double x2, double y2) {
        if (x1 > x2) {
            return 1;
        } else if (x1 < x2) {
            return -1;
        } else if (y1 > y2) {
            return 1;
        } else if (y1 < y2) {
            return -1;
        }
        return 0;
    }
}
