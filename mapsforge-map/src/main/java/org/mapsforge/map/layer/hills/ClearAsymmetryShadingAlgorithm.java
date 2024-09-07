/*
 * Copyright 2017 usrusr
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
package org.mapsforge.map.layer.hills;

import org.mapsforge.core.util.MercatorProjection;

import java.io.IOException;
import java.io.InputStream;

/**
 * Flat surfaces, or all surfaces with slope less than minSlope, will have no shade (they look the same as without hill shading);
 * Slopes are shaded non-linearly using sqrt mapping by default, so gentle slopes will have more pronounced differences between them.
 * Linear and square mappings are also available.
 * There is a special factor, less than 1, that determines in a simple way how much less shading the northwest slopes get,
 * as the idea is that NW slopes should be somewhat lighter than SE slopes.
 */
public class ClearAsymmetryShadingAlgorithm extends AbsShadingAlgorithmDefaults {

    /** Linear mapping from slope value to shade value. */
    public static final int MODE_LINEAR = 1;
    /** Steep slopes will have more pronounced differences between them. */
    public static final int MODE_SQUARE = 2;
    /** (Default) Gentle slopes will have more pronounced differences between them. */
    public static final int MODE_SQROOT = 3;
    public static final int MODE_DEFAULT = MODE_SQROOT;

    public static final double MAX_SLOPE_DEFAULT = 60;
    public static final double MIN_SLOPE_DEFAULT = 0;
    public static final double NW_FACTOR_DEFAULT = 0.6;

    public static final int SHADE_MIN = 0, SHADE_MAX = 255;

    public static double SqrtTwo = Math.sqrt(2);

    protected final double mMinSlope, mMaxSlope, mNorthWestFactor;
    protected final int mMode;

    /**
     * @param maxSlope        The smallest slope that will have the darkest shade.
     *                        All larger slopes will have the same shade, the darkest one.
     *                        [percentage, %]
     * @param minSlope        The largest slope that will have the lightest shade.
     *                        All smaller slopes will have the same shade, the lightest one.
     *                        Should be in the range [0..maxSlope>.
     *                        The default is 0 (zero).
     *                        [percentage, %]
     * @param northWestFactor Number in the range [0..1], as the idea is that NW slopes should be somewhat lighter than SE slopes.
     * @param mode            Should be one of the following:
     *                        MODE_SQRT (Default; Gentle slopes will have more pronounced differences between them),
     *                        MODE_SQUARE (Steep slopes will have more pronounced differences between them),
     *                        MODE_LINEAR (Linear mapping from slope value to shade value).
     */
    public ClearAsymmetryShadingAlgorithm(double maxSlope, double minSlope, double northWestFactor, int mode) {
        mMaxSlope = maxSlope;
        mMinSlope = minSlope;
        mNorthWestFactor = boundToLimits(0, northWestFactor, 1);
        mMode = mode;
    }

    /**
     * @param maxSlope        The smallest slope that will have the darkest shade.
     *                        All larger slopes will have the same shade, the darkest one.
     *                        [percentage, %]
     * @param northWestFactor Number in the range [0..1], as the idea is that NW slopes should be somewhat lighter than SE slopes.
     * @param mode            Should be one of the following:
     *                        MODE_SQRT (Default; Gentle slopes will have more pronounced differences between them),
     *                        MODE_SQUARE (Steep slopes will have more pronounced differences between them),
     *                        MODE_LINEAR (Linear mapping from slope value to shade value).
     */
    public ClearAsymmetryShadingAlgorithm(double maxSlope, double northWestFactor, int mode) {
        this(maxSlope, MIN_SLOPE_DEFAULT, northWestFactor, mode);
    }

    /**
     * Uses default minSlope = 0, and default mode (sqrt).
     *
     * @param maxSlope        The smallest slope that will have the darkest shade.
     *                        All larger slopes will have the same shade, the darkest one.
     *                        [percentage, %]
     * @param northWestFactor Number in the range [0..1], as the idea is that NW slopes should be somewhat lighter than SE slopes.
     */
    public ClearAsymmetryShadingAlgorithm(double maxSlope, double northWestFactor) {
        this(maxSlope, northWestFactor, MODE_DEFAULT);
    }

    /**
     * Uses default minSlope = 0, default northWestFactor = 0.6, and default mode (sqrt).
     *
     * @param maxSlope The smallest slope that will have the darkest shade.
     *                 All larger slopes will have the same shade, the darkest one.
     *                 [percentage, %]
     */
    public ClearAsymmetryShadingAlgorithm(double maxSlope) {
        this(maxSlope, NW_FACTOR_DEFAULT);
    }

    /**
     * Uses default maxSlope = 60, default minSlope = 0, default northWestFactor = 0.6, and default mode (sqrt).
     */
    public ClearAsymmetryShadingAlgorithm() {
        this(MAX_SLOPE_DEFAULT);
    }

    /**
     * Should return values from SHADE_MIN to SHADE_MAX (0 to 255) using whatever range required (within reason).
     *
     * @param slope [percentage, %]
     */
    protected byte getShade(final double slope) {
        double map = mapping(slope);

        if (slope < 0) {
            map = mNorthWestFactor * map;
        }

        return (byte) Math.round(map);
    }

    protected double mapping(final double slope) {
        double retVal = 0;

        switch (mMode)
        {
            case MODE_SQROOT:
            default:
                retVal = sqrtMapping(SHADE_MIN, SHADE_MAX, Math.abs(slope), mMinSlope, mMaxSlope);
                break;

            case MODE_LINEAR:
                retVal = linearMapping(SHADE_MIN, SHADE_MAX, Math.abs(slope), mMinSlope, mMaxSlope);
                break;

            case MODE_SQUARE:
                retVal = squareMapping(SHADE_MIN, SHADE_MAX, Math.abs(slope), mMinSlope, mMaxSlope);
                break;
        }

        return retVal;
    }

    public static double boundToLimits(final double min, final double value, final double max) {
        return Math.max(min, Math.min(value, max));
    }

    public static double safeSqrt(final double value) {
        return Math.sqrt(Math.max(0, value));
    }

    public static double square(final double value) {
        return value * value;
    }

    /**
     * Faster in the beginning, slower in the end.
     * To get a decreasing mapping, startLimit can be larger than the endLimit.
     */
    public static double sqrtMapping(double startLimit, double endLimit, double param, double paramLowLimit, double paramHighLimit) {
        double retVal = startLimit;

        if (paramLowLimit != paramHighLimit) {
            retVal = startLimit + boundToLimits(0, safeSqrt(Math.max(0, param - paramLowLimit) / (paramHighLimit - paramLowLimit)), 1) * (endLimit - startLimit);
        }

        return retVal;
    }

    /**
     * Slower in the beginning, faster in the end.
     * To get a decreasing mapping, startLimit can be larger than the endLimit.
     */
    public static double squareMapping(double startLimit, double endLimit, double param, double paramLowLimit, double paramHighLimit) {
        double retVal = startLimit;

        if (paramLowLimit != paramHighLimit) {
            retVal = startLimit + boundToLimits(0, square(Math.max(0, param - paramLowLimit) / (paramHighLimit - paramLowLimit)), 1) * (endLimit - startLimit);
        }

        return retVal;
    }

    /**
     * Linear, constant slope.
     * To get a decreasing mapping, start can be larger than the end.
     */
    public static double linearMapping(double start, double end, double param, double paramLow, double paramHigh) {
        double retVal = start;

        if (paramLow != paramHigh) {
            retVal = start + boundToLimits(0, (param - paramLow) / (paramHigh - paramLow), 1) * (end - start);
        }

        return retVal;
    }

    protected double getSlopeToUse(final double slope, final double slopeInPerpendicularDirection) {
        return Math.abs(slopeInPerpendicularDirection) > Math.abs(slope) ? slopeInPerpendicularDirection : slope;
    }

    @Override
    protected byte[] convert(InputStream din, int axisLength, int rowLen, int padding, HgtCache.HgtFileInfo fileInfo) throws IOException {
        final byte[] bytes = new byte[(axisLength + 2 * padding) * (axisLength + 2 * padding)];
        final short[] ringbuffer = new short[rowLen];

        int outidx = (axisLength + 2 * padding) * padding + padding;
        int rbcur = 0;
        {
            short last = 0;
            for (int col = 0; col < rowLen; col++) {
                last = readNext(din, last);
                ringbuffer[rbcur++] = last;
            }
        }

        double southPerPixel = MercatorProjection.calculateGroundResolution(fileInfo.southLat(), axisLength * 170L);
        double northPerPixel = MercatorProjection.calculateGroundResolution(fileInfo.northLat(), axisLength * 170L);

        double southPerPixelByLine = southPerPixel / (2 * axisLength);
        double northPerPixelByLine = northPerPixel / (2 * axisLength);

        for (int line = 1; line <= axisLength; line++) {
            if (rbcur >= rowLen) {
                rbcur = 0;
            }

            short nw = ringbuffer[rbcur];
            short sw = readNext(din, nw);
            ringbuffer[rbcur++] = sw;

            final double metersPerPixelDiagonal = SqrtTwo * (southPerPixelByLine * line + northPerPixelByLine * (axisLength - line));
            final double metersPerPixelDiagonalInv = 1. / metersPerPixelDiagonal;

            for (int col = 1; col <= axisLength; col++) {
                short ne = ringbuffer[rbcur];
                short se = readNext(din, ne);
                ringbuffer[rbcur++] = se;

                bytes[outidx++] = getShade(100 * getSlopeToUse(nw - se, ne - sw) * metersPerPixelDiagonalInv);

                nw = ne;
                sw = se;
            }
            outidx += 2 * padding;
        }

        return bytes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ClearAsymmetryShadingAlgorithm that = (ClearAsymmetryShadingAlgorithm) o;

        final boolean isMappingSame = Double.compare(mapping(0.5 * (mMaxSlope + mMinSlope)), that.mapping(0.5 * (that.mMaxSlope + that.mMinSlope))) == 0;

        return isMappingSame && Double.compare(mMinSlope, that.mMinSlope) == 0 && Double.compare(mMaxSlope, that.mMaxSlope) == 0 && Double.compare(mNorthWestFactor, that.mNorthWestFactor) == 0 && mMode == that.mMode;
    }

    @Override
    public int hashCode() {
        int result = Double.hashCode(mMinSlope);
        result = 31 * result + Double.hashCode(mMaxSlope);
        result = 31 * result + Double.hashCode(mNorthWestFactor);
        result = 31 * result + mMode;
        result = 31 * result + Double.hashCode(mapping(0.5 * (mMaxSlope + mMinSlope)));
        return result;
    }
}
