/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2015 devemux86
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
package org.mapsforge.map;

import org.junit.Assert;

public final class TestUtils {
    public static void equalsTest(Object object1, Object object2) {
        Assert.assertEquals(object1, object1);
        Assert.assertEquals(object2, object2);

        Assert.assertEquals(object1.hashCode(), object2.hashCode());
        Assert.assertEquals(object1, object2);
        Assert.assertEquals(object2, object1);
    }

    public static void notEqualsTest(Object object1, Object object2) {
        Assert.assertNotEquals(object1, object2);
        Assert.assertNotEquals(object2, object1);
    }

    private TestUtils() {
        throw new IllegalArgumentException();
    }
}
