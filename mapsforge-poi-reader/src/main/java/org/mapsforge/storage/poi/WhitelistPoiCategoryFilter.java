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
package org.mapsforge.storage.poi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

/**
 * Whitelist category filter that accepts all categories and their sub-categories in the whitelist.
 * <p/>
 * <strong>Warning: This class is bugged, there is an endless loop in getAcceptedCategories.</strong>
 */
public class WhitelistPoiCategoryFilter implements PoiCategoryFilter {
	/**
	 * Whitelist containing all elements (and implicitly their child elements) that will be accepted
	 * by this filter.
	 */
	private final List<PoiCategory> whiteList;

	/**
	 * Default constructor.
	 */
	public WhitelistPoiCategoryFilter() {
		this.whiteList = new ArrayList<>();
	}

	/**
	 * Adds a POI category to the whitelist. A parent category (e.g. amenity_food) automatically white
	 * lists its sub-categories. (Example: If amenity_food is in the whitelist and fast_food is a child
	 * category of amenity_food, then the filter will also accept POIs of category fast_food).
	 *
	 * @param category
	 *            The category to be added to the whitelist.
	 */
	@Override
	public void addCategory(PoiCategory category) {
		this.whiteList.add(category);
	}

	/**
	 * @return All elements in the whitelist, including their children.
	 */
	@Override
	public Collection<PoiCategory> getAcceptedCategories() {
		// Use a Set in case of joint sub-trees
		Collection<PoiCategory> ret = new HashSet<>();
		Stack<PoiCategory> stack = new Stack<>();

		// Assumption: whiteList sub-trees are disjoint; otherwise this algorithm is not optimal.
		for (PoiCategory wlCategory : this.whiteList) {
			stack.push(wlCategory);
		}

		while (!stack.isEmpty()) {
			PoiCategory c = stack.pop();
			ret.add(c);

			for (PoiCategory child : c.getChildren()) {
				stack.push(child);
			}
		}

		return ret;
	}

	@Override
	public Collection<PoiCategory> getAcceptedSuperCategories() {
		Collection<PoiCategory> ret = new HashSet<>();
		Collection<PoiCategory> acceptedCategories = this.getAcceptedCategories();

		for (PoiCategory c : this.whiteList) {
			// Check if category is a super category (= root of an accepted category's sub-tree)
			if (c.getParent() == null || !acceptedCategories.contains(c.getParent())) {
				ret.add(c);
			}
		}

		return ret;
	}

	@Override
	public boolean isAcceptedCategory(PoiCategory category) {
		// Found category
		if (this.whiteList.contains(category)) {
			return true;
		}

		// Check if parent category is accepted
		if (category.getParent() != null) {
			return isAcceptedCategory(category.getParent());
		}

		// Neither this, nor parent
		return false;
	}
}
