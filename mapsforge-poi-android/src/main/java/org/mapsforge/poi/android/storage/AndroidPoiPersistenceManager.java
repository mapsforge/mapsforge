/*
 * Copyright 2010, 2011 mapsforge.org
 * Copyright 2010, 2011 Karsten Groll
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
package org.mapsforge.poi.android.storage;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.poi.storage.AbstractPoiPersistenceManager;
import org.mapsforge.poi.storage.DbConstants;
import org.mapsforge.poi.storage.PoiCategoryFilter;
import org.mapsforge.poi.storage.PoiFileInfo;
import org.mapsforge.poi.storage.PoiFileInfoBuilder;
import org.mapsforge.poi.storage.PoiPersistenceManager;
import org.mapsforge.poi.storage.PointOfInterest;
import org.mapsforge.poi.storage.UnknownPoiCategoryException;

import java.util.Collection;
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

    private Stmt findByIDStatement = null;
    private Stmt insertPoiStatement1 = null;
    private Stmt insertPoiStatement2 = null;
    private Stmt deletePoiStatement1 = null;
    private Stmt deletePoiStatement2 = null;
    private Stmt isValidDBStatement = null;
    private Stmt metadataStatement = null;

    /**
     * @param dbFilePath Path to SQLite file containing POI data.
     * @param create     If the file does not exist it may be created and filled.
     */
    AndroidPoiPersistenceManager(String dbFilePath, boolean create) {
        super();

        // Open / create POI database
        createOrOpenDBFile(dbFilePath, create);

        // Load categories from database
        this.categoryManager = new AndroidPoiCategoryManager(this.db);

        // Queries
        try {
            // Finds a POI by its unique ID
            this.findByIDStatement = this.db.prepare(DbConstants.FIND_BY_ID_STATEMENT);

            // Inserts a POI into index and adds its data
            this.insertPoiStatement1 = this.db.prepare(DbConstants.INSERT_INDEX_STATEMENT);
            this.insertPoiStatement2 = this.db.prepare(DbConstants.INSERT_DATA_STATEMENT);

            // Deletes a POI given by its ID
            this.deletePoiStatement1 = this.db.prepare(DbConstants.DELETE_INDEX_STATEMENT);
            this.deletePoiStatement2 = this.db.prepare(DbConstants.DELETE_DATA_STATEMENT);

            // Metadata
            this.metadataStatement = this.db.prepare(DbConstants.FIND_METADATA_STATEMENT);
        } catch (Exception e) {
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
    }

    /**
     * @param dbFilePath Path to SQLite file containing POI data.
     * @param create     If the file does not exist it may be created and filled.
     */
    private void createOrOpenDBFile(String dbFilePath, boolean create) {
        // Open file
        this.db = new Database();
        try {
            this.db.open(dbFilePath, Constants.SQLITE_OPEN_READWRITE | Constants.SQLITE_OPEN_CREATE);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }

        // Create file
        if (!isValidDataBase() && create) {
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
        this.db.exec(DbConstants.DROP_DATA_STATEMENT, null);
        this.db.exec(DbConstants.DROP_CATEGORIES_STATEMENT, null);

        this.db.exec(DbConstants.CREATE_CATEGORIES_STATEMENT, null);
        this.db.exec(DbConstants.CREATE_DATA_STATEMENT, null);
        this.db.exec(DbConstants.CREATE_INDEX_STATEMENT, null);
        this.db.exec(DbConstants.CREATE_METADATA_STATEMENT, null);

        this.db.exec("COMMIT;", null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<PointOfInterest> findInRect(BoundingBox bb, PoiCategoryFilter filter,
                                                  String pattern, int limit) {
        // Clear previous results
        this.ret.clear();

        // Query
        try {
            Stmt stmt = this.db.prepare(AbstractPoiPersistenceManager.getSQLSelectString(filter, pattern));

            stmt.reset();
            stmt.clear_bindings();

            stmt.bind(1, bb.maxLatitude);
            stmt.bind(2, bb.maxLongitude);
            stmt.bind(3, bb.minLatitude);
            stmt.bind(4, bb.minLongitude);
            if (pattern != null) {
                stmt.bind(5, pattern);
            }
            stmt.bind(pattern != null ? 6 : 5, limit);

            while (stmt.step()) {
                long id = stmt.column_long(0);
                double lat = stmt.column_double(1);
                double lon = stmt.column_double(2);
                String data = stmt.column_string(3);
                int categoryID = stmt.column_int(4);

                try {
                    this.poi = new PointOfInterest(id, lat, lon, data, this.categoryManager.getPoiCategoryByID(categoryID));
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

    /**
     * {@inheritDoc}
     */
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
                    this.poi = new PointOfInterest(id, lat, lon, data, this.categoryManager.getPoiCategoryByID(categoryID));
                } catch (UnknownPoiCategoryException e) {
                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                }
            }
        } catch (Exception e) {
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

        return poiFileInfoBuilder.build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void insertPointOfInterest(PointOfInterest poi) {
        try {
            this.insertPoiStatement1.reset();
            this.insertPoiStatement1.clear_bindings();
            this.insertPoiStatement2.reset();
            this.insertPoiStatement2.clear_bindings();

            this.db.exec("BEGIN;", null);

            this.insertPoiStatement1.bind(1, poi.getId());
            this.insertPoiStatement1.bind(2, poi.getLatitude());
            this.insertPoiStatement1.bind(3, poi.getLatitude());
            this.insertPoiStatement1.bind(4, poi.getLongitude());
            this.insertPoiStatement1.bind(5, poi.getLongitude());

            this.insertPoiStatement2.bind(1, poi.getId());
            this.insertPoiStatement2.bind(2, poi.getData());
            this.insertPoiStatement2.bind(3, poi.getCategory().getID());

            this.insertPoiStatement1.step();
            this.insertPoiStatement2.step();

            this.db.exec("COMMIT;", null);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void insertPointsOfInterest(Collection<PointOfInterest> pois) {
        try {
            this.insertPoiStatement1.reset();
            this.insertPoiStatement1.clear_bindings();
            this.insertPoiStatement2.reset();
            this.insertPoiStatement2.clear_bindings();

            this.db.exec("BEGIN;", null);

            for (PointOfInterest poi : pois) {
                this.insertPoiStatement1.bind(1, poi.getId());
                this.insertPoiStatement1.bind(2, poi.getLatitude());
                this.insertPoiStatement1.bind(3, poi.getLatitude());
                this.insertPoiStatement1.bind(4, poi.getLongitude());
                this.insertPoiStatement1.bind(5, poi.getLongitude());

                this.insertPoiStatement2.bind(1, poi.getId());
                this.insertPoiStatement2.bind(2, poi.getData());
                this.insertPoiStatement2.bind(3, poi.getCategory().getID());

                this.insertPoiStatement1.step();
                this.insertPoiStatement2.step();
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
    public boolean isValidDataBase() {
        try {
            this.isValidDBStatement = this.db.prepare(DbConstants.VALID_DB_STATEMENT);
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

        return numTables == DbConstants.NUMBER_OF_TABLES;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removePointOfInterest(PointOfInterest poi) {
        try {
            this.deletePoiStatement1.reset();
            this.deletePoiStatement1.clear_bindings();
            this.deletePoiStatement2.reset();
            this.deletePoiStatement2.clear_bindings();

            this.db.exec("BEGIN;", null);

            this.deletePoiStatement1.bind(1, poi.getId());
            this.deletePoiStatement2.bind(1, poi.getId());

            this.deletePoiStatement1.step();
            this.deletePoiStatement2.step();

            this.db.exec("COMMIT;", null);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }
}
