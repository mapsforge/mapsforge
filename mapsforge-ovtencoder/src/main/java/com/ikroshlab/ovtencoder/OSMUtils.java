/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2015-2016 devemux86
 * Copyright 2015-2016 lincomatic
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

import org.oscim.core.Tag;

import java.util.Locale;

public final class OSMUtils {

    /**
     * Heuristic to determine from attributes if a map element is likely to be an area.
     * Precondition for this call is that the first and last node of a map element are the
     * same, so that this method should only return false if it is known that the
     * feature should not be an area even if the geometry is a polygon.
     * <p/>
     * Determining what is an area is neigh impossible in OSM, this method inspects tag elements
     * to give a likely answer. See http://wiki.openstreetmap.org/wiki/The_Future_of_Areas and
     * http://wiki.openstreetmap.org/wiki/Way
     *
     * @param mapElement the map element (which is assumed to be closed and have enough nodes to be an area)
     * @return true if tags indicate this is an area, otherwise false.
     */
    public static boolean isArea(MapElement mapElement) {
        boolean result = true;
        for (int i = 0; i < mapElement.tags.size(); i++) {
            Tag tag = mapElement.tags.get(i);
            String key = tag.key.toLowerCase(Locale.ENGLISH);
            String value = tag.value.toLowerCase(Locale.ENGLISH);
            if ("area".equals(key)) {
                // obvious result
                if (("yes").equals(value) || ("y").equals(value) || ("true").equals(value)) {
                    return true;
                }
                if (("no").equals(value) || ("n").equals(value) || ("false").equals(value)) {
                    return false;
                }
            }
            // as specified by http://wiki.openstreetmap.org/wiki/Key:area
            if ("aeroway".equals(key) || "building".equals(key) || "landuse".equals(key) || "leisure".equals(key) || "natural".equals(key) || "amenity".equals(key)) {
                return true;
            }
            if ("highway".equals(key) || "barrier".equals(key)) {
                // false unless something else overrides this.
                result = false;
            }
            if ("railway".equals(key)) {
                // there is more to the railway tag then just rails, this excludes the
                // most common railway lines from being detected as areas if they are closed.
                // Since this method is only called if the first and last node are the same
                // this should be safe
                if ("rail".equals(value) || "tram".equals(value) || "subway".equals(value)
                        || "monorail".equals(value) || "narrow_gauge".equals(value) || "preserved".equals(value)
                        || "light_rail".equals(value) || "construction".equals(value)) {
                    result = false;
                }
            }
        }
        return result;
    }

    private OSMUtils() {
    }
}
