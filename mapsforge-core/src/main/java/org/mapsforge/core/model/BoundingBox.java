/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
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

import org.mapsforge.core.util.LatLongUtils;

/**
 * A BoundingBox represents an immutable set of two latitude and two longitude coordinates.
 */
public class BoundingBox implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new BoundingBox from a comma-separated string of coordinates in the order minLat, minLon, maxLat,
	 * maxLon. All coordinate values must be in degrees.
	 * 
	 * @param boundingBoxString
	 *            the string that describes the BoundingBox.
	 * @return a new BoundingBox with the given coordinates.
	 * @throws IllegalArgumentException
	 *             if the string cannot be parsed or describes an invalid BoundingBox.
	 */
	public static BoundingBox fromString(String boundingBoxString) {
		double[] coordinates = LatLongUtils.parseCoordinateString(boundingBoxString, 4);
		return new BoundingBox(coordinates[0], coordinates[1], coordinates[2], coordinates[3]);
	}

	/**
	 * The maximum latitude coordinate of this BoundingBox in degrees.
	 */
	public final double maxLatitude;

	/**
	 * The maximum longitude coordinate of this BoundingBox in degrees.
	 */
	public final double maxLongitude;

	/**
	 * The minimum latitude coordinate of this BoundingBox in degrees.
	 */
	public final double minLatitude;

	/**
	 * The minimum longitude coordinate of this BoundingBox in degrees.
	 */
	public final double minLongitude;

	/**
	 * @param minLatitude
	 *            the minimum latitude coordinate in degrees.
	 * @param minLongitude
	 *            the minimum longitude coordinate in degrees.
	 * @param maxLatitude
	 *            the maximum latitude coordinate in degrees.
	 * @param maxLongitude
	 *            the maximum longitude coordinate in degrees.
	 * @throws IllegalArgumentException
	 *             if a coordinate is invalid.
	 */
	public BoundingBox(double minLatitude, double minLongitude, double maxLatitude, double maxLongitude) {
		LatLongUtils.validateLatitude(minLatitude);
		LatLongUtils.validateLongitude(minLongitude);
		LatLongUtils.validateLatitude(maxLatitude);
		LatLongUtils.validateLongitude(maxLongitude);

		if (minLatitude > maxLatitude) {
			throw new IllegalArgumentException("invalid latitude range: " + minLatitude + ' ' + maxLatitude);
		} else if (minLongitude > maxLongitude) {
			throw new IllegalArgumentException("invalid longitude range: " + minLongitude + ' ' + maxLongitude);
		}

		this.minLatitude = minLatitude;
		this.minLongitude = minLongitude;
		this.maxLatitude = maxLatitude;
		this.maxLongitude = maxLongitude;
	}

	/**
	 * @param latLong
	 *            the LatLong whose coordinates should be checked.
	 * @return true if this BoundingBox contains the given LatLong, false otherwise.
	 */
	public boolean contains(LatLong latLong) {
		return this.minLatitude <= latLong.latitude && this.maxLatitude >= latLong.latitude
				&& this.minLongitude <= latLong.longitude && this.maxLongitude >= latLong.longitude;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (!(obj instanceof BoundingBox)) {
			return false;
		}
		BoundingBox other = (BoundingBox) obj;
		if (Double.doubleToLongBits(this.maxLatitude) != Double.doubleToLongBits(other.maxLatitude)) {
			return false;
		} else if (Double.doubleToLongBits(this.maxLongitude) != Double.doubleToLongBits(other.maxLongitude)) {
			return false;
		} else if (Double.doubleToLongBits(this.minLatitude) != Double.doubleToLongBits(other.minLatitude)) {
			return false;
		} else if (Double.doubleToLongBits(this.minLongitude) != Double.doubleToLongBits(other.minLongitude)) {
			return false;
		}
		return true;
	}

	/**
	 * @return a new LatLong at the horizontal and vertical center of this BoundingBox.
	 */
	public LatLong getCenterPoint() {
		double latitudeOffset = (this.maxLatitude - this.minLatitude) / 2;
		double longitudeOffset = (this.maxLongitude - this.minLongitude) / 2;
		return new LatLong(this.minLatitude + latitudeOffset, this.minLongitude + longitudeOffset);
	}

	/**
	 * @return the latitude span of this BoundingBox in degrees.
	 */
	public double getLatitudeSpan() {
		return this.maxLatitude - this.minLatitude;
	}

	/**
	 * @return the longitude span of this BoundingBox in degrees.
	 */
	public double getLongitudeSpan() {
		return this.maxLongitude - this.minLongitude;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(this.maxLatitude);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(this.maxLongitude);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(this.minLatitude);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(this.minLongitude);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	/**
	 * @param boundingBox
	 *            the BoundingBox which should be checked for intersection with this BoundingBox.
	 * @return true if this BoundingBox intersects with the given BoundingBox, false otherwise.
	 */
	public boolean intersects(BoundingBox boundingBox) {
		if (this == boundingBox) {
			return true;
		}

		return this.maxLatitude >= boundingBox.minLatitude && this.maxLongitude >= boundingBox.minLongitude
				&& this.minLatitude <= boundingBox.maxLatitude && this.minLongitude <= boundingBox.maxLongitude;
	}

	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("minLatitude=");
		stringBuilder.append(this.minLatitude);
		stringBuilder.append(", minLongitude=");
		stringBuilder.append(this.minLongitude);
		stringBuilder.append(", maxLatitude=");
		stringBuilder.append(this.maxLatitude);
		stringBuilder.append(", maxLongitude=");
		stringBuilder.append(this.maxLongitude);
		return stringBuilder.toString();
	}
}
