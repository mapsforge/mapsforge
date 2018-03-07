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

import org.mapsforge.core.model.Tag;

import java.util.HashMap;
import java.util.List;

final class TextKey {
    private static final HashMap<String, TextKey> textKeys = new HashMap<>();

    static TextKey getInstance(String key) {
        TextKey textKey = textKeys.get(key);
        if (textKey == null) {
            textKey = new TextKey(key);
            textKeys.put(key, textKey);
        }
        return textKey;
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
