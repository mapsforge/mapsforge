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

/**
 * <p>
 * A standard implementation of the {@link AClasyHillShading}. Divides square unit elements into two triangles/planes
 * to calculate their average normal, which is then used to shade the unit element.
 * </p>
 * <p>
 * This is currently the algorithm of choice, as it provides the best results with excellent performance.
 * It is also more accurate than {@link SimpleClasyHillShading}.
 * </p>
 */
public class StandardClasyHillShading extends AClasyHillShading {

    /**
     * Construct this using the parameters provided.
     *
     * @param clasyParams Parameters to use while constructing this.
     * @see AClasyHillShading#AClasyHillShading(ClasyParams)
     */
    public StandardClasyHillShading(final ClasyParams clasyParams) {
        super(clasyParams);
    }

    /**
     * Uses a default values for all parameters.
     *
     * @see AClasyHillShading#AClasyHillShading()
     */
    public StandardClasyHillShading() {
        super();
    }

    /**
     * Map one unit element to a shade pixel, by dividing the unit element into two triangles/planes and using the average normal.
     *
     * @param nw  North-west value. [meters]
     * @param sw  South-west value. [meters]
     * @param se  South-east value. [meters]
     * @param ne  North-east value. [meters]
     * @param mpe Meters per unit element, ie. the length of one side of the unit element. [meters]
     * @return Shade value as a {@code byte}.
     */
    protected byte getAverageNormalShadePixel(double nw, double sw, double se, double ne, double mpe) {
        final double swne = sw - ne;
        final double senw = se - nw;

        // "Average" normal of two triangles, NW-SW-NE and SE-NE-SW, after simplifying the algebra and canceling the scaling factor
        final double normalX = swne - senw;
        final double normalY = swne + senw;
        final double normalZ = 2 * mpe;

        return normalToShadePixel(normalX, normalY, normalZ);
    }

    /**
     * Map one unit element to a shade pixel.
     *
     * @param nw  North-west value. [meters]
     * @param sw  South-west value. [meters]
     * @param se  South-east value. [meters]
     * @param ne  North-east value. [meters]
     * @param mpe Meters per unit element, ie. the length of one side of the unit element. [meters]
     * @return Shade value as a {@code byte}.
     */
    protected byte unitElementToShadePixel(double nw, double sw, double se, double ne, double mpe) {
        return getAverageNormalShadePixel(nw, sw, se, ne, mpe);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int processOneUnitElement(double nw, double sw, double se, double ne, double mpe, int outputIx, ComputingParams computingParams) {
        computingParams.mOutput[outputIx] = unitElementToShadePixel(nw, sw, se, ne, mpe);

        outputIx++;

        return outputIx;
    }
}
