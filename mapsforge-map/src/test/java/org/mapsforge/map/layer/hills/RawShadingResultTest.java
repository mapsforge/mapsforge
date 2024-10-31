/*
 * Copyright 2017 usrusr
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
package org.mapsforge.map.layer.hills;

import org.junit.Assert;

import java.util.Arrays;

public class RawShadingResultTest {
    private void check(byte[] expected, byte[] actual, int width) {
        Assert.assertEquals("should be same length " + Arrays.toString(expected) + " " + Arrays.toString(actual), expected.length, actual.length);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < actual.length; i++) {
            if (i % width == 0) sb.append("\n");
            byte x = expected[i];
            byte a = actual[i];
            if (x != actual[i]) {
                sb.append(" " + x + "!" + a + " ");
            } else {
                sb.append(" (" + a + ") ");
            }

        }
        Assert.assertArrayEquals(sb.toString(), expected, actual);
    }
}
