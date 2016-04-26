/*
 * Copyright 2014 Ludwig M Brinckmann
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

/**
 * A directed line segment between two Points.
 */
public final class LineSegment {

    private static int INSIDE = 0; // 0000
    private static int LEFT = 1;   // 0001
    private static int RIGHT = 2;  // 0010
    private static int BOTTOM = 4; // 0100
    private static int TOP = 8;    // 1000

    public final Point start;
    public final Point end;

    /**
     * Ctor with given start and end point
     *
     * @param start start point
     * @param end   end point
     */
    public LineSegment(Point start, Point end) {
        this.start = start;
        this.end = end;
    }

    /**
     * Ctor with given start point, a point that defines the direction of the line and a length
     *
     * @param start     start point
     * @param direction point that defines the direction (a line from start to direction point)
     * @param distance  how long to move along the line between start and direction
     */
    public LineSegment(Point start, Point direction, double distance) {
        this.start = start;
        this.end = new LineSegment(start, direction).pointAlongLineSegment(distance);
    }

    /**
     * Intersection of this LineSegment with the Rectangle as another LineSegment.
     * <p/>
     * Algorithm is Cohen-Sutherland, see https://en.wikipedia.org/wiki/Cohen%E2%80%93Sutherland_algorithm .
     *
     * @param r the rectangle to clip to.
     * @return the LineSegment that falls into the Rectangle, null if there is no intersection.
     */
    public LineSegment clipToRectangle(Rectangle r) {

        Point a = this.start;
        Point b = this.end;

        int codeStart = code(r, a);
        int codeEnd = code(r, b);

        while (true) {
            if (0 == (codeStart | codeEnd)) {
                // both points are inside, intersection is the computed line
                return new LineSegment(a, b);
            } else if (0 != (codeStart & codeEnd)) {
                // both points are either below, above, left or right of the box, no intersection
                return null;
            } else {
                double newX;
                double newY;
                // At least one endpoint is outside the clip rectangle; pick it.
                int outsideCode = (0 != codeStart) ? codeStart : codeEnd;

                if (0 != (outsideCode & TOP)) {
                    // point is above the clip rectangle
                    newX = a.x + (b.x - a.x) * (r.top - a.y) / (b.y - a.y);
                    newY = r.top;
                } else if (0 != (outsideCode & BOTTOM)) {
                    // point is below the clip rectangle
                    newX = a.x + (b.x - a.x) * (r.bottom - a.y) / (b.y - a.y);
                    newY = r.bottom;
                } else if (0 != (outsideCode & RIGHT)) {
                    // point is to the right of clip rectangle
                    newY = a.y + (b.y - a.y) * (r.right - a.x) / (b.x - a.x);
                    newX = r.right;
                } else if (0 != (outsideCode & LEFT)) {
                    // point is to the left of clip rectangle
                    newY = a.y + (b.y - a.y) * (r.left - a.x) / (b.x - a.x);
                    newX = r.left;
                } else {
                    throw new IllegalStateException("Should not get here");
                }
                // Now we move outside point to intersection point to clip
                // and get ready for next pass.
                if (outsideCode == codeStart) {
                    a = new Point(newX, newY);
                    codeStart = code(r, a);
                } else {
                    b = new Point(newX, newY);
                    codeEnd = code(r, b);
                }
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof LineSegment)) {
            return false;
        }
        LineSegment other = (LineSegment) obj;
        if (other.start.equals(this.start) && other.end.equals(this.end)) {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.start.hashCode();
        result = prime * result + this.end.hashCode();
        return result;
    }

    /**
     * Returns a fast computation if the line intersects the rectangle or bias if there
     * is no fast way to compute the intersection.
     *
     * @param r    retangle to test
     * @param bias the result if no fast computation is possible
     * @return either the fast and correct result or the bias (which might be wrong).
     */

    public boolean intersectsRectangle(Rectangle r, boolean bias) {
        int codeStart = code(r, this.start);
        int codeEnd = code(r, this.end);

        if (0 == (codeStart | codeEnd)) {
            // both points are inside, trivial case
            return true;
        } else if (0 != (codeStart & codeEnd)) {
            // both points are either below, above, left or right of the box, no intersection
            return false;
        }
        return bias;
    }

    /**
     * Euclidian distance between start and end.
     *
     * @return the length of the segment.
     */
    public double length() {
        return start.distance(end);
    }


    /**
     * Computes a Point along the line segment with a given distance to the start Point.
     *
     * @param distance distance from start point
     * @return point at given distance from start point
     */
    public Point pointAlongLineSegment(double distance) {
        if (start.x == end.x) {
            // we have a vertical line
            if (start.y > end.y) {
                return new Point(end.x, end.y + distance);
            } else {
                return new Point(start.x, start.y + distance);
            }
        } else {
            double slope = (end.y - start.y) / (end.x - start.x);
            double dx = Math.sqrt((distance * distance) / (1 + (slope * slope)));
            if (end.x < start.x) {
                dx *= -1;
            }
            return new Point(start.x + dx, start.y + slope * dx);
        }
    }

    /**
     * New line segment with start and end reversed.
     *
     * @return new LineSegment with start and end reversed
     */
    public LineSegment reverse() {
        return new LineSegment(this.end, this.start);
    }

    /**
     * LineSegment that starts at offset from start and runs for length towards end point
     *
     * @param offset offset applied at begin of line
     * @param length length of the new segment
     * @return new LineSegment computed
     */
    public LineSegment subSegment(double offset, double length) {
        Point subSegmentStart = pointAlongLineSegment(offset);
        Point subSegmentEnd = pointAlongLineSegment(offset + length);
        return new LineSegment(subSegmentStart, subSegmentEnd);
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(start).append(" ").append(end);
        return stringBuilder.toString();
    }

    /**
     * Computes the location code according to Cohen-Sutherland,
     * see https://en.wikipedia.org/wiki/Cohen%E2%80%93Sutherland_algorithm.
     */
    private static int code(Rectangle r, Point p) {
        int code = INSIDE;
        if (p.x < r.left) {
            // to the left of clip window
            code |= LEFT;
        } else if (p.x > r.right) {
            // to the right of clip window
            code |= RIGHT;
        }

        if (p.y > r.bottom) {
            // below the clip window
            code |= BOTTOM;
        } else if (p.y < r.top) {
            // above the clip window
            code |= TOP;
        }
        return code;
    }

}
