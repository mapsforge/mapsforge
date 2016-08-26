/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 * Copyright 2010, 2011, 2012 Karsten Groll
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
import java.util.HashSet;
import java.util.Set;

/**
 * A category filter that accepts all categories added to it. Unless
 * {@link WhitelistPoiCategoryFilter} no child categories of an added category are accepted.
 */
public class ExactMatchPoiCategoryFilter implements PoiCategoryFilter {
    private final Set<PoiCategory> whiteList;

    /**
     * Default constructor.
     */
    public ExactMatchPoiCategoryFilter() {
        this.whiteList = new HashSet<>();
    }

    @Override
    public void addCategory(PoiCategory category) {
        this.whiteList.add(category);
    }

    @Override
    public Collection<PoiCategory> getAcceptedCategories() {
        return this.whiteList;
    }

    @Override
    public Collection<PoiCategory> getAcceptedSuperCategories() {
        return this.whiteList;
    }

    @Override
    public boolean isAcceptedCategory(PoiCategory category) {
        return this.whiteList.contains(category);
    }
}
