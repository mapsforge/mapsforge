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

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * This class represents a point of interest. Every POI should be uniquely identifiable by its id,
 * so that for two POIs a and b, a.equals(b) if and only if a.id == b.id.
 */
public class PointOfInterest {
    private final long id;
    private final double latitude;
    private final double longitude;
    private final Set<Tag> tags;
    private final Set<PoiCategory> categories;

    public PointOfInterest(long id, double latitude, double longitude, String name, PoiCategory categories) {
        this(id, latitude, longitude, new HashSet<>(Collections.singletonList(new Tag("name", name))),
                new HashSet<>(Collections.singletonList(categories)));
    }

    public PointOfInterest(long id, double latitude, double longitude, Set<Tag> tags, Set<PoiCategory> categories) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.tags = (tags == null) ? new HashSet<Tag>() : tags;
        this.categories = (categories == null) ? new HashSet<PoiCategory>() : categories;
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
     * @return all categories of this point of interest.
     */
    public Set<PoiCategory> getCategories() {
        return this.categories;
    }

    /**
     * @return category of this point of interest.
     */
    public PoiCategory getCategory() {
        if (!categories.isEmpty()) {
            if (categories.size() == 1)
                return categories.toArray(new PoiCategory[categories.size()])[0];
            else {
                //TODO return the highest cat.-level of all its categories together
                return categories.toArray(new PoiCategory[categories.size()])[0];
            }
        }
        return null;
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

        return null;
    }

    /**
     * @return tags of this point of interest.
     */
    public Set<Tag> getTags() {
        return tags;
    }

    @Override
    public String toString() {
        String builder = "POI: (" + this.latitude + ',' + this.longitude + ") " + this.tags.toString() + ' ';
        for (PoiCategory category : categories) {
            builder += category.getID() + ' ';
        }
        return builder;
    }
}
