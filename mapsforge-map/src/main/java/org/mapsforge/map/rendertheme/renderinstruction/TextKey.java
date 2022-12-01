/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2018 b3nn0
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
import org.mapsforge.core.util.Utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class TextKey {
    private static final Map<Integer, TextKey> TEXT_KEYS = new HashMap<>();

    static TextKey getInstance(String key) {
        int keyCode = Utils.hashTagParameter(key);
        TextKey textKey = TEXT_KEYS.get(keyCode);
        if (textKey == null) {
            textKey = new TextKey(keyCode);
            TEXT_KEYS.put(keyCode, textKey);
        }
        return textKey;
    }

    private final int keyCode;

    private TextKey(int key) {
        this.keyCode = key;
    }

    String getValue(List<Tag> tags) {
        for (int i = 0, n = tags.size(); i < n; ++i) {
            if (this.keyCode == tags.get(i).keyCode) {
                return tags.get(i).value;
            }
        }
        return null;
    }
}
