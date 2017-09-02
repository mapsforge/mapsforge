/*
 * Copyright 2015-2016 devemux86
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
import org.mapsforge.poi.storage.AbstractPoiPersistenceManager;
import org.mapsforge.poi.storage.DbConstants;
import org.mapsforge.poi.storage.PoiCategory;
import org.mapsforge.poi.storage.PoiCategoryFilter;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    private PreparedStatement insertPoiStatementLoc = null;
    private PreparedStatement insertPoiStatementCat = null;
    private PreparedStatement insertPoiStatementData = null;
    private PreparedStatement deletePoiStatementLoc = null;
    private PreparedStatement deletePoiStatementCat = null;
    private PreparedStatement deletePoiStatementData = null;
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
            // Finds a POI-Location by its unique ID
            this.findLocByIDStatement = this.conn.prepareStatement(DbConstants.FIND_LOCATION_BY_ID_STATEMENT);
            // Finds a POI-Data by its unique ID
            if (getPoiFileInfo().version <= 1) {
                this.findCatByIDStatement = this.conn.prepareStatement(DbConstants.FIND_CATEGORIES_BY_ID_STATEMENT_V1);
            } else {
                this.findCatByIDStatement = this.conn.prepareStatement(DbConstants.FIND_CATEGORIES_BY_ID_STATEMENT);
            }
            // Finds a POI-Categories by its unique ID
            this.findDataByIDStatement = this.conn.prepareStatement(DbConstants.FIND_DATA_BY_ID_STATEMENT);

            // Inserts a POI into index and adds its data
            this.insertPoiStatementLoc = this.conn.prepareStatement(DbConstants.INSERT_INDEX_STATEMENT);
            this.insertPoiStatementData = this.conn.prepareStatement(DbConstants.INSERT_DATA_STATEMENT);
            this.insertPoiStatementCat = this.conn.prepareStatement(DbConstants.INSERT_CATEGORYMAP_STATEMENT);

            // Deletes a POI given by its ID
            this.deletePoiStatementLoc = this.conn.prepareStatement(DbConstants.DELETE_INDEX_STATEMENT);
            this.deletePoiStatementData = this.conn.prepareStatement(DbConstants.DELETE_DATA_STATEMENT);
            this.deletePoiStatementCat = this.conn.prepareStatement(DbConstants.DELETE_CATEGORYMAP_STATEMENT);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
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

        if (this.findLocByIDStatement != null) {
            try {
                this.findLocByIDStatement.close();
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
        }

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

        if (this.insertPoiStatementLoc != null) {
            try {
                this.insertPoiStatementLoc.close();
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
        }

        if (this.insertPoiStatementData != null) {
            try {
                this.insertPoiStatementData.close();
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
        }

        if (this.insertPoiStatementCat != null) {
            try {
                this.insertPoiStatementCat.close();
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
        }

        if (this.deletePoiStatementLoc != null) {
            try {
                this.deletePoiStatementLoc.close();
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
        }

        if (this.deletePoiStatementCat != null) {
            try {
                this.deletePoiStatementCat.close();
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
        }

        if (this.deletePoiStatementData != null) {
            try {
                this.deletePoiStatementData.close();
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
        stmt.execute(DbConstants.DROP_CATEGORYMAP_STATEMENT);
        stmt.execute(DbConstants.DROP_CATEGORIES_STATEMENT);

        stmt.execute(DbConstants.CREATE_CATEGORIES_STATEMENT);
        stmt.execute(DbConstants.CREATE_CATEGORYMAP_STATEMENT);
        stmt.execute(DbConstants.CREATE_DATA_STATEMENT);
        stmt.execute(DbConstants.CREATE_INDEX_STATEMENT);
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
            this.findCatByIDStatement.clearParameters();
            this.findCatByIDStatement.setLong(1, poiID);

            Set<PoiCategory> cats = new HashSet<>();
            rs = this.findCatByIDStatement.executeQuery();
            if (rs.next()) {
                long id = rs.getLong(2);
                cats.add(this.categoryManager.getPoiCategoryByID(
                        (int) id));
            }
            return cats;
        } catch (SQLException | UnknownPoiCategoryException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        } finally {
            try {
                if (rs != null) rs.close();
                // Statements are closed in close()-Method
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
    private Set<Tag> findTagsByID(long poiID) {
        ResultSet rs = null;
        try {
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
                if (rs != null) rs.close();
                // Statements are closed in close()-Method
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
        }
        return null;
    }

    /**
     * @param poiID Id of POI
     * @return Latitude and longitude values of POI
     */
    private LatLong findLocationByID(long poiID) {
        ResultSet rs = null;
        try {
            this.findLocByIDStatement.clearParameters();

            this.findLocByIDStatement.setLong(1, poiID);

            rs = this.findLocByIDStatement.executeQuery();
            if (rs.next()) {
                return new LatLong(rs.getDouble(2), rs.getDouble(3));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        } finally {
            try {
                if (rs != null) rs.close();
                // Statements are closed in close()-Method
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
                                                  List<Tag> patterns, int limit) {
        // Clear previous results
        this.ret.clear();

        // Query
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            int pSize = patterns == null ? 0 : patterns.size();
            stmt = this.conn.prepareStatement(AbstractPoiPersistenceManager.getSQLSelectString(filter, pSize, getPoiFileInfo().version));

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

                this.poi = new PointOfInterest(id, lat, lon, findTagsByID(id), findCategoriesByID(id));
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
     * {@inheritDoc}
     */
    @Override
    public PointOfInterest findPointByID(long poiID) {
        // Clear previous results
        this.poi = null;

        // Query
        LatLong latlong = findLocationByID(poiID);
        if (latlong != null) {
            this.poi = new PointOfInterest(poiID, latlong.getLatitude(), latlong.getLongitude(),
                    findTagsByID(poiID), findCategoriesByID(poiID));
        }

        return this.poi;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updatePoiFileInfo() {
        //Prepare metadataStatement separately, because its decisive for the further poi specification handling
        if (this.metadataStatement == null) {
            try {
                this.metadataStatement = this.conn.prepareStatement(DbConstants.FIND_METADATA_STATEMENT);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
        }

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

        poiFileInfo = poiFileInfoBuilder.build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void insertPointOfInterest(PointOfInterest poi) {
        Collection<PointOfInterest> c = new HashSet<>();
        c.add(poi);
        insertPointsOfInterest(c);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void insertPointsOfInterest(Collection<PointOfInterest> pois) {
        try {
            this.insertPoiStatementLoc.clearParameters();
            this.insertPoiStatementCat.clearParameters();
            this.insertPoiStatementData.clearParameters();

            Statement stmt = this.conn.createStatement();
            stmt.execute("BEGIN;");

            for (PointOfInterest poi : pois) {
                this.insertPoiStatementLoc.setLong(1, poi.getId());
                this.insertPoiStatementLoc.setDouble(2, poi.getLatitude());
                this.insertPoiStatementLoc.setDouble(3, poi.getLatitude());
                this.insertPoiStatementLoc.setDouble(4, poi.getLongitude());
                this.insertPoiStatementLoc.setDouble(5, poi.getLongitude());

                //Set poi tags
                String data = tagsToString(poi.getTags());
                this.insertPoiStatementData.setLong(1, poi.getId());
                this.insertPoiStatementData.setString(2, data);

                //Set multiple poi categories
                for (PoiCategory cat : poi.getCategories()) {

                    this.insertPoiStatementCat.setLong(1, poi.getId());
                    this.insertPoiStatementCat.setLong(2, cat.getID());
                    this.insertPoiStatementCat.executeUpdate();
                }

                this.insertPoiStatementData.executeUpdate();
                this.insertPoiStatementLoc.executeUpdate();
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
        int poiVersion = getPoiFileInfo().version;
        try {
            if (poiVersion <= 1) {
                this.isValidDBStatement = this.conn.prepareStatement(DbConstants.VALID_DB_STATEMENT_V1);
            } else {
                this.isValidDBStatement = this.conn.prepareStatement(DbConstants.VALID_DB_STATEMENT);
            }
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

        if (poiVersion <= 1) {
            return numTables == DbConstants.NUMBER_OF_TABLES_V1;
        } else {
            return numTables == DbConstants.NUMBER_OF_TABLES;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removePointOfInterest(PointOfInterest poi) {
        try {
            this.deletePoiStatementLoc.clearParameters();
            this.deletePoiStatementData.clearParameters();
            this.deletePoiStatementCat.clearParameters();

            Statement stmt = this.conn.createStatement();
            stmt.execute("BEGIN;");

            this.deletePoiStatementLoc.setLong(1, poi.getId());
            this.deletePoiStatementData.setLong(1, poi.getId());
            this.deletePoiStatementCat.setLong(1, poi.getId());

            this.deletePoiStatementLoc.executeUpdate();
            this.deletePoiStatementData.executeUpdate();
            this.deletePoiStatementCat.executeUpdate();

            stmt.execute("COMMIT;");
            stmt.close();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    /**
     * Convert string representation back to tags list.
     */
    private Set<Tag> stringToTags(String tagsmapstring) {
        String[] sb = tagsmapstring.split("\\r");
        Set<Tag> map = new HashSet<>();
        for (String key : sb) {
            if (key.contains("=")) {
                String[] set = key.split("=");
                if (set.length == 2)
                    map.add(new Tag(set[0], set[1]));
            }
        }
        return map;
    }

    /**
     * Convert tags to a string representation using '\r' delimiter.
     */
    private String tagsToString(Set<Tag> tagMap) {
        StringBuilder sb = new StringBuilder();
        for (Tag tag : tagMap) {
            if (sb.length() > 0) {
                sb.append('\r');
            }
            sb.append(tag.key).append('=').append(tag.value);
        }
        return sb.toString();
    }
}
