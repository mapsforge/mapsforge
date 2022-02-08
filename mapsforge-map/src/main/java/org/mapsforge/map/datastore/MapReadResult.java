/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014-2015 Ludwig M Brinckmann
 * Copyright 2015-2022 devemux86
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
package org.mapsforge.map.datastore;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * An immutable container for the data returned from a MapDataStore.
 */
public class MapReadResult {

    /**
     * Hash codes.
     */
    private final Set<Integer> hashPois = new HashSet<>();
    private final Set<Integer> hashWays = new HashSet<>();

    /**
     * True if the read area is completely covered by water, false otherwise.
     */
    public boolean isWater;

    /**
     * The read POIs.
     */
    public List<PointOfInterest> pointOfInterests;

    /**
     * The read ways.
     */
    public List<Way> ways;

    public MapReadResult() {
        this.pointOfInterests = new ArrayList<>();
        this.ways = new ArrayList<>();
    }

    public void add(PoiWayBundle poiWayBundle) {
        this.pointOfInterests.addAll(poiWayBundle.pois);
        this.ways.addAll(poiWayBundle.ways);
    }

    /**
     * Adds other MapReadResult by combining pois and ways.
     * Optionally deduplication can be requested (more expensive).
     *
     * @param other       the MapReadResult to add to this.
     * @param deduplicate true if check for duplicates is required.
     */
    public void add(MapReadResult other, boolean deduplicate) {
        if (deduplicate) {
            for (PointOfInterest poi : other.pointOfInterests) {
                if (this.hashPois.add(poi.hashCode())) {
                    this.pointOfInterests.add(poi);
                }
            }
            for (Way way : other.ways) {
                if (this.hashWays.add(way.hashCode())) {
                    this.ways.add(way);
                }
            }
        } else {
            this.pointOfInterests.addAll(other.pointOfInterests);
            this.ways.addAll(other.ways);
        }
    }
}
