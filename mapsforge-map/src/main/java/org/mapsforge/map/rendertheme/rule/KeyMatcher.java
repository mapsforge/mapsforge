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
package org.mapsforge.map.rendertheme.rule;

import java.util.ArrayList;
import java.util.List;

import org.mapsforge.core.model.Tag;

class KeyMatcher implements AttributeMatcher {
	private final List<String> keys;

	KeyMatcher(List<String> keys) {
		this.keys = keys;
	}

	@Override
	public boolean isCoveredBy(AttributeMatcher attributeMatcher) {
		if (attributeMatcher == this) {
			return true;
		}

		List<Tag> tags = new ArrayList<Tag>(this.keys.size());
		for (int i = 0, n = this.keys.size(); i < n; ++i) {
			tags.add(new Tag(this.keys.get(i), null));
		}
		return attributeMatcher.matches(tags);
	}

	@Override
	public boolean matches(List<Tag> tags) {
		for (int i = 0, n = tags.size(); i < n; ++i) {
			if (this.keys.contains(tags.get(i).key)) {
				return true;
			}
		}
		return false;
	}
}
