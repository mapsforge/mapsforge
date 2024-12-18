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
 * Simplified implementation of {@link StandardClasyHillShading} intended to maximize performance, at the cost of inaccuracy.
 * </p>
 * <p>
 * Note: For better-looking results and greater flexibility consider using {@link AdaptiveClasyHillShading}.
 * This algorithm is mostly useful when performance is of the highest priority, and you don't care about scaling.
 * </p>
 *
 * @see AdaptiveClasyHillShading
 * @see HiResClasyHillShading
 * @see StandardClasyHillShading
 * @see HalfResClasyHillShading
 * @see QuarterResClasyHillShading
 */
public class SimpleClasyHillShading extends StandardClasyHillShading {

    /**
     * Construct this using the parameters provided.
     *
     * @param clasyParams Parameters to use while constructing this.
     * @see AClasyHillShading#AClasyHillShading(ClasyParams)
     */
    public SimpleClasyHillShading(ClasyParams clasyParams) {
        super(clasyParams);
    }

    /**
     * Uses a default values for all parameters.
     *
     * @see AClasyHillShading#AClasyHillShading()
     */
    public SimpleClasyHillShading() {
        super();
    }

    /**
     * Heuristic that chooses the largest slope (in absolute value) between the two.
     */
    protected double getSlopeToUse(final double slope, final double slopeInPerpendicularDirection) {
        return Math.abs(slopeInPerpendicularDirection) > Math.abs(slope) ? slopeInPerpendicularDirection : slope;
    }

    /**
     * Map a slope value (in percent) to a shade pixel.
     *
     * @param slope Slope value in percent (%).
     * @return Shade value as a {@code byte}.
     */
    protected byte getShadePixel(final double slope) {
        double shade = HillShadingUtils.linearMapping(ShadeMin, slope, mMinSlope, mMaxSlope, mMainMappingFactor);

        if (slope < 0) {
            shade = (1 - mAsymmetryFactor) * shade;
        }

        return (byte) Math.round(shade);
    }

    /**
     * {@inheritDoc}
     */
    protected byte unitElementToShadePixel(double nw, double sw, double se, double ne, double dsf) {
        final double metersPerPixelDiagonal = SqrtTwo * 0.5 / dsf;

        final double slope = 100 * getSlopeToUse(nw - se, ne - sw) / metersPerPixelDiagonal;

        return getShadePixel(slope);
    }
}
