/*
 * Copyright 2015 Ludwig M Brinckmann
 * Copyright 2024 devemux86
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
package org.mapsforge.core.model;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class RotationTest {

    @Test
    public void equalsTest() {
        Rotation Rotation1 = new Rotation(1, 2, 1);
        Rotation Rotation2 = new Rotation(1, 2, 1);
        Rotation Rotation3 = new Rotation(1, 1, 1);
        Rotation Rotation4 = new Rotation(2, 2, 4);

        TestUtils.equalsTest(Rotation1, Rotation2);

        TestUtils.notEqualsTest(Rotation1, Rotation3);
        TestUtils.notEqualsTest(Rotation1, Rotation4);
        TestUtils.notEqualsTest(Rotation1, new Object());
        TestUtils.notEqualsTest(Rotation1, null);
    }

    @Test
    public void rotationTest() {
        Rotation rotation1 = new Rotation(1, 0, 0);
        Rotation noRotation = new Rotation(0, 0, 0);
        Rotation fullRotation = new Rotation(360, 44, 31);
        Rotation halfRotation = new Rotation(180, 44, 31);
        Rotation aroundOrigin = new Rotation(90, 0, 0);

        Point origin = new Point(3344.22d, 4455.33d);
        Point tmp = rotation1.rotate(origin);
        Point result = rotation1.reverseRotation().rotate(tmp);

        Assert.assertEquals(origin.x, result.x, 0.0001d);
        Assert.assertEquals(origin.y, result.y, 0.0001d);

        result = noRotation.rotate(origin);
        Assert.assertEquals(origin, result);
        result = noRotation.reverseRotation().rotate(origin);
        Assert.assertEquals(origin, result);

        result = origin.rotate(aroundOrigin);
        Assert.assertEquals(origin.y, result.x, 0.0001);
        Assert.assertEquals(-origin.x, result.y, 0.0001);

        result = fullRotation.rotate(origin);
        Assert.assertEquals(origin.x, result.x, 0.0001);
        Assert.assertEquals(origin.y, result.y, 0.0001);
        result = fullRotation.reverseRotation().rotate(origin);
        Assert.assertEquals(origin.y, result.y, 0.0001);
        Assert.assertEquals(origin.x, result.x, 0.0001);

        result = halfRotation.rotate(origin);
        result = halfRotation.rotate(result);
        Assert.assertEquals(origin.x, result.x, 0.0001);
        Assert.assertEquals(origin.y, result.y, 0.0001);
        result = fullRotation.reverseRotation().rotate(result);
        Assert.assertEquals(origin.y, result.y, 0.0001);
        Assert.assertEquals(origin.x, result.x, 0.0001);

        result = origin.rotate(noRotation).rotate(halfRotation).rotate(noRotation).rotate(halfRotation);
        Assert.assertEquals(origin.y, result.y, 0.0001);
        Assert.assertEquals(origin.x, result.x, 0.0001);
    }

    @Test
    public void nullRotationTest() {
        Rotation noRotation = new Rotation(0, 0, 1);

        Point origin = new Point(22.22d, -44d);
        Point result = Rotation.NULL_ROTATION.rotate(origin);
        Assert.assertTrue(origin == result);

        Assert.assertTrue(Rotation.NULL_ROTATION.reverseRotation() == Rotation.NULL_ROTATION);

        result = noRotation.rotate(origin);
        Assert.assertEquals(origin, result);
        result = noRotation.reverseRotation().rotate(origin);
        Assert.assertEquals(origin, result);
        Assert.assertEquals(result, Rotation.NULL_ROTATION.rotate(origin));
        Assert.assertTrue(result == Rotation.NULL_ROTATION.rotate(origin));
    }

    @Test
    public void fieldsTest() {
        Rotation rotation = new Rotation(1, 2, 3);
        Assert.assertEquals(1, rotation.degrees, 0);
        Assert.assertEquals(2, rotation.px, 0);
        Assert.assertEquals(3, rotation.py, 0);
    }

    @Test
    public void serializeTest() throws IOException, ClassNotFoundException {
        Rotation rotation = new Rotation(1, 2, 1);
        TestUtils.serializeTest(rotation);
    }
}
