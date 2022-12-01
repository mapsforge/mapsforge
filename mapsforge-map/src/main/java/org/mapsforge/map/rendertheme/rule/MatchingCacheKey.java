/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
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

import org.mapsforge.core.model.Tag;
import org.mapsforge.core.util.Utils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

class MatchingCacheKey {
    private final Closed closed;
    private final Set<Tag> tagsWithoutName;
    private final byte zoomLevel;
    private static final int keyCodeName = Utils.hashTagParameter("name");

    MatchingCacheKey(List<Tag> tags, byte zoomLevel, Closed closed) {
        this.zoomLevel = zoomLevel;
        this.closed = closed;
        this.tagsWithoutName = new HashSet<>();
        if (tags != null) {
            for (int i = 0, n = tags.size(); i < n; i++) {
                Tag tag = tags.get(i);
                if (keyCodeName != tag.keyCode) {
                    this.tagsWithoutName.add(tag);
                }
            }
        }
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
        if (this.tagsWithoutName == null && other.tagsWithoutName != null) {
            return false;
        } else if (!this.tagsWithoutName.equals(other.tagsWithoutName)) {
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
        result = prime * result + this.tagsWithoutName.hashCode();
        result = prime * result + this.zoomLevel;
        return result;
    }
}
