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
package org.mapsforge.android.maps;

import org.mapsforge.core.model.GeoPoint;

import android.graphics.Point;

/**
 * A Projection translates between the pixel coordinate system on the screen and geographical points on the earth. To
 * retrieve the currently used Projection for a given MapView, call the {@link MapView#getProjection()} method.
 */
public interface Projection {
	/**
	 * Translates the given screen coordinates to a {@link GeoPoint}. If the corresponding MapView has no valid
	 * dimensions (width and height > 0), null is returned.
	 * 
	 * @param x
	 *            the pixel x coordinate on the screen.
	 * @param y
	 *            the pixel y coordinate on the screen.
	 * @return a new {@link GeoPoint} or null, if the corresponding MapView has no valid dimensions.
	 */
	GeoPoint fromPixels(int x, int y);

	/**
	 * @return the latitude span from the top to the bottom of the map in degrees.
	 * @throws IllegalStateException
	 *             if the MapView dimensions are not valid (width and height > 0).
	 */
	double getLatitudeSpan();

	/**
	 * @return the longitude span from the left to the right of the map in degrees.
	 * @throws IllegalStateException
	 *             if the MapView dimensions are not valid (width and height > 0).
	 */
	double getLongitudeSpan();

	/**
	 * Converts the given distance in meters at the given zoom level to the corresponding number of horizontal pixels.
	 * The calculation is carried out at the current latitude coordinate.
	 * 
	 * @param meters
	 *            the distance in meters.
	 * @param zoomLevel
	 *            the zoom level at which the distance should be calculated.
	 * @return the number of pixels at the current map position and the given zoom level.
	 */
	float metersToPixels(float meters, byte zoomLevel);

	/**
	 * Translates the given {@link GeoPoint} to relative pixel coordinates on the screen. If the corresponding MapView
	 * has no valid dimensions (width and height > 0), null is returned.
	 * 
	 * @param in
	 *            the geographical point to convert.
	 * @param out
	 *            an already existing object to use for the output. If this parameter is null, a new Point object will
	 *            be created and returned.
	 * @return a Point which is relative to the top-left of the MapView or null, if the corresponding MapView has no
	 *         valid dimensions.
	 */
	Point toPixels(GeoPoint in, Point out);

	/**
	 * Translates the given {@link GeoPoint} to absolute pixel coordinates on the world map.
	 * 
	 * @param in
	 *            the geographical point to convert.
	 * @param out
	 *            an already existing object to use for the output. If this parameter is null, a new Point object will
	 *            be created and returned.
	 * @param zoomLevel
	 *            the zoom level at which the point should be converted.
	 * @return a Point which is relative to the top-left of the world map.
	 */
	Point toPoint(GeoPoint in, Point out, byte zoomLevel);
}
