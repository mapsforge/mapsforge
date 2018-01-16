/**
 * Copyright 2016 Andrey Novikov
 * Java implementation and adaptation for JTS / MapsForge of https://github.com/mapbox/polylabel
 * <p>
 * ISC License
 * Copyright (c) 2016 Mapbox
 * <p>
 * Permission to use, copy, modify, and/or distribute this software for any purpose
 * with or without fee is hereby granted, provided that the above copyright notice
 * and this permission notice appear in all copies.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS" AND ISC DISCLAIMS ALL WARRANTIES WITH REGARD TO
 * THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS.
 * IN NO EVENT SHALL ISC BE LIABLE FOR ANY SPECIAL, DIRECT, INDIRECT, OR
 * CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA
 * OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
 * ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS
 * SOFTWARE.
 */
package org.mapsforge.map.writer.util;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateFilter;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.logging.Logger;

public class PolyLabel {
    private static final Logger LOGGER = Logger.getLogger(PolyLabel.class.getName());

    private static final double PRECISION = 1.0 / (512 << 14); // a logical pixel for zoom 14
    private static final double SQRT2 = Math.sqrt(2);

    /**
     * Returns pole of inaccessibility, the most distant internal point from the polygon outline.
     *
     * @param geometry geometry that can be correctly interpreted as a polygon
     * @return optimal label placement point
     */
    public static Point get(Geometry geometry) {
        // Get polygon from geometry
        Polygon polygon;
        if (geometry instanceof LineString) {
            // Validity of LineString is checked outside of this scope
            polygon = geometry.getFactory().createPolygon(geometry.getCoordinates());
        } else if (geometry instanceof MultiPolygon) {
            // For MultiPolygon the widest Polygon is taken
            Geometry widestGeometry = geometry.getGeometryN(0);
            for (int i = 1; i < geometry.getNumGeometries(); i++) {
                if (geometry.getGeometryN(i).getEnvelopeInternal().getWidth() >
                        widestGeometry.getEnvelopeInternal().getWidth()) {
                    widestGeometry = geometry.getGeometryN(i);
                }
            }
            polygon = (Polygon) widestGeometry;
        } else if (geometry instanceof Polygon) {
            polygon = (Polygon) geometry;
        } else {
            LOGGER.warning("Failed to get label for geometry: " + geometry);
            return geometry.getCentroid();
        }

        // As original geometry is used in other places clone it before re-projecting
        polygon = (Polygon) polygon.clone();

        // Re-project coordinates. This is needed to get proper visual results for polygons
        // distorted my Mercator projection
        polygon.apply(new CoordinateFilter() {
            @Override
            public void filter(Coordinate c) {
                c.x = longitudeToX(c.x);
                c.y = latitudeToY(c.y);
            }
        });
        polygon.geometryChanged();

        Envelope envelope = polygon.getEnvelopeInternal();

        double width = envelope.getWidth();
        double height = envelope.getHeight();
        double cellSize = Math.min(width, height);
        double h = cellSize / 2;

        // A priority queue of cells in order of their "potential" (max distance to polygon)
        PriorityQueue<Cell> cellQueue = new PriorityQueue<>(1, new MaxComparator());

        // Cover polygon with initial cells
        for (double x = envelope.getMinX(); x < envelope.getMaxX(); x += cellSize) {
            for (double y = envelope.getMinY(); y < envelope.getMaxY(); y += cellSize) {
                cellQueue.add(new Cell(x + h, y + h, h, polygon));
            }
        }

        // Take centroid as the first best guess
        Cell bestCell = getCentroidCell(polygon);

        // Special case for rectangular polygons
        Cell bboxCell = new Cell(envelope.centre().x, envelope.centre().y, 0, polygon);
        if (bboxCell.d > bestCell.d) bestCell = bboxCell;

        while (!cellQueue.isEmpty()) {
            // Pick the most promising cell from the queue
            Cell cell = cellQueue.remove();

            // Update the best cell if we found a better one
            if (cell.d > bestCell.d)
                bestCell = cell;

            // Do not drill down further if there's no chance of a better solution
            if (cell.max - bestCell.d <= PRECISION) continue;

            // Split the cell into four cells
            h = cell.h / 2;
            cellQueue.add(new Cell(cell.x - h, cell.y - h, h, polygon));
            cellQueue.add(new Cell(cell.x + h, cell.y - h, h, polygon));
            cellQueue.add(new Cell(cell.x - h, cell.y + h, h, polygon));
            cellQueue.add(new Cell(cell.x + h, cell.y + h, h, polygon));
        }

        // Return the best found point projected back to geodesic coordinates
        return geometry.getFactory().createPoint(new Coordinate(toLongitude(bestCell.x), toLatitude(bestCell.y)));
    }

    private static class MaxComparator implements Comparator<Cell> {
        @Override
        public int compare(Cell a, Cell b) {
            return Double.compare(b.max, a.max);
        }
    }

    private static class Cell {
        final double x;
        final double y;
        final double h;
        final double d;
        final double max;

        Cell(double x, double y, double h, Polygon polygon) {
            this.x = x; // cell center x
            this.y = y; // cell center y
            this.h = h; // half the cell size
            this.d = pointToPolygonDist(x, y, polygon); // distance from cell center to polygon
            this.max = this.d + this.h * SQRT2; // max distance to polygon within a cell
        }
    }

    // Signed distance from point to polygon outline (negative if point is outside)
    private static float pointToPolygonDist(double x, double y, Polygon polygon) {
        boolean inside = false;
        double minDistSq = Double.POSITIVE_INFINITY;

        // External ring
        LineString exterior = polygon.getExteriorRing();
        for (int i = 0, n = exterior.getNumPoints() - 1, j = n - 1; i < n; j = i, i++) {
            Coordinate a = exterior.getCoordinateN(i);
            Coordinate b = exterior.getCoordinateN(j);

            if (((a.y > y) ^ (b.y > y)) &&
                    (x < (b.x - a.x) * (y - a.y) / (b.y - a.y) + a.x))
                inside = !inside;

            double seqDistSq = getSegDistSq(x, y, a, b);
            minDistSq = Math.min(minDistSq, seqDistSq);
        }
        // Internal rings
        for (int k = 0; k < polygon.getNumInteriorRing(); k++) {
            LineString interior = polygon.getInteriorRingN(k);
            for (int i = 0, n = interior.getNumPoints() - 1, j = n - 1; i < n; j = i, i++) {
                Coordinate a = interior.getCoordinateN(i);
                Coordinate b = interior.getCoordinateN(j);

                if (((a.y > y) ^ (b.y > y)) &&
                        (x < (b.x - a.x) * (y - a.y) / (b.y - a.y) + a.x))
                    inside = !inside;

                minDistSq = Math.min(minDistSq, getSegDistSq(x, y, a, b));
            }
        }

        return (float) ((inside ? 1 : -1) * Math.sqrt(minDistSq));
    }

    // Get polygon centroid
    private static Cell getCentroidCell(Polygon polygon) {
        double area = 0.0;
        double x = 0.0;
        double y = 0.0;

        LineString exterior = polygon.getExteriorRing();

        for (int i = 0, n = exterior.getNumPoints() - 1, j = n - 1; i < n; j = i, i++) {
            Coordinate a = exterior.getCoordinateN(i);
            Coordinate b = exterior.getCoordinateN(j);
            double f = a.x * b.y - b.x * a.y;
            x += (a.x + b.x) * f;
            y += (a.y + b.y) * f;
            area += f * 3.0;
        }
        return new Cell(x / area, y / area, 0.0, polygon);
    }

    // Get squared distance from a point to a segment
    private static double getSegDistSq(double px, double py, Coordinate a, Coordinate b) {
        double x = a.x;
        double y = a.y;
        double dx = b.x - x;
        double dy = b.y - y;

        if (dx != 0f || dy != 0f) {
            double t = ((px - x) * dx + (py - y) * dy) / (dx * dx + dy * dy);

            if (t > 1) {
                x = b.x;
                y = b.y;
            } else if (t > 0) {
                x += dx * t;
                y += dy * t;
            }
        }

        dx = px - x;
        dy = py - y;

        return dx * dx + dy * dy;
    }

    // Re-projection functions

    private static double latitudeToY(double latitude) {
        double sinLatitude = Math.sin(latitude * (Math.PI / 180));
        return 0.5 - Math.log((1 + sinLatitude) / (1 - sinLatitude)) / (4 * Math.PI);
    }

    private static double longitudeToX(double longitude) {
        return (longitude + 180.0) / 360.0;
    }

    private static double toLatitude(double y) {
        return 90 - 360 * Math.atan(Math.exp((y - 0.5) * (2 * Math.PI))) / Math.PI;
    }

    private static double toLongitude(double x) {
        return 360.0 * (x - 0.5);
    }
}
