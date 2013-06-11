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
 * A LatLong represents an immutable pair of latitude and longitude coordinates.
 */
public class LatLong implements Comparable<LatLong>, Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * The latitude coordinate of this LatLong in degrees.
	 */
	public final double latitude;

	/**
	 * The longitude coordinate of this LatLong in degrees.
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
	public LatLong(double latitude, double longitude) {
		LatLongUtils.validateLatitude(latitude);
		LatLongUtils.validateLongitude(longitude);

		this.latitude = latitude;
		this.longitude = longitude;
	}

	@Override
	public int compareTo(LatLong latLong) {
		if (this.longitude > latLong.longitude) {
			return 1;
		} else if (this.longitude < latLong.longitude) {
			return -1;
		} else if (this.latitude > latLong.latitude) {
			return 1;
		} else if (this.latitude < latLong.latitude) {
			return -1;
		}
		return 0;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (!(obj instanceof LatLong)) {
			return false;
		}
		LatLong other = (LatLong) obj;
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
