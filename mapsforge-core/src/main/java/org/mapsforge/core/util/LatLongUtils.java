/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2014 Christian Pesch
 * Copyright 2014-2017 devemux86
 * Copyright 2015 Andreas Schildbach
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
package org.mapsforge.core.util;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Dimension;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * A utility class to convert, parse and validate geographical latitude/longitude coordinates.
 */
public final class LatLongUtils {
    /**
     * The equatorial radius as defined by the <a href="http://en.wikipedia.org/wiki/World_Geodetic_System">WGS84
     * ellipsoid</a>. WGS84 is the reference coordinate system used by the Global Positioning System.
     */
    public static final double EQUATORIAL_RADIUS = 6378137.0;

    /**
     * The flattening factor of the earth's ellipsoid is required for distance computation.
     */
    public static final double INVERSE_FLATTENING = 298.257223563;

    /**
     * Polar radius of earth is required for distance computation.
     */
    public static final double POLAR_RADIUS = 6356752.3142;

    /**
     * Maximum possible latitude coordinate.
     */
    public static final double LATITUDE_MAX = 90;

    /**
     * Minimum possible latitude coordinate.
     */
    public static final double LATITUDE_MIN = -LATITUDE_MAX;

    /**
     * Maximum possible longitude coordinate.
     */
    public static final double LONGITUDE_MAX = 180;

    /**
     * Minimum possible longitude coordinate.
     */
    public static final double LONGITUDE_MIN = -LONGITUDE_MAX;

    /**
     * Conversion factor from degrees to microdegrees.
     */
    private static final double CONVERSION_FACTOR = 1000000.0;

    private static final String DELIMITER = ",";

    /**
     * Find if the given point lies within this polygon.
     * <p>
     * http://www.ecse.rpi.edu/Homepages/wrf/Research/Short_Notes/pnpoly.html
     *
     * @return true if this polygon contains the given point, false otherwise.
     */
    public static boolean contains(LatLong[] latLongs, LatLong latLong) {
        boolean result = false;
        for (int i = 0, j = latLongs.length - 1; i < latLongs.length; j = i++) {
            if ((latLongs[i].latitude > latLong.latitude) != (latLongs[j].latitude > latLong.latitude)
                    && (latLong.longitude < (latLongs[j].longitude - latLongs[i].longitude) * (latLong.latitude - latLongs[i].latitude)
                    / (latLongs[j].latitude - latLongs[i].latitude) + latLongs[i].longitude)) {
                result = !result;
            }
        }
        return result;
    }

    /**
     * Find if the given point lies within this polygon.
     * <p>
     * http://www.ecse.rpi.edu/Homepages/wrf/Research/Short_Notes/pnpoly.html
     *
     * @return true if this polygon contains the given point, false otherwise.
     */
    public static boolean contains(List<LatLong> latLongs, LatLong latLong) {
        boolean result = false;
        for (int i = 0, j = latLongs.size() - 1; i < latLongs.size(); j = i++) {
            if ((latLongs.get(i).latitude > latLong.latitude) != (latLongs.get(j).latitude > latLong.latitude)
                    && (latLong.longitude < (latLongs.get(j).longitude - latLongs.get(i).longitude) * (latLong.latitude - latLongs.get(i).latitude)
                    / (latLongs.get(j).latitude - latLongs.get(i).latitude) + latLongs.get(i).longitude)) {
                result = !result;
            }
        }
        return result;
    }

    /**
     * Converts a coordinate from degrees to microdegrees (degrees * 10^6). No validation is performed.
     *
     * @param coordinate the coordinate in degrees.
     * @return the coordinate in microdegrees (degrees * 10^6).
     */
    public static int degreesToMicrodegrees(double coordinate) {
        return (int) (coordinate * CONVERSION_FACTOR);
    }

    /**
     * Returns the destination point from this point having travelled the given distance on the
     * given initial bearing (bearing normally varies around path followed).
     *
     * @param start    the start point
     * @param distance the distance travelled, in same units as earth radius (default: meters)
     * @param bearing  the initial bearing in degrees from north
     * @return the destination point
     * @see <a href="http://www.movable-type.co.uk/scripts/latlon.js">latlon.js</a>
     */
    public static LatLong destinationPoint(LatLong start, double distance, float bearing) {
        double theta = Math.toRadians(bearing);
        double delta = distance / EQUATORIAL_RADIUS; // angular distance in radians

        double phi1 = Math.toRadians(start.latitude);
        double lambda1 = Math.toRadians(start.longitude);

        double phi2 = Math.asin(Math.sin(phi1) * Math.cos(delta)
                + Math.cos(phi1) * Math.sin(delta) * Math.cos(theta));
        double lambda2 = lambda1 + Math.atan2(Math.sin(theta) * Math.sin(delta) * Math.cos(phi1),
                Math.cos(delta) - Math.sin(phi1) * Math.sin(phi2));

        return new LatLong(Math.toDegrees(phi2), Math.toDegrees(lambda2));
    }

    /**
     * Calculate the Euclidean distance between two LatLongs in degrees using the Pythagorean
     * theorem.
     *
     * @param latLong1 first LatLong
     * @param latLong2 second LatLong
     * @return distance in degrees as a double
     */
    public static double distance(LatLong latLong1, LatLong latLong2) {
        return Math.hypot(latLong1.longitude - latLong2.longitude, latLong1.latitude - latLong2.latitude);
    }

    /**
     * Returns the distance between the given segment and point.
     * <p>
     * libGDX (Apache 2.0)
     */
    public static double distanceSegmentPoint(double startX, double startY, double endX, double endY, double pointX, double pointY) {
        Point nearest = nearestSegmentPoint(startX, startY, endX, endY, pointX, pointY);
        return Math.hypot(nearest.x - pointX, nearest.y - pointY);
    }

    /**
     * Creates a new LatLong from a comma-separated string of coordinates in the order latitude, longitude. All
     * coordinate values must be in degrees.
     *
     * @param latLongString the string that describes the LatLong.
     * @return a new LatLong with the given coordinates.
     * @throws IllegalArgumentException if the string cannot be parsed or describes an invalid LatLong.
     */
    public static LatLong fromString(String latLongString) {
        double[] coordinates = parseCoordinateString(latLongString, 2);
        return new LatLong(coordinates[0], coordinates[1]);
    }

    /**
     * Find if this way is closed.
     *
     * @return true if this way is closed, false otherwise.
     */
    public static boolean isClosedWay(LatLong[] latLongs) {
        return latLongs[0].distance(latLongs[latLongs.length - 1]) < 0.000000001;
    }

    /**
     * Calculates the amount of degrees of latitude for a given distance in meters.
     *
     * @param meters distance in meters
     * @return latitude degrees
     */
    public static double latitudeDistance(int meters) {
        return (meters * 360) / (2 * Math.PI * EQUATORIAL_RADIUS);
    }

    /**
     * Calculates the amount of degrees of longitude for a given distance in meters.
     *
     * @param meters   distance in meters
     * @param latitude the latitude at which the calculation should be performed
     * @return longitude degrees
     */
    public static double longitudeDistance(int meters, double latitude) {
        return (meters * 360) / (2 * Math.PI * EQUATORIAL_RADIUS * Math.cos(Math.toRadians(latitude)));
    }

    /**
     * Converts a coordinate from microdegrees (degrees * 10^6) to degrees. No validation is performed.
     *
     * @param coordinate the coordinate in microdegrees (degrees * 10^6).
     * @return the coordinate in degrees.
     */
    public static double microdegreesToDegrees(int coordinate) {
        return coordinate / CONVERSION_FACTOR;
    }

    /**
     * Returns a point on the segment nearest to the specified point.
     * <p>
     * libGDX (Apache 2.0)
     */
    public static Point nearestSegmentPoint(double startX, double startY, double endX, double endY, double pointX, double pointY) {
        double xDiff = endX - startX;
        double yDiff = endY - startY;
        double length2 = xDiff * xDiff + yDiff * yDiff;
        if (length2 == 0) return new Point(startX, startY);
        double t = ((pointX - startX) * (endX - startX) + (pointY - startY) * (endY - startY)) / length2;
        if (t < 0) return new Point(startX, startY);
        if (t > 1) return new Point(endX, endY);
        return new Point(startX + t * (endX - startX), startY + t * (endY - startY));
    }

    /**
     * Parses a given number of comma-separated coordinate values from the supplied string.
     *
     * @param coordinatesString   a comma-separated string of coordinate values.
     * @param numberOfCoordinates the expected number of coordinate values in the string.
     * @return the coordinate values in the order they have been parsed from the string.
     * @throws IllegalArgumentException if the string is invalid or does not contain the given number of coordinate values.
     */
    public static double[] parseCoordinateString(String coordinatesString, int numberOfCoordinates) {
        StringTokenizer stringTokenizer = new StringTokenizer(coordinatesString, DELIMITER, true);
        boolean isDelimiter = true;
        List<String> tokens = new ArrayList<>(numberOfCoordinates);

        while (stringTokenizer.hasMoreTokens()) {
            String token = stringTokenizer.nextToken();
            isDelimiter = !isDelimiter;
            if (isDelimiter) {
                continue;
            }

            tokens.add(token);
        }

        if (isDelimiter) {
            throw new IllegalArgumentException("invalid coordinate delimiter: " + coordinatesString);
        } else if (tokens.size() != numberOfCoordinates) {
            throw new IllegalArgumentException("invalid number of coordinate values: " + coordinatesString);
        }

        double[] coordinates = new double[numberOfCoordinates];
        for (int i = 0; i < numberOfCoordinates; ++i) {
            coordinates[i] = Double.parseDouble(tokens.get(i));
        }
        return coordinates;
    }

    /**
     * Calculate the spherical distance between two LatLongs in meters using the Haversine
     * formula.
     * <p/>
     * This calculation is done using the assumption, that the earth is a sphere, it is not
     * though. If you need a higher precision and can afford a longer execution time you might
     * want to use vincentyDistance.
     *
     * @param latLong1 first LatLong
     * @param latLong2 second LatLong
     * @return distance in meters as a double
     */
    public static double sphericalDistance(LatLong latLong1, LatLong latLong2) {
        double dLat = Math.toRadians(latLong2.latitude - latLong1.latitude);
        double dLon = Math.toRadians(latLong2.longitude - latLong1.longitude);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(latLong1.latitude))
                * Math.cos(Math.toRadians(latLong2.latitude)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return c * LatLongUtils.EQUATORIAL_RADIUS;
    }

    /**
     * @param latitude the latitude coordinate in degrees which should be validated.
     * @return the latitude value
     * @throws IllegalArgumentException if the latitude coordinate is invalid or {@link Double#NaN}.
     */
    public static double validateLatitude(double latitude) {
        if (Double.isNaN(latitude) || latitude < LATITUDE_MIN || latitude > LATITUDE_MAX) {
            throw new IllegalArgumentException("invalid latitude: " + latitude);
        }
        return latitude;
    }

    /**
     * @param longitude the longitude coordinate in degrees which should be validated.
     * @return the longitude value
     * @throws IllegalArgumentException if the longitude coordinate is invalid or {@link Double#NaN}.
     */
    public static double validateLongitude(double longitude) {
        if (Double.isNaN(longitude) || longitude < LONGITUDE_MIN || longitude > LONGITUDE_MAX) {
            throw new IllegalArgumentException("invalid longitude: " + longitude);
        }
        return longitude;
    }

    /**
     * Calculates geodetic distance between two LatLongs using Vincenty inverse formula
     * for ellipsoids. This is very accurate but consumes more resources and time than the
     * sphericalDistance method.
     * <p/>
     * Adaptation of Chriss Veness' JavaScript Code on
     * http://www.movable-type.co.uk/scripts/latlong-vincenty.html
     * <p/>
     * Paper: Vincenty inverse formula - T Vincenty, "Direct and Inverse Solutions of Geodesics
     * on the Ellipsoid with application of nested equations", Survey Review, vol XXII no 176,
     * 1975 (http://www.ngs.noaa.gov/PUBS_LIB/inverse.pdf)
     *
     * @param latLong1 first LatLong
     * @param latLong2 second LatLong
     * @return distance in meters between points as a double
     */
    public static double vincentyDistance(LatLong latLong1, LatLong latLong2) {
        double f = 1 / LatLongUtils.INVERSE_FLATTENING;
        double L = Math.toRadians(latLong2.getLongitude() - latLong1.getLongitude());
        double U1 = Math.atan((1 - f) * Math.tan(Math.toRadians(latLong1.getLatitude())));
        double U2 = Math.atan((1 - f) * Math.tan(Math.toRadians(latLong2.getLatitude())));
        double sinU1 = Math.sin(U1), cosU1 = Math.cos(U1);
        double sinU2 = Math.sin(U2), cosU2 = Math.cos(U2);

        double lambda = L, lambdaP, iterLimit = 100;

        double cosSqAlpha = 0, sinSigma = 0, cosSigma = 0, cos2SigmaM = 0, sigma = 0, sinLambda = 0, sinAlpha = 0, cosLambda = 0;
        do {
            sinLambda = Math.sin(lambda);
            cosLambda = Math.cos(lambda);
            sinSigma = Math.sqrt((cosU2 * sinLambda) * (cosU2 * sinLambda)
                    + (cosU1 * sinU2 - sinU1 * cosU2 * cosLambda)
                    * (cosU1 * sinU2 - sinU1 * cosU2 * cosLambda));
            if (sinSigma == 0)
                return 0; // co-incident points
            cosSigma = sinU1 * sinU2 + cosU1 * cosU2 * cosLambda;
            sigma = Math.atan2(sinSigma, cosSigma);
            sinAlpha = cosU1 * cosU2 * sinLambda / sinSigma;
            cosSqAlpha = 1 - sinAlpha * sinAlpha;
            if (cosSqAlpha != 0) {
                cos2SigmaM = cosSigma - 2 * sinU1 * sinU2 / cosSqAlpha;
            } else {
                cos2SigmaM = 0;
            }
            double C = f / 16 * cosSqAlpha * (4 + f * (4 - 3 * cosSqAlpha));
            lambdaP = lambda;
            lambda = L
                    + (1 - C)
                    * f
                    * sinAlpha
                    * (sigma + C * sinSigma
                    * (cos2SigmaM + C * cosSigma * (-1 + 2 * cos2SigmaM * cos2SigmaM)));
        } while (Math.abs(lambda - lambdaP) > 1e-12 && --iterLimit > 0);

        if (iterLimit == 0)
            return 0; // formula failed to converge

        double uSq = cosSqAlpha
                * (Math.pow(LatLongUtils.EQUATORIAL_RADIUS, 2) - Math.pow(LatLongUtils.POLAR_RADIUS, 2))
                / Math.pow(LatLongUtils.POLAR_RADIUS, 2);
        double A = 1 + uSq / 16384 * (4096 + uSq * (-768 + uSq * (320 - 175 * uSq)));
        double B = uSq / 1024 * (256 + uSq * (-128 + uSq * (74 - 47 * uSq)));
        double deltaSigma = B
                * sinSigma
                * (cos2SigmaM + B
                / 4
                * (cosSigma * (-1 + 2 * cos2SigmaM * cos2SigmaM) - B / 6 * cos2SigmaM
                * (-3 + 4 * sinSigma * sinSigma)
                * (-3 + 4 * cos2SigmaM * cos2SigmaM)));
        double s = LatLongUtils.POLAR_RADIUS * A * (sigma - deltaSigma);

        return s;
    }

    /**
     * Calculates the zoom level that allows to display the {@link BoundingBox} on a view with the {@link Dimension} and
     * tile size.
     *
     * @param dimension   the {@link Dimension} of the view.
     * @param boundingBox the {@link BoundingBox} to display.
     * @param tileSize    the size of the tiles.
     * @return the zoom level that allows to display the {@link BoundingBox} on a view with the {@link Dimension} and
     * tile size.
     */
    public static byte zoomForBounds(Dimension dimension, BoundingBox boundingBox, int tileSize) {
        long mapSize = MercatorProjection.getMapSize((byte) 0, tileSize);
        double pixelXMax = MercatorProjection.longitudeToPixelX(boundingBox.maxLongitude, mapSize);
        double pixelXMin = MercatorProjection.longitudeToPixelX(boundingBox.minLongitude, mapSize);
        double zoomX = -Math.log(Math.abs(pixelXMax - pixelXMin) / dimension.width) / Math.log(2);
        double pixelYMax = MercatorProjection.latitudeToPixelY(boundingBox.maxLatitude, mapSize);
        double pixelYMin = MercatorProjection.latitudeToPixelY(boundingBox.minLatitude, mapSize);
        double zoomY = -Math.log(Math.abs(pixelYMax - pixelYMin) / dimension.height) / Math.log(2);
        double zoom = Math.floor(Math.min(zoomX, zoomY));
        if (zoom < 0) {
            return 0;
        }
        if (zoom > Byte.MAX_VALUE) {
            return Byte.MAX_VALUE;
        }
        return (byte) zoom;
    }

    private LatLongUtils() {
        throw new IllegalStateException();
    }
}
