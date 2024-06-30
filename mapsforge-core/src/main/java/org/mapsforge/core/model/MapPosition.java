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

import java.util.Objects;

/**
 * A MapPosition represents an immutable pair of {@link LatLong} and zoom level.
 */
public class MapPosition {

    /**
     * The geographical coordinates of the map center.
     */
    public final LatLong latLong;

    /**
     * The fractional zoom of the map.
     */
    public final double zoom;

    /**
     * The zoom level of the map.
     */
    public final byte zoomLevel;

    /**
     * The rotation of the map (may be null).
     */
    public final Rotation rotation;

    /**
     * @param latLong   the geographical coordinates of the map center.
     * @param zoomLevel the zoom level of the map.
     * @throws IllegalArgumentException if {@code latLong} is null or {@code zoomLevel} is negative.
     */
    public MapPosition(LatLong latLong, byte zoomLevel) {
        this(latLong, zoomLevel, Rotation.NULL_ROTATION);
    }

    /**
     * @param latLong  the geographical coordinates of the map center.
     * @param zoom     the fractional zoom of the map.
     * @param rotation the rotation of the map (may be null).
     * @throws IllegalArgumentException if {@code latLong} is null or {@code zoom} is negative.
     */
    public MapPosition(LatLong latLong, double zoom, Rotation rotation) {
        if (latLong == null) {
            throw new IllegalArgumentException("latLong must not be null");
        } else if (zoom < 0) {
            throw new IllegalArgumentException("zoom must not be negative: " + zoom);
        }
        this.latLong = latLong;
        this.zoom = zoom;
        this.zoomLevel = (byte) Math.floor(zoom);
        this.rotation = rotation;
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
        } else if (!Objects.equals(this.rotation, other.rotation)) {
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
        result = prime * result + this.rotation.hashCode();
        return result;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("latLong=");
        stringBuilder.append(this.latLong);
        stringBuilder.append(", zoomLevel=");
        stringBuilder.append(this.zoomLevel);
        stringBuilder.append(", rotation=");
        stringBuilder.append(this.rotation);
        return stringBuilder.toString();
    }
}
