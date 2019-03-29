package org.mapsforge.core.model;

import java.util.ArrayList;
import java.util.List;

public class LineString {
    private List<LineSegment> segments = new ArrayList<>();

    public void LineString() {
    }

    public void addSegment(LineSegment segment) {
        this.segments.add(segment);
    }

    public LineSegment getSegment(int index) {
        return segments.get(index);
    }

    public double length() {
        double res = 0;
        for (LineSegment segment : segments)
            res += segment.length();
        return res;
    }

    public int segmentCount() {
        return segments.size();
    }

    /**
     * Interpolates on the segment and returns the coordinate of the interpolation result.
     * Returns null if distance is < 0 or > length()
     * @param distance
     * @return
     */
    public Point interpolate(double distance) {
        if (distance < 0)
            return null;

        for (LineSegment seg : segments) {
            double segLen = seg.length();
            if (distance <= segLen)
                return seg.pointAlongLineSegment(distance);
            distance -= segLen;
        }
        return null;
    }

    /** Creates a new LineString that consists of only the part between startDist and endDist
    */
    public LineString extractPart(double startDist, double endDist) {
        LineString res = new LineString();

        for (int i = 0; i < segments.size(); startDist -= segments.get(i).length(), endDist -= segments.get(i).length(), i++) {
            LineSegment seg = segments.get(i);

            // Skip first segments that we don't need
            double segLen = seg.length();
            if (segLen < startDist)
                continue;

            Point startPoint = null, endPoint = null;
            if (startDist >= 0) {
                // This will be our starting point
                startPoint = seg.pointAlongLineSegment(startDist);
            }
            if (endDist < segLen) {
                // this will be our ending point
                endPoint = seg.pointAlongLineSegment(endDist);
            }

            if (startPoint != null && endPoint == null) {
                // This ist the starting segment, end will come in a later segment
                res.addSegment(new LineSegment(startPoint, seg.end));
            } else if (startPoint == null && endPoint == null) {
                // Center segment between start and end segment. Add completely
                res.addSegment(seg);
            } else if (startPoint == null && endPoint != null) {
                // End segment, start was in earlier segment
                res.addSegment(new LineSegment(seg.start, endPoint));
            } else if (startPoint != null && endPoint != null) {
                // Start and end on same segment
                res.addSegment(new LineSegment(startPoint, endPoint));
            }


            if (endPoint != null)
                break;
        }

        return res;
    }

    public Rectangle getBoundingRect() {
        double xmin = Double.MAX_VALUE;
        double ymin = Double.MAX_VALUE;
        double xmax = Double.MIN_VALUE;
        double ymax = Double.MIN_VALUE;

        for (LineSegment seg : segments) {
            xmin = Math.min(xmin, Math.min(seg.start.x, seg.end.x));
            ymin = Math.min(ymin, Math.min(seg.start.y, seg.end.y));
            xmax = Math.max(xmax, Math.max(seg.start.x, seg.end.x));
            ymax = Math.max(ymax, Math.max(seg.start.y, seg.end.y));
        }
        return new Rectangle(xmin, ymin, xmax, ymax);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof LineString))
            return false;

        LineString other = (LineString) o;
        if (other.segmentCount() != segmentCount())
            return false;
        for (int i = 0; i < segmentCount(); i++) {
            if (!getSegment(i).equals(other.getSegment(i)))
                return false;
        }
        return true;
    }
}
