/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright © 2014 Ludwig M Brinckmann
 * Copyright © 2014 Christian Pesch
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

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Dimension;
import org.mapsforge.core.model.LatLong;

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
	 * Converts a coordinate from degrees to microdegrees (degrees * 10^6). No validation is performed.
	 * 
	 * @param coordinate
	 *            the coordinate in degrees.
	 * @return the coordinate in microdegrees (degrees * 10^6).
	 */
	public static int degreesToMicrodegrees(double coordinate) {
		return (int) (coordinate * CONVERSION_FACTOR);
	}

	/**
	 * Creates a new LatLong from a comma-separated string of coordinates in the order latitude, longitude. All
	 * coordinate values must be in degrees.
	 * 
	 * @param latLongString
	 *            the string that describes the LatLong.
	 * @return a new LatLong with the given coordinates.
	 * @throws IllegalArgumentException
	 *             if the string cannot be parsed or describes an invalid LatLong.
	 */
	public static LatLong fromString(String latLongString) {
		double[] coordinates = parseCoordinateString(latLongString, 2);
		return new LatLong(coordinates[0], coordinates[1]);
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
	 * Converts a coordinate from microdegrees (degrees * 10^6) to degrees. No validation is performed.
	 * 
	 * @param coordinate
	 *            the coordinate in microdegrees (degrees * 10^6).
	 * @return the coordinate in degrees.
	 */
	public static double microdegreesToDegrees(int coordinate) {
		return coordinate / CONVERSION_FACTOR;
	}

	/**
	 * Parses a given number of comma-separated coordinate values from the supplied string.
	 * 
	 * @param coordinatesString
	 *            a comma-separated string of coordinate values.
	 * @param numberOfCoordinates
	 *            the expected number of coordinate values in the string.
	 * @return the coordinate values in the order they have been parsed from the string.
	 * @throws IllegalArgumentException
	 *             if the string is invalid or does not contain the given number of coordinate values.
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
	 * @param latitude
	 *            the latitude coordinate in degrees which should be validated.
	 * @throws IllegalArgumentException
	 *             if the latitude coordinate is invalid or {@link Double#NaN}.
	 */
	public static void validateLatitude(double latitude) {
		if (Double.isNaN(latitude) || latitude < LATITUDE_MIN || latitude > LATITUDE_MAX) {
			throw new IllegalArgumentException("invalid latitude: " + latitude);
		}
	}

	/**
	 * @param longitude
	 *            the longitude coordinate in degrees which should be validated.
	 * @throws IllegalArgumentException
	 *             if the longitude coordinate is invalid or {@link Double#NaN}.
	 */
	public static void validateLongitude(double longitude) {
		if (Double.isNaN(longitude) || longitude < LONGITUDE_MIN || longitude > LONGITUDE_MAX) {
			throw new IllegalArgumentException("invalid longitude: " + longitude);
		}
	}

	/**
	 * Calculates the zoom level that allows to display the {@link BoundingBox} on a view with the {@link Dimension} and
	 * tile size.
	 * 
	 * @param dimension
	 *            the {@link Dimension} of the view.
	 * @param boundingBox
	 *            the {@link BoundingBox} to display.
	 * @param tileSize
	 *            the size of the tiles.
	 * @return the zoom level that allows to display the {@link BoundingBox} on a view with the {@link Dimension} and
	 *         tile size.
	 */
	public static byte zoomForBounds(Dimension dimension, BoundingBox boundingBox, int tileSize) {
		double dxMax = MercatorProjection.longitudeToPixelX(boundingBox.maxLongitude, (byte) 0, tileSize) / tileSize;
		double dxMin = MercatorProjection.longitudeToPixelX(boundingBox.minLongitude, (byte) 0, tileSize) / tileSize;
		double zoomX = Math.floor(-Math.log(3.8) * Math.log(Math.abs(dxMax - dxMin)) + (float) dimension.width
				/ tileSize);
		double dyMax = MercatorProjection.latitudeToPixelY(boundingBox.maxLatitude, (byte) 0, tileSize) / tileSize;
		double dyMin = MercatorProjection.latitudeToPixelY(boundingBox.minLatitude, (byte) 0, tileSize) / tileSize;
		double zoomY = Math.floor(-Math.log(3.8) * Math.log(Math.abs(dyMax - dyMin)) + (float) dimension.height
				/ tileSize);
		return (byte) Math.min(zoomX, zoomY);
	}

	private LatLongUtils() {
		throw new IllegalStateException();
	}
}
