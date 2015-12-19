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

/**
 * Defines DB constants.
 */
public final class DbConstants {
	public static final String CREATE_CATEGORIES_STATEMENT = "CREATE TABLE poi_categories (id INTEGER, name TEXT, parent INTEGER, PRIMARY KEY (id));";
	public static final String CREATE_DATA_STATEMENT = "CREATE TABLE poi_data (id INTEGER, data TEXT, category INTEGER, PRIMARY KEY (id));";
	public static final String CREATE_INDEX_STATEMENT = "CREATE VIRTUAL TABLE poi_index USING rtree(id, minLat, maxLat, minLon, maxLon);";
	public static final String CREATE_METADATA_STATEMENT = "CREATE TABLE metadata (name TEXT, value TEXT);";

	public static final String DELETE_DATA_STATEMENT = "DELETE FROM poi_data WHERE id = ?;";
	public static final String DELETE_INDEX_STATEMENT = "DELETE FROM poi_index WHERE id = ?;";

	public static final String DROP_CATEGORIES_STATEMENT = "DROP TABLE IF EXISTS poi_categories;";
	public static final String DROP_DATA_STATEMENT = "DROP TABLE IF EXISTS poi_data;";
	public static final String DROP_INDEX_STATEMENT = "DROP TABLE IF EXISTS poi_index;";
	public static final String DROP_METADATA_STATEMENT = "DROP TABLE IF EXISTS metadata;";

	public static final String FIND_BY_ID_STATEMENT =
			"SELECT poi_index.id, poi_index.minLat, poi_index.minLon, poi_data.data, poi_data.category "
					+ "FROM poi_index "
					+ "JOIN poi_data ON poi_index.id = poi_data.id "
					+ "WHERE "
					+ "poi_index.id = ?;";
	public static final String FIND_BY_NAME_CLAUSE = " AND poi_data.data LIKE ?";
	public static final String FIND_IN_BOX_STATEMENT =
			"SELECT poi_index.id, poi_index.minLat, poi_index.minLon, poi_data.data, poi_data.category "
					+ "FROM poi_index "
					+ "JOIN poi_data ON poi_index.id = poi_data.id "
					+ "WHERE "
					+ "minLat <= ? AND "
					+ "minLon <= ? AND "
					+ "minLat >= ? AND "
					+ "minLon >= ?";
	public static final String FIND_METADATA_STATEMENT = "SELECT name, value FROM metadata;";

	public static final String INSERT_CATEGORIES_STATEMENT = "INSERT INTO poi_categories VALUES (?, ?, ?);";
	public static final String INSERT_DATA_STATEMENT = "INSERT INTO poi_data VALUES (?, ?, ?);";
	public static final String INSERT_INDEX_STATEMENT = "INSERT INTO poi_index VALUES (?, ?, ?, ?, ?);";
	public static final String INSERT_METADATA_STATEMENT = "INSERT INTO metadata VALUES (?, ?);";

	public static final String METADATA_BOUNDS = "bounds";
	public static final String METADATA_COMMENT = "comment";
	public static final String METADATA_DATE = "date";
	public static final String METADATA_LANGUAGE = "language";
	public static final String METADATA_VERSION = "version";
	public static final String METADATA_WRITER = "writer";

	// Number of tables needed for DB verification
	public static final int NUMBER_OF_TABLES = 4;

	public static final String VALID_DB_STATEMENT = "SELECT count(name) "
			+ "FROM sqlite_master "
			+ "WHERE name IN "
			+ "('metadata', 'poi_categories', 'poi_data', 'poi_index');";

	private DbConstants() {
		throw new IllegalStateException();
	}
}
