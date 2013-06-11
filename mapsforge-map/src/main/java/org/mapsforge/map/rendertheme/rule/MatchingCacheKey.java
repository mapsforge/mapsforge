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

import java.util.List;

import org.mapsforge.core.model.Tag;

class MatchingCacheKey {
	private final Closed closed;
	private final List<Tag> tags;
	private final byte zoomLevel;

	MatchingCacheKey(List<Tag> tags, byte zoomLevel, Closed closed) {
		this.tags = tags;
		this.zoomLevel = zoomLevel;
		this.closed = closed;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (!(obj instanceof MatchingCacheKey)) {
			return false;
		}
		MatchingCacheKey other = (MatchingCacheKey) obj;
		if (this.closed != other.closed) {
			return false;
		}
		if (this.tags == null) {
			if (other.tags != null) {
				return false;
			}
		} else if (!this.tags.equals(other.tags)) {
			return false;
		}
		if (this.zoomLevel != other.zoomLevel) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.closed == null) ? 0 : this.closed.hashCode());
		result = prime * result + ((this.tags == null) ? 0 : this.tags.hashCode());
		result = prime * result + this.zoomLevel;
		return result;
	}
}
