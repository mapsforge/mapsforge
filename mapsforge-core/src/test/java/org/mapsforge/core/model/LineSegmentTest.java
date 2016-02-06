/*
 * Copyright 2014 Ludwig M Brinckmann
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

public class LineSegmentTest {

    @Test
    public void ctorTest() {

        // tests the second ctor that computes a line segment from point, target and distance

        Point[] points = {new Point(1, 0), new Point(0, 1), new Point(0, 0),
                new Point(1, 1), new Point(2, 2), new Point(0, 4),
                new Point(-182.9934, 0), new Point(34.6, -356.1)};
        Point point1 = new Point(0, 0);
        Point point2 = new Point(1, 0);
        Point point3 = new Point(2, 2);

        LineSegment ls1 = new LineSegment(point1, point2, 0.5);
        Assert.assertEquals(new Point(0.5, 0), ls1.end);

        Assert.assertEquals(Math.sqrt(8), point1.distance(point3), 0.001d);

        for (Point point : points) {
            LineSegment ls2 = new LineSegment(point1, point, point1.distance(point));
            Assert.assertEquals(point, ls2.end);
        }

        LineSegment ls3 = new LineSegment(point1, point3, 0.5 * point1.distance(point3));
        Assert.assertEquals(new Point(1, 1), ls3.end);

    }


    @Test
    public void equalsTest() {
        Point point1 = new Point(1, 2);
        Point point2 = new Point(1, 2);
        Point point3 = new Point(1, 1);

        LineSegment point1ToPoint2 = new LineSegment(point1, point2);
        LineSegment point1ToPoint3 = new LineSegment(point1, point3);
        LineSegment point1ToPoint1 = new LineSegment(point1, point1);

        Assert.assertNotEquals(point1ToPoint1, point1ToPoint3);
        Assert.assertEquals(point1ToPoint1, point1ToPoint1);
        Assert.assertEquals(point1ToPoint1, point1ToPoint2);
    }

    @Test
    public void intersectionTest() {
        Point point1 = new Point(1, 2);
        // Point point2 = new Point(1, 2);
        Point point3 = new Point(1, 1);
        Point point4 = new Point(2, -22);
        Point point5 = new Point(2, 22);
        Point point6 = new Point(2, 0);
        Point point7 = new Point(2, 5);

        LineSegment point1ToPoint3 = new LineSegment(point1, point3);
        LineSegment point1ToPoint1 = new LineSegment(point1, point1);
        LineSegment vertical = new LineSegment(point4, point5);

        Rectangle r1 = new Rectangle(0, 0, 5, 5);
        Rectangle r2 = new Rectangle(-22, -22, -11, -11);

        LineSegment s1 = point1ToPoint3.clipToRectangle(r1);
        Assert.assertEquals(point1ToPoint3, s1);

        LineSegment s2 = point1ToPoint1.clipToRectangle(r1);
        Assert.assertEquals(s2, point1ToPoint1);

        LineSegment s3 = point1ToPoint1.clipToRectangle(r2);
        Assert.assertEquals(s3, null);

        LineSegment verticalClipped = new LineSegment(point6, point7);
        LineSegment s4 = vertical.clipToRectangle(r1);
        Assert.assertEquals(s4, verticalClipped);

        LineSegment s5 = vertical.clipToRectangle(r2);
        Assert.assertEquals(s5, null);

        Rectangle r3 = new Rectangle(-1, -1, 1, 1);
        Point point8 = new Point(10, 10);
        Point point9 = new Point(-10, -10);
        Point point10 = new Point(-1, -1);
        Point point11 = new Point(1, 1);
        LineSegment s6 = new LineSegment(point8, point9);
        LineSegment s7 = s6.clipToRectangle(r3);
        LineSegment s8 = new LineSegment(point11, point10);
        Assert.assertEquals(s8, s7);

        LineSegment s6r = new LineSegment(point9, point8);
        LineSegment s7r = s6r.clipToRectangle(r3);
        LineSegment s8r = new LineSegment(point10, point11);
        Assert.assertEquals(s8r, s7r);
    }
}
