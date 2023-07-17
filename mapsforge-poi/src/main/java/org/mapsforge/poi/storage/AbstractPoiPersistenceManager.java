/*
 * Copyright 2015-2017 devemux86
 * Copyright 2017 Gustl22
 * Copyright 2022 Juanjo-MC
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

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Tag;
import org.mapsforge.core.util.LatLongUtils;

import java.util.*;

/**
 * Abstract implementation for the {@link PoiPersistenceManager} interface. This implementation
 * provides functionality for accessing the SQLite database.
 */
public abstract class AbstractPoiPersistenceManager implements PoiPersistenceManager {
    protected PoiCategoryManager categoryManager = null;
    protected String poiFile;
    protected PoiFileInfo poiFileInfo;

    // Containers for return values
    protected final List<PointOfInterest> ret;
    protected PointOfInterest poi = null;

    protected AbstractPoiPersistenceManager() {
        this.ret = new ArrayList<>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<PointOfInterest> findNearPosition(LatLong point, int distance,
                                                        PoiCategoryFilter filter, List<Tag> patterns,
                                                        LatLong orderBy, int limit, boolean findCategories) {
        double minLat = point.latitude - LatLongUtils.latitudeDistance(distance);
        double minLon = point.longitude - LatLongUtils.longitudeDistance(distance, point.latitude);
        double maxLat = point.latitude + LatLongUtils.latitudeDistance(distance);
        double maxLon = point.longitude + LatLongUtils.longitudeDistance(distance, point.latitude);

        return findInRect(new BoundingBox(minLat, minLon, maxLat, maxLon), filter, patterns, orderBy, limit, findCategories);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PoiCategoryManager getCategoryManager() {
        return this.categoryManager;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPoiFile() {
        return poiFile;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PoiFileInfo getPoiFileInfo() {
        // Lazy initialization
        if (poiFileInfo == null) {
            readPoiFileInfo();
        }
        return poiFileInfo;
    }

    /**
     * Gets the SQL query that looks up POI entries.
     *
     * @param filter  The filter object for determining all wanted categories (may be null).
     * @param count   Count of patterns to search in points of interest data (may be 0).
     * @param orderBy {@link LatLong} location of the sort.
     * @return The SQL query.
     */
    protected static String getSQLSelectString(PoiCategoryFilter filter, int count, LatLong orderBy) {
        if (filter != null) {
            return PoiCategoryRangeQueryGenerator.getSQLSelectString(filter, count, orderBy);
        }
        StringBuilder sb = new StringBuilder();
        sb.append(DbConstants.FIND_IN_BOX_CLAUSE_SELECT);
        sb.append(DbConstants.JOIN_DATA_CLAUSE);
        sb.append(DbConstants.FIND_IN_BOX_CLAUSE_WHERE);
        for (int i = 0; i < count; i++) {
            sb.append(i == 0 ? " AND (" : " OR ");
            sb.append(DbConstants.FIND_BY_DATA_CLAUSE);
            if (i == count - 1) {
                sb.append(")");
            }
        }
        if (orderBy != null) {
            sb.append(" ORDER BY ((").append(orderBy.latitude).append(" - poi_index.lat) * (").append(orderBy.latitude).append(" - poi_index.lat))")
                    .append(" + ((").append(orderBy.longitude).append(" - poi_index.lon) * (").append(orderBy.longitude).append(" - poi_index.lon)) ASC");
        }
        return sb.append(" LIMIT ?;").toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCategoryManager(PoiCategoryManager categoryManager) {
        this.categoryManager = categoryManager;
    }

    /**
     * Convert tags string representation with '\r' delimiter to collection.
     */
    protected static Set<Tag> stringToTags(String data) {
        Set<Tag> tags = new HashSet<>();
        String[] split = data.split("\r");
        for (String s : split) {
            if (s.indexOf(Tag.KEY_VALUE_SEPARATOR) > -1) {
                String key = s.split(String.valueOf(Tag.KEY_VALUE_SEPARATOR))[0];
                String value = s.substring(key.length() + 1);
                tags.add(new Tag(key, value));
            }
        }
        return tags;
    }

    /**
     * Convert tags collection to string representation with '\r' delimiter.
     */
    protected static String tagsToString(Set<Tag> tags) {
        StringBuilder sb = new StringBuilder();
        for (Tag tag : tags) {
            if (sb.length() > 0) {
                sb.append('\r');
            }
            sb.append(tag.key).append(Tag.KEY_VALUE_SEPARATOR).append(tag.value);
        }
        return sb.toString();
    }
}
