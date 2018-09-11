/*
 * Copyright 2013 Hannes Janetzek
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


/**
 * Clip polygons and lines to a rectangle. Output cannot expected to be valid
 * Simple-Feature geometry, i.e. all polygon rings are clipped independently
 * so that inner and outer rings might touch, etc.
 * <p/>
 * based on http://www.cs.rit.edu/~icss571/clipTrans/PolyClipBack.html
 */
public class TileClipper {
    private float xmin;
    private float xmax;
    private float ymin;
    private float ymax;

    public TileClipper(float xmin, float ymin, float xmax, float ymax) {
        this.xmin = xmin;
        this.ymin = ymin;
        this.xmax = xmax;
        this.ymax = ymax;
        mLineClipper = new LineClipper(xmin, ymin, xmax, ymax);
    }

    public void setRect(float xmin, float ymin, float xmax, float ymax) {
        this.xmin = xmin;
        this.ymin = ymin;
        this.xmax = xmax;
        this.ymax = ymax;
        mLineClipper.setRect(xmin, ymin, xmax, ymax);
    }

    private final LineClipper mLineClipper;

    private final GeometryBuffer mGeomOut = new GeometryBuffer(10, 1);

    public boolean clip(GeometryBuffer geom) {
        if (geom.isPoly()) {

            GeometryBuffer out = mGeomOut;
            out.clear();

            clipEdge(geom, out, LineClipper.LEFT);
            geom.clear();

            clipEdge(out, geom, LineClipper.TOP);
            out.clear();

            clipEdge(geom, out, LineClipper.RIGHT);
            geom.clear();

            clipEdge(out, geom, LineClipper.BOTTOM);

            if ((geom.indexCurrentPos == 0) && (geom.index[0] < 6))
                return false;
        } else if (geom.isLine()) {

            GeometryBuffer out = mGeomOut;
            out.clear();

            int numLines = mLineClipper.clipLine(geom, out);

            int idx[] = geom.ensureIndexSize(numLines + 1, false);
            System.arraycopy(out.index, 0, idx, 0, numLines);
            geom.index[numLines] = -1;

            float pts[] = geom.ensurePointSize(out.pointNextPos >> 1, false);
            System.arraycopy(out.points, 0, pts, 0, out.pointNextPos);
            geom.indexCurrentPos = out.indexCurrentPos;
            geom.pointNextPos = out.pointNextPos;

            if ((geom.indexCurrentPos == 0) && (geom.index[0] < 4))
                return false;
        }
        return true;
    }

    private boolean clipEdge(GeometryBuffer in, GeometryBuffer out, int edge) {

        out.startPolygon();
        boolean outer = true;

        int pointPos = 0;

        for (int indexPos = 0, n = in.index.length; indexPos < n; indexPos++) {
            int len = in.index[indexPos];
            if (len < 0)
                break;

            if (len == 0) {
                out.startPolygon();
                outer = true;
                continue;
            }

            if (len < 6) {
                pointPos += len;
                continue;
            }

            if (!outer)
                out.startHole();

            switch (edge) {
                case LineClipper.LEFT:
                    clipRingLeft(indexPos, pointPos, in, out);
                    break;
                case LineClipper.RIGHT:
                    clipRingRight(indexPos, pointPos, in, out);
                    break;
                case LineClipper.TOP:
                    clipRingTop(indexPos, pointPos, in, out);
                    break;
                case LineClipper.BOTTOM:
                    clipRingBottom(indexPos, pointPos, in, out);
                    break;
            }

            //if (out.index[i] < 6) {
            //    out.index[i] = 0;
            //    //if (out.indexPos > 0)
            //    //    out.indexPos--;
            //    // TODO if outer skip holes
            //}

            pointPos += len;

            outer = false;
        }
        return true;
    }

    private void clipRingLeft(int indexPos, int pointPos, GeometryBuffer in, GeometryBuffer out) {
        int end = in.index[indexPos] + pointPos;
        float px = in.points[end - 2];
        float py = in.points[end - 1];

        for (int i = pointPos; i < end; ) {
            float cx = in.points[i++];
            float cy = in.points[i++];
            if (cx > xmin) {
                /* current is inside */
                if (px > xmin) {
                    /* previous was inside */
                    out.addPoint(cx, cy);
                } else {
                    /* previous was outside, add edge point */
                    out.addPoint(xmin, py + (cy - py) * (xmin - px) / (cx - px));
                    out.addPoint(cx, cy);
                }
            } else {
                if (px > xmin) {
                    /* previous was inside, add edge point */
                    out.addPoint(xmin, py + (cy - py) * (xmin - px) / (cx - px));
                }
                /* else skip point */
            }
            px = cx;
            py = cy;
        }
    }

    private void clipRingRight(int indexPos, int pointPos, GeometryBuffer in, GeometryBuffer out) {
        int len = in.index[indexPos] + pointPos;
        float px = in.points[len - 2];
        float py = in.points[len - 1];

        for (int i = pointPos; i < len; ) {
            float cx = in.points[i++];
            float cy = in.points[i++];

            if (cx < xmax) {
                if (px < xmax) {
                    out.addPoint(cx, cy);
                } else {
                    out.addPoint(xmax, py + (cy - py) * (xmax - px) / (cx - px));
                    out.addPoint(cx, cy);
                }
            } else {
                if (px < xmax) {
                    out.addPoint(xmax, py + (cy - py) * (xmax - px) / (cx - px));
                }
            }
            px = cx;
            py = cy;
        }
    }

    private void clipRingTop(int indexPos, int pointPos, GeometryBuffer in, GeometryBuffer out) {
        int len = in.index[indexPos] + pointPos;
        float px = in.points[len - 2];
        float py = in.points[len - 1];

        for (int i = pointPos; i < len; ) {
            float cx = in.points[i++];
            float cy = in.points[i++];

            if (cy < ymax) {
                if (py < ymax) {
                    out.addPoint(cx, cy);
                } else {
                    out.addPoint(px + (cx - px) * (ymax - py) / (cy - py), ymax);
                    out.addPoint(cx, cy);
                }
            } else {
                if (py < ymax) {
                    out.addPoint(px + (cx - px) * (ymax - py) / (cy - py), ymax);
                }
            }
            px = cx;
            py = cy;
        }
    }

    private void clipRingBottom(int indexPos, int pointPos, GeometryBuffer in, GeometryBuffer out) {
        int len = in.index[indexPos] + pointPos;
        float px = in.points[len - 2];
        float py = in.points[len - 1];

        for (int i = pointPos; i < len; ) {
            float cx = in.points[i++];
            float cy = in.points[i++];
            if (cy > ymin) {
                if (py > ymin) {
                    out.addPoint(cx, cy);
                } else {
                    out.addPoint(px + (cx - px) * (ymin - py) / (cy - py), ymin);
                    out.addPoint(cx, cy);
                }
            } else {
                if (py > ymin) {
                    out.addPoint(px + (cx - px) * (ymin - py) / (cy - py), ymin);
                }
            }
            px = cx;
            py = cy;
        }
    }
}
