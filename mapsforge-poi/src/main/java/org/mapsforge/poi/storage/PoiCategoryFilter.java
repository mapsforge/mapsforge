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
package org.mapsforge.poi.storage;

import java.util.Collection;

/**
 * Interface for filtering accepted POIs based on their tag.
 */
public interface PoiCategoryFilter {
    /**
     * Adds a category to the whitelist.
     *
     * @param category The category to be added.
     */
    void addCategory(PoiCategory category);

    /**
     * @return Set of all categories that are accepted by this filter.
     */
    Collection<PoiCategory> getAcceptedCategories();

    /**
     * Returns a set of top-level categories for all accepted categories. These are all accepted
     * categories whose parent category is null or not an accepted category.
     *
     * @return Set of top-level categories for all accepted categories.
     */
    Collection<PoiCategory> getAcceptedSuperCategories();

    /**
     * Returns true if a POI's category is accepted and therefore shall be added to the poi file.
     *
     * @param category The POI's category.
     * @return true if a POI's category is accepted and therefore shall be added to the poi file.
     */
    boolean isAcceptedCategory(PoiCategory category);
}
