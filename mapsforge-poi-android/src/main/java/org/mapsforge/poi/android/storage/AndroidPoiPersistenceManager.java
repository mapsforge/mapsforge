/*
 * Copyright 2010, 2011 mapsforge.org
 * Copyright 2010, 2011 Karsten Groll
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
package org.mapsforge.poi.android.storage;

import org.mapsforge.core.model.GeoCoordinate;
import org.mapsforge.poi.storage.PoiCategoryFilter;
import org.mapsforge.poi.storage.PoiCategoryManager;
import org.mapsforge.poi.storage.PoiCategoryRangeQueryGenerator;
import org.mapsforge.poi.storage.PoiImpl;
import org.mapsforge.poi.storage.PoiPersistenceManager;
import org.mapsforge.poi.storage.PointOfInterest;
import org.mapsforge.poi.storage.UnknownPoiCategoryException;
import org.sqlite.android.Database;
import org.sqlite.android.Exception;
import org.sqlite.android.Stmt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A {@link PoiPersistenceManager} implementation using a SQLite database via wrapper.
 * <p/>
 * This class can only be used within Android.
 */
class AndroidPoiPersistenceManager implements PoiPersistenceManager {
	private static final Logger LOGGER = Logger.getLogger(AndroidPoiPersistenceManager.class.getName());

	// Number of tables needed for db verification
	private static final int NUMBER_OF_TABLES = 3;

	private final String dbFilePath;
	private Database db = null;
	private PoiCategoryManager categoryManager = null;

	private Stmt findInBoxStatement = null;
	private Stmt findByIDStatement = null;
	private Stmt insertPoiStatement1 = null;
	private Stmt insertPoiStatement2 = null;
	private Stmt deletePoiStatement1 = null;
	private Stmt deletePoiStatement2 = null;
	private Stmt isValidDBStatement = null;

	// Containers for return values
	private final List<PointOfInterest> ret;
	private PointOfInterest poi;

	/**
	 * @param dbFilePath
	 *            Path to SQLite file containing POI data. If the file does not exist the file and
	 *            its tables will be created.
	 */
	AndroidPoiPersistenceManager(String dbFilePath) {
		this.dbFilePath = dbFilePath;

		// Open / create POI database
		createOrOpenDBFile();

		// Load categories from database
		this.categoryManager = new AndroidPoiCategoryManager(this.db);

		// Queries
		try {
			// Finds POIs by a given bounding box
			this.findInBoxStatement = this.db
					.prepare("SELECT poi_index.id, poi_index.minLat, poi_index.minLon, poi_data.data, poi_data.category "
							+ "FROM poi_index "
							+ "JOIN poi_data ON poi_index.id = poi_data.id "
							+ "WHERE "
							+ "minLat <= ? AND " + "minLon <= ? AND " + "minLat >= ? AND " + "minLon >= ? LIMIT ?");

			// Finds a POI by its unique ID
			this.findByIDStatement = this.db
					.prepare("SELECT poi_index.id, poi_index.minLat, poi_index.minLon, poi_data.data, poi_data.category "
							+ "FROM poi_index "
							+ "JOIN poi_data ON poi_index.id = poi_data.id "
							+ "WHERE "
							+ "poi_index.id = ?;");

			// Inserts a POI into index and adds its data
			this.insertPoiStatement1 = this.db.prepare("INSERT INTO poi_index VALUES (?, ?, ?, ?, ?);");
			this.insertPoiStatement2 = this.db.prepare("INSERT INTO poi_data VALUES (?, ?, ?);");

			// Deletes a POI given by its ID
			this.deletePoiStatement1 = this.db.prepare("DELETE FROM poi_index WHERE id == ?;");
			this.deletePoiStatement2 = this.db.prepare("DELETE FROM poi_data WHERE id == ?;");
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}

		this.ret = new ArrayList<>();
	}

	@Override
	public void close() {
		// Close statements

		if (this.findInBoxStatement != null) {
			try {
				this.findInBoxStatement.close();
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, e.getMessage(), e);
			}
		}

		if (this.findByIDStatement != null) {
			try {
				this.findByIDStatement.close();
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, e.getMessage(), e);
			}
		}

		if (this.insertPoiStatement1 != null) {
			try {
				this.insertPoiStatement1.close();
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, e.getMessage(), e);
			}
		}

		if (this.insertPoiStatement2 != null) {
			try {
				this.insertPoiStatement2.close();
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, e.getMessage(), e);
			}
		}

		if (this.deletePoiStatement1 != null) {
			try {
				this.deletePoiStatement1.close();
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, e.getMessage(), e);
			}
		}

		if (this.deletePoiStatement2 != null) {
			try {
				this.deletePoiStatement2.close();
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, e.getMessage(), e);
			}
		}

		if (this.isValidDBStatement != null) {
			try {
				this.isValidDBStatement.close();
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, e.getMessage(), e);
			}
		}

		// Close connection
		if (this.db != null) {
			try {
				this.db.close();
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, e.getMessage(), e);
			}
		}
	}

	/**
	 * If the file does not exist it will be created and filled.
	 */
	private void createOrOpenDBFile() {
		// FIXME This method causes a crash on native level if the file cannot be created.
		// (Use Java methods to avoid this).

		// Create or open File
		this.db = new Database();
		try {
			this.db.open(this.dbFilePath, 0666);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}

		if (!isValidDataBase()) {
			try {
				createTables();
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, e.getMessage(), e);
			}
		}
	}

	private void createTables() throws Exception {
		// db.open() created a new file, so let's create its tables
		this.db.exec("DROP TABLE IF EXISTS poi_index;", null);
		this.db.exec("DROP TABLE IF EXISTS poi_data;", null);
		this.db.exec("DROP TABLE IF EXISTS poi_categories;", null);

		this.db.exec("CREATE TABLE poi_categories (id INTEGER, name VARCHAR, parent INTEGER, PRIMARY KEY (id));", null);
		this.db.exec("CREATE TABLE poi_data (id LONG, data BLOB, category INT, PRIMARY KEY (id));", null);
		this.db.exec("CREATE VIRTUAL TABLE poi_index USING rtree(id, minLat, maxLat, minLon, maxLon);", null);
	}

	@Override
	public Collection<PointOfInterest> findInRect(GeoCoordinate p1, GeoCoordinate p2, int limit) {
		// Clear previous results
		this.ret.clear();

		// Query
		try {
			this.findInBoxStatement.reset();
			this.findInBoxStatement.clear_bindings();

			this.findInBoxStatement.bind(1, p2.getLatitude());
			this.findInBoxStatement.bind(2, p2.getLongitude());
			this.findInBoxStatement.bind(3, p1.getLatitude());
			this.findInBoxStatement.bind(4, p1.getLongitude());
			this.findInBoxStatement.bind(5, limit);

			while (this.findInBoxStatement.step()) {
				long id = this.findInBoxStatement.column_long(0);
				double lat = this.findInBoxStatement.column_double(1);
				double lon = this.findInBoxStatement.column_double(2);
				String data = this.findInBoxStatement.column_string(3);
				int categoryID = this.findInBoxStatement.column_int(4);

				try {
					this.poi = new PoiImpl(id, lat, lon, data, this.categoryManager.getPoiCategoryByID(categoryID));
					this.ret.add(this.poi);
				} catch (UnknownPoiCategoryException e) {
					LOGGER.log(Level.SEVERE, e.getMessage(), e);
				}
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}

		return this.ret;
	}

	@Override
	public Collection<PointOfInterest> findInRectWithFilter(GeoCoordinate p1, GeoCoordinate p2,
															PoiCategoryFilter filter, int limit) {
		// Clear previous results
		this.ret.clear();

		// Query
		try {
			Stmt stmt = this.db.prepare(PoiCategoryRangeQueryGenerator.getSQLSelectString(filter));

			stmt.reset();
			stmt.clear_bindings();

			stmt.bind(1, p2.getLatitude());
			stmt.bind(2, p2.getLongitude());
			stmt.bind(3, p1.getLatitude());
			stmt.bind(4, p1.getLongitude());
			stmt.bind(5, limit);

			while (stmt.step()) {
				long id = stmt.column_long(0);
				double lat = stmt.column_double(1);
				double lon = stmt.column_double(2);
				String data = stmt.column_string(3);
				int categoryID = stmt.column_int(4);

				try {
					this.poi = new PoiImpl(id, lat, lon, data, this.categoryManager.getPoiCategoryByID(categoryID));
					this.ret.add(this.poi);
				} catch (UnknownPoiCategoryException e) {
					LOGGER.log(Level.SEVERE, e.getMessage(), e);
				}
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}

		return this.ret;
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
																  PoiCategoryFilter categoryFilter,
																  int limit) {
		double minLat = point.getLatitude() - GeoCoordinate.latitudeDistance(distance);
		double minLon = point.getLongitude() - GeoCoordinate.longitudeDistance(distance, point.getLatitude());
		double maxLat = point.getLatitude() + GeoCoordinate.latitudeDistance(distance);
		double maxLon = point.getLongitude() + GeoCoordinate.longitudeDistance(distance, point.getLatitude());

		return findInRectWithFilter(new GeoCoordinate(minLat, minLon), new GeoCoordinate(maxLat, maxLon), categoryFilter, limit);
	}

	@Override
	public PointOfInterest findPointByID(long poiID) {
		// Clear previous results
		this.poi = null;

		// Query
		try {
			this.findByIDStatement.reset();
			this.findByIDStatement.clear_bindings();

			this.findByIDStatement.bind(1, poiID);

			if (this.findByIDStatement.step()) {
				long id = this.findByIDStatement.column_long(0);
				double lat = this.findByIDStatement.column_double(1);
				double lon = this.findByIDStatement.column_double(2);
				String data = this.findByIDStatement.column_string(3);
				int categoryID = this.findByIDStatement.column_int(4);

				try {
					this.poi = new PoiImpl(id, lat, lon, data, this.categoryManager.getPoiCategoryByID(categoryID));
				} catch (UnknownPoiCategoryException e) {
					LOGGER.log(Level.SEVERE, e.getMessage(), e);
				}
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}

		return this.poi;
	}

	@Override
	public PoiCategoryManager getCategoryManager() {
		return this.categoryManager;
	}

	@Override
	public void insertPointOfInterest(PointOfInterest p) {
		try {
			this.insertPoiStatement1.reset();
			this.insertPoiStatement1.clear_bindings();
			this.insertPoiStatement2.reset();
			this.insertPoiStatement2.clear_bindings();

			this.db.exec("BEGIN;", null);
			this.insertPoiStatement1.bind(1, p.getId());
			this.insertPoiStatement1.bind(2, p.getLatitude());
			this.insertPoiStatement1.bind(3, p.getLatitude());
			this.insertPoiStatement1.bind(4, p.getLongitude());
			this.insertPoiStatement1.bind(5, p.getLongitude());

			this.insertPoiStatement2.bind(1, p.getId());
			this.insertPoiStatement2.bind(2, p.getData());
			this.insertPoiStatement2.bind(3, p.getCategory().getID());

			this.insertPoiStatement1.step();
			this.insertPoiStatement2.step();

			this.db.exec("COMMIT", null);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	@Override
	public void insertPointsOfInterest(Collection<PointOfInterest> pois) {
		try {
			this.insertPoiStatement1.reset();
			this.insertPoiStatement1.clear_bindings();
			this.insertPoiStatement2.reset();
			this.insertPoiStatement2.clear_bindings();

			this.db.exec("BEGIN;", null);
			for (PointOfInterest p : pois) {
				this.insertPoiStatement1.bind(1, p.getId());
				this.insertPoiStatement1.bind(2, p.getLatitude());
				this.insertPoiStatement1.bind(3, p.getLatitude());
				this.insertPoiStatement1.bind(4, p.getLongitude());
				this.insertPoiStatement1.bind(5, p.getLongitude());

				this.insertPoiStatement2.bind(1, p.getId());
				this.insertPoiStatement2.bind(2, p.getData());
				this.insertPoiStatement2.bind(3, p.getCategory().getID());

				this.insertPoiStatement1.step();
				this.insertPoiStatement2.step();
			}

			this.db.exec("COMMIT", null);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	/**
	 * @return True if the database is a valid POI database.
	 */
	private boolean isValidDataBase() {
		try {
			this.isValidDBStatement = this.db.prepare("SELECT count(name) " + "FROM sqlite_master "
					+ "WHERE name IN " + "('poi_index', 'poi_data', 'poi_categories');");
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}

		// Check for table names
		// TODO Is it necessary to get the tables meta data as well?
		int numTables = 0;
		try {
			if (this.isValidDBStatement.step()) {
				numTables = this.isValidDBStatement.column_int(0);
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}

		return numTables == NUMBER_OF_TABLES;
	}

	@Override
	public void removePointOfInterest(PointOfInterest aPoi) {
		try {
			this.deletePoiStatement1.reset();
			this.deletePoiStatement1.clear_bindings();
			this.deletePoiStatement2.reset();
			this.deletePoiStatement2.clear_bindings();

			this.db.exec("BEGIN", null);

			this.deletePoiStatement1.bind(1, aPoi.getId());
			this.deletePoiStatement2.bind(1, aPoi.getId());

			this.deletePoiStatement1.step();
			this.deletePoiStatement2.step();

			this.db.exec("COMMIT", null);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	@Override
	public void setCategoryManager(PoiCategoryManager categoryManager) {
		this.categoryManager = categoryManager;
	}
}
