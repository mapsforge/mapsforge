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

/**
 * A category manager is a storage for {@link PoiCategory}s. It manages the categories' hierarchy in
 * a tree structure. Adding and deleting categories should be done via this interface.
 */
public interface PoiCategoryManager {
	/**
	 * Returns a category given by its ID or throws an exception if this category does not exist.
	 *
	 * @param id
	 *            The category's ID.
	 * @return The category having this ID or null.
	 * @throws UnknownPoiCategoryException
	 *             if the category has not been added to the {@link PoiCategoryManager}.
	 */
	PoiCategory getPoiCategoryByID(int id) throws UnknownPoiCategoryException;

	/**
	 * Returns a category given by unique title or throws an exception if this category does not
	 * exist.
	 *
	 * @param title
	 *            The category's title
	 * @return The category c with <code>c.title.equalsIgnoreCase(title)</code>.
	 * @throws UnknownPoiCategoryException
	 *             if the category has not been added to the {@link PoiCategoryManager}.
	 */
	PoiCategory getPoiCategoryByTitle(String title) throws UnknownPoiCategoryException;

	/**
	 * Returns the category tree's root.
	 *
	 * @return The tree's root category or null if the tree is empty.
	 * @throws UnknownPoiCategoryException
	 *             if the category has not been added to the {@link PoiCategoryManager}.
	 */
	PoiCategory getRootCategory() throws UnknownPoiCategoryException;
}
