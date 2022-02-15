/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
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
import org.mapsforge.core.util.Parameters;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This immutable class represents a geographic coordinate with a latitude and longitude value.
 */
public class LatLong implements Comparable<LatLong> {
    /**
     * The RegEx pattern to read WKT points.
     */
    private static final Pattern WKT_POINT_PATTERN = Pattern
            .compile(".*POINT\\s?\\(([\\d\\.]+)\\s([\\d\\.]+)\\).*");

    /**
     * The internal latitude value.
     */
    public final double latitude;

    /**
     * The internal longitude value.
     */
    public final double longitude;

    /**
     * Constructs a new LatLong with the given latitude and longitude values, measured in
     * degrees.
     *
     * @param latitude  the latitude value in degrees.
     * @param longitude the longitude value in degrees.
     * @throws IllegalArgumentException if the latitude or longitude value is invalid.
     */
    public LatLong(double latitude, double longitude) throws IllegalArgumentException {
        this.latitude = Parameters.VALIDATE_COORDINATES ? LatLongUtils.validateLatitude(latitude) : latitude;
        this.longitude = Parameters.VALIDATE_COORDINATES ? LatLongUtils.validateLongitude(longitude) : longitude;
    }

    /**
     * Constructs a new LatLong from a Well-Known-Text (WKT) representation of a point.
     * For example: POINT(13.4125 52.52235)
     * <p/>
     * WKT is used in PostGIS and other spatial databases.
     *
     * @param wellKnownText is the WKT point which describes the new LatLong, this needs to be in
     *                      degrees using a WGS84 representation. The coordinate order in the POINT is
     *                      defined as POINT(long lat).
     */
    public LatLong(String wellKnownText) {
        Matcher m = WKT_POINT_PATTERN.matcher(wellKnownText);
        m.matches();
        double longitude = Double.parseDouble(m.group(1));
        this.longitude = Parameters.VALIDATE_COORDINATES ? LatLongUtils.validateLongitude(longitude) : longitude;
        double latitude = Double.parseDouble(m.group(2));
        this.latitude = Parameters.VALIDATE_COORDINATES ? LatLongUtils.validateLatitude(latitude) : latitude;
    }

    /**
     * This method is necessary for inserting LatLongs into tree data structures.
     */
    @Override
    public int compareTo(LatLong latLong) {
        if (this.latitude > latLong.latitude || this.longitude > latLong.longitude) {
            return 1;
        } else if (this.latitude < latLong.latitude
                || this.longitude < latLong.longitude) {
            return -1;
        }
        return 0;
    }

    /**
     * Returns the destination point from this point having travelled the given distance on the
     * given initial bearing (bearing normally varies around path followed).
     *
     * @param distance the distance travelled, in same units as earth radius (default: meters)
     * @param bearing  the initial bearing in degrees from north
     * @return the destination point
     * @see <a href="http://www.movable-type.co.uk/scripts/latlon.js">latlon.js</a>
     */
    public LatLong destinationPoint(double distance, float bearing) {
        return LatLongUtils.destinationPoint(this, distance, bearing);
    }

    /**
     * Calculate the Euclidean distance from this LatLong to another.
     *
     * @param other The LatLong to calculate the distance to
     * @return the distance in degrees as a double
     */
    public double distance(LatLong other) {
        return LatLongUtils.distance(this, other);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof LatLong)) {
            return false;
        } else {
            LatLong other = (LatLong) obj;
            if (this.latitude != other.latitude) {
                return false;
            } else if (this.longitude != other.longitude) {
                return false;
            }
            return true;
        }
    }

    /**
     * Constructs a new LatLong with the given latitude and longitude values, measured in
     * microdegrees.
     *
     * @param latitudeE6  the latitude value in microdegrees.
     * @param longitudeE6 the longitude value in microdegrees.
     * @return the LatLong
     * @throws IllegalArgumentException if the latitudeE6 or longitudeE6 value is invalid.
     */
    public static LatLong fromMicroDegrees(int latitudeE6, int longitudeE6) {
        return new LatLong(LatLongUtils.microdegreesToDegrees(latitudeE6),
                LatLongUtils.microdegreesToDegrees(longitudeE6));
    }

    /**
     * Constructs a new LatLong from a comma-separated String containing latitude and
     * longitude values (also ';', ':' and whitespace work as separator).
     * Latitude and longitude are interpreted as measured in degrees.
     *
     * @param latLonString the String containing the latitude and longitude values
     * @return the LatLong
     * @throws IllegalArgumentException if the latLonString could not be interpreted as a coordinate
     */
    public static LatLong fromString(String latLonString) {
        String[] split = latLonString.split("[,;:\\s]");
        if (split.length != 2)
            throw new IllegalArgumentException("cannot read coordinate, not a valid format");
        double latitude = Double.parseDouble(split[0]);
        double longitude = Double.parseDouble(split[1]);
        return new LatLong(latitude, longitude);
    }

    /**
     * Returns the latitude value of this coordinate.
     *
     * @return the latitude value of this coordinate.
     */
    public double getLatitude() {
        return this.latitude;
    }

    /**
     * Returns the latitude value in microdegrees of this coordinate.
     *
     * @return the latitude value in microdegrees of this coordinate.
     */
    public int getLatitudeE6() {
        return LatLongUtils.degreesToMicrodegrees(this.latitude);
    }

    /**
     * Returns the longitude value of this coordinate.
     *
     * @return the longitude value of this coordinate.
     */
    public double getLongitude() {
        return this.longitude;
    }

    /**
     * Returns the longitude value in microdegrees of this coordinate.
     *
     * @return the longitude value in microdegrees of this coordinate.
     */
    public int getLongitudeE6() {
        return LatLongUtils.degreesToMicrodegrees(this.longitude);
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

    /**
     * Calculate the spherical distance from this LatLong to another.
     * <p/>
     * Use vincentyDistance for more accuracy but less performance.
     *
     * @param other The LatLong to calculate the distance to
     * @return the distance in meters as a double
     */
    public double sphericalDistance(LatLong other) {
        return LatLongUtils.sphericalDistance(this, other);
    }

    @Override
    public String toString() {
        return "latitude=" + this.latitude + ", longitude=" + this.longitude;
    }

    /**
     * Calculate the spherical distance from this LatLong to another.
     * <p/>
     * Use "distance" for faster computation with less accuracy.
     *
     * @param other The LatLong to calculate the distance to
     * @return the distance in meters as a double
     */
    public double vincentyDistance(LatLong other) {
        return LatLongUtils.vincentyDistance(this, other);
    }
}
