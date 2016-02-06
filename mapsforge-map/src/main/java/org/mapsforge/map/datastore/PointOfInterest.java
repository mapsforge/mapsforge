/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014-2015 Ludwig M Brinckmann
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
public class PointOfInterest {
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
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof PointOfInterest)) {
            return false;
        }
        PointOfInterest other = (PointOfInterest) obj;
        if (this.layer != other.layer) {
            return false;
        } else if (!this.tags.equals(other.tags)) {
            return false;
        } else if (!this.position.equals(other.position)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + layer;
        result = prime * result + tags.hashCode();
        result = prime * result + position.hashCode();
        return result;
    }

}
