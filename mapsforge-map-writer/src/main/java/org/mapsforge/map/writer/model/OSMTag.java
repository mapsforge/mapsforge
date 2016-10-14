/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
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

/**
 * Represents an OSM entity which is defined by a tag/value pair. Each OSM entity is attributed with the zoom level on
 * which it should appear first.
 */
public class OSMTag {
    private static final String KEY_VALUE_SEPARATOR = "=";

    /**
     * Convenience method that constructs a new OSMTag with a new id from another OSMTag.
     *
     * @param otherTag the OSMTag to copy
     * @param newID    the new id
     * @return a newly constructed OSMTag with the attributes of otherTag
     */
    public static OSMTag fromOSMTag(OSMTag otherTag, short newID) {
        return new OSMTag(newID, otherTag.getKey(), otherTag.getValue(), otherTag.getZoomAppear(),
                otherTag.isRenderable(), otherTag.isForcePolygonLine(), otherTag.isLabelPosition());
    }

    /**
     * Convenience method for generating a string representation of a key/value pair.
     *
     * @param key   the key of the tag
     * @param value the value of the tag
     * @return a string representation of the key/Value pair
     */
    public static String tagKey(String key, String value) {
        return key + KEY_VALUE_SEPARATOR + value;
    }

    private final boolean labelPosition;
    private final boolean forcePolygonLine;
    private final short id;
    private final String key;
    // TODO is the renderable attribute still needed?
    private final boolean renderable;

    private final String value;

    private final byte zoomAppear;

    /**
     * @param id               the internal id of the tag
     * @param key              the key of the tag
     * @param value            the value of the tag
     * @param zoomAppear       the minimum zoom level the tag appears first
     * @param renderable       flag if the tag represents a renderable entity
     * @param forcePolygonLine flag if polygon line instead of area is forced with closed polygons
     * @param labelPosition    flag if label position should be computed for polygons with such tag
     */
    public OSMTag(short id, String key, String value, byte zoomAppear, boolean renderable, boolean forcePolygonLine, boolean labelPosition) {
        super();
        this.id = id;
        this.key = key;
        this.value = value;
        this.zoomAppear = zoomAppear;
        this.renderable = renderable;
        this.forcePolygonLine = forcePolygonLine;
        this.labelPosition = labelPosition;
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof OSMTag)) {
            return false;
        }
        OSMTag other = (OSMTag) obj;
        return this.id == other.id;
    }

    /**
     * @return the id
     */
    public final short getId() {
        return this.id;
    }

    /**
     * @return the key
     */
    public final String getKey() {
        return this.key;
    }

    /**
     * @return the value
     */
    public final String getValue() {
        return this.value;
    }

    /**
     * @return the zoomAppear
     */
    public final byte getZoomAppear() {
        return this.zoomAppear;
    }

    @Override
    public final int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.id;
        return result;
    }

    /**
     * @return whether the tag represents a coastline
     */
    public final boolean isCoastline() {
        return this.key.equals("natural") && this.value.equals("coastline");
    }

    /**
     * @return the forcePolygonLine
     */
    public final boolean isForcePolygonLine() {
        return this.forcePolygonLine;
    }

    /**
     * @return the renderable
     */
    public final boolean isRenderable() {
        return this.renderable;
    }

	/**
	 * @return the labelPosition
	 */
    public boolean isLabelPosition() {
        return labelPosition;
    }

    /**
     * @return the string representation of the OSMTag
     */
    public final String tagKey() {
        return this.key + KEY_VALUE_SEPARATOR + this.value;
    }

    @Override
    public final String toString() {
        return "OSMTag [id=" + this.id + ", key=" + this.key + ", value=" + this.value + ", zoomAppear="
                + this.zoomAppear + ", renderable=" + this.renderable + ", labelPosition=" + this.labelPosition + "]";
    }
}
