/*
 * Copyright 2015-2016 devemux86
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
import org.mapsforge.poi.storage.DbConstants;
import org.mapsforge.poi.storage.PoiCategoryFilter;
import org.mapsforge.poi.storage.PoiFileInfo;
import org.mapsforge.poi.storage.PoiFileInfoBuilder;
import org.mapsforge.poi.storage.PoiPersistenceManager;
import org.mapsforge.poi.storage.PointOfInterest;
import org.mapsforge.poi.storage.UnknownPoiCategoryException;
import org.sqlite.SQLiteConfig;

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

    private PreparedStatement findByIDStatement = null;
    private PreparedStatement insertPoiStatement1 = null;
    private PreparedStatement insertPoiStatement2 = null;
    private PreparedStatement deletePoiStatement1 = null;
    private PreparedStatement deletePoiStatement2 = null;
    private PreparedStatement isValidDBStatement = null;
    private PreparedStatement metadataStatement = null;

    /**
     * @param dbFilePath Path to SQLite file containing POI data.
     * @param readOnly   If the file does not exist it can be created and filled.
     */
    AwtPoiPersistenceManager(String dbFilePath, boolean readOnly) {
        super();

        // Open / create POI database
        createOrOpenDBFile(dbFilePath, readOnly);

        // Load categories from database
        this.categoryManager = new AwtPoiCategoryManager(this.conn);

        // Queries
        try {
            // Finds a POI by its unique ID
            this.findByIDStatement = this.conn.prepareStatement(DbConstants.FIND_BY_ID_STATEMENT);

            // Inserts a POI into index and adds its data
            this.insertPoiStatement1 = this.conn.prepareStatement(DbConstants.INSERT_INDEX_STATEMENT);
            this.insertPoiStatement2 = this.conn.prepareStatement(DbConstants.INSERT_DATA_STATEMENT);

            // Deletes a POI given by its ID
            this.deletePoiStatement1 = this.conn.prepareStatement(DbConstants.DELETE_INDEX_STATEMENT);
            this.deletePoiStatement2 = this.conn.prepareStatement(DbConstants.DELETE_DATA_STATEMENT);

            // Metadata
            this.metadataStatement = this.conn.prepareStatement(DbConstants.FIND_METADATA_STATEMENT);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        // Close statements

        if (this.findByIDStatement != null) {
            try {
                this.findByIDStatement.close();
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

        if (this.metadataStatement != null) {
            try {
                this.metadataStatement.close();
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

        this.poiFile = null;
    }

    /**
     * @param dbFilePath Path to SQLite file containing POI data.
     * @param readOnly   If the file does not exist it can be created and filled.
     */
    private void createOrOpenDBFile(String dbFilePath, boolean readOnly) {
        // Open file
        try {
            Class.forName("org.sqlite.JDBC");
            SQLiteConfig config = new SQLiteConfig();
            config.setReadOnly(readOnly);
            this.conn = DriverManager.getConnection("jdbc:sqlite:" + dbFilePath, config.toProperties());
            this.conn.setAutoCommit(false);
            this.poiFile = dbFilePath;
        } catch (ClassNotFoundException | SQLException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }

        // Create file
        if (!isValidDataBase() && !readOnly) {
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

        stmt.execute(DbConstants.DROP_METADATA_STATEMENT);
        stmt.execute(DbConstants.DROP_INDEX_STATEMENT);
        stmt.execute(DbConstants.DROP_DATA_STATEMENT);
        stmt.execute(DbConstants.DROP_CATEGORIES_STATEMENT);

        stmt.execute(DbConstants.CREATE_CATEGORIES_STATEMENT);
        stmt.execute(DbConstants.CREATE_DATA_STATEMENT);
        stmt.execute(DbConstants.CREATE_INDEX_STATEMENT);
        stmt.execute(DbConstants.CREATE_METADATA_STATEMENT);

        stmt.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<PointOfInterest> findInRect(BoundingBox bb, PoiCategoryFilter filter,
                                                  String pattern, int limit) {
        return findInRect(bb, filter, new String[]{pattern}, limit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<PointOfInterest> findInRect(BoundingBox bb, PoiCategoryFilter filter,
                                                  String[] patterns, int limit) {
        // Clear previous results
        this.ret.clear();

        // Query
        try {
            PreparedStatement stmt = this.conn.prepareStatement(AbstractPoiPersistenceManager.getSQLSelectString(filter, patterns));

            stmt.clearParameters();

            stmt.setDouble(1, bb.maxLatitude);
            stmt.setDouble(2, bb.maxLongitude);
            stmt.setDouble(3, bb.minLatitude);
            stmt.setDouble(4, bb.minLongitude);
            int i = 0;
            if (patterns != null) {
                for(i=0; i<patterns.length; i++){
                    stmt.setString(5+i, patterns[i]);
                }
            }
            stmt.setInt(5+i, limit);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                long id = rs.getLong(1);
                double lat = rs.getDouble(2);
                double lon = rs.getDouble(3);
                String data = rs.getString(4);
                int categoryID = rs.getInt(5);

                try {
                    this.poi = new PointOfInterest(id, lat, lon, data, this.categoryManager.getPoiCategoryByID(categoryID));
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

    /**
     * {@inheritDoc}
     */
    @Override
    public PointOfInterest findPointByID(long poiID) {
        // Clear previous results
        this.poi = null;

        // Query
        try {
            this.findByIDStatement.clearParameters();

            this.findByIDStatement.setLong(1, poiID);

            ResultSet rs = this.findByIDStatement.executeQuery();
            if (rs.next()) {
                long id = rs.getLong(1);
                double lat = rs.getDouble(2);
                double lon = rs.getDouble(3);
                String data = rs.getString(4);
                int categoryID = rs.getInt(5);

                try {
                    this.poi = new PointOfInterest(id, lat, lon, data, this.categoryManager.getPoiCategoryByID(categoryID));
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

    /**
     * {@inheritDoc}
     */
    @Override
    public PoiFileInfo getPoiFileInfo() {
        PoiFileInfoBuilder poiFileInfoBuilder = new PoiFileInfoBuilder();

        // Query
        try {
            ResultSet rs = this.metadataStatement.executeQuery();
            while (rs.next()) {
                String name = rs.getString(1);

                switch (name) {
                    case DbConstants.METADATA_BOUNDS:
                        String bounds = rs.getString(2);
                        if (bounds != null) {
                            poiFileInfoBuilder.bounds = BoundingBox.fromString(bounds);
                        }
                        break;
                    case DbConstants.METADATA_COMMENT:
                        poiFileInfoBuilder.comment = rs.getString(2);
                        break;
                    case DbConstants.METADATA_DATE:
                        poiFileInfoBuilder.date = rs.getLong(2);
                        break;
                    case DbConstants.METADATA_LANGUAGE:
                        poiFileInfoBuilder.language = rs.getString(2);
                        break;
                    case DbConstants.METADATA_VERSION:
                        poiFileInfoBuilder.version = rs.getInt(2);
                        break;
                    case DbConstants.METADATA_WAYS:
                        poiFileInfoBuilder.ways = Boolean.parseBoolean(rs.getString(2));
                        break;
                    case DbConstants.METADATA_WRITER:
                        poiFileInfoBuilder.writer = rs.getString(2);
                        break;
                }
            }
            rs.close();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }

        return poiFileInfoBuilder.build();
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
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
     * {@inheritDoc}
     */
    @Override
    public boolean isValidDataBase() {
        try {
            this.isValidDBStatement = this.conn.prepareStatement(DbConstants.VALID_DB_STATEMENT);
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

        return numTables == DbConstants.NUMBER_OF_TABLES;
    }

    /**
     * {@inheritDoc}
     */
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
