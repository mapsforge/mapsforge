/*
 * Copyright 2010, 2011 mapsforge.org
 * Copyright 2010, 2011 weise
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
package org.mapsforge.poi.storage;

import java.util.Collection;

/**
 * This class represents a category for {@link PointOfInterest}. Every {@link PoiCategory} should
 * have a unique title so that for two {@link PoiCategory}s a and b a.equals(b) if and only if
 * a.title.equalsIgnoreCase(b.title).
 */
public interface PoiCategory {
    /**
     * @return All child categories of the category, including their children.
     */
    Collection<PoiCategory> deepChildren();

    /**
     * @return All child categories of the category.
     */
    Collection<PoiCategory> getChildren();

    /**
     * @return The category's id.
     */
    int getID();

    /**
     * @return The parent category of this category or null if this category has no parent.
     */
    PoiCategory getParent();

    /**
     * @return The title of this category.
     */
    String getTitle();

    /**
     * Sets the category's parent category.
     *
     * @param parent The category to be set as parent.
     */
    void setParent(PoiCategory parent);
}
