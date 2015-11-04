/*
 * Copyright 2010, 2011 mapsforge.org
 * Copyright 2010, 2011 weise
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
package org.mapsforge.storage.poi;

import org.mapsforge.core.GeoCoordinate;

/**
 * This class represents a point of interest. Every POI should be uniquely identifiable by its id so that for two POIs a
 * and b a.equals(b) if and only if a.id == b.id.
 */
public interface PointOfInterest {
	/**
	 * @return id of this point of interest.
	 */
	long getId();

	/**
	 * @return latitude of this point of interest.
	 */
	double getLatitude();

	/**
	 * @return longitude of this point of interest
	 */
	double getLongitude();

	/**
	 * @return binary data of this point of interest.
	 */
	String getData();

	/**
	 * @return url of this point of interest.
	 */
	String getUrl();

	/**
	 * @return category of this point of interest.
	 */
	PoiCategory getCategory();

	/**
	 * @return {@link GeoCoordinate} of this point of interest.
	 */
	GeoCoordinate getGeoCoordinate();
}
