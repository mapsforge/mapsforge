/*
 * Copyright 2015-2016 devemux86
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
import org.mapsforge.core.util.LatLongUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Abstract implementation for the {@link PoiPersistenceManager} interface. This implementation
 * provides functionality for accessing the SQLite database.
 */
public abstract class AbstractPoiPersistenceManager implements PoiPersistenceManager {
    protected PoiCategoryManager categoryManager = null;
    protected String poiFile;

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
                                                        PoiCategoryFilter filter, String pattern,
                                                        int limit) {
        double minLat = point.latitude - LatLongUtils.latitudeDistance(distance);
        double minLon = point.longitude - LatLongUtils.longitudeDistance(distance, point.latitude);
        double maxLat = point.latitude + LatLongUtils.latitudeDistance(distance);
        double maxLon = point.longitude + LatLongUtils.longitudeDistance(distance, point.latitude);

        return findInRect(new BoundingBox(minLat, minLon, maxLat, maxLon), filter, pattern, limit);
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
     * Gets the SQL query that looks up POI entries.
     *
     * @param filter  The filter object for determining all wanted categories (may be null).
     * @param pattern the pattern to search in points of interest data (may be null).
     * @return The SQL query.
     */
    protected static String getSQLSelectString(PoiCategoryFilter filter, String pattern) {
        if (filter != null) {
            return PoiCategoryRangeQueryGenerator.getSQLSelectString(filter, pattern);
        }
        return DbConstants.FIND_IN_BOX_STATEMENT + (pattern != null ? DbConstants.FIND_BY_DATA_CLAUSE : "") + " LIMIT ?;";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCategoryManager(PoiCategoryManager categoryManager) {
        this.categoryManager = categoryManager;
    }
}
