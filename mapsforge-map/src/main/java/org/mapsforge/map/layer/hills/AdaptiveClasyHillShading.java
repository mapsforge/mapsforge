/*
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

import java.io.IOException;
import java.io.InputStream;

/**
 * <p>
 * Adaptive implementation of {@link StandardClasyHillShading}.
 * It will dynamically decide on the resolution and quality of the output depending on the display parameters, to maximize efficiency.
 * <p>
 * It conserves memory and CPU at lower zoom levels without significant quality degradation, yet it switches to high quality
 * when details are needed at larger zoom levels.
 * Switching to high quality only at larger zoom levels is also a resource-saving tactic, since less hill shading data needs to be processed the more you zoom in.
 * <p>
 * This is currently the algorithm of choice, as it provides the best results with excellent performance throughout the zoom level range.
 *
 * @see StandardClasyHillShading
 * @see HiResClasyHillShading
 * @see HalfResClasyHillShading
 * @see QuarterResClasyHillShading
 */
public class AdaptiveClasyHillShading extends HiResClasyHillShading implements IAdaptiveHillShading {

    /**
     * This is the length of one side of a 1" HGT file.
     */
    public static final int HGTFILE_WIDTH_BASE = 3600;

    /**
     * Default max zoom level when using a 1" HGT file and high quality (bicubic) algorithm is enabled.
     */
    public static final int ZoomLevelMaxBaseDefault = 17;
    public static final boolean IsHqEnabledDefault = true;
    public static final boolean IsAdaptiveZoomEnabledDefault = true;

    protected final boolean mIsHqEnabled;
    protected volatile boolean mIsAdaptiveZoomEnabled = IsAdaptiveZoomEnabledDefault;

    /**
     * Our quality factors are packed integers, to save resources.
     * If the result of dividing the quality factor by the base is a number greater than one, that number is the divisor for scaling purposes.
     * If there's a reminder when the quality factor is divided by the base, this number is the multiplier (the maximum multiplier at this point is 2,
     * so there is no reason for the base to be large).
     */
    protected static final int QualityFactorPacketBase = 8;

    /**
     * Construct this using the parameters provided.
     *
     * @param clasyParams Parameters to use while constructing this.
     * @param isHqEnabled Whether to enable the use of high-quality (bicubic) algorithm for larger zoom levels. Disabling will reduce memory usage at high zoom levels.
     * @see AClasyHillShading#AClasyHillShading(ClasyParams)
     * @see ClasyParams
     * @see HiResClasyHillShading
     */
    public AdaptiveClasyHillShading(final ClasyParams clasyParams, boolean isHqEnabled) {
        super(clasyParams);
        this.mIsHqEnabled = isHqEnabled;
    }

    /**
     * Uses default values for all parameters.
     *
     * @param isHqEnabled Whether to enable the use of high-quality (bicubic) algorithm for larger zoom levels. Disabling will reduce memory usage at high zoom levels.
     * @see AClasyHillShading#AClasyHillShading()
     * @see HiResClasyHillShading
     */
    public AdaptiveClasyHillShading(boolean isHqEnabled) {
        super();
        this.mIsHqEnabled = isHqEnabled;
    }

    /**
     * Uses default values for all parameters, and enables the high-quality (bicubic) algorithm for higher zoom levels.
     *
     * @see AClasyHillShading#AClasyHillShading()
     * @see HiResClasyHillShading
     */
    public AdaptiveClasyHillShading() {
        this(IsHqEnabledDefault);
    }

    @Override
    public long getCacheTagBin(HgtFileInfo hgtFileInfo, int zoomLevel, double pxPerLat, double pxPerLon) {
        return getQualityFactor(hgtFileInfo, zoomLevel, pxPerLat, pxPerLon);
    }

    @Override
    public int getOutputAxisLen(final HgtFileInfo hgtFileInfo, int zoomLevel, double pxPerLat, double pxPerLon) {
        final int inputAxisLen = getInputAxisLen(hgtFileInfo);

        return scaleByQualityFactor(inputAxisLen, getQualityFactor(hgtFileInfo, zoomLevel, pxPerLat, pxPerLon));
    }

    @Override
    protected byte[] convert(InputStream inputStream, int dummyAxisLen, int dummyRowLen, int padding, int zoomLevel, double pxPerLat, double pxPerLon, HgtFileInfo hgtFileInfo) throws IOException {
        final boolean isHighQuality = isHighQuality(hgtFileInfo, zoomLevel, pxPerLat, pxPerLon);

        return doTheWork_(hgtFileInfo, isHighQuality, padding, zoomLevel, pxPerLat, pxPerLon);
    }

    @Override
    public boolean isHqEnabled() {
        return mIsHqEnabled;
    }

    @Override
    public boolean isAdaptiveZoomEnabled() {
        return mIsAdaptiveZoomEnabled;
    }

    @Override
    public AdaptiveClasyHillShading setAdaptiveZoomEnabled(boolean isEnabled) {
        mIsAdaptiveZoomEnabled = isEnabled;
        return this;
    }

    @Override
    public int getZoomMax(HgtFileInfo hgtFileInfo) {
        int retVal = ZoomLevelMaxBaseDefault;

        if (false == isHqEnabled()) {
            retVal -= 1;
        }

        final int inputAxisLen = getInputAxisLen(hgtFileInfo);

        if (inputAxisLen < HGTFILE_WIDTH_BASE) {
            for (int len = HGTFILE_WIDTH_BASE; inputAxisLen < len; len /= 2) {
                retVal -= 1;
            }
        } else if (inputAxisLen > HGTFILE_WIDTH_BASE) {
            for (int len = HGTFILE_WIDTH_BASE; inputAxisLen > len; len *= 2) {
                retVal += 1;
            }
        }

        return retVal;
    }

    /**
     * @param hgtFileInfo HGT file info
     * @param zoomLevel   Zoom level (to determine shading quality requirements)
     * @param pxPerLat    Tile pixels per degree of latitude (to determine shading quality requirements)
     * @param pxPerLon    Tile pixels per degree of longitude (to determine shading quality requirements)
     * @return {@code true} if the parameters provided result in a high quality (bicubic) algorithm being applied, {@code false} otherwise.
     */
    protected boolean isHighQuality(HgtFileInfo hgtFileInfo, int zoomLevel, double pxPerLat, double pxPerLon) {
        return getQualityFactor(hgtFileInfo, zoomLevel, pxPerLat, pxPerLon) % QualityFactorPacketBase > 1;
    }

    @SuppressWarnings("unused")
    public int getQualityFactor(HgtFileInfo hgtFileInfo, int zoomLevel, double pxPerLat, double pxPerLon) {
        final int output;

        final int inputPxPerDeg = getInputAxisLen(hgtFileInfo);
        final double scale = (double) inputPxPerDeg / Math.max(4, pxPerLat);

        if (scale >= 2.0) {
            // Note, integer arithmetic and truncations are deliberate here.
            int stride = (int) scale;
            int target = inputPxPerDeg / stride * stride;
            while (target != inputPxPerDeg && stride > 1) {
                stride -= 1;
                target = inputPxPerDeg / stride * stride;
            }
            if (target == inputPxPerDeg) {
                output = QualityFactorPacketBase * stride;
            } else {
                output = 1;
            }
        } else if (scale > 1./1.25 || false == isHqEnabled()) {
            output = 1;
        } else {
            output = 2;
        }

        return output;
    }

    /**
     * Our quality factors are packed integers, to save resources.
     * If the result of dividing the quality factor by the base is a number greater than one, that number is the divisor for scaling purposes.
     * If there's a reminder when the quality factor is divided by the base, this number is the multiplier (the maximum multiplier at this point is 2,
     * so there is no reason for the base to be large).
     *
     * @param value         Value to scale.
     * @param qualityFactor A quality factor to scale with.
     * @return Scaled value.
     */
    public static int scaleByQualityFactor(int value, int qualityFactor) {
        final int i = qualityFactor / QualityFactorPacketBase;
        if (i > 0) {
            return value / i;
        } else {
            return value * qualityFactor;
        }
    }
}
