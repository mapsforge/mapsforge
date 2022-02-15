/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Christian Pesch
 * Copyright 2015-2022 devemux86
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

import org.mapsforge.core.util.LatLongUtils;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.core.util.Parameters;

import java.io.Serializable;
import java.util.List;

/**
 * A BoundingBox represents an immutable set of two latitude and two longitude coordinates.
 */
public class BoundingBox implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new BoundingBox from a comma-separated string of coordinates in the order minLat, minLon, maxLat,
     * maxLon. All coordinate values must be in degrees.
     *
     * @param boundingBoxString the string that describes the BoundingBox.
     * @return a new BoundingBox with the given coordinates.
     * @throws IllegalArgumentException if the string cannot be parsed or describes an invalid BoundingBox.
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
     * @param minLatitude  the minimum latitude coordinate in degrees.
     * @param minLongitude the minimum longitude coordinate in degrees.
     * @param maxLatitude  the maximum latitude coordinate in degrees.
     * @param maxLongitude the maximum longitude coordinate in degrees.
     * @throws IllegalArgumentException if a coordinate is invalid.
     */
    public BoundingBox(double minLatitude, double minLongitude, double maxLatitude, double maxLongitude) {
        if (Parameters.VALIDATE_COORDINATES) {
            LatLongUtils.validateLatitude(minLatitude);
            LatLongUtils.validateLongitude(minLongitude);
            LatLongUtils.validateLatitude(maxLatitude);
            LatLongUtils.validateLongitude(maxLongitude);
        }

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
     * @param latLongs the coordinates list.
     */
    public BoundingBox(List<LatLong> latLongs) {
        double minLatitude = Double.POSITIVE_INFINITY;
        double minLongitude = Double.POSITIVE_INFINITY;
        double maxLatitude = Double.NEGATIVE_INFINITY;
        double maxLongitude = Double.NEGATIVE_INFINITY;
        for (LatLong latLong : latLongs) {
            double latitude = latLong.latitude;
            double longitude = latLong.longitude;

            minLatitude = Math.min(minLatitude, latitude);
            minLongitude = Math.min(minLongitude, longitude);
            maxLatitude = Math.max(maxLatitude, latitude);
            maxLongitude = Math.max(maxLongitude, longitude);
        }

        this.minLatitude = minLatitude;
        this.minLongitude = minLongitude;
        this.maxLatitude = maxLatitude;
        this.maxLongitude = maxLongitude;
    }

    /**
     * @param latitude  the latitude coordinate in degrees.
     * @param longitude the longitude coordinate in degrees.
     * @return true if this BoundingBox contains the given coordinates, false otherwise.
     */
    public boolean contains(double latitude, double longitude) {
        return this.minLatitude <= latitude && this.maxLatitude >= latitude
                && this.minLongitude <= longitude && this.maxLongitude >= longitude;
    }

    /**
     * @param latLong the LatLong whose coordinates should be checked.
     * @return true if this BoundingBox contains the given LatLong, false otherwise.
     */
    public boolean contains(LatLong latLong) {
        return contains(latLong.latitude, latLong.longitude);
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
     * @param boundingBox the BoundingBox which this BoundingBox should be extended if it is larger
     * @return a BoundingBox that covers this BoundingBox and the given BoundingBox.
     */
    public BoundingBox extendBoundingBox(BoundingBox boundingBox) {
        return new BoundingBox(Math.min(this.minLatitude, boundingBox.minLatitude),
                Math.min(this.minLongitude, boundingBox.minLongitude),
                Math.max(this.maxLatitude, boundingBox.maxLatitude),
                Math.max(this.maxLongitude, boundingBox.maxLongitude));
    }

    /**
     * Creates a BoundingBox extended up to coordinates (but does not cross date line/poles).
     *
     * @param latitude  up to the extension
     * @param longitude up to the extension
     * @return an extended BoundingBox or this (if contains coordinates)
     */
    public BoundingBox extendCoordinates(double latitude, double longitude) {
        if (contains(latitude, longitude)) {
            return this;
        }

        double minLat = Math.max(MercatorProjection.LATITUDE_MIN, Math.min(this.minLatitude, latitude));
        double minLon = Math.max(-180, Math.min(this.minLongitude, longitude));
        double maxLat = Math.min(MercatorProjection.LATITUDE_MAX, Math.max(this.maxLatitude, latitude));
        double maxLon = Math.min(180, Math.max(this.maxLongitude, longitude));

        return new BoundingBox(minLat, minLon, maxLat, maxLon);
    }

    /**
     * Creates a BoundingBox extended up to <code>LatLong</code> (but does not cross date line/poles).
     *
     * @param latLong coordinates up to the extension
     * @return an extended BoundingBox or this (if contains coordinates)
     */
    public BoundingBox extendCoordinates(LatLong latLong) {
        return extendCoordinates(latLong.latitude, latLong.longitude);
    }

    /**
     * Creates a BoundingBox that is a fixed degree amount larger on all sides (but does not cross date line/poles).
     *
     * @param verticalExpansion   degree extension (must be >= 0)
     * @param horizontalExpansion degree extension (must be >= 0)
     * @return an extended BoundingBox or this (if degrees == 0)
     */
    public BoundingBox extendDegrees(double verticalExpansion, double horizontalExpansion) {
        if (verticalExpansion == 0 && horizontalExpansion == 0) {
            return this;
        } else if (verticalExpansion < 0 || horizontalExpansion < 0) {
            throw new IllegalArgumentException("BoundingBox extend operation does not accept negative values");
        }

        double minLat = Math.max(MercatorProjection.LATITUDE_MIN, this.minLatitude - verticalExpansion);
        double minLon = Math.max(-180, this.minLongitude - horizontalExpansion);
        double maxLat = Math.min(MercatorProjection.LATITUDE_MAX, this.maxLatitude + verticalExpansion);
        double maxLon = Math.min(180, this.maxLongitude + horizontalExpansion);

        return new BoundingBox(minLat, minLon, maxLat, maxLon);
    }

    /**
     * Creates a BoundingBox that is a fixed margin factor larger on all sides (but does not cross date line/poles).
     *
     * @param margin extension (must be > 0)
     * @return an extended BoundingBox or this (if margin == 1)
     */
    public BoundingBox extendMargin(float margin) {
        if (margin == 1) {
            return this;
        } else if (margin <= 0) {
            throw new IllegalArgumentException("BoundingBox extend operation does not accept negative or zero values");
        }

        double verticalExpansion = (this.getLatitudeSpan() * margin - this.getLatitudeSpan()) * 0.5;
        double horizontalExpansion = (this.getLongitudeSpan() * margin - this.getLongitudeSpan()) * 0.5;

        double minLat = Math.max(MercatorProjection.LATITUDE_MIN, this.minLatitude - verticalExpansion);
        double minLon = Math.max(-180, this.minLongitude - horizontalExpansion);
        double maxLat = Math.min(MercatorProjection.LATITUDE_MAX, this.maxLatitude + verticalExpansion);
        double maxLon = Math.min(180, this.maxLongitude + horizontalExpansion);

        return new BoundingBox(minLat, minLon, maxLat, maxLon);
    }

    /**
     * Creates a BoundingBox that is a fixed meter amount larger on all sides (but does not cross date line/poles).
     *
     * @param meters extension (must be >= 0)
     * @return an extended BoundingBox or this (if meters == 0)
     */
    public BoundingBox extendMeters(int meters) {
        if (meters == 0) {
            return this;
        } else if (meters < 0) {
            throw new IllegalArgumentException("BoundingBox extend operation does not accept negative values");
        }

        double verticalExpansion = LatLongUtils.latitudeDistance(meters);
        double horizontalExpansion = LatLongUtils.longitudeDistance(meters, Math.max(Math.abs(minLatitude), Math.abs(maxLatitude)));

        double minLat = Math.max(MercatorProjection.LATITUDE_MIN, this.minLatitude - verticalExpansion);
        double minLon = Math.max(-180, this.minLongitude - horizontalExpansion);
        double maxLat = Math.min(MercatorProjection.LATITUDE_MAX, this.maxLatitude + verticalExpansion);
        double maxLon = Math.min(180, this.maxLongitude + horizontalExpansion);

        return new BoundingBox(minLat, minLon, maxLat, maxLon);
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

    /**
     * Computes the coordinates of this bounding box relative to a tile.
     *
     * @param tile the tile to compute the relative position for.
     * @return rectangle giving the relative position.
     */
    public Rectangle getPositionRelativeToTile(Tile tile) {
        Point upperLeft = MercatorProjection.getPixelRelativeToTile(new LatLong(this.maxLatitude, minLongitude), tile);
        Point lowerRight = MercatorProjection.getPixelRelativeToTile(new LatLong(this.minLatitude, maxLongitude), tile);
        return new Rectangle(upperLeft.x, upperLeft.y, lowerRight.x, lowerRight.y);
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
     * @param boundingBox the BoundingBox which should be checked for intersection with this BoundingBox.
     * @return true if this BoundingBox intersects with the given BoundingBox, false otherwise.
     */
    public boolean intersects(BoundingBox boundingBox) {
        if (this == boundingBox) {
            return true;
        }

        return this.maxLatitude >= boundingBox.minLatitude && this.maxLongitude >= boundingBox.minLongitude
                && this.minLatitude <= boundingBox.maxLatitude && this.minLongitude <= boundingBox.maxLongitude;
    }

    /**
     * Returns if an area built from the latLongs intersects with a bias towards
     * returning true.
     * The method returns fast if any of the points lie within the bbox. If none of the points
     * lie inside the box, it constructs the outer bbox for all the points and tests for intersection
     * (so it is possible that the area defined by the points does not actually intersect)
     *
     * @param latLongs the points that define an area
     * @return false if there is no intersection, true if there could be an intersection
     */
    public boolean intersectsArea(LatLong[][] latLongs) {
        if (latLongs.length == 0 || latLongs[0].length == 0) {
            return false;
        }
        for (LatLong[] outer : latLongs) {
            for (LatLong latLong : outer) {
                if (this.contains(latLong)) {
                    // if any of the points is inside the bbox return early
                    return true;
                }
            }
        }

        // no fast solution, so accumulate boundary points
        double tmpMinLat = latLongs[0][0].latitude;
        double tmpMinLon = latLongs[0][0].longitude;
        double tmpMaxLat = latLongs[0][0].latitude;
        double tmpMaxLon = latLongs[0][0].longitude;

        for (LatLong[] outer : latLongs) {
            for (LatLong latLong : outer) {
                tmpMinLat = Math.min(tmpMinLat, latLong.latitude);
                tmpMaxLat = Math.max(tmpMaxLat, latLong.latitude);
                tmpMinLon = Math.min(tmpMinLon, latLong.longitude);
                tmpMaxLon = Math.max(tmpMaxLon, latLong.longitude);
            }
        }
        return this.intersects(new BoundingBox(tmpMinLat, tmpMinLon, tmpMaxLat, tmpMaxLon));
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
