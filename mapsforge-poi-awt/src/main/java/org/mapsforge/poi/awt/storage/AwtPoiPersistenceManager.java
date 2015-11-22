/*
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
package org.mapsforge.poi.awt.storage;

import org.mapsforge.core.model.GeoCoordinate;
import org.mapsforge.poi.storage.PoiCategoryFilter;
import org.mapsforge.poi.storage.PoiCategoryManager;
import org.mapsforge.poi.storage.PoiPersistenceManager;
import org.mapsforge.poi.storage.PointOfInterest;

import java.util.Collection;

/**
 * A {@link PoiPersistenceManager} implementation using a SQLite database via JDBC.
 * <p/>
 * This class can only be used within AWT.
 */
class AwtPoiPersistenceManager implements PoiPersistenceManager {
	private final String dbFilePath;

	/**
	 * @param dbFilePath
	 *            Path to SQLite file containing POI data. If the file does not exist the file and
	 *            its tables will be created.
	 */
	AwtPoiPersistenceManager(String dbFilePath) {
		this.dbFilePath = dbFilePath;
	}

	@Override
	public void close() {
	}

	@Override
	public Collection<PointOfInterest> findInRect(GeoCoordinate p1, GeoCoordinate p2, int limit) {
		return null;
	}

	@Override
	public Collection<PointOfInterest> findInRect(GeoCoordinate p1, GeoCoordinate p2, String categoryName, int limit) {
		return null;
	}

	@Override
	public Collection<PointOfInterest> findInRectWithFilter(GeoCoordinate p1, GeoCoordinate p2, PoiCategoryFilter categoryFilter, int limit) {
		return null;
	}

	@Override
	public Collection<PointOfInterest> findNearPosition(GeoCoordinate point, int distance, int limit) {
		return null;
	}

	@Override
	public Collection<PointOfInterest> findNearPosition(GeoCoordinate point, int distance, String categoryName, int limit) {
		return null;
	}

	@Override
	public Collection<PointOfInterest> findNearPositionWithFilter(GeoCoordinate point, int distance, PoiCategoryFilter categoryFilter, int limit) {
		return null;
	}

	@Override
	public PointOfInterest findPointByID(long poiID) {
		return null;
	}

	@Override
	public PoiCategoryManager getCategoryManager() {
		return null;
	}

	@Override
	public void insertPointOfInterest(PointOfInterest poi) {
	}

	@Override
	public void insertPointsOfInterest(Collection<PointOfInterest> pois) {
	}

	@Override
	public void removePointOfInterest(PointOfInterest poi) {
	}

	@Override
	public void setCategoryManager(PoiCategoryManager categoryManager) {
	}
}
