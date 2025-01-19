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
 * An immutable container for all data associated with a single way or area (closed way).
 */
public class Way implements Comparable<Way> {
    /**
     * The position of the area label (may be null).
     */
    public final LatLong labelPosition;

    /**
     * The geographical coordinates of the way nodes.
     */
    public final LatLong[][] latLongs;

    /**
     * The layer of this way + 5 (to avoid negative values).
     */
    public final byte layer;

    /**
     * The tags of this way.
     */
    public final List<Tag> tags;

    public Way(byte layer, List<Tag> tags, LatLong[][] latLongs, LatLong labelPosition) {
        this.layer = layer;
        this.tags = tags;
        this.latLongs = latLongs;
        this.labelPosition = labelPosition;
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
    public int compareTo(Way other) {
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

        retVal = Integer.compare(this.latLongs.length, other.latLongs.length);
        if (retVal != 0) {
            return retVal;
        }

        retVal = Integer.compare(this.tags.size(), other.tags.size());
        if (retVal != 0) {
            return retVal;
        }

        if (this.labelPosition != null && other.labelPosition != null) {
            retVal = this.labelPosition.compareTo(other.labelPosition);
            if (retVal != 0) {
                return retVal;
            }
        } else if (this.labelPosition != null) {
            return -1;
        } else if (other.labelPosition != null) {
            return 1;
        }

        for (int i = 0; i < this.tags.size(); i++) {
            retVal = this.tags.get(i).compareTo(other.tags.get(i));
            if (retVal != 0) {
                return retVal;
            }
        }

        for (int i = 0; i < this.latLongs.length; i++) {
            retVal = Integer.compare(this.latLongs[i].length, other.latLongs[i].length);
            if (retVal != 0) {
                return retVal;
            }

            for (int j = 0; j < this.latLongs[i].length; j++) {
                retVal = this.latLongs[i][j].compareTo(other.latLongs[i][j]);
                if (retVal != 0) {
                    return retVal;
                }
            }
        }

        return retVal;
    }
}
