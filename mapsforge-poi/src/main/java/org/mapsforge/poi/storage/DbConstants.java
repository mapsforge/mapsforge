/*
 * Copyright 2015-2017 devemux86
 * Copyright 2017 Gustl22
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
    public static final String CREATE_DATA_STATEMENT = "CREATE TABLE poi_data (id INTEGER, data TEXT, PRIMARY KEY (id));";
    public static final String CREATE_CATEGORYMAP_STATEMENT =
            "CREATE TABLE poi_cmap (id INTEGER, category INTEGER not null, PRIMARY KEY (id, category)); ";
    public static final String CREATE_INDEX_STATEMENT = "CREATE VIRTUAL TABLE poi_index USING rtree(id, minLat, maxLat, minLon, maxLon);";
    public static final String CREATE_METADATA_STATEMENT = "CREATE TABLE metadata (name TEXT, value TEXT);";
    public static final String CREATE_NODES_STATEMENT = "CREATE TABLE nodes (id INTEGER, lat REAL, lon REAL, PRIMARY KEY (id));";
    public static final String CREATE_WAYNODES_STATEMENT = "CREATE TABLE waynodes (way INTEGER not null, node INTEGER not null, position INTEGER, PRIMARY KEY (way, node, position));";

    public static final String DELETE_DATA_STATEMENT = "DELETE FROM poi_data WHERE id = ?;";
    public static final String DELETE_INDEX_STATEMENT = "DELETE FROM poi_index WHERE id = ?;";
    public static final String DELETE_CATEGORYMAP_STATEMENT = "DELETE FROM poi_cmap WHERE id = ?;";

    public static final String DROP_CATEGORIES_STATEMENT = "DROP TABLE IF EXISTS poi_categories;";
    public static final String DROP_DATA_STATEMENT = "DROP TABLE IF EXISTS poi_data;";
    public static final String DROP_INDEX_STATEMENT = "DROP TABLE IF EXISTS poi_index;";
    public static final String DROP_CATEGORYMAP_STATEMENT = "DROP TABLE IF EXISTS poi_cmap;";
    public static final String DROP_METADATA_STATEMENT = "DROP TABLE IF EXISTS metadata;";
    public static final String DROP_NODES_STATEMENT = "DROP TABLE IF EXISTS nodes;";
    public static final String DROP_WAYNODES_STATEMENT = "DROP TABLE IF EXISTS waynodes;";

    public static final String FIND_BY_DATA_CLAUSE = " AND poi_data.data LIKE ?";
    public static final String JOIN_DATA_CLAUSE = "JOIN poi_data ON poi_index.id = poi_data.id ";
    public static final String JOIN_CATEGORY_CLAUSE = "JOIN poi_cmap ON poi_index.id = poi_cmap.id ";

    public static final String FIND_LOCATION_BY_ID_STATEMENT =
            "SELECT poi_index.id, poi_index.minLat, poi_index.minLon "
                    + "FROM poi_index "
                    + "WHERE poi_index.id = ?;";
    public static final String FIND_CATEGORIES_BY_ID_STATEMENT =
            "SELECT poi_cmap.id, poi_cmap.category "
                    + "FROM poi_cmap "
                    + "WHERE poi_cmap.id = ?;";
    public static final String FIND_DATA_BY_ID_STATEMENT =
            "SELECT poi_data.id, poi_data.data "
                    + "FROM poi_data "
                    + "WHERE poi_data.id = ?;";

    public static final String FIND_IN_BOX_CLAUSE_SELECT =
            "SELECT poi_index.id, poi_index.minLat, poi_index.minLon "
                    + "FROM poi_index ";
    public static final String FIND_IN_BOX_CLAUSE_WHERE =
            "WHERE "
                    + "minLat <= ? AND "
                    + "minLon <= ? AND "
                    + "minLat >= ? AND "
                    + "minLon >= ?";
    public static final String FIND_IN_BOX_CLAUSE_WHERE_CATEGORY_IN = "poi_cmap.category IN (";
    public static final String FIND_IN_BOX_STATEMENT = FIND_IN_BOX_CLAUSE_SELECT + FIND_IN_BOX_CLAUSE_WHERE;

    public static final String FIND_METADATA_STATEMENT = "SELECT name, value FROM metadata;";
    public static final String FIND_NODES_STATEMENT = "SELECT lat, lon FROM nodes WHERE id = ?;";
    public static final String FIND_WAYNODES_BY_ID_STATEMENT = "SELECT node, position FROM waynodes WHERE way = ?;";

    public static final String INSERT_CATEGORIES_STATEMENT = "INSERT INTO poi_categories VALUES (?, ?, ?);";
    public static final String INSERT_DATA_STATEMENT = "INSERT INTO poi_data VALUES (?, ?);";
    public static final String INSERT_CATEGORYMAP_STATEMENT = "INSERT INTO poi_cmap VALUES (?, ?);";
    public static final String INSERT_INDEX_STATEMENT = "INSERT INTO poi_index VALUES (?, ?, ?, ?, ?);";
    public static final String INSERT_METADATA_STATEMENT = "INSERT INTO metadata VALUES (?, ?);";
    public static final String INSERT_NODES_STATEMENT = "INSERT INTO nodes VALUES (?, ?, ?);";
    public static final String INSERT_WAYNODES_STATEMENT = "INSERT INTO waynodes VALUES (?, ?, ?);";

    public static final String UPDATE_DATA_STATEMENT = "UPDATE poi_data SET data = ? WHERE id = ?;";

    public static final String METADATA_BOUNDS = "bounds";
    public static final String METADATA_COMMENT = "comment";
    public static final String METADATA_DATE = "date";
    public static final String METADATA_LANGUAGE = "language";
    public static final String METADATA_VERSION = "version";
    public static final String METADATA_WAYS = "ways";
    public static final String METADATA_WRITER = "writer";

    // Number of tables needed for DB verification
    public static final int NUMBER_OF_TABLES = 5;

    public static final String VALID_DB_STATEMENT = "SELECT count(name) "
            + "FROM sqlite_master "
            + "WHERE name IN "
            + "('metadata', 'poi_categories', 'poi_data', 'poi_index', 'poi_cmap');";

    // V1 Statements
    public static final String FIND_CATEGORIES_BY_ID_STATEMENT_V1 =
            "SELECT poi_data.id, poi_data.category "
                    + "FROM poi_data "
                    + "WHERE poi_data.id = ?;";

    public static final int NUMBER_OF_TABLES_V1 = 4;

    public static final String VALID_DB_STATEMENT_V1 = "SELECT count(name) "
            + "FROM sqlite_master "
            + "WHERE name IN "
            + "('metadata', 'poi_categories', 'poi_data', 'poi_index', 'poi_cmap');";

    public static final String FIND_IN_BOX_CLAUSE_WHERE_CATEGORY_IN_V1 = "poi_data.category IN (";
    // V1 Statements end


    private DbConstants() {
        throw new IllegalStateException();
    }
}
