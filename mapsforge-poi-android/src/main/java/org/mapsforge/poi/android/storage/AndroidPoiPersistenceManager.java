/*
 * Copyright 2010, 2011 mapsforge.org
 * Copyright 2010, 2011 Karsten Groll
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
package org.mapsforge.poi.android.storage;

import androidx.sqlite.SQLiteConnection;
import androidx.sqlite.SQLiteStatement;
import androidx.sqlite.driver.bundled.BundledSQLite;
import androidx.sqlite.driver.bundled.BundledSQLiteDriver;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Tag;
import org.mapsforge.poi.storage.*;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A {@link PoiPersistenceManager} implementation using a SQLite database via wrapper.
 * <p/>
 * This class can only be used within Android.
 */
class AndroidPoiPersistenceManager extends AbstractPoiPersistenceManager {
    private static final Logger LOGGER = Logger.getLogger(AndroidPoiPersistenceManager.class.getName());

    private SQLiteConnection connection = null;

    /**
     * @param dbFilePath Path to SQLite file containing POI data.
     * @param readOnly   If the file does not exist it can be created and filled.
     */
    AndroidPoiPersistenceManager(String dbFilePath, boolean readOnly) {
        super();

        // Open / create POI database
        createOrOpenDBFile(dbFilePath, readOnly);

        // Load categories from database
        this.categoryManager = new AndroidPoiCategoryManager(this.connection);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void close() {
        if (isClosed()) {
            return;
        }

        // Close connection
        if (this.connection != null) {
            try {
                this.connection.close();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, e.toString(), e);
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
            this.connection = new BundledSQLiteDriver().open(dbFilePath, readOnly ? BundledSQLite.SQLITE_OPEN_READONLY : BundledSQLite.SQLITE_OPEN_CREATE);
            this.poiFile = dbFilePath;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.toString(), e);
        }

        // Create file
        if (!isValidDataBase() && !readOnly) {
            try {
                createTables();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, e.toString(), e);
            }
        }
    }

    /**
     * DB open created a new file, so let's create its tables.
     */
    private void createTables() {
        this.connection.prepare(DbConstants.DROP_METADATA_STATEMENT).step();
        this.connection.prepare(DbConstants.DROP_INDEX_STATEMENT).step();
        this.connection.prepare(DbConstants.DROP_CATEGORY_MAP_IDX_STATEMENT).step();
        this.connection.prepare(DbConstants.DROP_CATEGORY_MAP_STATEMENT).step();
        this.connection.prepare(DbConstants.DROP_DATA_FTS_STATEMENT).step();
        this.connection.prepare(DbConstants.DROP_DATA_STATEMENT).step();
        this.connection.prepare(DbConstants.DROP_CATEGORIES_STATEMENT).step();

        this.connection.prepare(DbConstants.CREATE_CATEGORIES_STATEMENT).step();
        this.connection.prepare(DbConstants.CREATE_DATA_STATEMENT).step();
        this.connection.prepare(DbConstants.CREATE_DATA_FTS_STATEMENT).step();
        this.connection.prepare(DbConstants.CREATE_CATEGORY_MAP_STATEMENT).step();
        this.connection.prepare(DbConstants.CREATE_CATEGORY_MAP_IDX_STATEMENT).step();
        this.connection.prepare(DbConstants.CREATE_INDEX_STATEMENT).step();
        this.connection.prepare(DbConstants.CREATE_METADATA_STATEMENT).step();
    }

    /**
     * @param poiID Id of POI
     * @return Set of PoiCategories
     */
    private Set<PoiCategory> findCategoriesByID(long poiID) throws UnknownPoiCategoryException {
        SQLiteStatement statement = null;
        try {
            Set<PoiCategory> categories = new HashSet<>();
            String sql = DbConstants.FIND_CATEGORIES_BY_ID_STATEMENT;
            statement = this.connection.prepare(sql);
            statement.bindText(1, String.valueOf(poiID));
            while (statement.step()) {
                long id = statement.getLong(1);
                categories.add(this.categoryManager.getPoiCategoryByID((int) id));
            }
            return categories;
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, e.toString(), e);
            }
        }
    }

    /**
     * @param poiID Id of POI
     * @return Set of Tags
     */
    private Set<Tag> findDataByID(long poiID) {
        SQLiteStatement statement = null;
        try {
            Set<Tag> tags = new HashSet<>();
            statement = this.connection.prepare(DbConstants.FIND_DATA_BY_ID_STATEMENT);
            statement.bindText(1, String.valueOf(poiID));
            while (statement.step()) {
                String data = statement.getText(1);
                tags.addAll(stringToTags(data));
            }
            return tags;
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, e.toString(), e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<PointOfInterest> findInRect(BoundingBox bb, PoiCategoryFilter filter,
                                                  List<Tag> patterns, LatLong orderBy, int limit, boolean findCategories) {
        // Clear previous results
        this.ret.clear();

        // Query
        SQLiteStatement statement = null;
        try {
            int pSize = patterns == null ? 0 : patterns.size();
            String sql = AbstractPoiPersistenceManager.getSQLSelectString(filter, pSize, orderBy, getPoiFileInfo().version);

            List<String> selectionArgs = new ArrayList<>();
            selectionArgs.add(String.valueOf(bb.maxLatitude));
            selectionArgs.add(String.valueOf(bb.maxLongitude));
            selectionArgs.add(String.valueOf(bb.minLatitude));
            selectionArgs.add(String.valueOf(bb.minLongitude));
            if (pSize > 0) {
                if (getPoiFileInfo().version <= 3) {
                    for (Tag tag : patterns) {
                        if (tag == null) {
                            continue;
                        }
                        selectionArgs.add("%" + (tag.key.equals("*") ? "" : (tag.key + "=")) + tag.value + "%");
                    }
                } else {
                    StringBuilder sb = new StringBuilder();
                    for (Tag tag : patterns) {
                        if (tag == null) {
                            continue;
                        }
                        if (sb.length() > 0) {
                            sb.append(" OR ");
                        }
                        String text = (tag.key.equals("*") ? "" : (tag.key + "=")) + tag.value;
                        if (!tag.key.equals("*") || text.contains("=")) {
                            text = "\"" + text + "\"";
                        }
                        sb.append(text);
                    }
                    selectionArgs.add(sb.toString());
                }
            }
            selectionArgs.add(String.valueOf(limit));

            if (DEBUG)
                LOGGER.info(sql + "\nparameters=" + Arrays.toString(selectionArgs.toArray(new String[0])));

            statement = this.connection.prepare(sql);
            for (int i = 0; i < selectionArgs.size(); i++) {
                statement.bindText(i + 1, selectionArgs.get(i));
            }
            while (statement.step()) {
                long id = statement.getLong(0);
                double lat = statement.getDouble(1);
                double lon = statement.getDouble(2);
                String data = statement.getText(3);

                this.poi = new PointOfInterest(id, lat, lon, stringToTags(data), findCategories ? findCategoriesByID(id) : null);
                this.ret.add(this.poi);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.toString(), e);
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, e.toString(), e);
            }
        }

        return this.ret;
    }

    /**
     * @param poiID Id of POI
     * @return Latitude and longitude values of POI
     */
    private LatLong findLocationByID(long poiID) {
        SQLiteStatement statement = null;
        try {
            statement = this.connection.prepare(getPoiFileInfo().version <= 3 ? DbConstants.FIND_LOCATION_BY_ID_STATEMENT_V3 : DbConstants.FIND_LOCATION_BY_ID_STATEMENT);
            statement.bindText(1, String.valueOf(poiID));
            if (statement.step()) {
                double lat = statement.getDouble(1);
                double lon = statement.getDouble(2);
                return new LatLong(lat, lon);
            }
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, e.toString(), e);
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
        try {
            LatLong latlong = findLocationByID(poiID);
            if (latlong != null) {
                this.poi = new PointOfInterest(poiID, latlong.latitude, latlong.longitude,
                        findDataByID(poiID), findCategoriesByID(poiID));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.toString(), e);
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
            for (PointOfInterest poi : pois) {
                // POI location
                SQLiteStatement statement = this.connection.prepare(DbConstants.INSERT_INDEX_STATEMENT);
                statement.bindText(1, String.valueOf(poi.getId()));
                statement.bindText(2, String.valueOf(poi.getLatitude()));
                statement.bindText(3, String.valueOf(poi.getLatitude()));
                statement.bindText(4, String.valueOf(poi.getLongitude()));
                statement.bindText(5, String.valueOf(poi.getLongitude()));
                statement.step();

                // POI data
                statement = this.connection.prepare(DbConstants.INSERT_DATA_STATEMENT);
                statement.bindText(1, String.valueOf(poi.getId()));
                statement.bindText(2, tagsToString(poi.getTags()));
                statement.step();

                // POI categories
                for (PoiCategory cat : poi.getCategories()) {
                    statement = this.connection.prepare(DbConstants.INSERT_CATEGORY_MAP_STATEMENT);
                    statement.bindText(1, String.valueOf(poi.getId()));
                    statement.bindText(2, String.valueOf(cat.getID()));
                    statement.step();
                }
            }

            if (getPoiFileInfo().version >= 4) {
                this.connection.prepare(DbConstants.INSERT_DATA_FTS_REBUILD_STATEMENT).step();
                this.connection.prepare(DbConstants.INSERT_DATA_FTS_OPTIMIZE_STATEMENT).step();
                this.connection.prepare(DbConstants.INSERT_DATA_FTS_INTEGRITY_CHECK_STATEMENT).step();
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.toString(), e);
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
        // Check for table names
        // TODO Is it necessary to get the tables meta data as well?
        int numTables = 0;
        SQLiteStatement statement = null;
        try {
            String sql = DbConstants.VALID_DB_STATEMENT;
            statement = this.connection.prepare(sql);
            if (statement.step()) {
                numTables = statement.getInt(0);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.toString(), e);
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, e.toString(), e);
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

        SQLiteStatement statement = null;
        try {
            statement = this.connection.prepare(DbConstants.FIND_METADATA_STATEMENT);
            while (statement.step()) {
                String name = statement.getText(0);

                switch (name) {
                    case DbConstants.METADATA_BOUNDS:
                        String bounds = statement.getText(1);
                        if (bounds != null) {
                            poiFileInfoBuilder.bounds = BoundingBox.fromString(bounds);
                        }
                        break;
                    case DbConstants.METADATA_COMMENT:
                        poiFileInfoBuilder.comment = statement.getText(1);
                        break;
                    case DbConstants.METADATA_DATE:
                        poiFileInfoBuilder.date = statement.getLong(1);
                        break;
                    case DbConstants.METADATA_LANGUAGE:
                        poiFileInfoBuilder.language = statement.getText(1);
                        break;
                    case DbConstants.METADATA_VERSION:
                        poiFileInfoBuilder.version = statement.getInt(1);
                        break;
                    case DbConstants.METADATA_WAYS:
                        poiFileInfoBuilder.ways = Boolean.parseBoolean(statement.getText(1));
                        break;
                    case DbConstants.METADATA_WRITER:
                        poiFileInfoBuilder.writer = statement.getText(1);
                        break;
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.toString(), e);
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, e.toString(), e);
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
            SQLiteStatement statement = this.connection.prepare(DbConstants.DELETE_INDEX_STATEMENT);
            statement.bindText(1, String.valueOf(poi.getId()));
            statement.step();
            statement = this.connection.prepare(DbConstants.DELETE_DATA_STATEMENT);
            statement.bindText(1, String.valueOf(poi.getId()));
            statement.step();
            statement = this.connection.prepare(DbConstants.DELETE_CATEGORY_MAP_STATEMENT);
            statement.bindText(1, String.valueOf(poi.getId()));
            statement.step();

            if (getPoiFileInfo().version >= 4) {
                this.connection.prepare(DbConstants.INSERT_DATA_FTS_REBUILD_STATEMENT).step();
                this.connection.prepare(DbConstants.INSERT_DATA_FTS_OPTIMIZE_STATEMENT).step();
                this.connection.prepare(DbConstants.INSERT_DATA_FTS_INTEGRITY_CHECK_STATEMENT).step();
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.toString(), e);
        }
    }
}
