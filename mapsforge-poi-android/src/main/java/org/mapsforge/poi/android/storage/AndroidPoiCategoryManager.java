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

import org.mapsforge.poi.storage.AbstractPoiCategoryManager;
import org.mapsforge.poi.storage.DoubleLinkedPoiCategory;
import org.mapsforge.poi.storage.PoiCategory;
import org.mapsforge.poi.storage.PoiCategoryManager;
import org.mapsforge.poi.storage.UnknownPoiCategoryException;
import org.sqlite.android.Database;
import org.sqlite.android.Exception;
import org.sqlite.android.Stmt;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * A {@link PoiCategoryManager} implementation using a SQLite database via wrapper.
 * <p/>
 * This class can only be used within Android.
 */
class AndroidPoiCategoryManager extends AbstractPoiCategoryManager {
	private Stmt loadCategoriesStatement = null;

	/**
	 * @param db
	 *            SQLite database object. (Using SQLite wrapper for Android).
	 */
	AndroidPoiCategoryManager(Database db) {
		// Log.d(LOG_TAG, "Initializing category manager");
		this.categoryMap = new TreeMap<>();

		try {
			this.loadCategoriesStatement = db.prepare("SELECT * FROM poi_categories ORDER BY id ASC;");
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			loadCategories();
		} catch (UnknownPoiCategoryException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Load categories from database.
	 *
	 * @throws UnknownPoiCategoryException
	 *             if a category cannot be retrieved by its ID or unique name.
	 */
	private void loadCategories() throws UnknownPoiCategoryException {
		// Maximum ID (for root node)
		int maxID = 0;

		// Maps categories to their parent IDs
		Map<PoiCategory, Integer> parentMap = new HashMap<>();

		try {
			this.loadCategoriesStatement.reset();
			while (this.loadCategoriesStatement.step()) {
				// Column values
				int categoryID = this.loadCategoriesStatement.column_int(0);
				String categoryTitle = this.loadCategoriesStatement.column_string(1);
				int categoryParentID = this.loadCategoriesStatement.column_int(2);

				// Log.d(LOG_TAG, categoryID + "|" + categoryTitle + "|" + categoryParentID);

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
			e.printStackTrace();
		}

		// Set root category
		this.rootCategory = getPoiCategoryByID(maxID);

		// Assign parent categories;
		for (PoiCategory c : parentMap.keySet()) {
			c.setParent(getPoiCategoryByID(parentMap.get(c)));
		}
	}
}
