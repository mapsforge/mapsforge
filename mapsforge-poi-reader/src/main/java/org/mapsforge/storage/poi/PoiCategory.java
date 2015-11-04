/*
 * Copyright 2010, 2011 mapsforge.org
 * Copyright 2010, 2011 weise
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
 * This class represents a category for {@link PointOfInterest}. Every {@link PoiCategory} should have a
 * unique title so that for two {@link PoiCategory}s a and b a.equals(b) if and only if
 * a.title.equalsIgnoreCase(b.title).
 */
public interface PoiCategory {
	/**
	 * @return title of this category.
	 */
	String getTitle();

	/**
	 * @return parent category of this category or null if this category has no parent.
	 */
	PoiCategory getParent();

	/**
	 * Sets the node's parent node.
	 * 
	 * @param parent
	 *            The category node to be set as parent.
	 */
	void setParent(PoiCategory parent);

	/**
	 * @return All child categories of the category or null if this category has no children.
	 */
	Collection<PoiCategory> getChildren();

	/**
	 * 
	 * @return The category's id.
	 */
	int getID();
}
