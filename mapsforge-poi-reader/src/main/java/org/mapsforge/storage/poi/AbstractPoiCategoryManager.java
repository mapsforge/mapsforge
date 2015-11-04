/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 * Copyright 2010, 2011, 2012 Karsten Groll
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

import java.util.TreeMap;

/**
 * Abstract implementation for the {@link PoiCategoryManager} interface. This implementation provides functionality for
 * getting categories by their ID / name.
 */
public class AbstractPoiCategoryManager implements PoiCategoryManager {
	/**
	 * The hierarchies root category.
	 */
	protected PoiCategory rootCategory = null;
	/**
	 * Maps category IDs to categories.
	 */
	protected TreeMap<Integer, PoiCategory> categoryMap = null;

	@Override
	public PoiCategory getPoiCategoryByID(int id) throws UnknownPoiCategoryException {
		if (this.categoryMap.get(new Integer(id)) == null) {
			throw new UnknownPoiCategoryException();
		}

		return this.categoryMap.get(new Integer(id));
	}

	@Override
	public PoiCategory getPoiCategoryByTitle(String title) throws UnknownPoiCategoryException {
		for (Integer key : this.categoryMap.keySet()) {
			if (this.categoryMap.get(key).getTitle().equalsIgnoreCase(title)) {

				return this.categoryMap.get(key);
			}
		}

		throw new UnknownPoiCategoryException();
	}

	@Override
	public PoiCategory getRootCategory() throws UnknownPoiCategoryException {
		if (this.rootCategory == null) {
			throw new UnknownPoiCategoryException();
		}

		return this.rootCategory;
	}
}
