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
 * Quarter resolution implementation of {@link StandardClasyHillShading}.
 * </p>
 * <p>
 * It reduces the number of pixels in the output bitmap and thus the memory used by a factor of 1/16 (1/4 in width and 1/4 in height).
 * This is done by processing only every fourth row and fourth column in the input, and discarding the rest.
 * </p>
 * <p>
 * Main purpose is to improve performance and reduce memory use when the output bitmap is going to be down-scaled anyway,
 * for example when viewing a map on a low zoom level (very wide).
 * </p>
 * <p>
 * Standard 1" DEM file containing 1° square data will be processed to an output bitmap of about 900x900 px and 800 kB in size.
 * </p>
 *
 * @see AdaptiveClasyHillShading
 * @see StandardClasyHillShading
 * @see HalfResClasyHillShading
 */
public class QuarterResClasyHillShading extends StandardClasyHillShading {

    /**
     * Construct this using the parameters provided.
     *
     * @param clasyParams Parameters to use while constructing this.
     * @see AClasyHillShading#AClasyHillShading(ClasyParams)
     * @see ClasyParams
     */
    public QuarterResClasyHillShading(final ClasyParams clasyParams) {
        super(clasyParams);
    }

    /**
     * Uses default values for all parameters.
     *
     * @see AClasyHillShading#AClasyHillShading()
     */
    public QuarterResClasyHillShading() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getOutputAxisLen(final HgtFileInfo hgtFileInfo, int zoomLevel, double pxPerLat, double pxPerLon) {
        return getInputAxisLen(hgtFileInfo) / 4;
    }
}
