/*
 * Copyright 2010, 2011 mapsforge.org
 * Copyright 2015 devemux86
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
 * Implementation for the {@link PointOfInterest} interface.
 */
public class PoiImpl implements PointOfInterest {
	private final long id;
	private final double latitude;
	private final double longitude;
	private final String data;
	private final PoiCategory category;

	public PoiImpl(long id, double latitude, double longitude, String data, PoiCategory category) {
		this.id = id;
		this.latitude = latitude;
		this.longitude = longitude;
		this.data = data;
		this.category = category;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PoiCategory getCategory() {
		return this.category;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getData() {
		return this.data;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long getId() {
		return this.id;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public double getLatitude() {
		return this.latitude;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public LatLong getLatLong() {
		return new LatLong(this.latitude, this.longitude);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public double getLongitude() {
		return this.longitude;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getName() {
		return getName(Locale.getDefault().getLanguage());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
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
	 * {@inheritDoc}
	 */
	@Override
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
