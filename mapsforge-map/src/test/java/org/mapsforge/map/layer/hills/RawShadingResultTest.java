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
import org.junit.Test;
import org.mapsforge.core.graphics.HillshadingBitmap;

import java.util.Arrays;

public class RawShadingResultTest {
    @Test
    public void fillOnePaddingTest() {
        byte[] array = new byte[]{
                0, 0, 0, 0, 0,
                0, 1, 2, 3, 0,
                0, 4, 5, 6, 0,
                0, 0, 0, 0, 0
        };

        ShadingAlgorithm.RawShadingResult result = new ShadingAlgorithm.RawShadingResult(array, 3, 2, 1);


        result.fillPadding(HillshadingBitmap.Border.EAST);
        check(new byte[]{
                0, 0, 0, 0, 0,
                0, 1, 2, 3, 3,
                0, 4, 5, 6, 6,
                0, 0, 0, 0, 0
        }, array, 5);


        result.fillPadding(HillshadingBitmap.Border.SOUTH);
        check(new byte[]{
                0, 0, 0, 0, 0,
                0, 1, 2, 3, 3,
                0, 4, 5, 6, 6,
                0, 4, 5, 6, 0
        }, array, 5);

        result.fillPadding(HillshadingBitmap.Border.NORTH);
        check(new byte[]{
                0, 1, 2, 3, 0,
                0, 1, 2, 3, 3,
                0, 4, 5, 6, 6,
                0, 4, 5, 6, 0
        }, array, 5);

        result.fillPadding(HillshadingBitmap.Border.WEST);
        check(new byte[]{
                0, 1, 2, 3, 0,
                1, 1, 2, 3, 3,
                4, 4, 5, 6, 6,
                0, 4, 5, 6, 0
        }, array, 5);


        result.fillPadding();
        check(new byte[]{
                1, 1, 2, 3, 3,
                1, 1, 2, 3, 3,
                4, 4, 5, 6, 6,
                4, 4, 5, 6, 6
        }, array, 5);
    }

    @Test
    public void fillTwoPaddingTest() {
        byte[] array = new byte[]{
                0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0,
                0, 0, 1, 2, 3, 0, 0,
                0, 0, 4, 5, 6, 0, 0,
                0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0
        };

        ShadingAlgorithm.RawShadingResult result = new ShadingAlgorithm.RawShadingResult(array, 3, 2, 2);


        result.fillPadding(HillshadingBitmap.Border.EAST);
        check(new byte[]{
                0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0,
                0, 0, 1, 2, 3, 3, 3,
                0, 0, 4, 5, 6, 6, 6,
                0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0
        }, array, 7);
        result.fillPadding(HillshadingBitmap.Border.WEST);
        check(new byte[]{
                0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0,
                1, 1, 1, 2, 3, 3, 3,
                4, 4, 4, 5, 6, 6, 6,
                0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0
        }, array, 7);

        result.fillPadding(HillshadingBitmap.Border.NORTH);
        check(new byte[]{
                0, 0, 1, 2, 3, 0, 0,
                0, 0, 1, 2, 3, 0, 0,
                1, 1, 1, 2, 3, 3, 3,
                4, 4, 4, 5, 6, 6, 6,
                0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0
        }, array, 7);

        result.fillPadding(HillshadingBitmap.Border.SOUTH);
        check(new byte[]{
                0, 0, 1, 2, 3, 0, 0,
                0, 0, 1, 2, 3, 0, 0,
                1, 1, 1, 2, 3, 3, 3,
                4, 4, 4, 5, 6, 6, 6,
                0, 0, 4, 5, 6, 0, 0,
                0, 0, 4, 5, 6, 0, 0
        }, array, 7);


        result.fillPadding();
        check(new byte[]{
                1, 1, 1, 2, 3, 3, 3,
                1, 1, 1, 2, 3, 3, 3,
                1, 1, 1, 2, 3, 3, 3,
                4, 4, 4, 5, 6, 6, 6,
                4, 4, 4, 5, 6, 6, 6,
                4, 4, 4, 5, 6, 6, 6
        }, array, 7);
    }

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
