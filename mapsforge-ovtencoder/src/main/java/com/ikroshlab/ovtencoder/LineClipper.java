/*
 * Copyright 2012, 2013 Hannes Janetzek
 * Copyright 2016 Bezzu
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
 * from http://en.wikipedia.org/wiki/Cohen%E2%80%93
 * Sutherland_algorithm
 */

public class LineClipper {

    public static final int INSIDE = 0; // 0000
    public static final int LEFT = 1; // 0001
    public static final int RIGHT = 2; // 0010
    public static final int BOTTOM = 4; // 0100
    public static final int TOP = 8; // 1000

    private float xmin, xmax, ymin, ymax;

    public LineClipper(float minx, float miny, float maxx, float maxy) {
        this.xmin = minx;
        this.ymin = miny;
        this.xmax = maxx;
        this.ymax = maxy;
    }

    public void setRect(float minx, float miny, float maxx, float maxy) {
        this.xmin = minx;
        this.ymin = miny;
        this.xmax = maxx;
        this.ymax = maxy;
    }

    private int mPrevOutcode;
    private float mPrevX;
    private float mPrevY;

    public float outX1;
    public float outY1;
    public float outX2;
    public float outY2;

    public boolean clipStart(float x0, float y0) {
        mPrevX = x0;
        mPrevY = y0;

        mPrevOutcode = INSIDE;
        if (x0 < xmin)
            mPrevOutcode |= LEFT;
        else if (x0 > xmax)
            mPrevOutcode |= RIGHT;
        if (y0 < ymin)
            mPrevOutcode |= BOTTOM;
        else if (y0 > ymax)
            mPrevOutcode |= TOP;

        return mPrevOutcode == INSIDE;
    }

    public int outcode(float x, float y) {

        int outcode = INSIDE;
        if (x < xmin)
            outcode |= LEFT;
        else if (x > xmax)
            outcode |= RIGHT;
        if (y < ymin)
            outcode |= BOTTOM;
        else if (y > ymax)
            outcode |= TOP;

        return outcode;
    }

    /**
     * @return 0 if not intersection, 1 fully within, -1 clipped (and 'out' set
     * to new points)
     */
    public int clipNext(float x1, float y1) {
        int accept;

        int outcode = INSIDE;
        if (x1 < xmin)
            outcode |= LEFT;
        else if (x1 > xmax)
            outcode |= RIGHT;

        if (y1 < ymin)
            outcode |= BOTTOM;
        else if (y1 > ymax)
            outcode |= TOP;

        if ((mPrevOutcode | outcode) == 0) {
            // Bitwise OR is 0. Trivially accept
            accept = 1;
        } else if ((mPrevOutcode & outcode) != 0) {
            // Bitwise AND is not 0. Trivially reject
            accept = 0;
        } else {
            accept = clip(mPrevX, mPrevY, x1, y1, mPrevOutcode, outcode) ? -1 : 0;
        }
        mPrevOutcode = outcode;
        mPrevX = x1;
        mPrevY = y1;

        return accept;
    }

    public int clipSegment(float x1, float y1, float x2, float y2) {
        clipStart(x1, y1);
        return clipNext(x2, y2);
    }

    /* CohenSutherland clipping algorithm clips a line from
     * P0 = (x0, y0) to P1 = (x1, y1) against a rectangle with
     * diagonal from (xmin, ymin) to (xmax, ymax).
     * based on en.wikipedia.org/wiki/Cohen-Sutherland */
    private boolean clip(float x0, float y0, float x1, float y1, int outcode0, int outcode1) {
        boolean accept = false;

        while (true) {
            if ((outcode0 | outcode1) == 0) {
                /* Bitwise OR is 0. Trivially accept and get out of loop */
                accept = true;
                break;
            } else if ((outcode0 & outcode1) != 0) {
                /* Bitwise AND is not 0. Trivially reject and get out of loop */
                break;
            } else {
                /* failed both tests, so calculate the line segment to clip
                 * from an outside point to an intersection with clip edge */
                float x = 0;
                float y = 0;

                /* At least one endpoint is outside the clip rectangle; pick it. */
                int outcodeOut = (outcode0 == 0) ? outcode1 : outcode0;
                /* Now find the intersection point;
                 * use formulas y = y0 + slope * (x - x0), x = x0 + (1 / slope)
                 * * (y - y0) */
                if ((outcodeOut & TOP) != 0) {
                    /* point is above the clip rectangle */
                    x = x0 + (x1 - x0) * (ymax - y0) / (y1 - y0);
                    y = ymax;
                } else if ((outcodeOut & BOTTOM) != 0) {
                    /* point is below the clip rectangle */
                    x = x0 + (x1 - x0) * (ymin - y0) / (y1 - y0);
                    y = ymin;
                } else if ((outcodeOut & RIGHT) != 0) {
                    /* point is to the right of clip rectangle */
                    y = y0 + (y1 - y0) * (xmax - x0) / (x1 - x0);
                    x = xmax;
                } else if ((outcodeOut & LEFT) != 0) {
                    /* point is to the left of clip rectangle */
                    y = y0 + (y1 - y0) * (xmin - x0) / (x1 - x0);
                    x = xmin;
                }

                int outcode = INSIDE;
                if (x < xmin)
                    outcode |= LEFT;
                else if (x > xmax)
                    outcode |= RIGHT;
                if (y < ymin)
                    outcode |= BOTTOM;
                else if (y > ymax)
                    outcode |= TOP;

                /* Now we move outside point to intersection point to clip
                 * and get ready for next pass. */
                if (outcodeOut == outcode0) {
                    x0 = x;
                    y0 = y;
                    outcode0 = outcode;
                } else {
                    x1 = x;
                    y1 = y;
                    outcode1 = outcode;
                }
            }
        }
        if (accept) {
            outX1 = x0;
            outY1 = y0;
            outX2 = x1;
            outY2 = y1;
        }
        return accept;
    }

    public float[] getLine(float out[], int offset) {
        if (out == null)
            return new float[]{outX1, outY1, outX2, outY2};

        out[offset + 0] = outX1;
        out[offset + 1] = outY1;
        out[offset + 2] = outX2;
        out[offset + 3] = outY2;
        return out;
    }

    public int getPrevOutcode() {
        return mPrevOutcode;
    }

    public int clipLine(GeometryBuffer in, GeometryBuffer out) {
        out.clear();
        int pointPos = 0;
        int numLines = 0;

        for (int i = 0, n = in.index.length; i < n; i++) {
            int len = in.index[i];
            if (len < 0)
                break;

            if (len < 4) {
                pointPos += len;
                continue;
            }

            if (len == 0) {
                continue;
            }

            int inPos = pointPos;
            int end = inPos + len;

            float x = in.points[inPos++];
            float y = in.points[inPos++];

            boolean inside = clipStart(x, y);

            if (inside) {
                out.startLine();
                out.addPoint(x, y);
                numLines++;
            }

            while (inPos < end) {
                /* get the current way point coordinates */
                x = in.points[inPos++];
                y = in.points[inPos++];

                int clip = clipNext(x, y);
                if (clip == 0) {
                    /* current segment is fully outside */
                    inside = false; // needed?
                } else if (clip == 1) {
                    /* current segment is fully within */
                    out.addPoint(x, y);
                } else { /* clip == -1 */
                    if (inside) {
                        /* previous was inside */
                        out.addPoint(outX2, outY2);
                    } else {
                        /* previous was outside */
                        out.startLine();
                        numLines++;
                        out.addPoint(outX1, outY1);
                        out.addPoint(outX2, outY2);
                    }
                    inside = clipStart(x, y);
                }
            }
            pointPos = end;
        }
        return numLines;
    }
}
