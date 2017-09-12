/*
 * Copyright 2010, 2011 mapsforge.org
 * Copyright 2010, 2011 Karsten Groll
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
package org.mapsforge.poi.android.storage;

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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import jsqlite.Constants;
import jsqlite.Database;
import jsqlite.Stmt;

/**
 * A {@link PoiPersistenceManager} implementation using a SQLite database via wrapper.
 * <p/>
 * This class can only be used within Android.
 */
class AndroidPoiPersistenceManager extends AbstractPoiPersistenceManager {
    private static final Logger LOGGER = Logger.getLogger(AndroidPoiPersistenceManager.class.getName());

    private Database db = null;

    private Stmt findCatByIDStatement = null;
    private Stmt findDataByIDStatement = null;
    private Stmt findLocByIDStatement = null;
    private Stmt insertPoiCatStatement = null;
    private Stmt insertPoiDataStatement = null;
    private Stmt insertPoiLocStatement = null;
    private Stmt deletePoiCatStatement = null;
    private Stmt deletePoiDataStatement = null;
    private Stmt deletePoiLocStatement = null;
    private Stmt isValidDBStatement = null;
    private Stmt metadataStatement = null;

    /**
     * @param dbFilePath Path to SQLite file containing POI data.
     * @param readOnly   If the file does not exist it can be created and filled.
     */
    AndroidPoiPersistenceManager(String dbFilePath, boolean readOnly) {
        super();

        // Open / create POI database
        createOrOpenDBFile(dbFilePath, readOnly);

        // Load categories from database
        this.categoryManager = new AndroidPoiCategoryManager(this.db);
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
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
        }

        if (this.findDataByIDStatement != null) {
            try {
                this.findDataByIDStatement.close();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
        }

        if (this.findLocByIDStatement != null) {
            try {
                this.findLocByIDStatement.close();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
        }

        if (this.insertPoiCatStatement != null) {
            try {
                this.insertPoiCatStatement.close();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
        }

        if (this.insertPoiDataStatement != null) {
            try {
                this.insertPoiDataStatement.close();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
        }

        if (this.insertPoiLocStatement != null) {
            try {
                this.insertPoiLocStatement.close();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
        }

        if (this.deletePoiCatStatement != null) {
            try {
                this.deletePoiCatStatement.close();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
        }

        if (this.deletePoiDataStatement != null) {
            try {
                this.deletePoiDataStatement.close();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
        }

        if (this.deletePoiLocStatement != null) {
            try {
                this.deletePoiLocStatement.close();
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

        if (this.metadataStatement != null) {
            try {
                this.metadataStatement.close();
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

        this.poiFile = null;
    }

    /**
     * @param dbFilePath Path to SQLite file containing POI data.
     * @param readOnly   If the file does not exist it can be created and filled.
     */
    private void createOrOpenDBFile(String dbFilePath, boolean readOnly) {
        // Open file
        this.db = new Database();
        try {
            this.db.open(dbFilePath, readOnly ? Constants.SQLITE_OPEN_READONLY : Constants.SQLITE_OPEN_READWRITE | Constants.SQLITE_OPEN_CREATE);
            this.poiFile = dbFilePath;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }

        // Create file
        if (!isValidDataBase() && !readOnly) {
            try {
                createTables();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
        }
    }

    /**
     * DB open created a new file, so let's create its tables.
     */
    private void createTables() throws Exception {
        this.db.exec(DbConstants.DROP_METADATA_STATEMENT, null);
        this.db.exec(DbConstants.DROP_INDEX_STATEMENT, null);
        this.db.exec(DbConstants.DROP_CATEGORY_MAP_STATEMENT, null);
        this.db.exec(DbConstants.DROP_DATA_STATEMENT, null);
        this.db.exec(DbConstants.DROP_CATEGORIES_STATEMENT, null);

        this.db.exec(DbConstants.CREATE_CATEGORIES_STATEMENT, null);
        this.db.exec(DbConstants.CREATE_DATA_STATEMENT, null);
        this.db.exec(DbConstants.CREATE_CATEGORY_MAP_STATEMENT, null);
        this.db.exec(DbConstants.CREATE_INDEX_STATEMENT, null);
        this.db.exec(DbConstants.CREATE_METADATA_STATEMENT, null);
    }

    /**
     * @param poiID Id of POI
     * @return Set of PoiCategories
     */
    private Set<PoiCategory> findCategoriesByID(long poiID) {
        try {
            if (this.findCatByIDStatement == null) {
                if (getPoiFileInfo().version < 2) {
                    this.findCatByIDStatement = this.db.prepare(DbConstants.FIND_CATEGORIES_BY_ID_STATEMENT_V1);
                } else {
                    this.findCatByIDStatement = this.db.prepare(DbConstants.FIND_CATEGORIES_BY_ID_STATEMENT);
                }
            }

            this.findCatByIDStatement.reset();
            this.findCatByIDStatement.clear_bindings();

            this.findCatByIDStatement.bind(1, poiID);

            Set<PoiCategory> categories = new HashSet<>();
            while (this.findCatByIDStatement.step()) {
                long id = this.findCatByIDStatement.column_long(1);
                categories.add(this.categoryManager.getPoiCategoryByID((int) id));
            }
            return categories;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        } // Statements are closed in close() method
        return null;
    }

    /**
     * @param poiID Id of POI
     * @return Set of Tags
     */
    private Set<Tag> findDataByID(long poiID) {
        try {
            if (this.findDataByIDStatement == null) {
                this.findDataByIDStatement = this.db.prepare(DbConstants.FIND_DATA_BY_ID_STATEMENT);
            }

            this.findDataByIDStatement.reset();
            this.findDataByIDStatement.clear_bindings();

            this.findDataByIDStatement.bind(1, poiID);

            Set<Tag> tags = new HashSet<>();
            while (this.findDataByIDStatement.step()) {
                String data = this.findDataByIDStatement.column_string(1);
                tags.addAll(stringToTags(data));
            }
            return tags;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        } // Statements are closed in close() method
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
        Stmt stmt = null;
        try {
            int pSize = patterns == null ? 0 : patterns.size();
            stmt = this.db.prepare(AbstractPoiPersistenceManager.getSQLSelectString(filter, pSize, getPoiFileInfo().version));

            stmt.reset();
            stmt.clear_bindings();

            stmt.bind(1, bb.maxLatitude);
            stmt.bind(2, bb.maxLongitude);
            stmt.bind(3, bb.minLatitude);
            stmt.bind(4, bb.minLongitude);

            int i = 0; // i is only counted if pattern is not null
            if (pSize > 0) {
                for (Tag tag : patterns) {
                    if (tag == null) {
                        continue;
                    }
                    stmt.bind(5 + i, "%"
                            + (tag.key.equals("*") ? "" : (tag.key + "="))
                            + tag.value + "%");
                    i++;
                }
            }
            stmt.bind(5 + i, limit);

            while (stmt.step()) {
                long id = stmt.column_long(0);
                double lat = stmt.column_double(1);
                double lon = stmt.column_double(2);

                this.poi = new PointOfInterest(id, lat, lon, findDataByID(id), findCategoriesByID(id));
                this.ret.add(this.poi);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
            } catch (Exception e) {
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
        try {
            if (this.findLocByIDStatement == null) {
                this.findLocByIDStatement = this.db.prepare(DbConstants.FIND_LOCATION_BY_ID_STATEMENT);
            }

            this.findLocByIDStatement.reset();
            this.findLocByIDStatement.clear_bindings();

            this.findLocByIDStatement.bind(1, poiID);

            if (this.findLocByIDStatement.step()) {
                double lat = this.findLocByIDStatement.column_double(1);
                double lon = this.findLocByIDStatement.column_double(2);
                return new LatLong(lat, lon);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        } // Statements are closed in close() method
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
                this.insertPoiLocStatement = this.db.prepare(DbConstants.INSERT_INDEX_STATEMENT);
            }
            if (this.insertPoiDataStatement == null) {
                this.insertPoiDataStatement = this.db.prepare(DbConstants.INSERT_DATA_STATEMENT);
            }
            if (this.insertPoiCatStatement == null) {
                this.insertPoiCatStatement = this.db.prepare(DbConstants.INSERT_CATEGORY_MAP_STATEMENT);
            }

            this.db.exec("BEGIN;", null);

            for (PointOfInterest poi : pois) {
                // POI location
                this.insertPoiLocStatement.reset();
                this.insertPoiLocStatement.clear_bindings();

                this.insertPoiLocStatement.bind(1, poi.getId());
                this.insertPoiLocStatement.bind(2, poi.getLatitude());
                this.insertPoiLocStatement.bind(3, poi.getLatitude());
                this.insertPoiLocStatement.bind(4, poi.getLongitude());
                this.insertPoiLocStatement.bind(5, poi.getLongitude());
                this.insertPoiLocStatement.step();

                // POI data
                this.insertPoiDataStatement.reset();
                this.insertPoiDataStatement.clear_bindings();

                this.insertPoiDataStatement.bind(1, poi.getId());
                this.insertPoiDataStatement.bind(2, tagsToString(poi.getTags()));
                this.insertPoiDataStatement.step();

                // POI categories
                for (PoiCategory cat : poi.getCategories()) {
                    this.insertPoiCatStatement.reset();
                    this.insertPoiCatStatement.clear_bindings();

                    this.insertPoiCatStatement.bind(1, poi.getId());
                    this.insertPoiCatStatement.bind(2, cat.getID());
                    this.insertPoiCatStatement.step();
                }
            }

            this.db.exec("COMMIT;", null);
        } catch (Exception e) {
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
        int version = getPoiFileInfo().version;
        try {
            if (version < 2) {
                this.isValidDBStatement = this.db.prepare(DbConstants.VALID_DB_STATEMENT_V1);
            } else {
                this.isValidDBStatement = this.db.prepare(DbConstants.VALID_DB_STATEMENT);
            }
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

        if (version < 2) {
            return numTables == DbConstants.NUMBER_OF_TABLES_V1;
        } else {
            return numTables == DbConstants.NUMBER_OF_TABLES;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readPoiFileInfo() {
        PoiFileInfoBuilder poiFileInfoBuilder = new PoiFileInfoBuilder();

        try {
            if (this.metadataStatement == null) {
                this.metadataStatement = this.db.prepare(DbConstants.FIND_METADATA_STATEMENT);
            }

            while (this.metadataStatement.step()) {
                String name = this.metadataStatement.column_string(0);

                switch (name) {
                    case DbConstants.METADATA_BOUNDS:
                        String bounds = this.metadataStatement.column_string(1);
                        if (bounds != null) {
                            poiFileInfoBuilder.bounds = BoundingBox.fromString(bounds);
                        }
                        break;
                    case DbConstants.METADATA_COMMENT:
                        poiFileInfoBuilder.comment = this.metadataStatement.column_string(1);
                        break;
                    case DbConstants.METADATA_DATE:
                        poiFileInfoBuilder.date = this.metadataStatement.column_long(1);
                        break;
                    case DbConstants.METADATA_LANGUAGE:
                        poiFileInfoBuilder.language = this.metadataStatement.column_string(1);
                        break;
                    case DbConstants.METADATA_VERSION:
                        poiFileInfoBuilder.version = this.metadataStatement.column_int(1);
                        break;
                    case DbConstants.METADATA_WAYS:
                        poiFileInfoBuilder.ways = Boolean.parseBoolean(this.metadataStatement.column_string(1));
                        break;
                    case DbConstants.METADATA_WRITER:
                        poiFileInfoBuilder.writer = this.metadataStatement.column_string(1);
                        break;
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
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
                this.deletePoiLocStatement = this.db.prepare(DbConstants.DELETE_INDEX_STATEMENT);
            }
            if (this.deletePoiDataStatement == null) {
                this.deletePoiDataStatement = this.db.prepare(DbConstants.DELETE_DATA_STATEMENT);
            }
            if (this.deletePoiCatStatement == null) {
                this.deletePoiCatStatement = this.db.prepare(DbConstants.DELETE_CATEGORY_MAP_STATEMENT);
            }

            this.deletePoiLocStatement.reset();
            this.deletePoiLocStatement.clear_bindings();
            this.deletePoiDataStatement.reset();
            this.deletePoiDataStatement.clear_bindings();
            this.deletePoiCatStatement.reset();
            this.deletePoiCatStatement.clear_bindings();

            this.db.exec("BEGIN;", null);

            this.deletePoiLocStatement.bind(1, poi.getId());
            this.deletePoiDataStatement.bind(1, poi.getId());
            this.deletePoiCatStatement.bind(1, poi.getId());

            this.deletePoiLocStatement.step();
            this.deletePoiDataStatement.step();
            this.deletePoiCatStatement.step();

            this.db.exec("COMMIT;", null);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }
}
