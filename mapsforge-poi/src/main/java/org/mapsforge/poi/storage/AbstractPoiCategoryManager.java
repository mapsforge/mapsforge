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

import org.mapsforge.core.model.Tag;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Abstract implementation for the {@link PoiCategoryManager} interface. This implementation
 * provides functionality for getting categories by their ID / name.
 */
public abstract class AbstractPoiCategoryManager implements PoiCategoryManager {
    protected static final String SELECT_STATEMENT = "SELECT * FROM poi_categories ORDER BY id;";

    /**
     * The hierarchies root category.
     */
    protected PoiCategory rootCategory = null;
    /**
     * Contains the immutable metadata of a POI file.
     */
    protected final PoiFileInfo poiFileInfo;
    /**
     * Maps category IDs to categories.
     */
    protected final Map<Integer, PoiCategory> categoryMap;

    protected AbstractPoiCategoryManager(PoiFileInfo poiFileInfo) {
        this.poiFileInfo = poiFileInfo;
        this.categoryMap = new TreeMap<>();
    }

    protected PoiCategory createPoiCategory(String categoryTitle, int categoryID) {
        if (this.poiFileInfo.version <= 4) {
            return new DoubleLinkedPoiCategory(categoryTitle, null, categoryID);
        }

        String[] sb = categoryTitle.split("\\r");
        Map<String, String> map = new HashMap<>();
        for (String set : sb) {
            if (set.indexOf(Tag.KEY_VALUE_SEPARATOR) > -1) {
                String key = set.split(String.valueOf(Tag.KEY_VALUE_SEPARATOR))[0];
                String value = set.substring(key.length() + 1);
                map.put(key, value);
            }
        }
        PoiCategory pc = new DoubleLinkedPoiCategory(map.get(this.poiFileInfo.catLanguage), null, categoryID);
        for (Map.Entry<String, String> entry : map.entrySet()) {
            pc.getTitles().put(entry.getKey(), entry.getValue());
        }
        return pc;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PoiCategory getPoiCategoryByID(int id) throws UnknownPoiCategoryException {
        if (this.categoryMap.get(id) == null) {
            throw new UnknownPoiCategoryException();
        }

        return this.categoryMap.get(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PoiCategory getPoiCategoryByTitle(String title) throws UnknownPoiCategoryException {
        for (int key : this.categoryMap.keySet()) {
            final PoiCategory category = this.categoryMap.get(key);
            if (category.getTitle().equalsIgnoreCase(title)) {
                return category;
            }
            for (String value : category.getTitles().values()) {
                if (value.equalsIgnoreCase(title)) {
                    return category;
                }
            }
        }

        throw new UnknownPoiCategoryException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PoiCategory getRootCategory() throws UnknownPoiCategoryException {
        if (this.rootCategory == null) {
            throw new UnknownPoiCategoryException();
        }

        return this.rootCategory;
    }
}
