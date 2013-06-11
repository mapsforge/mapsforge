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

/**
 * A MapPosition represents an immutable pair of {@link LatLong} and zoom level.
 */
public class MapPosition implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * The geographical coordinates of the map center.
	 */
	public final LatLong latLong;

	/**
	 * The zoom level of the map.
	 */
	public final byte zoomLevel;

	/**
	 * @param latLong
	 *            the geographical coordinates of the map center.
	 * @param zoomLevel
	 *            the zoom level of the map.
	 * @throws IllegalArgumentException
	 *             if {@code latLong} is null or {@code zoomLevel} is negative.
	 */
	public MapPosition(LatLong latLong, byte zoomLevel) {
		if (latLong == null) {
			throw new IllegalArgumentException("latLong must not be null");
		} else if (zoomLevel < 0) {
			throw new IllegalArgumentException("zoomLevel must not be negative: " + zoomLevel);
		}
		this.latLong = latLong;
		this.zoomLevel = zoomLevel;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (!(obj instanceof MapPosition)) {
			return false;
		}
		MapPosition other = (MapPosition) obj;
		if (!this.latLong.equals(other.latLong)) {
			return false;
		} else if (this.zoomLevel != other.zoomLevel) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + this.latLong.hashCode();
		result = prime * result + this.zoomLevel;
		return result;
	}

	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("latLong=");
		stringBuilder.append(this.latLong);
		stringBuilder.append(", zoomLevel=");
		stringBuilder.append(this.zoomLevel);
		return stringBuilder.toString();
	}
}
