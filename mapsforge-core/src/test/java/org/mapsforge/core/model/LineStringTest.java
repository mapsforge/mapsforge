/*
 * Copyright 2019 Adrian Batzill
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

public class LineStringTest {
    @Test
    public void boundsTest() {
        Point point1 = new Point(0, 0);
        Point point2 = new Point(1, 0);
        Point point3 = new Point(1, 1);

        LineString lineString = new LineString();
        lineString.segments.add(new LineSegment(point1, point2));
        lineString.segments.add(new LineSegment(point2, point3));

        Assert.assertEquals(new Rectangle(0, 0, 1, 1), lineString.getBounds());
    }

    @Test
    public void extractPartTest() {
        Point point1 = new Point(0, 0);
        Point point2 = new Point(1, 0);
        Point point3 = new Point(1, 1);

        LineString lineString = new LineString();
        lineString.segments.add(new LineSegment(point1, point2));
        lineString.segments.add(new LineSegment(point2, point3));

        LineString extract1 = new LineString();
        extract1.segments.add(new LineSegment(new Point(0, 0), new Point(0.5, 0)));

        LineString extract2 = new LineString();
        extract2.segments.add(new LineSegment(new Point(0.5, 0), new Point(1, 0)));
        extract2.segments.add(new LineSegment(new Point(1, 0), new Point(1, 0.5)));

        LineString extract3 = new LineString();
        extract3.segments.add(new LineSegment(new Point(1, 0.5), new Point(1, 1)));

        Assert.assertEquals(extract1, lineString.extractPart(0, 0.5));
        Assert.assertEquals(extract2, lineString.extractPart(0.5, 1.5));
        Assert.assertEquals(extract3, lineString.extractPart(1.5, 2));
    }

    @Test
    public void interpolateTest() {
        Point point1 = new Point(0, 0);
        Point point2 = new Point(1, 0);
        Point point3 = new Point(1, 1);

        LineString lineString = new LineString();
        lineString.segments.add(new LineSegment(point1, point2));
        lineString.segments.add(new LineSegment(point2, point3));

        Assert.assertEquals(point1, lineString.interpolate(0));
        Assert.assertEquals(new Point(0.5, 0), lineString.interpolate(0.5));
        Assert.assertEquals(point2, lineString.interpolate(1));
        Assert.assertEquals(new Point(1, 0.5), lineString.interpolate(1.5));
        Assert.assertEquals(point3, lineString.interpolate(2));
    }

    @Test
    public void lengthTest() {
        Point point1 = new Point(0, 0);
        Point point2 = new Point(1, 0);
        Point point3 = new Point(1, 1);
        Point point4 = new Point(3, 1);

        LineString lineString = new LineString();
        lineString.segments.add(new LineSegment(point1, point2));
        lineString.segments.add(new LineSegment(point2, point3));
        lineString.segments.add(new LineSegment(point3, point4));

        Assert.assertEquals(4, lineString.length(), 0.0001);
        Assert.assertEquals(3, lineString.segments.size());
    }
}
