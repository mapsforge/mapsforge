/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2015 lincomatic
 * Copyright 2016 devemux86
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
package org.mapsforge.map.writer.model;

import org.mapsforge.core.util.LatLongUtils;
import org.mapsforge.map.writer.OSMTagMapping;
import org.mapsforge.map.writer.util.OSMUtils;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class TDNode {

    private static final byte ZOOM_HOUSENUMBER = (byte) 17;

    // private static final byte ZOOM_NAME = (byte) 16;

    /**
     * Constructs a new TDNode from a given osmosis node entity. Checks the validity of the entity.
     *
     * @param node               the osmosis entity
     * @param preferredLanguages the preferred language(s) or null if no preference
     * @return a new TDNode
     */
    public static TDNode fromNode(Node node, List<String> preferredLanguages) {
        SpecialTagExtractionResult ster = OSMUtils.extractSpecialFields(node, preferredLanguages);
        Map<Short, Object> knownWayTags = OSMUtils.extractKnownPOITags(node);

        return new TDNode(node.getId(), LatLongUtils.degreesToMicrodegrees(node.getLatitude()),
                LatLongUtils.degreesToMicrodegrees(node.getLongitude()), ster.getElevation(), ster.getLayer(),
                ster.getHousenumber(), ster.getName(), knownWayTags);
    }

    private final short elevation;
    private final String houseNumber;

    private final long id;
    private final int latitude;
    private final byte layer;
    private final int longitude;
    private final String name;

    private Map<Short, Object> tags;

    /**
     * @param id          the OSM id
     * @param latitude    the latitude
     * @param longitude   the longitude
     * @param elevation   the elevation if existent
     * @param layer       the layer if existent
     * @param houseNumber the house number if existent
     * @param name        the name if existent
     */
    public TDNode(long id, int latitude, int longitude, short elevation, byte layer, String houseNumber, String name) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.elevation = elevation;
        this.houseNumber = houseNumber;
        this.layer = layer;
        this.name = name;
    }

    /**
     * @param id          the OSM id
     * @param latitude    the latitude
     * @param longitude   the longitude
     * @param elevation   the elevation if existent
     * @param layer       the layer if existent
     * @param houseNumber the house number if existent
     * @param name        the name if existent
     * @param tags        the tags (tag map contains optional values)
     */
    public TDNode(long id, int latitude, int longitude, short elevation, byte layer, String houseNumber, String name,
                  Map<Short, Object> tags) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.elevation = elevation;
        this.houseNumber = houseNumber;
        this.layer = layer;
        this.name = name;
        this.tags = tags;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        TDNode other = (TDNode) obj;
        if (this.id != other.id) {
            return false;
        }
        return true;
    }

    /**
     * @return the elevation
     */
    public short getElevation() {
        return this.elevation;
    }

    /**
     * @return the houseNumber
     */
    public String getHouseNumber() {
        return this.houseNumber;
    }

    /**
     * @return the id
     */
    public long getId() {
        return this.id;
    }

    /**
     * @return the latitude
     */
    public int getLatitude() {
        return this.latitude;
    }

    /**
     * @return the layer
     */
    public byte getLayer() {
        return this.layer;
    }

    /**
     * @return the longitude
     */
    public int getLongitude() {
        return this.longitude;
    }

    /**
     * @return the name
     */
    public String getName() {
        return this.name;
    }

    /**
     * @return the tags
     */
    public Map<Short, Object> getTags() {
        return this.tags;
    }

    /**
     * @return the zoom level on which the node appears first
     */
    public byte getZoomAppear() {
        if (this.tags == null || this.tags.size() == 0) {
            if (this.houseNumber != null) {
                return ZOOM_HOUSENUMBER;
            }
            return Byte.MAX_VALUE;
        }
        return OSMTagMapping.getInstance().getZoomAppearPOI(this.tags.keySet());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (this.id ^ (this.id >>> 32));
        return result;
    }

    /**
     * @return true if the node represents a POI
     */
    public boolean isPOI() {
        return this.houseNumber != null || this.elevation != 0 || this.tags.size() > 0;
    }

    /**
     * @param tags the tags to set
     */
    public void setTags(Map<Short, Object> tags) {
        this.tags = tags;
    }

    @Override
    public final String toString() {
        return "TDNode [id=" + this.id + ", latitude=" + this.latitude + ", longitude=" + this.longitude + ", name="
                + this.name + ", tags=" + Arrays.toString(this.tags.keySet().toArray()) + "]";
    }
}
