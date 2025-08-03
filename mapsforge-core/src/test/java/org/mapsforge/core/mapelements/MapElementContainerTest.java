/*
 * Copyright 2024-2025 Sublimis
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
package org.mapsforge.core.mapelements;

import org.junit.Assert;
import org.junit.Test;
import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.Display;
import org.mapsforge.core.graphics.Matrix;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Rectangle;
import org.mapsforge.core.model.Rotation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MapElementContainerTest {

    static class MyDummyContainer extends MapElementContainer {
        protected final Rectangle boundary;

        protected MyDummyContainer(Point xy, Display display, int priority) {
            super(xy, display, priority);
            boundary = new Rectangle(0, 0, 10, 10);
        }

        @Override
        protected Rectangle getBoundary() {
            return boundary;
        }

        @Override
        public void draw(Canvas canvas, Point origin, Matrix matrix, Rotation rotation) {
        }
    }

    @Test
    public void testCompareTo() {

        // Some map element containers
        final MapElementContainer container1 = new MyDummyContainer(new Point(0, 0), Display.ALWAYS, 0);
        final MapElementContainer container2 = new MyDummyContainer(new Point(10, 0), Display.ALWAYS, 0);
        final MapElementContainer container3 = new MyDummyContainer(new Point(10, 10), Display.ALWAYS, 0);
        final MapElementContainer container3_2 = new MyDummyContainer(new Point(10, 10), Display.ALWAYS, 0);
        final MapElementContainer container4 = new MyDummyContainer(new Point(0, 0), Display.FORCED, 0);
        final MapElementContainer container5 = new MyDummyContainer(new Point(10, 0), Display.FORCED, 0);

        // The containers all have equal priorities, but should not be considered equal
        Assert.assertTrue(0 > container1.compareTo(container2));
        Assert.assertTrue(0 < container2.compareTo(container1));
        Assert.assertTrue(0 > container1.compareTo(container3));
        Assert.assertTrue(0 < container3.compareTo(container1));
        Assert.assertTrue(0 > container2.compareTo(container3));
        Assert.assertTrue(0 < container3.compareTo(container2));
        Assert.assertTrue(0 > container4.compareTo(container5));
        Assert.assertTrue(0 < container5.compareTo(container4));

        // The containers should be equal
        Assert.assertEquals(0, container3.compareTo(container3_2));

        // The list is already sorted
        final List<MapElementContainer> list1 = Arrays.asList(container1, container2, container3, container3_2, container4, container5);
        final List<MapElementContainer> list2 = new ArrayList<>(list1);

        // This sort should not change an already sorted list (guaranteed to be stable)
        Collections.sort(list2);

        Assert.assertSame(list1.get(0), list2.get(0));
        Assert.assertSame(list1.get(1), list2.get(1));
        Assert.assertSame(list2.get(2), container3);
        Assert.assertSame(list1.get(4), list2.get(4));
        Assert.assertSame(list1.get(5), list2.get(5));
    }
}
