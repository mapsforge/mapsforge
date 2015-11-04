/*
 * Copyright 2010, 2011 mapsforge.org
 * Copyright 2010, 2011 Karsten Groll
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
package org.mapsforge.storage.poi;

import java.util.Collection;

/**
 * Interface for filtering accepted POIs based on their tag.
 */
public interface PoiCategoryFilter {
	/**
	 * Returns true if a POIs category is accepted and therefore shall be added to the map file.
	 * 
	 * @param category
	 *            The POI's category.
	 * @return true if a POIs category is accepted and therefore shall be added to the map file.
	 */
	boolean isAcceptedCategory(PoiCategory category);

	/**
	 * Adds a category to the white list.
	 * 
	 * @param category
	 *            The category to be added.
	 */
	void addCategory(PoiCategory category);

	/**
	 * 
	 * @return Set of all categories that are accepted by this filter.
	 */
	Collection<PoiCategory> getAcceptedCategories();
}
