/*
 * Copyright 2015-2016 devemux86
 * Copyright 2017 Gustl22
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
                                                        int limit) {
        double minLat = point.latitude - LatLongUtils.latitudeDistance(distance);
        double minLon = point.longitude - LatLongUtils.longitudeDistance(distance, point.latitude);
        double maxLat = point.latitude + LatLongUtils.latitudeDistance(distance);
        double maxLon = point.longitude + LatLongUtils.longitudeDistance(distance, point.latitude);

        return findInRect(new BoundingBox(minLat, minLon, maxLat, maxLon), filter, patterns, limit);
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
        // Update File info only if its null to avoid frequent requests.
        if (poiFileInfo == null) {
            updatePoiFileInfo();
        }
        return poiFileInfo;
    }

    /**
     * Updates the metadata for the current POI file.
     */
    public abstract void updatePoiFileInfo();

    /**
     * Gets the SQL query that looks up POI entries.
     *
     * @param filter The filter object for determining all wanted categories (may be null).
     * @param count  Count of patterns to search in points of interest data (may be 0).
     * @param version The version of poi-specification.
     * @return The SQL query.
     */
    protected static String getSQLSelectString(PoiCategoryFilter filter, int count, int version) {
        if (filter != null) {
            return PoiCategoryRangeQueryGenerator.getSQLSelectString(filter, count, version);
        }
        StringBuilder query = new StringBuilder();
        query.append(DbConstants.FIND_IN_BOX_CLAUSE_SELECT);
        if (count > 0) {
            query.append(DbConstants.JOIN_DATA_CLAUSE);
        }
        query.append(DbConstants.FIND_IN_BOX_CLAUSE_WHERE);
        for (int i = 0; i < count; i++) {
            query.append(DbConstants.FIND_BY_DATA_CLAUSE);
        }
        return query.append(" LIMIT ?;").toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCategoryManager(PoiCategoryManager categoryManager) {
        this.categoryManager = categoryManager;
    }
}
