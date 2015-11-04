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

import java.util.ArrayList;
import java.util.Collection;

/**
 * Whitelist category filter that accepts all categories and their sub-categories in the white list.
 */
public class WhitelistPoiCategoryFilter implements PoiCategoryFilter {
	/**
	 * Whitelist containing all elements (and implicitly their child elements) that will be accepted by
	 * this filter.
	 */
	protected final ArrayList<PoiCategory> whiteList;

	/**
	 * Default constructor.
	 */
	public WhitelistPoiCategoryFilter() {
		whiteList = new ArrayList<PoiCategory>();
	}

	/**
	 * Adds a POI category to the white list. A parent category (e.g. amenity_food) automatically white
	 * lists its sub-categories. (Example: If amenity_food is in the whitelist and fast_food is a child
	 * category of amenity_food, then the filter will also accept POIs of category fast_food.)
	 * 
	 * @param category
	 *            The category to be added to the white list.
	 */
	@Override
	public void addCategory(PoiCategory category) {
		this.whiteList.add(category);
	}

	@Override
	public boolean isAcceptedCategory(PoiCategory category) {
		// Found category
		if (this.whiteList.contains(category)) {
			return true;
		}

		if (category.getParent() != null) {
			return isAcceptedCategory(category.getParent());
		}

		return false;

	}

	/**
	 * @return All elements in the whitelist, but not their child elements.
	 */
	@Override
	public Collection<PoiCategory> getAcceptedCategories() {
		// TODO Should the sub categories also be included here?
		return this.whiteList;
	}
}
