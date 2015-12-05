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

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.poi.storage.AbstractPoiPersistenceManager;
import org.mapsforge.poi.storage.PoiCategoryFilter;
import org.mapsforge.poi.storage.PoiCategoryRangeQueryGenerator;
import org.mapsforge.poi.storage.PoiImpl;
import org.mapsforge.poi.storage.PoiPersistenceManager;
import org.mapsforge.poi.storage.PointOfInterest;
import org.mapsforge.poi.storage.UnknownPoiCategoryException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A {@link PoiPersistenceManager} implementation using a SQLite database via JDBC.
 * <p/>
 * This class can only be used within AWT.
 */
class AwtPoiPersistenceManager extends AbstractPoiPersistenceManager {
	private static final Logger LOGGER = Logger.getLogger(AwtPoiPersistenceManager.class.getName());

	private Connection conn = null;

	private PreparedStatement findInBoxStatement = null;
	private PreparedStatement findByIDStatement = null;
	private PreparedStatement findByNameStatement = null;
	private PreparedStatement insertPoiStatement1 = null;
	private PreparedStatement insertPoiStatement2 = null;
	private PreparedStatement deletePoiStatement1 = null;
	private PreparedStatement deletePoiStatement2 = null;
	private PreparedStatement isValidDBStatement = null;

	/**
	 * @param dbFilePath
	 *            Path to SQLite file containing POI data. If the file does not exist the file and
	 *            its tables will be created.
	 */
	AwtPoiPersistenceManager(String dbFilePath) {
		super(dbFilePath);

		// Open / create POI database
		createOrOpenDBFile();

		// Load categories from database
		this.categoryManager = new AwtPoiCategoryManager(this.conn);

		// Queries
		try {
			// Finds POIs by a given bounding box
			this.findInBoxStatement = this.conn.prepareStatement(FIND_IN_BOX_STATEMENT);

			// Finds a POI by its unique ID
			this.findByIDStatement = this.conn.prepareStatement(FIND_BY_ID_STATEMENT);

			// Finds POIs by name
			this.findByNameStatement = this.conn.prepareStatement(FIND_BY_NAME_STATEMENT);

			// Inserts a POI into index and adds its data
			this.insertPoiStatement1 = this.conn.prepareStatement(INSERT_INDEX_STATEMENT);
			this.insertPoiStatement2 = this.conn.prepareStatement(INSERT_DATA_STATEMENT);

			// Deletes a POI given by its ID
			this.deletePoiStatement1 = this.conn.prepareStatement(DELETE_INDEX_STATEMENT);
			this.deletePoiStatement2 = this.conn.prepareStatement(DELETE_DATA_STATEMENT);
		} catch (SQLException e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	@Override
	public void close() {
		// Close statements

		if (this.findInBoxStatement != null) {
			try {
				this.findInBoxStatement.close();
			} catch (SQLException e) {
				LOGGER.log(Level.SEVERE, e.getMessage(), e);
			}
		}

		if (this.findByIDStatement != null) {
			try {
				this.findByIDStatement.close();
			} catch (SQLException e) {
				LOGGER.log(Level.SEVERE, e.getMessage(), e);
			}
		}

		if (this.findByNameStatement != null) {
			try {
				this.findByNameStatement.close();
			} catch (SQLException e) {
				LOGGER.log(Level.SEVERE, e.getMessage(), e);
			}
		}

		if (this.insertPoiStatement1 != null) {
			try {
				this.insertPoiStatement1.close();
			} catch (SQLException e) {
				LOGGER.log(Level.SEVERE, e.getMessage(), e);
			}
		}

		if (this.insertPoiStatement2 != null) {
			try {
				this.insertPoiStatement2.close();
			} catch (SQLException e) {
				LOGGER.log(Level.SEVERE, e.getMessage(), e);
			}
		}

		if (this.deletePoiStatement1 != null) {
			try {
				this.deletePoiStatement1.close();
			} catch (SQLException e) {
				LOGGER.log(Level.SEVERE, e.getMessage(), e);
			}
		}

		if (this.deletePoiStatement2 != null) {
			try {
				this.deletePoiStatement2.close();
			} catch (SQLException e) {
				LOGGER.log(Level.SEVERE, e.getMessage(), e);
			}
		}

		if (this.isValidDBStatement != null) {
			try {
				this.isValidDBStatement.close();
			} catch (SQLException e) {
				LOGGER.log(Level.SEVERE, e.getMessage(), e);
			}
		}

		// Close connection
		if (this.conn != null) {
			try {
				this.conn.close();
			} catch (SQLException e) {
				LOGGER.log(Level.SEVERE, e.getMessage(), e);
			}
		}
	}

	/**
	 * If the file does not exist it will be created and filled.
	 */
	private void createOrOpenDBFile() {
		// Create or open file
		try {
			Class.forName("org.sqlite.JDBC");
			this.conn = DriverManager.getConnection("jdbc:sqlite:" + this.dbFilePath);
			this.conn.setAutoCommit(false);
		} catch (ClassNotFoundException | SQLException e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}

		if (!isValidDataBase()) {
			try {
				createTables();
			} catch (SQLException e) {
				LOGGER.log(Level.SEVERE, e.getMessage(), e);
			}
		}
	}

	/**
	 * DB open created a new file, so let's create its tables.
	 */
	private void createTables() throws SQLException {
		Statement stmt = this.conn.createStatement();
		stmt.execute(DROP_INDEX_STATEMENT);
		stmt.execute(DROP_DATA_STATEMENT);
		stmt.execute(DROP_CATEGORIES_STATEMENT);

		stmt.execute(CREATE_CATEGORIES_STATEMENT);
		stmt.execute(CREATE_DATA_STATEMENT);
		stmt.execute(CREATE_INDEX_STATEMENT);
		stmt.close();
	}

	@Override
	public Collection<PointOfInterest> findInRect(BoundingBox bb, PoiCategoryFilter filter,
												  int limit) {
		// Clear previous results
		this.ret.clear();

		// Query
		try {
			PreparedStatement stmt = (filter != null
					? this.conn.prepareStatement(PoiCategoryRangeQueryGenerator.getSQLSelectString(filter))
					: this.findInBoxStatement);

			stmt.clearParameters();

			stmt.setDouble(1, bb.maxLatitude);
			stmt.setDouble(2, bb.maxLongitude);
			stmt.setDouble(3, bb.minLatitude);
			stmt.setDouble(4, bb.minLongitude);
			stmt.setInt(5, limit);

			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				long id = rs.getLong(1);
				double lat = rs.getDouble(2);
				double lon = rs.getDouble(3);
				String data = rs.getString(4);
				int categoryID = rs.getInt(5);

				try {
					this.poi = new PoiImpl(id, lat, lon, data, this.categoryManager.getPoiCategoryByID(categoryID));
					this.ret.add(this.poi);
				} catch (UnknownPoiCategoryException e) {
					LOGGER.log(Level.SEVERE, e.getMessage(), e);
				}
			}
			rs.close();
		} catch (SQLException e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}

		return this.ret;
	}

	@Override
	public PointOfInterest findPointByID(long poiID) {
		// Clear previous results
		this.poi = null;

		// Query
		try {
			this.findByIDStatement.clearParameters();

			this.findByIDStatement.setLong(1, poiID);

			ResultSet rs = this.findByIDStatement.executeQuery();
			while (rs.next()) {
				long id = rs.getLong(1);
				double lat = rs.getDouble(2);
				double lon = rs.getDouble(3);
				String data = rs.getString(4);
				int categoryID = rs.getInt(5);

				try {
					this.poi = new PoiImpl(id, lat, lon, data, this.categoryManager.getPoiCategoryByID(categoryID));
				} catch (UnknownPoiCategoryException e) {
					LOGGER.log(Level.SEVERE, e.getMessage(), e);
				}
			}
			rs.close();
		} catch (SQLException e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}

		return this.poi;
	}

	@Override
	public Collection<PointOfInterest> findPointsByName(String pattern) {
		// Clear previous results
		this.ret.clear();

		// Query
		try {
			this.findByNameStatement.clearParameters();

			this.findByNameStatement.setString(1, pattern);

			ResultSet rs = this.findByNameStatement.executeQuery();
			while (rs.next()) {
				long id = rs.getLong(1);
				double lat = rs.getDouble(2);
				double lon = rs.getDouble(3);
				String data = rs.getString(4);
				int categoryID = rs.getInt(5);

				try {
					this.poi = new PoiImpl(id, lat, lon, data, this.categoryManager.getPoiCategoryByID(categoryID));
					this.ret.add(this.poi);
				} catch (UnknownPoiCategoryException e) {
					LOGGER.log(Level.SEVERE, e.getMessage(), e);
				}
			}
			rs.close();
		} catch (SQLException e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}

		return this.ret;
	}

	@Override
	public void insertPointOfInterest(PointOfInterest poi) {
		try {
			this.insertPoiStatement1.clearParameters();
			this.insertPoiStatement2.clearParameters();

			Statement stmt = this.conn.createStatement();
			stmt.execute("BEGIN;");

			this.insertPoiStatement1.setLong(1, poi.getId());
			this.insertPoiStatement1.setDouble(2, poi.getLatitude());
			this.insertPoiStatement1.setDouble(3, poi.getLatitude());
			this.insertPoiStatement1.setDouble(4, poi.getLongitude());
			this.insertPoiStatement1.setDouble(5, poi.getLongitude());

			this.insertPoiStatement2.setLong(1, poi.getId());
			this.insertPoiStatement2.setString(2, poi.getData());
			this.insertPoiStatement2.setInt(3, poi.getCategory().getID());

			this.insertPoiStatement1.executeUpdate();
			this.insertPoiStatement2.executeUpdate();

			stmt.execute("COMMIT;");
			stmt.close();
		} catch (SQLException e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	@Override
	public void insertPointsOfInterest(Collection<PointOfInterest> pois) {
		try {
			this.insertPoiStatement1.clearParameters();
			this.insertPoiStatement2.clearParameters();

			Statement stmt = this.conn.createStatement();
			stmt.execute("BEGIN;");

			for (PointOfInterest poi : pois) {
				this.insertPoiStatement1.setLong(1, poi.getId());
				this.insertPoiStatement1.setDouble(2, poi.getLatitude());
				this.insertPoiStatement1.setDouble(3, poi.getLatitude());
				this.insertPoiStatement1.setDouble(4, poi.getLongitude());
				this.insertPoiStatement1.setDouble(5, poi.getLongitude());

				this.insertPoiStatement2.setLong(1, poi.getId());
				this.insertPoiStatement2.setString(2, poi.getData());
				this.insertPoiStatement2.setInt(3, poi.getCategory().getID());

				this.insertPoiStatement1.executeUpdate();
				this.insertPoiStatement2.executeUpdate();
			}

			stmt.execute("COMMIT;");
			stmt.close();
		} catch (SQLException e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	/**
	 * @return True if the database is a valid POI database.
	 */
	private boolean isValidDataBase() {
		try {
			this.isValidDBStatement = this.conn.prepareStatement(VALID_DB_STATEMENT);
		} catch (SQLException e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}

		// Check for table names
		// TODO Is it necessary to get the tables meta data as well?
		int numTables = 0;
		try {
			ResultSet rs = this.isValidDBStatement.executeQuery();
			if (rs.next()) {
				numTables = rs.getInt(1);
			}
			rs.close();
		} catch (SQLException e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}

		return numTables == NUMBER_OF_TABLES;
	}

	@Override
	public void removePointOfInterest(PointOfInterest poi) {
		try {
			this.deletePoiStatement1.clearParameters();
			this.deletePoiStatement2.clearParameters();

			Statement stmt = this.conn.createStatement();
			stmt.execute("BEGIN;");

			this.deletePoiStatement1.setLong(1, poi.getId());
			this.deletePoiStatement2.setLong(1, poi.getId());

			this.deletePoiStatement1.executeUpdate();
			this.deletePoiStatement2.executeUpdate();

			stmt.execute("COMMIT;");
			stmt.close();
		} catch (SQLException e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
	}
}
