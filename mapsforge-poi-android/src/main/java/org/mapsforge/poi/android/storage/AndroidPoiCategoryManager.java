/*
 * Copyright 2010, 2011 mapsforge.org
 * Copyright 2010, 2011 Karsten Groll
 * Copyright 2015-2018 devemux86
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
import org.mapsforge.poi.storage.*;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A {@link PoiCategoryManager} implementation using a SQLite database via wrapper.
 * <p/>
 * This class can only be used within Android.
 */
class AndroidPoiCategoryManager extends AbstractPoiCategoryManager {
    private static final Logger LOGGER = Logger.getLogger(AndroidPoiCategoryManager.class.getName());

    /**
     * @param db SQLite database object. (Using SQLite wrapper for Android).
     */
    AndroidPoiCategoryManager(SQLiteDatabase db) {
        this.categoryMap = new TreeMap<>();

        try {
            loadCategories(db);
        } catch (UnknownPoiCategoryException e) {
            LOGGER.log(Level.SEVERE, e.toString(), e);
        }
    }

    /**
     * Load categories from database.
     *
     * @throws UnknownPoiCategoryException if a category cannot be retrieved by its ID or unique name.
     */
    private void loadCategories(SQLiteDatabase db) throws UnknownPoiCategoryException {
        // Maximum ID (for root node)
        int maxID = 0;

        // Maps categories to their parent IDs
        Map<PoiCategory, Integer> parentMap = new HashMap<>();

        Cursor cursor = null;
        try {
            cursor = db.rawQuery(SELECT_STATEMENT, null);
            while (cursor.moveToNext()) {
                // Column values
                int categoryID = cursor.getInt(0);
                String categoryTitle = cursor.getString(1);
                int categoryParentID = cursor.getInt(2);

                PoiCategory pc = new DoubleLinkedPoiCategory(categoryTitle, null, categoryID);
                this.categoryMap.put(categoryID, pc);

                // category --> parent ID
                parentMap.put(pc, categoryParentID);

                // check for root node
                if (categoryID > maxID) {
                    maxID = categoryID;
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.toString(), e);
        } finally {
            try {
                if (cursor != null) {
                    cursor.close();
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, e.toString(), e);
            }
        }

        // Set root category and remove it from parents map
        this.rootCategory = getPoiCategoryByID(maxID);
        parentMap.remove(this.rootCategory);

        // Assign parent categories
        for (PoiCategory c : parentMap.keySet()) {
            c.setParent(getPoiCategoryByID(parentMap.get(c)));
        }
    }
}
