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

import java.io.InputStream;

import static org.mapsforge.map.layer.hills.HillShadingUtils.SqrtTwo;

/**
 * <p>
 * Clear asymmetry hill shading.
 * There is a special azimuthal asymmetry factor, between 0 and 1, which determines in a simple way how much less shading
 * slopes in a reference direction get (northwest by convention):
 * When 1 slopes in the reference direction will get minimal shading (max asymmetry effect),
 * when 0 the shading will be symmetrical in all directions (no asymmetry effect).
 * Any value larger than 0 will make the shading smoothly asymmetrical, with slopes closer to the reference direction
 * less shaded than slopes in the opposite direction as the factor increases.
 * </p>
 * <p>
 * Horizontal surfaces, or all surfaces with slope less than {@code minSlope}, will have minimum shade.
 * </p>
 * <p>
 * Slopes are shaded linearly by default, ie. main shade (shade before applying azimuthal asymmetry factor) is a linear function of slope, for performance.
 * Sqrt and square mappings are also available and can be used by overriding the {@link #slopeToShade(double)} method.
 * </p>
 * <p>
 * For performance reasons, azimuthal asymmetry is also a linear function of the azimuth angle cosine.
 * This can be customized by overriding the {@link #azimuthalAsymmetryFactor(double)} method.
 * </p>
 * <p>
 * There are many behaviors you can modify by overriding methods if you know what you're doing:
 * </p>
 * <p>
 * {@link #azimuthalDotProduct(double, double)} to change the "light source" direction (NW by default),
 * <br />
 * {@link #azimuthalAsymmetryFactor(double)} to change the mapping of azimuth angle cosine to asymmetry factor,
 * <br />
 * {@link #slopeToShade(double)} to change the mapping of slope to shade,
 * <br />
 * {@link #normalToShade(double, double, double)} to change the whole 3D normal to shade mapping.
 * </p>
 *
 * @see StandardClasyHillShading
 * @see HiResStandardClasyHillShading
 */
public abstract class AClasyHillShading extends AThreadedHillShading {

    public static final double MaxSlopeDefault = 80;
    public static final double MinSlopeDefault = 0;
    public static final double AsymmetryFactorDefault = 0.5;

    public static final int ShadeMin = 0, ShadeMax = 255;

    protected final double mMinSlope, mMaxSlope, mAsymmetryFactor;

    protected final double mMainMappingFactor;
    protected final double mAsymmetryMappingFactor;

    public AClasyHillShading(final ClasyParams clasyParams) {
        super(clasyParams.getReadingThreadsCount(), clasyParams.getComputingThreadsCount(), clasyParams.isHighQuality());

        mMaxSlope = clasyParams.getMaxSlope();
        mMinSlope = clasyParams.getMinSlope();
        mAsymmetryFactor = HillShadingUtils.boundToLimits(0, 1, clasyParams.getAsymmetryFactor());

        // Factor for main mapping from slope to shade
        mMainMappingFactor = (ShadeMax - ShadeMin) / (mMaxSlope - mMinSlope);

        // Factor for azimuthal asymmetry mapping (cosine of the azimuth angle goes from -1 to 1)
        mAsymmetryMappingFactor = -mAsymmetryFactor / (1 - (-1));
    }

    /**
     * Uses default maxSlope = 80, minSlope = 0, asymmetryFactor = 0.5, and default number of computing threads = 1.
     */
    public AClasyHillShading() {
        this(new ClasyParams.Builder().build());
    }

    /**
     * Map a 3D normal vector to a shade pixel.
     *
     * @param normalX x-component of the normal
     * @param normalY y-component of the normal
     * @param normalZ z-component of the normal
     * @return Shade value as a {@code byte}.
     */
    protected byte normalToShadePixel(final double normalX, final double normalY, final double normalZ) {
        return roundImpl(normalToShade(normalX, normalY, normalZ));
    }

    /**
     * Map a 3D normal vector to a shade value.
     *
     * @param normalX x-component of the normal
     * @param normalY y-component of the normal
     * @param normalZ z-component of the normal
     * @return Shade value as a number in the range [{@link #ShadeMin}..{@link #ShadeMax}] (0 to 255 by default).
     */
    protected double normalToShade(final double normalX, final double normalY, final double normalZ) {
        final double normalXYLen = hypotImpl(normalX, normalY);

        // Tangent of the angle between the 3D normal and the z-axis.
        // z-component of the normal is always above zero in our calculations; thus no abs() and no special checks needed.
        final double zenithAngleTangent = normalXYLen / normalZ;
        final double slope = 100 * zenithAngleTangent;

        double shade = slopeToShade(slope);

        if (mAsymmetryMappingFactor < 0 && normalXYLen > 0) {
            // Cosine of the azimuth angle between the normal and the reference direction ("light source"; NW by convention)
            final double azimuthAngleCosine = azimuthalDotProduct(normalX, normalY) / (SqrtTwo * normalXYLen);

            // This is just to provide asymmetry (NW-SE by convention), accuracy is not important, thus we are content with simply using cos to maximize performance
            shade *= azimuthalAsymmetryFactor(azimuthAngleCosine);
        }

        return shade;
    }

    /**
     * Map a slope value (in percent) to a shade value.
     *
     * @param slope Slope value in percent (%).
     * @return Shade value as a number in the range [{@link #ShadeMin}..{@link #ShadeMax}] (0 to 255 by default).
     */
    protected double slopeToShade(final double slope) {
        return HillShadingUtils.linearMapping(ShadeMin, slope, mMinSlope, mMaxSlope, mMainMappingFactor);
    }

    /**
     * <p>
     * Dot product of the projection of 3D normal to the XY plane with a unit vector in reference direction ("light source")
     * on the same plane, multiplied by {@code sqrt(2)} to simplify cases when the direction is on a diagonal.
     * The reference direction is usually NW, so this vector is {@code -i + j} by default
     * (where i and j are unit vectors in x and y directions respectively).
     * </p>
     * <p>
     * Return values for some common reference directions should be:
     * </p>
     * <p>
     * NW: {@code -normalX + normalY}
     * <br />
     * SW: {@code -normalX - normalY}
     * <br />
     * SE: {@code normalX - normalY}
     * <br />
     * NE: {@code normalX + normalY}
     * <br />
     * N: {@code normalY * sqrt(2)}
     * <br />
     * W: {@code -normalX * sqrt(2)}
     * <br />
     * S: {@code -normalY * sqrt(2)}
     * <br />
     * E: {@code normalX * sqrt(2)}
     * </p>
     *
     * @param normalX x-component of the normal
     * @param normalY y-component of the normal
     * @return Dot product multiplied by {@code sqrt(2)}.
     */
    protected double azimuthalDotProduct(final double normalX, final double normalY) {
        // Dot product of the normal with a unit vector in NW direction multiplied by sqrt(2), (-i + j)
        return -normalX + normalY;
    }

    /**
     * Map an azimuth angle cosine to an asymmetry factor.
     *
     * @param azimuthAngleCosine A number in the range [-1, 1], e.g. a cosine.
     * @return A number in the range [0, 1].
     */
    protected double azimuthalAsymmetryFactor(final double azimuthAngleCosine) {
        return HillShadingUtils.linearMappingWithoutLimits(1, azimuthAngleCosine, -1, mAsymmetryMappingFactor);
    }

    /**
     * @return {@code sqrt(x*x + y*y)} using {@link #sqrtImpl(double)}.
     */
    protected double hypotImpl(final double x, final double y) {
        // return Math.hypot(x, y);
        return sqrtImpl(x * x + y * y);
    }

    /**
     * @return {@code Math.sqrt(x)} by default.
     */
    protected double sqrtImpl(final double x) {
        // Math.sqrt performance is quite good, and appears to be even faster than approximating methods
        return Math.sqrt(x);
    }

    /**
     * @param x A small positive number that will be rounded. Should be in the range [0..255].
     * @return Rounded x casted to a {@code byte}.
     */
    protected byte roundImpl(final double x) {
        // return (byte) Math.round(x);
        return HillShadingUtils.crudeRoundSmallPositives(x);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AClasyHillShading that = (AClasyHillShading) o;

        final boolean isMappingSame = Double.compare(slopeToShade(0.5 * (mMaxSlope + mMinSlope)), that.slopeToShade(0.5 * (that.mMaxSlope + that.mMinSlope))) == 0;

        return isMappingSame && Double.compare(mMinSlope, that.mMinSlope) == 0 && Double.compare(mMaxSlope, that.mMaxSlope) == 0 && Double.compare(mAsymmetryFactor, that.mAsymmetryFactor) == 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int result = Double.hashCode(mMinSlope);
        result = 31 * result + Double.hashCode(mMaxSlope);
        result = 31 * result + Double.hashCode(mAsymmetryFactor);
        result = 31 * result + Double.hashCode(slopeToShade(0.5 * (mMaxSlope + mMinSlope)));
        return result;
    }

    /**
     * Parameters that are used by an {@link AClasyHillShading}.
     * An instance should be created using the provided builder, {@link Builder}.
     */
    public static class ClasyParams {
        public final double mMaxSlope;
        public final double mMinSlope;
        public final double mAsymmetryFactor;
        public final int mReadingThreadsCount;
        public final int mComputingThreadsCount;
        public final boolean mIsHighQuality;

        protected ClasyParams(final Builder builder) {
            mMaxSlope = builder.mMaxSlope;
            mMinSlope = builder.mMinSlope;
            mAsymmetryFactor = builder.mAsymmetryFactor;
            mReadingThreadsCount = builder.mReadingThreadsCount;
            mComputingThreadsCount = builder.mComputingThreadsCount;
            mIsHighQuality = builder.mIsHighQuality;
        }

        public static class Builder {
            protected volatile double mMaxSlope = MaxSlopeDefault;
            protected volatile double mMinSlope = MinSlopeDefault;
            protected volatile double mAsymmetryFactor = AsymmetryFactorDefault;
            protected volatile int mReadingThreadsCount = ReadingThreadsCountDefault;
            protected volatile int mComputingThreadsCount = ComputingThreadsCountDefault;
            protected volatile boolean mIsHighQuality = IsHighQualityDefault;

            public Builder() {
            }

            /**
             * Create the {@link ClasyParams} instance using parameter values from this builder.
             * Any parameter not explicitly set will get the default value.
             *
             * @return New {@link ClasyParams} instance built using parameter values from this {@link Builder}.
             */
            public ClasyParams build() {
                return new ClasyParams(this);
            }

            /**
             * @param maxSlope The smallest slope that will have the darkest shade.
             *                 All larger slopes will have the same shade, the darkest one.
             *                 Should be larger than zero.
             *                 The default is 80.
             *                 [percentage, %]
             */
            public Builder setMaxSlope(final double maxSlope) {
                mMaxSlope = maxSlope;
                return this;
            }

            /**
             * @param minSlope The largest slope that will have the lightest shade.
             *                 All smaller slopes will have the same shade, the lightest one.
             *                 Should be in the range [0..{@code maxSlope}>.
             *                 The default is 0 (zero).
             *                 [percentage, %]
             */
            public Builder setMinSlope(final double minSlope) {
                mMinSlope = minSlope;
                return this;
            }

            /**
             * @param asymmetryFactor Number in the range [0..1].
             *                        When 1 the reference direction (NW by default) slopes will get minimal shading (max asymmetry effect),
             *                        when 0 the shading will be symmetrical in all directions (no asymmetry effect).
             *                        Any value larger than 0 will make the shading smoothly asymmetrical, with slopes closer to the reference direction
             *                        less shaded than slopes in the opposite direction as the factor increases.
             *                        The default is 0.5.
             */
            public Builder setAsymmetryFactor(final double asymmetryFactor) {
                mAsymmetryFactor = asymmetryFactor;
                return this;
            }

            /**
             * @param readingThreadsCount Number of "producer" threads that will do the reading, >= 0.
             *                            Number N (>0) means there will be N additional threads (per caller thread) that will do the reading,
             *                            while 0 means that only the caller thread will do the reading.
             *                            The only time you'd want to set this to zero is when your data source does not support skipping,
             *                            ie. the data source is not a file and/or its {@link InputStream#skip(long)} is inefficient.
             *                            The default is 1.
             */
            public Builder setReadingThreadsCount(final int readingThreadsCount) {
                mReadingThreadsCount = readingThreadsCount;
                return this;
            }

            /**
             * @param computingThreadsCount Number of "consumer" threads that will do the computations (per caller thread), >= 0.
             *                              Number M (>0) means there will be M additional threads (per caller thread) that will do the computing,
             *                              while 0 means that producer thread(s) will also do the computing.
             *                              The only times you'd want to set this to zero are when memory conservation is a top priority
             *                              or when you're running on a single-threaded system.
             *                              The default is 1.
             */
            public Builder setComputingThreadsCount(final int computingThreadsCount) {
                mComputingThreadsCount = computingThreadsCount;
                return this;
            }

            /**
             * @param highQuality When {@code true}, a unit element is 4x4 data points in size instead of 2x2, for better interpolation capabilities.
             *                    To make use of this, you should override the
             *                    {@link #processOneUnitElement(double, double, double, double, double, double, double, double, double, double, double, double, double, double, double, double, double, int, ComputingParams)}
             *                    method.
             *                    The default is {@code false}.
             */
            public Builder setHighQuality(boolean highQuality) {
                mIsHighQuality = highQuality;
                return this;
            }
        }

        public double getMaxSlope() {
            return mMaxSlope;
        }

        public double getMinSlope() {
            return mMinSlope;
        }

        public double getAsymmetryFactor() {
            return mAsymmetryFactor;
        }

        public int getReadingThreadsCount() {
            return mReadingThreadsCount;
        }

        public int getComputingThreadsCount() {
            return mComputingThreadsCount;
        }

        public boolean isHighQuality() {
            return mIsHighQuality;
        }
    }
}
