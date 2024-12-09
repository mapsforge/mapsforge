/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2024 Sublimis
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
package org.mapsforge.map.layer;

import org.junit.Assert;
import org.junit.Test;
import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.Display;
import org.mapsforge.core.graphics.Matrix;
import org.mapsforge.core.mapelements.MapElementContainer;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Rectangle;
import org.mapsforge.core.model.Rotation;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.util.LayerUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class LayerUtilTest {
    private static final int[] TILE_SIZES = {256, 128, 376, 512, 100};

    static class MyDummyContainer extends MapElementContainer {
        public static final int Width = 100;
        public static final int Height = 10;

        protected final Rectangle boundary;

        protected MyDummyContainer(Point xy, Display display, int priority) {
            super(xy, display, priority);
            boundary = new Rectangle(0, 0, Width, Height);
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
    public void getTilePositionsTest() {
        for (int tileSize : TILE_SIZES) {
            BoundingBox boundingBox = new BoundingBox(-1, -1, 1, 1);
            List<TilePosition> tilePositions = LayerUtil.getTilePositions(boundingBox, (byte) 0, new Point(0, 0),
                    tileSize);
            Assert.assertEquals(1, tilePositions.size());

            TilePosition tilePosition = tilePositions.get(0);
            Assert.assertEquals(new Tile(0, 0, (byte) 0, tileSize), tilePosition.tile);
            Assert.assertEquals(new Point(0, 0), tilePosition.point);
        }
    }

    @Test
    public void collisionAndContestingFreeOrderedTest() {
        final Tile tile = new Tile(0, 0, (byte) 1, 256);
        final Point tileOrigin = tile.getOrigin();

        final MapElementContainer container1 = new MyDummyContainer(new Point(220, 105).offset(tileOrigin.x, tileOrigin.y), Display.ALWAYS, 1);
        final MapElementContainer container2 = new MyDummyContainer(new Point(200, 100).offset(tileOrigin.x, tileOrigin.y), Display.ALWAYS, 1);
        final MapElementContainer container3 = new MyDummyContainer(new Point(200, 200).offset(tileOrigin.x, tileOrigin.y), Display.ALWAYS, 0);

        final List<MapElementContainer> mainList = Arrays.asList(container1, container2, container3);


        Collections.sort(mainList, Collections.reverseOrder());

        // The list is already sorted
        Assert.assertSame(mainList.get(0), container1);
        Assert.assertSame(mainList.get(1), container2);


        final List<MapElementContainer> list1 = LayerUtil.collisionFreeOrdered(new ArrayList<>(mainList), Rotation.NULL_ROTATION, false);

        // One element should be removed, namely container2 as it clashes with container1
        // and at the same time has lower drawing priority because of its position
        Assert.assertEquals(list1.size(), 2);
        Assert.assertSame(list1.get(0), container1);
        Assert.assertSame(list1.get(1), container3);


        final List<MapElementContainer> list2 = LayerUtil.collisionAndContestingFreeOrdered(new ArrayList<>(mainList), tile, Rotation.NULL_ROTATION, false);

        // Two elements should be removed, specifically container1 and container2 because they are multi-tiled
        // and also clash with each other (their drawing priority is defined to be the same in this case)
        Assert.assertEquals(list2.size(), 1);

        // Only container3 survives, although it is multi-tiled, because its position is not contested by other labels
        Assert.assertSame(list2.get(0), container3);
    }
}
