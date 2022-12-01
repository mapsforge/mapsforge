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

import org.mapsforge.core.model.Tag;

import java.util.List;

class PositiveRule extends Rule {

    final AttributeMatcher keyMatcher;
    final AttributeMatcher valueMatcher;

    PositiveRule(RuleBuilder ruleBuilder, AttributeMatcher keyMatcher, AttributeMatcher valueMatcher) {
        super(ruleBuilder);

        this.keyMatcher = keyMatcher;
        this.valueMatcher = valueMatcher;
    }

    @Override
    boolean matchesNode(List<Tag> tags, byte zoomLevel) {
        return this.zoomMin <= zoomLevel
                && this.zoomMax >= zoomLevel
                && this.elementMatcher.matches(Element.NODE)
                && matchesAnyTag(tags);
    }

    @Override
    boolean matchesWay(List<Tag> tags, byte zoomLevel, Closed closed) {
        return this.zoomMin <= zoomLevel
                && this.zoomMax >= zoomLevel
                && this.elementMatcher.matches(Element.WAY)
                && this.closedMatcher.matches(closed)
                && matchesAnyTag(tags);
    }

    private boolean matchesAnyTag(List<Tag> tags) {
        for (int i = 0, n = tags.size(); i < n; i++) {
            Tag tag = tags.get(i);
            if (keyMatcher.matches(tag) && valueMatcher.matches(tag)) {
                return true;
            }
        }
        return false;
    }
}
