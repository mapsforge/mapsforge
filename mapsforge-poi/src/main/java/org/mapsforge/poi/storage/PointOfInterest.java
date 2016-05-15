/*
 * Copyright 2010, 2011 mapsforge.org
 * Copyright 2015-2016 devemux86
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

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * This class represents a point of interest. Every POI should be uniquely identifiable by its id,
 * so that for two POIs a and b, a.equals(b) if and only if a.id == b.id.
 */
public class PointOfInterest {
    private final long id;
    private final double latitude;
    private final double longitude;
    private final String data;
    private final PoiCategory category;

    public PointOfInterest(long id, double latitude, double longitude, String data, PoiCategory category) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.data = data;
        this.category = category;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof PointOfInterest)) {
            return false;
        }
        PointOfInterest other = (PointOfInterest) obj;
        return this.id == other.id;
    }

    /**
     * @return category of this point of interest.
     */
    public PoiCategory getCategory() {
        return this.category;
    }

    /**
     * @return data of this point of interest.
     */
    public String getData() {
        return this.data;
    }

    /**
     * @return id of this point of interest.
     */
    public long getId() {
        return this.id;
    }

    /**
     * @return latitude of this point of interest.
     */
    public double getLatitude() {
        return this.latitude;
    }

    /**
     * @return {@link LatLong} of this point of interest.
     */
    public LatLong getLatLong() {
        return new LatLong(this.latitude, this.longitude);
    }

    /**
     * @return longitude of this point of interest
     */
    public double getLongitude() {
        return this.longitude;
    }

    /**
     * @return name of this point of interest at default locale
     */
    public String getName() {
        return getName(Locale.getDefault().getLanguage());
    }

    /**
     * @return name of this point of interest at preferred language
     */
    public String getName(String language) {
        List<Tag> tags = getTags();

        if (language != null && language.trim().length() > 0) {
            // Exact match
            String nameStr = "name:" + language.toLowerCase(Locale.ENGLISH);
            for (Tag tag : tags) {
                if (nameStr.equalsIgnoreCase(tag.key)) {
                    return tag.value;
                }
            }

            // Fall back to base
            String baseLanguage = language.split("[-_]")[0];
            nameStr = "name:" + baseLanguage.toLowerCase(Locale.ENGLISH);
            for (Tag tag : tags) {
                if (nameStr.equalsIgnoreCase(tag.key)) {
                    return tag.value;
                }
            }
        }

        // Default name
        for (Tag tag : tags) {
            if ("name".equalsIgnoreCase(tag.key)) {
                return tag.value;
            }
        }

        if (tags.isEmpty()) {
            return data;
        }

        return null;
    }

    /**
     * @return tags of this point of interest.
     */
    public List<Tag> getTags() {
        List<Tag> tags = new ArrayList<>();
        if (this.data != null && this.data.trim().length() > 0) {
            String[] split = this.data.split("\r");
            for (String s : split) {
                if (s.indexOf(Tag.KEY_VALUE_SEPARATOR) > -1) {
                    tags.add(new Tag(s));
                }
            }
        }
        return tags;
    }

    @Override
    public String toString() {
        return "POI: (" + this.latitude + ',' + this.longitude + ") " + this.data + ' '
                + this.category.getID();
    }
}
