/*
 * Copyright 2010, 2011 mapsforge.org
 * Copyright 2010, 2011 Eike
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
package org.mapsforge.core;

/**
 * This class provides methods and constants for dealing with distances on earth using the World
 * Geodetic System 1984
 */
public class WGS84 {
	/**
	 * Equatorial radius of earth is required for distance computation.
	 */
	public static final double EQUATORIALRADIUS = 6378137.0;

	/**
	 * Polar radius of earth is required for distance computation.
	 */
	public static final double POLARRADIUS = 6356752.3142;

	/**
	 * The flattening factor of the earth's ellipsoid is required for distance computation.
	 */
	public static final double INVERSEFLATTENING = 298.257223563;
}
