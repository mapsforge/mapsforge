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

/**
 * <p>
 * Clear asymmetry hill shading.
 * There is a special azimuthal asymmetry factor, between 0 and 1, which determines in a simple way how much less shading
 * slopes in a reference direction should get (northwest by convention):
 * When 1 slopes in the reference direction should get minimal shading (max asymmetry effect),
 * when 0 the shading should be symmetrical in all directions (no asymmetry effect).
 * Any value larger than 0 should make the shading smoothly asymmetrical, with slopes closer to the reference direction
 * less shaded than slopes in the opposite direction as the factor increases.
 * </p>
 * <p>
 * You can modify some behaviors by implementing methods:
 * </p>
 * <p>
 * {@link #azimuthalDotProduct(double, double)} to change the "light source" direction (NW by default),
 * <br />
 * {@link #unitElementToShadePixel(double, double, double, double, double)} to change the mapping of slope to shade pixel.
 * </p>
 *
 * @see AdaptiveClasyHillShading
 * @see HiResClasyHillShading
 * @see StandardClasyHillShading
 * @see HalfResClasyHillShading
 * @see QuarterResClasyHillShading
 */
public abstract class AClasyHillShading extends AThreadedHillShading {

    public static final double MaxSlopeDefault = 80;
    public static final double MinSlopeDefault = 0;
    public static final double AsymmetryFactorDefault = 0.5;

    public static final int ShadeMin = 0, ShadeMax = 255;

    protected final double mMinSlope, mMaxSlope, mAsymmetryFactor;

    protected final double mMainMappingFactor;
    protected final double mAsymmetryMappingFactor;

    /**
     * Construct this using the parameters provided.
     *
     * @param clasyParams Parameters to use while constructing this.
     * @see AClasyHillShading#AClasyHillShading(ClasyParams)
     * @see ClasyParams
     */
    public AClasyHillShading(final ClasyParams clasyParams) {
        super(clasyParams.getReadingThreadsCount(), clasyParams.getComputingThreadsCount(), clasyParams.isPreprocess());

        mMaxSlope = clasyParams.getMaxSlope();
        mMinSlope = clasyParams.getMinSlope();
        mAsymmetryFactor = HillShadingUtils.boundToLimits(0, 1, clasyParams.getAsymmetryFactor());

        // Factor for main mapping from slope to shade
        mMainMappingFactor = (ShadeMax - ShadeMin) / (mMaxSlope - mMinSlope);

        // Factor for azimuthal asymmetry mapping (cosine of the azimuth angle goes from -1 to 1)
        mAsymmetryMappingFactor = -mAsymmetryFactor / (1 - (-1));
    }

    /**
     * Uses default values for all parameters.
     *
     * @see AClasyHillShading#AClasyHillShading()
     */
    public AClasyHillShading() {
        this(new ClasyParams());
    }

    /**
     * Map one unit element to a shade pixel, usually by dividing the unit element into two triangles/planes and using the average normal.
     *
     * @param nw  North-west value. [meters]
     * @param sw  South-west value. [meters]
     * @param se  South-east value. [meters]
     * @param ne  North-east value. [meters]
     * @param dsf Distance scale factor, that is half the length of one side of the standard 2x2 unit element inverted, i.e. {@code 0.5 / length}. [1/meters]
     * @return Shade value as a {@code byte}.
     */
    protected abstract byte unitElementToShadePixel(double nw, double sw, double se, double ne, double dsf);

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
    protected abstract double azimuthalDotProduct(final double normalX, final double normalY);

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

        final boolean isMappingSame = unitElementToShadePixel(5, 4, 3, 2, 1) == that.unitElementToShadePixel(5, 4, 3, 2, 1);

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
        result = 31 * result + Byte.hashCode(unitElementToShadePixel(5, 4, 3, 2, 1));
        return result;
    }

    /**
     * Parameters that are used by an {@link AClasyHillShading}.
     */
    public static class ClasyParams {
        protected volatile double mMaxSlope = MaxSlopeDefault;
        protected volatile double mMinSlope = MinSlopeDefault;
        protected volatile double mAsymmetryFactor = AsymmetryFactorDefault;
        protected volatile int mReadingThreadsCount = ReadingThreadsCountDefault;
        protected volatile int mComputingThreadsCount = ComputingThreadsCountDefault;
        protected volatile boolean mIsPreprocess = IsPreprocessDefault;

        public ClasyParams() {
        }

        /**
         * @param maxSlope The smallest slope that will have the darkest shade.
         *                 All larger slopes will have the same shade, the darkest one.
         *                 Should be larger than zero.
         *                 The default is 80.
         *                 [percentage, %]
         */
        public ClasyParams setMaxSlope(final double maxSlope) {
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
        public ClasyParams setMinSlope(final double minSlope) {
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
        public ClasyParams setAsymmetryFactor(final double asymmetryFactor) {
            mAsymmetryFactor = asymmetryFactor;
            return this;
        }

        /**
         * @param readingThreadsCount Number of "producer" threads that will do the reading, >= 1.
         *                            Number N (>=1) means there will be N threads that will do the reading.
         *                            Zero (0) is not permitted.
         *                            The only time you'd want to set this to 1 is when your data source does not support skipping,
         *                            i.e. the data source is not a file and/or its {@link InputStream#skip(long)} is inefficient.
         *                            The default is computed as {@code Math.max(1,} {@link #AvailableProcessors}{@code )}.
         */
        public ClasyParams setReadingThreadsCount(final int readingThreadsCount) {
            mReadingThreadsCount = readingThreadsCount;
            return this;
        }

        /**
         * @param computingThreadsCount Number of "consumer" threads that will do the computations, >= 0.
         *                              Number M (>=0) means there will be M threads that will do the computing.
         *                              Zero (0) means that producer thread(s) will also do the computing.
         *                              The only times you'd want to set this to zero are when memory conservation is a top priority
         *                              or when you're running on a single-threaded system.
         *                              The default is {@link #AvailableProcessors}, the number of processors available to the Java runtime.
         */
        public ClasyParams setComputingThreadsCount(final int computingThreadsCount) {
            mComputingThreadsCount = computingThreadsCount;
            return this;
        }

        /**
         * @param preprocess When {@code true}, input data will be preprocessed to remove possible invalid values.
         *                   The default is {@code true}.
         */
        public ClasyParams setPreprocess(boolean preprocess) {
            mIsPreprocess = preprocess;
            return this;
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

        public boolean isPreprocess() {
            return mIsPreprocess;
        }
    }
}
