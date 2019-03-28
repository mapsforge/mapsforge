/*
 * Copyright 2019 Adrian Batzill
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

import java.util.ArrayList;
import java.util.List;

public class LineString {
    public final List<LineSegment> segments = new ArrayList<>();

    public void LineString() {
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof LineString)) {
            return false;
        }
        LineString other = (LineString) obj;
        if (other.segments.size() != segments.size()) {
            return false;
        }
        for (int i = 0; i < segments.size(); i++) {
            if (!segments.get(i).equals(other.segments.get(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Creates a new LineString that consists of only the part between startDistance and endDistance.
     */
    public LineString extractPart(double startDistance, double endDistance) {
        LineString result = new LineString();

        for (int i = 0; i < this.segments.size(); startDistance -= this.segments.get(i).length(), endDistance -= this.segments.get(i).length(), i++) {
            LineSegment segment = this.segments.get(i);

            // Skip first segments that we don't need
            double length = segment.length();
            if (length < startDistance) {
                continue;
            }

            Point startPoint = null, endPoint = null;
            if (startDistance >= 0) {
                // This will be our starting point
                startPoint = segment.pointAlongLineSegment(startDistance);
            }
            if (endDistance < length) {
                // this will be our ending point
                endPoint = segment.pointAlongLineSegment(endDistance);
            }

            if (startPoint != null && endPoint == null) {
                // This ist the starting segment, end will come in a later segment
                result.segments.add(new LineSegment(startPoint, segment.end));
            } else if (startPoint == null && endPoint == null) {
                // Center segment between start and end segment, add completely
                result.segments.add(segment);
            } else if (startPoint == null && endPoint != null) {
                // End segment, start was in earlier segment
                result.segments.add(new LineSegment(segment.start, endPoint));
            } else if (startPoint != null && endPoint != null) {
                // Start and end on same segment
                result.segments.add(new LineSegment(startPoint, endPoint));
            }

            if (endPoint != null)
                break;
        }

        return result;
    }

    public Rectangle getBounds() {
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double maxY = Double.MIN_VALUE;

        for (LineSegment segment : this.segments) {
            minX = Math.min(minX, Math.min(segment.start.x, segment.end.x));
            minY = Math.min(minY, Math.min(segment.start.y, segment.end.y));
            maxX = Math.max(maxX, Math.max(segment.start.x, segment.end.x));
            maxY = Math.max(maxY, Math.max(segment.start.y, segment.end.y));
        }
        return new Rectangle(minX, minY, maxX, maxY);
    }

    /**
     * Interpolates on the segment and returns the coordinate of the interpolation result.
     * Returns null if distance is < 0 or > length().
     */
    public Point interpolate(double distance) {
        if (distance < 0) {
            return null;
        }

        for (LineSegment segment : this.segments) {
            double length = segment.length();
            if (distance <= length) {
                return segment.pointAlongLineSegment(distance);
            }
            distance -= length;
        }
        return null;
    }

    public double length() {
        double result = 0;
        for (LineSegment segment : this.segments) {
            result += segment.length();
        }
        return result;
    }
}
