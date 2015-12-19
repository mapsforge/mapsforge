/*
 * Copyright 2010, 2011 mapsforge.org
 * Copyright 2015 devemux86
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
package org.mapsforge.poi.storage;

import org.mapsforge.core.model.LatLong;

/**
 * Implementation for the {@link PointOfInterest} interface.
 */
public class PoiImpl implements PointOfInterest {
	private final long id;
	private final double latitude;
	private final double longitude;
	private final String data;
	private final PoiCategory category;

	public PoiImpl(long id, double latitude, double longitude, String data, PoiCategory category) {
		this.id = id;
		this.latitude = latitude;
		this.longitude = longitude;
		this.data = data;
		this.category = category;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PoiCategory getCategory() {
		return this.category;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getData() {
		return this.data;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long getId() {
		return this.id;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public double getLatitude() {
		return this.latitude;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public LatLong getLatLong() {
		return new LatLong(this.latitude, this.longitude);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public double getLongitude() {
		return this.longitude;
	}

	@Override
	public String toString() {
		return "POI: (" + this.latitude + ',' + this.longitude + ") " + this.data + ' '
				+ this.category.getID();
	}
}
