/*
 * Copyright 2015-2018 devemux86
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
package org.mapsforge.poi.awt.storage;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Tag;
import org.mapsforge.poi.storage.*;
import org.sqlite.SQLiteConfig;

import java.sql.*;
import java.util.*;
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

    private PreparedStatement findCatByIDStatement = null;
    private PreparedStatement findDataByIDStatement = null;
    private PreparedStatement findLocByIDStatement = null;
    private PreparedStatement insertPoiCatStatement = null;
    private PreparedStatement insertPoiDataStatement = null;
    private PreparedStatement insertPoiLocStatement = null;
    private PreparedStatement deletePoiCatStatement = null;
    private PreparedStatement deletePoiDataStatement = null;
    private PreparedStatement deletePoiLocStatement = null;
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
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void close() {
        if (isClosed()) {
            return;
        }

        // Close statements

        if (this.findCatByIDStatement != null) {
            try {
                this.findCatByIDStatement.close();
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
        }

        if (this.findDataByIDStatement != null) {
            try {
                this.findDataByIDStatement.close();
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
        }

        if (this.findLocByIDStatement != null) {
            try {
                this.findLocByIDStatement.close();
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
        }

        if (this.insertPoiCatStatement != null) {
            try {
                this.insertPoiCatStatement.close();
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
        }

        if (this.insertPoiDataStatement != null) {
            try {
                this.insertPoiDataStatement.close();
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
        }

        if (this.insertPoiLocStatement != null) {
            try {
                this.insertPoiLocStatement.close();
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
        }

        if (this.deletePoiCatStatement != null) {
            try {
                this.deletePoiCatStatement.close();
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
        }

        if (this.deletePoiDataStatement != null) {
            try {
                this.deletePoiDataStatement.close();
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
        }

        if (this.deletePoiLocStatement != null) {
            try {
                this.deletePoiLocStatement.close();
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
        stmt.execute(DbConstants.DROP_INDEX_IDX_STATEMENT);
        stmt.execute(DbConstants.DROP_INDEX_STATEMENT);
        stmt.execute(DbConstants.DROP_CATEGORY_MAP_STATEMENT);
        stmt.execute(DbConstants.DROP_DATA_IDX_STATEMENT);
        stmt.execute(DbConstants.DROP_DATA_STATEMENT);
        stmt.execute(DbConstants.DROP_CATEGORIES_STATEMENT);

        stmt.execute(DbConstants.CREATE_CATEGORIES_STATEMENT);
        stmt.execute(DbConstants.CREATE_DATA_STATEMENT);
        stmt.execute(DbConstants.CREATE_DATA_IDX_STATEMENT);
        stmt.execute(DbConstants.CREATE_CATEGORY_MAP_STATEMENT);
        stmt.execute(DbConstants.CREATE_INDEX_STATEMENT);
        stmt.execute(DbConstants.CREATE_INDEX_IDX_STATEMENT);
        stmt.execute(DbConstants.CREATE_METADATA_STATEMENT);

        stmt.close();
    }

    /**
     * @param poiID Id of POI
     * @return Set of PoiCategories
     */
    private Set<PoiCategory> findCategoriesByID(long poiID) {
        ResultSet rs = null;
        try {
            if (this.findCatByIDStatement == null) {
                this.findCatByIDStatement = this.conn.prepareStatement(DbConstants.FIND_CATEGORIES_BY_ID_STATEMENT);
            }

            this.findCatByIDStatement.clearParameters();

            this.findCatByIDStatement.setLong(1, poiID);

            Set<PoiCategory> categories = new HashSet<>();
            rs = this.findCatByIDStatement.executeQuery();
            while (rs.next()) {
                long id = rs.getLong(2);
                categories.add(this.categoryManager.getPoiCategoryByID((int) id));
            }
            return categories;
        } catch (SQLException | UnknownPoiCategoryException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                // Statements are closed in close() method
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
        }
        return null;
    }

    /**
     * @param poiID Id of POI
     * @return Set of Tags
     */
    private Set<Tag> findDataByID(long poiID) {
        ResultSet rs = null;
        try {
            if (this.findDataByIDStatement == null) {
                this.findDataByIDStatement = this.conn.prepareStatement(DbConstants.FIND_DATA_BY_ID_STATEMENT);
            }

            this.findDataByIDStatement.clearParameters();

            this.findDataByIDStatement.setLong(1, poiID);

            Set<Tag> tags = new HashSet<>();
            rs = this.findDataByIDStatement.executeQuery();
            while (rs.next()) {
                String data = rs.getString(2);
                tags.addAll(stringToTags(data));
            }
            return tags;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                // Statements are closed in close() method
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<PointOfInterest> findInRect(BoundingBox bb, PoiCategoryFilter filter,
                                                  List<Tag> patterns, LatLong orderBy, int limit) {
        // Clear previous results
        this.ret.clear();

        // Query
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            int pSize = patterns == null ? 0 : patterns.size();
            stmt = this.conn.prepareStatement(AbstractPoiPersistenceManager.getSQLSelectString(filter, pSize, orderBy));

            stmt.clearParameters();

            stmt.setDouble(1, bb.maxLatitude);
            stmt.setDouble(2, bb.maxLongitude);
            stmt.setDouble(3, bb.minLatitude);
            stmt.setDouble(4, bb.minLongitude);

            int i = 0; // i is only counted, if pattern is not null
            if (pSize > 0) {
                for (Tag tag : patterns) {
                    if (tag == null) {
                        continue;
                    }
                    stmt.setString(5 + i, "%"
                            + (tag.key.equals("*") ? "" : (tag.key + "="))
                            + tag.value + "%");
                    i++;
                }
            }
            stmt.setInt(5 + i, limit);

            rs = stmt.executeQuery();
            while (rs.next()) {
                long id = rs.getLong(1);
                double lat = rs.getDouble(2);
                double lon = rs.getDouble(3);

                this.poi = new PointOfInterest(id, lat, lon, findDataByID(id), findCategoriesByID(id));
                this.ret.add(this.poi);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
        }

        return this.ret;
    }

    /**
     * @param poiID Id of POI
     * @return Latitude and longitude values of POI
     */
    private LatLong findLocationByID(long poiID) {
        ResultSet rs = null;
        try {
            if (this.findLocByIDStatement == null) {
                this.findLocByIDStatement = this.conn.prepareStatement(DbConstants.FIND_LOCATION_BY_ID_STATEMENT);
            }

            this.findLocByIDStatement.clearParameters();

            this.findLocByIDStatement.setLong(1, poiID);

            rs = this.findLocByIDStatement.executeQuery();
            if (rs.next()) {
                double lat = rs.getDouble(2);
                double lon = rs.getDouble(3);
                return new LatLong(lat, lon);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                // Statements are closed in close() method
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PointOfInterest findPointByID(long poiID) {
        // Clear previous results
        this.poi = null;

        // Query
        LatLong latlong = findLocationByID(poiID);
        if (latlong != null) {
            this.poi = new PointOfInterest(poiID, latlong.latitude, latlong.longitude,
                    findDataByID(poiID), findCategoriesByID(poiID));
        }

        return this.poi;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void insertPointOfInterest(PointOfInterest poi) {
        insertPointsOfInterest(Collections.singleton(poi));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void insertPointsOfInterest(Collection<PointOfInterest> pois) {
        try {
            if (this.insertPoiLocStatement == null) {
                this.insertPoiLocStatement = this.conn.prepareStatement(DbConstants.INSERT_INDEX_STATEMENT);
            }
            if (this.insertPoiDataStatement == null) {
                this.insertPoiDataStatement = this.conn.prepareStatement(DbConstants.INSERT_DATA_STATEMENT);
            }
            if (this.insertPoiCatStatement == null) {
                this.insertPoiCatStatement = this.conn.prepareStatement(DbConstants.INSERT_CATEGORY_MAP_STATEMENT);
            }

            this.insertPoiLocStatement.clearParameters();
            this.insertPoiDataStatement.clearParameters();
            this.insertPoiCatStatement.clearParameters();

            Statement stmt = this.conn.createStatement();
            //stmt.execute("BEGIN;");

            for (PointOfInterest poi : pois) {
                // POI location
                this.insertPoiLocStatement.setLong(1, poi.getId());
                this.insertPoiLocStatement.setDouble(2, poi.getLatitude());
                this.insertPoiLocStatement.setDouble(3, poi.getLongitude());
                this.insertPoiLocStatement.executeUpdate();

                // POI data
                this.insertPoiDataStatement.setLong(1, poi.getId());
                this.insertPoiDataStatement.setString(2, tagsToString(poi.getTags()));
                this.insertPoiDataStatement.executeUpdate();

                // POI categories
                for (PoiCategory cat : poi.getCategories()) {
                    this.insertPoiCatStatement.setLong(1, poi.getId());
                    this.insertPoiCatStatement.setLong(2, cat.getID());
                    this.insertPoiCatStatement.executeUpdate();
                }
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
    public boolean isClosed() {
        return this.poiFile == null;
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
        ResultSet rs = null;
        try {
            rs = this.isValidDBStatement.executeQuery();
            if (rs.next()) {
                numTables = rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
        }

        return numTables == DbConstants.NUMBER_OF_TABLES;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readPoiFileInfo() {
        PoiFileInfoBuilder poiFileInfoBuilder = new PoiFileInfoBuilder();

        ResultSet rs = null;
        try {
            if (this.metadataStatement == null) {
                this.metadataStatement = this.conn.prepareStatement(DbConstants.FIND_METADATA_STATEMENT);
            }

            rs = this.metadataStatement.executeQuery();
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
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
        }

        poiFileInfo = poiFileInfoBuilder.build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removePointOfInterest(PointOfInterest poi) {
        try {
            if (this.deletePoiLocStatement == null) {
                this.deletePoiLocStatement = this.conn.prepareStatement(DbConstants.DELETE_INDEX_STATEMENT);
            }
            if (this.deletePoiDataStatement == null) {
                this.deletePoiDataStatement = this.conn.prepareStatement(DbConstants.DELETE_DATA_STATEMENT);
            }
            if (this.deletePoiCatStatement == null) {
                this.deletePoiCatStatement = this.conn.prepareStatement(DbConstants.DELETE_CATEGORY_MAP_STATEMENT);
            }

            this.deletePoiLocStatement.clearParameters();
            this.deletePoiDataStatement.clearParameters();
            this.deletePoiCatStatement.clearParameters();

            Statement stmt = this.conn.createStatement();
            //stmt.execute("BEGIN;");

            this.deletePoiLocStatement.setLong(1, poi.getId());
            this.deletePoiDataStatement.setLong(1, poi.getId());
            this.deletePoiCatStatement.setLong(1, poi.getId());

            this.deletePoiLocStatement.executeUpdate();
            this.deletePoiDataStatement.executeUpdate();
            this.deletePoiCatStatement.executeUpdate();

            stmt.execute("COMMIT;");
            stmt.close();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }
}
