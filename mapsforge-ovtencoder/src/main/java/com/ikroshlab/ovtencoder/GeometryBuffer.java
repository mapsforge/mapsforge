/*
 * Copyright 2013 Hannes Janetzek
 * Copyright 2016 Andrey Novikov
 * Copyright 2017 Gustl22
 *
 * This file is part of the OpenScienceMap project (http://www.opensciencemap.org).
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
package com.ikroshlab.ovtencoder;

import java.util.Arrays;
import org.oscim.core.Point;
import org.oscim.core.PointF;

/* TODO
 * - check indexPos < Short.Max
 * - should make internals private, maybe
 */

/**
 * The GeometryBuffer class holds temporary geometry data for processing.
 * Only One geometry type can be set at a time. Use 'clear()' to reset the
 * internal state.
 * <p/>
 * 'points[]' holds interleaved x,y coordinates
 * <p/>
 * 'index[]' is used to store number of points within a geometry and encode
 * multi-linestrings and (multi-)polygons.
 */
// extends org.oscim.core.GeometryBuffer
public class GeometryBuffer  {

    private final static int GROW_INDICES = 1;
    private final static int GROW_POINTS  = 1;

    /**
     * The Enum GeometryType.
     */
    public enum GeometryType {
        NONE(0),
        POINT(1),
        LINE(2),
        POLY(3),
        TRIS(4);

        private GeometryType(int type) {
            nativeInt = type;
        }

        public final int nativeInt;
    }

    /**
     * The points.
     * <p>
     * POLY/LINE: store point in order of polygon with
     * points[2 * n + 0] = x; points[2 * n + 1] = y; n is a N.
     * <p>
     * MESH: store points anywhere with
     * points[3 * n + 0] = x; points[3 * n + 1] = y; points[3 * n + 2] = z; n ∈ ℕ0.
     */
    public float[] points;

    /**
     * The indexes.
     * <p>
     * POLY/LINE: store 2 * number of points of each polygon / line. Point is (x, y).
     * <p>
     * MESH: store point indices of triangle (p1, p2, p3) with
     * index[3 * n + 0] = p1; index[3 * n + 1] = p2; index[3 * n + 2] = p3; n ∈ ℕ0.
     * Point p is (x, y, z).
     */
    public int[]   index;

    /**
     * The current index position.
     */
    public int     indexCurrentPos;

    /**
     * The next position to insert a point in points array (equal to array size).
     */
    public int     pointNextPos;

    /**
     * The current geometry type.
     */
    public GeometryType type;

    public PointF  mTmpPoint = new PointF();
    public int     pointLimit;

    public GeometryBuffer() {
        this(32, 4);
    }

    /**
     * Instantiates a new geometry buffer.
     *
     * @param numPoints  the num of expected points
     * @param numIndices the num of expected indices
     */
    public GeometryBuffer(int numPoints, int numIndices) {
        this(new float[numPoints * 2], new int[numIndices]);
    }

    /**
     * Instantiates a new geometry buffer.
     *
     * @param points the points
     * @param index  the index
     */
    public GeometryBuffer(float[] points, int[] index) {
        if (points == null)  points = new float[GROW_POINTS];
        if (index == null)   index  = new int[GROW_INDICES];

        this.points          = points;
        this.index           = index;
        this.type            = GeometryType.NONE;
        this.indexCurrentPos = 0;
        this.pointNextPos    = 0;
        this.pointLimit      = points.length - 2;
        
        //logDebug(" numPoints: " + points.length + "  GROW_POINTS: " + GROW_POINTS);
        //logDebug("numIndices: " + index.length  + " GROW_INDICES: " + GROW_INDICES);
    }
    
    private void logDebug(String msg) { System.out.println(" ### Geometry ###  " + msg); }
    

    /**
     * @param buffer the buffer to copy
     */
    public GeometryBuffer(GeometryBuffer buffer) {
        int indexSize = 0;
        while (indexSize < buffer.index.length && buffer.index[indexSize] != -1) {
            indexSize++;
        }
        this.points = Arrays.copyOf(buffer.points, buffer.pointNextPos);
        this.index = Arrays.copyOf(buffer.index, indexSize);

        this.pointNextPos = buffer.pointNextPos;
        this.indexCurrentPos = buffer.indexCurrentPos;
        this.type = buffer.type;
    }



    public int getTypeInt() {
        if      (this.type == GeometryType.NONE)  return 0;
        else if (this.type == GeometryType.POINT) return 1;
        else if (this.type == GeometryType.LINE)  return 2;
        else if (this.type == GeometryType.POLY)  return 3;
        else if (this.type == GeometryType.TRIS)  return 4;
        else                                      return -1;
    }

    public void setTypeInt(int t) {
        if      (t == 0) this.type = GeometryType.NONE;
        else if (t == 1) this.type = GeometryType.POINT;
        else if (t == 2) this.type = GeometryType.LINE;
        else if (t == 3) this.type = GeometryType.POLY;
        else if (t == 4) this.type = GeometryType.TRIS;
        else             this.type = GeometryType.NONE;
    }




    /**
     * @param out PointF to set coordinates to.
     * @return when out is null a temporary PointF is
     * returned which belongs to GeometryBuffer.
     */
    public void getPoint(int i, PointF out) {
        out.x = points[(i << 1)];
        out.y = points[(i << 1) + 1];
    }

    public float getPointX(int i) {
        return points[(i << 1)];
    }

    public float getPointY(int i) {
        return points[(i << 1) + 1];
    }

    /**
     * @return PointF belongs to GeometryBuffer.
     */
    public PointF getPoint(int i) {
        PointF out = mTmpPoint;
        out.x = points[(i << 1)];
        out.y = points[(i << 1) + 1];
        return out;
    }

    public int getNumPoints() {
        return pointNextPos >> 1;
    }

    /**
     * Reset buffer.
     */
    public GeometryBuffer clear() {
        index[0] = 0;
        indexCurrentPos = 0;
        pointNextPos = 0;
        type = GeometryType.NONE;
        return this;
    }

    /**
     * Adds a point with the coordinate x, y.
     *
     * @param x the x ordinate
     * @param y the y ordinate
     */
    public GeometryBuffer addPoint(float x, float y) {
        if (pointNextPos > pointLimit)
            ensurePointSize((pointNextPos >> 1) + 1, true);

        points[pointNextPos++] = x;
        points[pointNextPos++] = y;

        index[indexCurrentPos] += 2;
        return this;
    }

    public boolean isPoly() {
        return type == GeometryType.POLY;
    }

    public boolean isLine() {
        return type == GeometryType.LINE;
    }

    public boolean isPoint() {
        return type == GeometryType.POINT;
    }

    public boolean isTris() {
        return type == GeometryType.TRIS;
    }

    /**
     * Sets the point x,y at position pos.
     *
     * @param pos the pos
     * @param x   the x ordinate
     * @param y   the y ordinate
     */
    public void setPoint(int pos, float x, float y) {
        points[(pos << 1) + 0] = x;
        points[(pos << 1) + 1] = y;
    }

    /**
     * Set geometry type for points.
     */
    public void startPoints() {
        setOrCheckMode(GeometryType.POINT);
    }

    /**
     * Start a new line. Sets geometry type for lines.
     */
    public GeometryBuffer startLine() {
        setOrCheckMode(GeometryType.LINE);

        /* ignore */
        if (index[indexCurrentPos] > 0) {

            /* start next */
            if ((index[0] >= 0) && (++indexCurrentPos >= index.length))
                ensureIndexSize(indexCurrentPos, true);

            /* initialize with zero points */
            index[indexCurrentPos] = 0;
        }

        /* set new end marker */
        if (index.length > indexCurrentPos + 1)
            index[indexCurrentPos + 1] = -1;
        return this;
    }

    /**
     * Start a new polygon. Sets geometry type for polygons.
     */
    public GeometryBuffer startPolygon() {
        boolean start = (type == GeometryType.NONE);
        setOrCheckMode(GeometryType.POLY);

        if ((indexCurrentPos + 3) > index.length)
            ensureIndexSize(indexCurrentPos + 2, true);

        if (!start && index[indexCurrentPos] != 0) {
            /* end polygon */
            index[++indexCurrentPos] = 0;

            /* next polygon start */
            indexCurrentPos++;
        }

        /* initialize with zero points */
        index[indexCurrentPos] = 0;

        /* set new end marker */
        if (index.length > indexCurrentPos + 1)
            index[indexCurrentPos + 1] = -1;

        return this;
    }

    /**
     * Starts a new polygon hole (inner ring).
     */
    public void startHole() {
        checkMode(GeometryType.POLY);

        if ((indexCurrentPos + 2) > index.length)
            ensureIndexSize(indexCurrentPos + 1, true);

        /* initialize with zero points */
        index[++indexCurrentPos] = 0;

        /* set new end marker */
        if (index.length > indexCurrentPos + 1)
            index[indexCurrentPos + 1] = -1;
    }

    public GeometryBuffer translate(float dx, float dy) {
        for (int i = 0; i < pointNextPos; i += 2) {
            points[i] += dx;
            points[i + 1] += dy;
        }
        return this;
    }

    public GeometryBuffer scale(float scaleX, float scaleY) {
        for (int i = 0; i < pointNextPos; i += 2) {
            points[i] *= scaleX;
            points[i + 1] *= scaleY;
        }
        return this;
    }

    /**
     * Ensure that 'points' array can hold the number of points.
     *
     * @param size the number of points to hold
     * @param copy the the current data when array is reallocated
     * @return the float[] array holding current coordinates
     */
    public float[] ensurePointSize(int size, boolean copy) {
        if (size * 2 < points.length)
            return points;

        size = size * 2 + GROW_POINTS;

        float[] newPoints = new float[size];
        if (copy)
            System.arraycopy(points, 0, newPoints, 0, points.length);

        points = newPoints;
        pointLimit = size - 2;

        return points;
    }

    /**
     * Ensure index size.
     *
     * @param size the size
     * @param copy the copy
     * @return the short[] array holding current index
     */
    public int[] ensureIndexSize(int size, boolean copy) {
        if (size < index.length)
            return index;

        int[] newIndex = new int[size + GROW_INDICES];
        if (copy)
            System.arraycopy(index, 0, newIndex, 0, index.length);

        index = newIndex;

        return index;
    }

    private void setOrCheckMode(GeometryType m) {
        if (type == m)
            return;

        if (type != GeometryType.NONE)
            throw new IllegalArgumentException("not cleared " + m + "<>" + type);

        type = m;
    }

    private void checkMode(GeometryType m) {
        if (type != m)
            throw new IllegalArgumentException("not cleared " + m + "<>" + type);
    }

    public void addPoint(Point p) {
        addPoint((float) p.x, (float) p.y);
    }

    public void addPoint(PointF p) {
        addPoint(p.x, p.y);
    }

    /**
     * Remove points with distance less than minSqDist
     * <p/>
     * TODO could avoid superfluous copying
     *
     * @param minSqDist
     * @param keepLines keep endpoint when line would
     *                  otherwise collapse into a single point
     */
    public void simplify(float minSqDist, boolean keepLines) {
        int outPos = 0;
        int inPos = 0;
        for (int idx = 0; idx < index.length; idx++) {
            if (index[idx] < 0)
                break;
            if (index[idx] == 0)
                continue;

            int first = inPos;
            float px = points[inPos++];
            float py = points[inPos++];

            /* add first point */
            points[outPos++] = px;
            points[outPos++] = py;
            int cnt = 2;

            for (int pt = 2, end = index[idx]; pt < end; pt += 2) {
                float cx = points[inPos++];
                float cy = points[inPos++];
                float dx = cx - px;
                float dy = cy - py;

                if ((dx * dx + dy * dy) < minSqDist) {
                    if (!keepLines || (pt < end - 2))
                        continue;
                }
                px = cx;
                py = cy;
                points[outPos++] = cx;
                points[outPos++] = cy;
                cnt += 2;
            }

            if ((type == GeometryType.POLY) &&
                    (points[first] == px) &&
                    (points[first + 1] == py)) {
                /* remove identical start/end point */
                cnt -= 2;
                outPos -= 2;
            }
            index[idx] = cnt;
        }
    }

    /**
     * Calculates geometry area, only polygon outer ring is taken into account.
     *
     * @return polygon area, 0 for other geometries
     */
    public float area() {
        if (isPoint() || isLine() || getNumPoints() < 3)
            return 0f;

        float area = 0f;
        // use only outer ring
        int n = index[0];

        for (int i = 0; i < n - 2; i += 2) {
            area = area + (points[i] * points[i + 3]) - (points[i + 1] * points[i + 2]);
        }
        area = area + (points[n - 2] * points[1]) - (points[n - 1] * points[0]);

        return 0.5f * area;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        int o = 0;
        for (int i = 0; i < index.length; i++) {
            if (index[i] < 0)
                break;

            if (!isTris()) {
                if (index[i] == 0)
                    continue;
                sb.append("POLY (")
                        .append(i)
                        .append(") { ");

                for (int j = 0; j < index[i]; j += 2) {
                    sb.append('[')
                            .append(points[o + j])
                            .append(", ")
                            .append(points[o + j + 1])
                            .append(']');

                    if (j % 4 == 0)
                        sb.append('\n');
                }
                sb.append(" } \tnumPoints:")
                        .append(index[i])
                        .append('\n');
                o += index[i];
            } else {
                if (i % 3 == 0)
                    sb.append("TRIS { ");
                sb.append('\t')
                        .append(index[i])
                        .append('[')
                        .append(points[3 * index[i]])
                        .append(", ")
                        .append(points[3 * index[i] + 1])
                        .append(", ")
                        .append(points[3 * index[i] + 2])
                        .append(']');
                if (i % 3 == 2)
                    sb.append(" }\n");
            }
        }
        return sb.toString();
    }

    public static GeometryBuffer makeCircle(float x, float y,
                                            float radius, int segments) {
        GeometryBuffer g = new GeometryBuffer(segments, 1);
        makeCircle(g, x, y, radius, segments);
        return g;
    }

    public static GeometryBuffer makeCircle(GeometryBuffer g,
                                            float x, float y, float radius, int segments) {
        g.clear();
        g.startPolygon();
        for (int i = 0; i < segments; i++) {
            double rad = Math.toRadians(i * (360f / segments));

            g.addPoint((float) (x + Math.cos(rad) * radius),
                    (float) (y + Math.sin(rad) * radius));
        }
        return g;
    }
}
