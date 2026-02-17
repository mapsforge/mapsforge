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

import static org.mapsforge.map.layer.hills.HillShadingUtils.SqrtTwo;

/**
 * <p>
 * A standard implementation of the {@link AClasyHillShading}. Divides square unit elements into two triangles/planes
 * to calculate their average normal, which is then used to shade the unit element.
 * </p>
 * <p>
 * Horizontal surfaces, or all surfaces with slope less than {@code minSlope}, will have minimum shade.
 * </p>
 * <p>
 * Slopes are shaded linearly by default, i.e. main shade (shade before applying azimuthal asymmetry factor) is a linear function of slope, for performance.
 * </p>
 * <p>
 * For performance reasons, azimuthal asymmetry is also a linear function of the azimuth angle cosine.
 * </p>
 * <p>
 * Standard 1" DEM file containing 1° square data will be processed to an output bitmap of about 3600px by 3600px and 13 MB in size.
 * </p>
 * <p>
 * This algorithm is more accurate than {@link SimpleClasyHillShading}, and should be preferred.
 * </p>
 * <p>
 * High resolution version is also available: {@link HiResClasyHillShading}.
 * It provides high quality output using bicubic interpolation, use it when you are not limited by memory or processing performance.
 * </p>
 * <p>
 * To greatly improve efficiency at wider zoom levels, you should consider using the adaptive quality version instead: {@link AdaptiveClasyHillShading}.
 * It provides the best results with excellent performance throughout the zoom level range.
 * </p>
 *
 * @see AdaptiveClasyHillShading
 * @see HiResClasyHillShading
 * @see HalfResClasyHillShading
 * @see QuarterResClasyHillShading
 */
public class StandardClasyHillShading extends AClasyHillShading {

    // Scaled parameters are used to save some arithmetic cycles later in a loop
    protected final double mMinSlopeScaled, mMaxSlopeScaled;
    protected final double mMainMappingFactorScaled;
    protected final double mAzimuthLowScaled, mAsymmetryMappingFactorScaled;

    /**
     * Construct this using the parameters provided.
     *
     * @param clasyParams Parameters to use while constructing this.
     * @see AClasyHillShading#AClasyHillShading(ClasyParams)
     * @see ClasyParams
     */
    public StandardClasyHillShading(final ClasyParams clasyParams) {
        super(clasyParams);

        mMinSlopeScaled = mMinSlope / 100.;
        mMaxSlopeScaled = mMaxSlope / 100.;
        mMainMappingFactorScaled = mMainMappingFactor * 100.;

        mAzimuthLowScaled = -1. * SqrtTwo;
        mAsymmetryMappingFactorScaled = mAsymmetryMappingFactor / SqrtTwo;
    }

    /**
     * Uses default values for all parameters.
     *
     * @see AClasyHillShading#AClasyHillShading()
     */
    public StandardClasyHillShading() {
        super();

        mMinSlopeScaled = mMinSlope / 100.;
        mMaxSlopeScaled = mMaxSlope / 100.;
        mMainMappingFactorScaled = mMainMappingFactor * 100.;

        mAzimuthLowScaled = -1. * SqrtTwo;
        mAsymmetryMappingFactorScaled = mAsymmetryMappingFactor / SqrtTwo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected double azimuthalDotProduct(final double normalX, final double normalY) {
        // Dot product of the normal with a unit vector in NW direction multiplied by sqrt(2), (-i + j)
        return -normalX + normalY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int processUnitElement_2x2(double nw, double sw, double se, double ne, double dsf, int outputIx, ComputingParams computingParams) {
        computingParams.mOutput[outputIx] = unitElementToShadePixel(nw, sw, se, ne, dsf);

        return outputIx + 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected byte unitElementToShadePixel(final double nw, final double sw, final double se, final double ne, final double dsf) {
        double shade = ShadeMin;

        if (sw != ne || se != nw) {
            final double swne = sw - ne;
            final double senw = se - nw;

            // "Average" normal of two triangles, NW-SW-NE and SE-NE-SW, after simplifying the algebra and canceling the scaling factor
            final double normalX = swne - senw;
            final double normalY = swne + senw;
            final double normalZInv = dsf;

            // Always greater than zero due to checks above
//            final double normalXYLen = Math.sqrt(normalX * normalX + normalY * normalY);
            final double normalXYLen = sqrt(normalX * normalX + normalY * normalY);

            // Tangent of the angle between the 3D normal and the z-axis.
            // Z-component of the normal is always above zero due to hills being a graph of a function; thus no abs() and no special checks needed.
            // This is our slope as a simple ratio (i.e. slope as a percent, divided by 100).
            final double zenithAngleTangent = normalXYLen * normalZInv;

            shade = HillShadingUtils.linearMapping(ShadeMin, zenithAngleTangent, mMinSlopeScaled, mMaxSlopeScaled, mMainMappingFactorScaled);

            // Cosine of the azimuth angle between the normal and the reference direction ("light source"; NW by default convention), multiplied by sqrt(2)
            final double azimuthAngleCosine = azimuthalDotProduct(normalX, normalY) / normalXYLen;

            // This is just to provide asymmetry (NW-SE by default convention), accuracy is not important,
            // thus we are content with simply using cos to maximize performance.
            shade *= HillShadingUtils.linearMappingWithoutLimits(1, azimuthAngleCosine, mAzimuthLowScaled, mAsymmetryMappingFactorScaled);
        }

        // Crude rounding of small positive numbers. Rounding mode is "half away from zero".
        // Intended to be faster than Math.round() for small positive numbers.
        return (byte) (shade + 0.5);
    }

    protected double sqrt(final double x) {
//        return Math.sqrt(x);
        return HillShadingUtils.sqrtApprox(x);
    }
}
