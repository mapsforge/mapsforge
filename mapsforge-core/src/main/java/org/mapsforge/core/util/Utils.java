/*
 * Copyright 2016 devemux86
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
package org.mapsforge.core.util;

import java.util.List;

public final class Utils {

    /**
     * Null safe equals.
     */
    public static boolean equals(Object o1, Object o2) {
        return (o1 == o2) || (o1 != null && o1.equals(o2));
    }

    private Utils() {
        throw new IllegalStateException();
    }

    // UTILS FOR RULES

    public static int hashTagParameter(String keyValue) {
        return keyValue.hashCode();
    }

    public static int[] convertListString(List<String> list) {
        int[] values = new int[list.size()];
        for (int i = 0, m = list.size(); i < m; i++) {
            values[i] = hashTagParameter(list.get(i));
        }
        return values;
    }

    public static boolean contains(int[] data, int item) {
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < data.length; i++) {
            if (data[i] == item) {
                return true;
            }
        }
        return false;
    }
}
