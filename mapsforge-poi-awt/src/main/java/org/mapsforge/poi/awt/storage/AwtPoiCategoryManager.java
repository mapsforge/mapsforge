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

import org.mapsforge.poi.storage.AbstractPoiCategoryManager;
import org.mapsforge.poi.storage.DoubleLinkedPoiCategory;
import org.mapsforge.poi.storage.PoiCategory;
import org.mapsforge.poi.storage.PoiCategoryManager;
import org.mapsforge.poi.storage.UnknownPoiCategoryException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A {@link PoiCategoryManager} implementation using a SQLite database via JDBC.
 * <p/>
 * This class can only be used within AWT.
 */
class AwtPoiCategoryManager extends AbstractPoiCategoryManager {
	private static final Logger LOGGER = Logger.getLogger(AwtPoiCategoryManager.class.getName());

	/**
	 * @param conn
	 *			SQLite connection. (Using SQLite JDBC for AWT).
	 */
	AwtPoiCategoryManager(Connection conn) {
		this.categoryMap = new TreeMap<>();

		try {
			loadCategories(conn);
		} catch (UnknownPoiCategoryException e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	/**
	 * Load categories from database.
	 *
	 * @throws UnknownPoiCategoryException
	 *			 if a category cannot be retrieved by its ID or unique name.
	 */
	private void loadCategories(Connection conn) throws UnknownPoiCategoryException {
		// Maximum ID (for root node)
		int maxID = 0;

		// Maps categories to their parent IDs
		Map<PoiCategory, Integer> parentMap = new HashMap<>();

		try {
			PreparedStatement stmt = conn.prepareStatement(SELECT_STATEMENT);
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				// Column values
				int categoryID = rs.getInt(1);
				String categoryTitle = rs.getString(2);
				int categoryParentID = rs.getInt(3);

				PoiCategory pc = new DoubleLinkedPoiCategory(categoryTitle, null, categoryID);
				this.categoryMap.put(categoryID, pc);

				// category --> parent ID
				parentMap.put(pc, categoryParentID);

				// check for root node
				if (categoryID > maxID) {
					maxID = categoryID;
				}
			}
			rs.close();
			stmt.close();
		} catch (SQLException e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
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
