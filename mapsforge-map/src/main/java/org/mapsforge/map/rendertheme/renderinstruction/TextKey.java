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
package org.mapsforge.map.rendertheme.renderinstruction;

import java.util.List;

import org.mapsforge.core.model.Tag;

final class TextKey {
	private static final String KEY_ELEVATION = "ele";
	private static final String KEY_HOUSENUMBER = "addr:housenumber";
	private static final String KEY_NAME = "name";
	private static final String KEY_REF = "ref";
	private static final TextKey TEXT_KEY_ELEVATION = new TextKey(KEY_ELEVATION);
	private static final TextKey TEXT_KEY_HOUSENUMBER = new TextKey(KEY_HOUSENUMBER);
	private static final TextKey TEXT_KEY_NAME = new TextKey(KEY_NAME);
	private static final TextKey TEXT_KEY_REF = new TextKey(KEY_REF);

	static TextKey getInstance(String key) {
		if (KEY_ELEVATION.equals(key)) {
			return TEXT_KEY_ELEVATION;
		} else if (KEY_HOUSENUMBER.equals(key)) {
			return TEXT_KEY_HOUSENUMBER;
		} else if (KEY_NAME.equals(key)) {
			return TEXT_KEY_NAME;
		} else if (KEY_REF.equals(key)) {
			return TEXT_KEY_REF;
		} else {
			throw new IllegalArgumentException("invalid key: " + key);
		}
	}

	private final String key;

	private TextKey(String key) {
		this.key = key;
	}

	String getValue(List<Tag> tags) {
		for (int i = 0, n = tags.size(); i < n; ++i) {
			if (this.key.equals(tags.get(i).key)) {
				return tags.get(i).value;
			}
		}
		return null;
	}
}
