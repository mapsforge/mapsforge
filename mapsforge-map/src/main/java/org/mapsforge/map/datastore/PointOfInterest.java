/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014-2015 Ludwig M Brinckmann
 * Copyright 2025 Sublimis
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

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Tag;

import java.util.List;

/**
 * An immutable container for all data associated with a single point of interest node (POI).
 */
public class PointOfInterest implements Comparable<PointOfInterest> {
    /**
     * The layer of this POI + 5 (to avoid negative values).
     */
    public final byte layer;

    /**
     * The position of this POI.
     */
    public final LatLong position;

    /**
     * The tags of this POI.
     */
    public final List<Tag> tags;

    public PointOfInterest(byte layer, List<Tag> tags, LatLong position) {
        this.layer = layer;
        this.tags = tags;
        this.position = position;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    @Override
    public boolean equals(Object o) {
        return this == o;
    }

    @Override
    public int compareTo(PointOfInterest other) {
        if (this == other) {
            return 0;
        }
        if (null == other) {
            return -1;
        }

        int retVal = 0;

        retVal = Byte.compare(this.layer, other.layer);
        if (retVal != 0) {
            return retVal;
        }

        retVal = this.position.compareTo(other.position);
        if (retVal != 0) {
            return retVal;
        }

        retVal = Integer.compare(this.tags.size(), other.tags.size());
        if (retVal != 0) {
            return retVal;
        }

        for (int i = 0; i < this.tags.size(); i++) {
            retVal = this.tags.get(i).compareTo(other.tags.get(i));
            if (retVal != 0) {
                return retVal;
            }
        }

        return retVal;
    }
}
