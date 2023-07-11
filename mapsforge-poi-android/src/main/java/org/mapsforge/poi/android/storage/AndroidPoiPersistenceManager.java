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

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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

    private SQLiteDatabase db = null;

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
        try {
            this.db = SQLiteDatabase.openDatabase(dbFilePath, null, readOnly ? SQLiteDatabase.OPEN_READONLY : SQLiteDatabase.CREATE_IF_NECESSARY);
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
    private void createTables() {
        this.db.execSQL(DbConstants.DROP_METADATA_STATEMENT);
        //this.db.execSQL(DbConstants.DROP_INDEX_IDX_STATEMENT);
        this.db.execSQL(DbConstants.DROP_INDEX_STATEMENT);
        this.db.execSQL(DbConstants.DROP_CATEGORY_MAP_STATEMENT);
        //this.db.execSQL(DbConstants.DROP_DATA_IDX_STATEMENT);
        this.db.execSQL(DbConstants.DROP_DATA_STATEMENT);
        this.db.execSQL(DbConstants.DROP_CATEGORIES_STATEMENT);

        this.db.execSQL(DbConstants.CREATE_CATEGORIES_STATEMENT);
        this.db.execSQL(DbConstants.CREATE_DATA_STATEMENT);
        //this.db.execSQL(DbConstants.CREATE_DATA_IDX_STATEMENT);
        this.db.execSQL(DbConstants.CREATE_CATEGORY_MAP_STATEMENT);
        this.db.execSQL(DbConstants.CREATE_INDEX_STATEMENT);
        //this.db.execSQL(DbConstants.CREATE_INDEX_IDX_STATEMENT);
        this.db.execSQL(DbConstants.CREATE_METADATA_STATEMENT);
    }

    /**
     * @param poiID Id of POI
     * @return Set of PoiCategories
     */
    private Set<PoiCategory> findCategoriesByID(long poiID) throws UnknownPoiCategoryException {
        Cursor cursor = null;
        try {
            Set<PoiCategory> categories = new HashSet<>();
            String sql = DbConstants.FIND_CATEGORIES_BY_ID_STATEMENT;
            cursor = this.db.rawQuery(sql, new String[]{String.valueOf(poiID)});
            while (cursor.moveToNext()) {
                long id = cursor.getLong(1);
                categories.add(this.categoryManager.getPoiCategoryByID((int) id));
            }
            return categories;
        } finally {
            try {
                if (cursor != null) {
                    cursor.close();
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
        }
    }

    /**
     * @param poiID Id of POI
     * @return Set of Tags
     */
    private Set<Tag> findDataByID(long poiID) {
        Cursor cursor = null;
        try {
            Set<Tag> tags = new HashSet<>();
            cursor = this.db.rawQuery(DbConstants.FIND_DATA_BY_ID_STATEMENT, new String[]{String.valueOf(poiID)});
            while (cursor.moveToNext()) {
                String data = cursor.getString(1);
                tags.addAll(stringToTags(data));
            }
            return tags;
        } finally {
            try {
                if (cursor != null) {
                    cursor.close();
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
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
        Cursor cursor = null;
        try {
            int pSize = patterns == null ? 0 : patterns.size();
            String sql = AbstractPoiPersistenceManager.getSQLSelectString(filter, pSize, orderBy);

            List<String> selectionArgs = new ArrayList<>();
            selectionArgs.add(String.valueOf(bb.maxLatitude));
            selectionArgs.add(String.valueOf(bb.maxLongitude));
            selectionArgs.add(String.valueOf(bb.minLatitude));
            selectionArgs.add(String.valueOf(bb.minLongitude));
            if (pSize > 0) {
                for (Tag tag : patterns) {
                    if (tag == null) {
                        continue;
                    }
                    selectionArgs.add((tag.key.equals("*") ? "" : (tag.key + "=")) + tag.value);
                }
            }
            selectionArgs.add(String.valueOf(limit));

            cursor = this.db.rawQuery(sql, selectionArgs.toArray(new String[0]));
            while (cursor.moveToNext()) {
                long id = cursor.getLong(0);
                double lat = cursor.getDouble(1);
                double lon = cursor.getDouble(2);
                String data = cursor.getString(3);

                this.poi = new PointOfInterest(id, lat, lon, stringToTags(data), findCategories ? findCategoriesByID(id) : null);
                this.ret.add(this.poi);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        } finally {
            try {
                if (cursor != null) {
                    cursor.close();
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
        Cursor cursor = null;
        try {
            cursor = this.db.rawQuery(DbConstants.FIND_LOCATION_BY_ID_STATEMENT, new String[]{String.valueOf(poiID)});
            if (cursor.moveToNext()) {
                double lat = cursor.getDouble(1);
                double lon = cursor.getDouble(2);
                return new LatLong(lat, lon);
            }
        } finally {
            try {
                if (cursor != null) {
                    cursor.close();
                }
            } catch (Exception e) {
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
        try {
            LatLong latlong = findLocationByID(poiID);
            if (latlong != null) {
                this.poi = new PointOfInterest(poiID, latlong.latitude, latlong.longitude,
                        findDataByID(poiID), findCategoriesByID(poiID));
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
                this.db.execSQL(DbConstants.INSERT_INDEX_STATEMENT, new String[]{
                        String.valueOf(poi.getId()),
                        String.valueOf(poi.getLatitude()),
                        String.valueOf(poi.getLongitude())
                });

                // POI data
                this.db.execSQL(DbConstants.INSERT_DATA_STATEMENT, new String[]{
                        String.valueOf(poi.getId()),
                        tagsToString(poi.getTags())
                });

                // POI categories
                for (PoiCategory cat : poi.getCategories()) {
                    this.db.execSQL(DbConstants.INSERT_CATEGORY_MAP_STATEMENT, new String[]{
                            String.valueOf(poi.getId()),
                            String.valueOf(cat.getID())
                    });
                }
            }
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
        // Check for table names
        // TODO Is it necessary to get the tables meta data as well?
        int numTables = 0;
        Cursor cursor = null;
        try {
            String sql = DbConstants.VALID_DB_STATEMENT;
            cursor = this.db.rawQuery(sql, null);
            if (cursor.moveToNext()) {
                numTables = cursor.getInt(0);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        } finally {
            try {
                if (cursor != null) {
                    cursor.close();
                }
            } catch (Exception e) {
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

        Cursor cursor = null;
        try {
            cursor = this.db.rawQuery(DbConstants.FIND_METADATA_STATEMENT, null);
            while (cursor.moveToNext()) {
                String name = cursor.getString(0);

                switch (name) {
                    case DbConstants.METADATA_BOUNDS:
                        String bounds = cursor.getString(1);
                        if (bounds != null) {
                            poiFileInfoBuilder.bounds = BoundingBox.fromString(bounds);
                        }
                        break;
                    case DbConstants.METADATA_COMMENT:
                        poiFileInfoBuilder.comment = cursor.getString(1);
                        break;
                    case DbConstants.METADATA_DATE:
                        poiFileInfoBuilder.date = cursor.getLong(1);
                        break;
                    case DbConstants.METADATA_LANGUAGE:
                        poiFileInfoBuilder.language = cursor.getString(1);
                        break;
                    case DbConstants.METADATA_VERSION:
                        poiFileInfoBuilder.version = cursor.getInt(1);
                        break;
                    case DbConstants.METADATA_WAYS:
                        poiFileInfoBuilder.ways = Boolean.parseBoolean(cursor.getString(1));
                        break;
                    case DbConstants.METADATA_WRITER:
                        poiFileInfoBuilder.writer = cursor.getString(1);
                        break;
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        } finally {
            try {
                if (cursor != null) {
                    cursor.close();
                }
            } catch (Exception e) {
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
            this.db.execSQL(DbConstants.DELETE_INDEX_STATEMENT, new String[]{String.valueOf(poi.getId())});
            this.db.execSQL(DbConstants.DELETE_DATA_STATEMENT, new String[]{String.valueOf(poi.getId())});
            this.db.execSQL(DbConstants.DELETE_CATEGORY_MAP_STATEMENT, new String[]{String.valueOf(poi.getId())});
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }
}
