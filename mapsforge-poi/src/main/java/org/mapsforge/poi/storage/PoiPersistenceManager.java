/*
 * Copyright 2010, 2011 mapsforge.org
 * Copyright 2010, 2011 weise
 * Copyright 2010, 2011 Karsten Groll
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

import java.util.Collection;
import java.util.List;

/**
 * Abstracts from an underlying Storage/DB by providing methods for inserting / deleting / searching
 * {@link PointOfInterest} objects in named Storage/DB.
 * <p/>
 * Remember to call the {@link #close()} method as soon as you are done manipulating the Storage/DB
 * via this {@link PoiPersistenceManager}.
 */
public interface PoiPersistenceManager {
    /**
     * Use this to free claimed resources. After that you might no longer be able to query for
     * points of interest. This should always be called a soon as you are done querying.
     */
    void close();

    /**
     * Find all {@link PointOfInterest} in a rectangle specified by the given {@link BoundingBox}.
     * Only the POIs that are allowed by the {@link PoiCategoryFilter} object and matching the data
     * pattern will be returned.
     *
     * @param bb       {@link BoundingBox} specifying the rectangle.
     * @param filter   POI category filter object that helps determining whether a POI should be added to
     *                 the set or not (may be null).
     * @param patterns the patterns to search in points of interest data (may be null).
     * @param limit    max number of {@link PointOfInterest} to be returned.
     * @return {@link Collection} of {@link PointOfInterest} matching a given
     * {@link PoiCategoryFilter} and data pattern contained in the rectangle specified by
     * the given {@link BoundingBox}.
     */
    Collection<PointOfInterest> findInRect(BoundingBox bb, PoiCategoryFilter filter, List<Tag> patterns,
                                           int limit);

    /**
     * Fetch {@link PointOfInterest} from underlying storage near a given position.
     * Only the POIs that are allowed by the {@link PoiCategoryFilter} object and matching the data
     * pattern will be returned.
     *
     * @param point    {@link LatLong} center of the search.
     * @param distance in meters
     * @param filter   POI category filter object that helps determining whether a POI should be added to
     *                 the set or not (may be null).
     * @param patterns the patterns to search in points of interest data (may be null).
     * @param limit    max number of {@link PointOfInterest} to be returned.
     * @return {@link Collection} of {@link PointOfInterest} matching a given
     * {@link PoiCategoryFilter} and data pattern near the given position.
     */
    Collection<PointOfInterest> findNearPosition(LatLong point, int distance,
                                                 PoiCategoryFilter filter, List<Tag> patterns,
                                                 int limit);

    /**
     * @param poiID the id of the point of interest that shall be returned.
     * @return a single {@link PointOfInterest} p where p.id == poiID.
     */
    PointOfInterest findPointByID(long poiID);

    /**
     * @return The persistence manager's category manager for retrieving and editing POI categories.
     */
    PoiCategoryManager getCategoryManager();

    /**
     * @return the current POI file.
     */
    String getPoiFile();

    /**
     * @return the metadata for the current POI file.
     */
    PoiFileInfo getPoiFileInfo();

    /**
     * Inserts a single {@link PointOfInterest} into storage.
     *
     * @param poi {@link PointOfInterest} to insert into storage.
     */
    void insertPointOfInterest(PointOfInterest poi);

    /**
     * Inserts {@link PointOfInterest} into storage.
     *
     * @param pois collection of {@link PointOfInterest} to insert into storage.
     */
    void insertPointsOfInterest(Collection<PointOfInterest> pois);

    /**
     * @return true if the manager is closed.
     */
    boolean isClosed();

    /**
     * @return true if the database is a valid POI database.
     */
    boolean isValidDataBase();

    /**
     * Removes a point of interest from storage.
     *
     * @param poi the {@link PointOfInterest} to be removed.
     */
    void removePointOfInterest(PointOfInterest poi);

    /**
     * Sets this manager's {@link PoiCategoryManager} for retrieving and editing POI categories.
     *
     * @param categoryManager The category manager to be set.
     */
    void setCategoryManager(PoiCategoryManager categoryManager);
}
