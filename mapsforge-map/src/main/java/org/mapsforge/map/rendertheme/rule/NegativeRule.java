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

class NegativeRule extends Rule {

    final NegativeMatcher attributeMatcher;

    /* (-) 'exclusive negation' matches when either KEY is not present
     * or KEY is present and any VALUE is NOT present
     *
     * (\) 'except negation' matches when KEY is present
     * none items of VALUE is present (TODO).
     * (can be emulated by <rule k="a"><rule k=a v="-|b|c">...</rule></rule>)
     *
     * (~) 'non-exclusive negation' matches when either KEY is not present
     * or KEY is present and any VALUE is present */

    final boolean exclusive;

    NegativeRule(RuleBuilder ruleBuilder, NegativeMatcher attributeMatcher, boolean exclusive) {
        super(ruleBuilder);

        this.attributeMatcher = attributeMatcher;
        this.exclusive = exclusive;
    }

    @Override
    boolean matchesNode(List<Tag> tags, byte zoomLevel) {
        return this.zoomMin <= zoomLevel
                && this.zoomMax >= zoomLevel
                && this.elementMatcher.matches(Element.NODE)
                && matchesTags(tags);
    }

    @Override
    boolean matchesWay(List<Tag> tags, byte zoomLevel, Closed closed) {
        return this.zoomMin <= zoomLevel
                && this.zoomMax >= zoomLevel
                && this.elementMatcher.matches(Element.WAY)
                && this.closedMatcher.matches(closed)
                && matchesTags(tags);
    }

    private boolean matchesTags(List<Tag> tags) {
        if (attributeMatcher.keyListDoesNotContainKeys(tags)) {
            return true;
        }

        // check tags
        for (int i = 0, n = tags.size(); i < n; i++) {
            if (attributeMatcher.matches(tags.get(i))) {
                return !exclusive;
            }
        }
        return exclusive;
    }
}
