/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014-2015 Ludwig M Brinckmann
 * Copyright 2017 devemux86
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
package com.ikroshlab.ovtencoder;

import com.ikroshlab.ovtencoder.GeometryBuffer.GeometryType;
import org.oscim.core.GeoPoint;
import org.oscim.core.Tag;

import java.util.Arrays;
import java.util.List;

/**
 * An immutable container for all data associated with a single way or area (closed way).
 */
public class Way {
    /**
     * The position of the area label (may be null).
     */
    public final GeoPoint labelPosition;

    /**
     * The geometry type.
     */
    public GeometryBuffer.GeometryType geometryType = GeometryBuffer.GeometryType.NONE;

    /**
     * The geographical coordinates of the way nodes.
     */
    public final GeoPoint[][] geoPoints;

    /**
     * The layer of this way + 5 (to avoid negative values).
     */
    public final byte layer;

    /**
     * The tags of this way.
     */
    public final List<Tag> tags;

    public Way(byte layer, List<Tag> tags, GeoPoint[][] geoPoints, GeoPoint labelPosition) {
        this.layer = layer;
        this.tags = tags;
        this.geoPoints = geoPoints;
        this.labelPosition = labelPosition;
    }

    public Way(byte layer, List<Tag> tags, GeoPoint[][] geoPoints, GeoPoint labelPosition, final GeometryBuffer.GeometryType geometryType) {
        this(layer, tags, geoPoints, labelPosition);
        this.geometryType = geometryType;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof Way)) {
            return false;
        }
        Way other = (Way) obj;
        if (this.layer != other.layer) {
            return false;
        } else if (!this.tags.equals(other.tags)) {
            return false;
        } else if (this.labelPosition == null && other.labelPosition != null) {
            return false;
        } else if (this.labelPosition != null && !this.labelPosition.equals(other.labelPosition)) {
            return false;
        } else if (this.geoPoints.length != other.geoPoints.length) {
            return false;
        } else {
            for (int i = 0; i < this.geoPoints.length; i++) {
                if (this.geoPoints[i].length != other.geoPoints[i].length) {
                    return false;
                } else {
                    for (int j = 0; j < this.geoPoints[i].length; j++) {
                        if (!geoPoints[i][j].equals(other.geoPoints[i][j])) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + layer;
        result = prime * result + tags.hashCode();
        result = prime * result + Arrays.deepHashCode(geoPoints);
        if (labelPosition != null) {
            result = prime * result + labelPosition.hashCode();
        }
        return result;
    }

}
