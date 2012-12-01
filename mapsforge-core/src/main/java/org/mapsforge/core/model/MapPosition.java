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
 * A MapPosition represents an immutable pair of {@link GeoPoint} and zoom level.
 */
public class MapPosition implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * The geographical coordinates of the map center.
	 */
	public final GeoPoint geoPoint;

	/**
	 * The zoom level of the map.
	 */
	public final byte zoomLevel;

	/**
	 * @param geoPoint
	 *            the geographical coordinates of the map center.
	 * @param zoomLevel
	 *            the zoom level of the map.
	 * @throws IllegalArgumentException
	 *             if {@code geoPoint} is null or {@code zoomLevel} is negative.
	 */
	public MapPosition(GeoPoint geoPoint, byte zoomLevel) {
		if (geoPoint == null) {
			throw new IllegalArgumentException("geoPoint must not be null");
		} else if (zoomLevel < 0) {
			throw new IllegalArgumentException("zoomLevel must not be negative: " + zoomLevel);
		}
		this.geoPoint = geoPoint;
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
		if (this.geoPoint == null) {
			if (other.geoPoint != null) {
				return false;
			}
		} else if (!this.geoPoint.equals(other.geoPoint)) {
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
		result = prime * result + ((this.geoPoint == null) ? 0 : this.geoPoint.hashCode());
		result = prime * result + this.zoomLevel;
		return result;
	}

	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("MapPosition [geoPoint=");
		stringBuilder.append(this.geoPoint);
		stringBuilder.append(", zoomLevel=");
		stringBuilder.append(this.zoomLevel);
		stringBuilder.append("]");
		return stringBuilder.toString();
	}
}
