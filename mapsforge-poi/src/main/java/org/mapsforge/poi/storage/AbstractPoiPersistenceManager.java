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
package org.mapsforge.poi.storage;

import org.mapsforge.core.model.GeoCoordinate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Abstract implementation for the {@link PoiPersistenceManager} interface. This implementation
 * provides functionality for accessing the SQLite database.
 */
public abstract class AbstractPoiPersistenceManager implements PoiPersistenceManager {
	protected static final String CREATE_CATEGORIES_STATEMENT = "CREATE TABLE poi_categories (id INTEGER, name VARCHAR, parent INTEGER, PRIMARY KEY (id));";
	protected static final String CREATE_DATA_STATEMENT = "CREATE TABLE poi_data (id LONG, data BLOB, category INT, PRIMARY KEY (id));";
	protected static final String CREATE_INDEX_STATEMENT = "CREATE VIRTUAL TABLE poi_index USING rtree(id, minLat, maxLat, minLon, maxLon);";

	protected static final String DELETE_DATA_STATEMENT = "DELETE FROM poi_data WHERE id == ?;";
	protected static final String DELETE_INDEX_STATEMENT = "DELETE FROM poi_index WHERE id == ?;";

	protected static final String DROP_CATEGORIES_STATEMENT = "DROP TABLE IF EXISTS poi_categories;";
	protected static final String DROP_DATA_STATEMENT = "DROP TABLE IF EXISTS poi_data;";
	protected static final String DROP_INDEX_STATEMENT = "DROP TABLE IF EXISTS poi_index;";

	protected static final String FIND_BY_ID_STATEMENT =
			"SELECT poi_index.id, poi_index.minLat, poi_index.minLon, poi_data.data, poi_data.category "
					+ "FROM poi_index "
					+ "JOIN poi_data ON poi_index.id = poi_data.id "
					+ "WHERE "
					+ "poi_index.id = ?;";
	protected static final String FIND_IN_BOX_STATEMENT =
			"SELECT poi_index.id, poi_index.minLat, poi_index.minLon, poi_data.data, poi_data.category "
					+ "FROM poi_index "
					+ "JOIN poi_data ON poi_index.id = poi_data.id "
					+ "WHERE "
					+ "minLat <= ? AND "
					+ "minLon <= ? AND "
					+ "minLat >= ? AND "
					+ "minLon >= ? LIMIT ?";

	protected static final String INSERT_INDEX_STATEMENT = "INSERT INTO poi_index VALUES (?, ?, ?, ?, ?);";
	protected static final String INSERT_DATA_STATEMENT = "INSERT INTO poi_data VALUES (?, ?, ?);";

	protected static final String VALID_DB_STATEMENT = "SELECT count(name) "
			+ "FROM sqlite_master "
			+ "WHERE name IN "
			+ "('poi_index', 'poi_data', 'poi_categories');";

	// Number of tables needed for db verification
	protected static final int NUMBER_OF_TABLES = 3;

	protected final String dbFilePath;
	protected PoiCategoryManager categoryManager = null;

	// Containers for return values
	protected final List<PointOfInterest> ret;
	protected PointOfInterest poi = null;

	/**
	 * @param dbFilePath
	 *            Path to SQLite file containing POI data. If the file does not exist the file and
	 *            its tables will be created.
	 */
	protected AbstractPoiPersistenceManager(String dbFilePath) {
		this.dbFilePath = dbFilePath;

		this.ret = new ArrayList<>();
	}

	@Override
	public Collection<PointOfInterest> findNearPosition(GeoCoordinate point, int distance, int limit) {
		double minLat = point.getLatitude() - GeoCoordinate.latitudeDistance(distance);
		double minLon = point.getLongitude() - GeoCoordinate.longitudeDistance(distance, point.getLatitude());
		double maxLat = point.getLatitude() + GeoCoordinate.latitudeDistance(distance);
		double maxLon = point.getLongitude() + GeoCoordinate.longitudeDistance(distance, point.getLatitude());

		return findInRect(new GeoCoordinate(minLat, minLon), new GeoCoordinate(maxLat, maxLon), limit);
	}

	@Override
	public Collection<PointOfInterest> findNearPositionWithFilter(GeoCoordinate point, int distance,
																  PoiCategoryFilter filter, int limit) {
		double minLat = point.getLatitude() - GeoCoordinate.latitudeDistance(distance);
		double minLon = point.getLongitude() - GeoCoordinate.longitudeDistance(distance, point.getLatitude());
		double maxLat = point.getLatitude() + GeoCoordinate.latitudeDistance(distance);
		double maxLon = point.getLongitude() + GeoCoordinate.longitudeDistance(distance, point.getLatitude());

		return findInRectWithFilter(new GeoCoordinate(minLat, minLon), new GeoCoordinate(maxLat, maxLon), filter, limit);
	}

	@Override
	public PoiCategoryManager getCategoryManager() {
		return this.categoryManager;
	}

	@Override
	public void setCategoryManager(PoiCategoryManager categoryManager) {
		this.categoryManager = categoryManager;
	}
}
