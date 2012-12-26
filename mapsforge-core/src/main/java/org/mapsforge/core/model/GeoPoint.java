/*
 * Copyright 2010, 2011, 2012 mapsforge.org
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
package org.mapsforge.core.model;

import java.io.Serializable;

/**
 * A GeoPoint represents an immutable pair of latitude and longitude coordinates.
 */
public class GeoPoint implements Comparable<GeoPoint>, Serializable {
	private static final double EQUATORIAL_RADIUS = 6378137.0;
	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new GeoPoint from a comma-separated string of coordinates in the order latitude, longitude. All
	 * coordinate values must be in degrees.
	 * 
	 * @param geoPointString
	 *            the string that describes the GeoPoint.
	 * @return a new GeoPoint with the given coordinates.
	 * @throws IllegalArgumentException
	 *             if the string cannot be parsed or describes an invalid GeoPoint.
	 */
	public static GeoPoint fromString(String geoPointString) {
		double[] coordinates = CoordinatesUtil.parseCoordinateString(geoPointString, 2);
		return new GeoPoint(coordinates[0], coordinates[1]);
	}

	/**
	 * Calculates the amount of degrees of latitude for a given distance in meters.
	 * 
	 * @param meters
	 *            distance in meters
	 * @return latitude degrees
	 */
	public static double latitudeDistance(int meters) {
		return (meters * 360) / (2 * Math.PI * EQUATORIAL_RADIUS);
	}

	/**
	 * Calculates the amount of degrees of longitude for a given distance in meters.
	 * 
	 * @param meters
	 *            distance in meters
	 * @param latitude
	 *            the latitude at which the calculation should be performed
	 * @return longitude degrees
	 */
	public static double longitudeDistance(int meters, double latitude) {
		return (meters * 360) / (2 * Math.PI * EQUATORIAL_RADIUS * Math.cos(Math.toRadians(latitude)));
	}

	/**
	 * The latitude coordinate of this GeoPoint in degrees.
	 */
	public final double latitude;

	/**
	 * The longitude coordinate of this GeoPoint in degrees.
	 */
	public final double longitude;

	/**
	 * @param latitude
	 *            the latitude coordinate in degrees.
	 * @param longitude
	 *            the longitude coordinate in degrees.
	 * @throws IllegalArgumentException
	 *             if a coordinate is invalid.
	 */
	public GeoPoint(double latitude, double longitude) {
		CoordinatesUtil.validateLatitude(latitude);
		CoordinatesUtil.validateLongitude(longitude);

		this.latitude = latitude;
		this.longitude = longitude;
	}

	@Override
	public int compareTo(GeoPoint geoPoint) {
		if (this.longitude > geoPoint.longitude) {
			return 1;
		} else if (this.longitude < geoPoint.longitude) {
			return -1;
		} else if (this.latitude > geoPoint.latitude) {
			return 1;
		} else if (this.latitude < geoPoint.latitude) {
			return -1;
		}
		return 0;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (!(obj instanceof GeoPoint)) {
			return false;
		}
		GeoPoint other = (GeoPoint) obj;
		if (Double.doubleToLongBits(this.latitude) != Double.doubleToLongBits(other.latitude)) {
			return false;
		} else if (Double.doubleToLongBits(this.longitude) != Double.doubleToLongBits(other.longitude)) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(this.latitude);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(this.longitude);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("latitude=");
		stringBuilder.append(this.latitude);
		stringBuilder.append(", longitude=");
		stringBuilder.append(this.longitude);
		return stringBuilder.toString();
	}
}
