/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014-2015 Ludwig M Brinckmann
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
package org.mapsforge.map.util;

import org.mapsforge.core.mapelements.MapElementContainer;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Rotation;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.layer.TilePosition;

import java.util.*;

public final class LayerUtil {

    public static List<TilePosition> getTilePositions(BoundingBox boundingBox, byte zoomLevel, Point topLeftPoint,
                                                      int tileSize) {
        return getTilePositions(boundingBox, zoomLevel, topLeftPoint, tileSize, 0);
    }

    public static List<TilePosition> getTilePositions(BoundingBox boundingBox, byte zoomLevel, Point topLeftPoint,
                                                      int tileSize, int margin) {
        int maxTileNumber = Tile.getMaxTileNumber(zoomLevel);
        int tileLeft = Math.max(0, MercatorProjection.longitudeToTileX(boundingBox.minLongitude, zoomLevel) - margin);
        int tileTop = Math.max(0, MercatorProjection.latitudeToTileY(boundingBox.maxLatitude, zoomLevel) - margin);
        int tileRight = Math.min(MercatorProjection.longitudeToTileX(boundingBox.maxLongitude, zoomLevel) + margin, maxTileNumber);
        int tileBottom = Math.min(MercatorProjection.latitudeToTileY(boundingBox.minLatitude, zoomLevel) + margin, maxTileNumber);

        int initialCapacity = (tileRight - tileLeft + 1) * (tileBottom - tileTop + 1);
        List<TilePosition> tilePositions = new ArrayList<>(initialCapacity);

        for (int tileY = tileTop; tileY <= tileBottom; ++tileY) {
            for (int tileX = tileLeft; tileX <= tileRight; ++tileX) {
                double pixelX = MercatorProjection.tileToPixel(tileX, tileSize) - topLeftPoint.x;
                double pixelY = MercatorProjection.tileToPixel(tileY, tileSize) - topLeftPoint.y;

                tilePositions.add(new TilePosition(new Tile(tileX, tileY, zoomLevel, tileSize), new Point(pixelX, pixelY)));
            }
        }

        return tilePositions;
    }

    /**
     * Upper left tile for an area.
     *
     * @param boundingBox the area boundingBox
     * @param zoomLevel   the zoom level.
     * @param tileSize    the tile size.
     * @return the tile at the upper left of the bbox.
     */
    public static Tile getUpperLeft(BoundingBox boundingBox, byte zoomLevel, int tileSize) {
        int tileLeft = MercatorProjection.longitudeToTileX(boundingBox.minLongitude, zoomLevel);
        int tileTop = MercatorProjection.latitudeToTileY(boundingBox.maxLatitude, zoomLevel);
        return new Tile(tileLeft, tileTop, zoomLevel, tileSize);
    }

    /**
     * Lower right tile for an area.
     *
     * @param boundingBox the area boundingBox
     * @param zoomLevel   the zoom level.
     * @param tileSize    the tile size.
     * @return the tile at the lower right of the bbox.
     */
    public static Tile getLowerRight(BoundingBox boundingBox, byte zoomLevel, int tileSize) {
        int tileRight = MercatorProjection.longitudeToTileX(boundingBox.maxLongitude, zoomLevel);
        int tileBottom = MercatorProjection.latitudeToTileY(boundingBox.minLatitude, zoomLevel);
        return new Tile(tileRight, tileBottom, zoomLevel, tileSize);
    }

    public static Set<Tile> getTiles(Tile upperLeft, Tile lowerRight) {
        Set<Tile> tiles = new HashSet<>();
        for (int tileY = upperLeft.tileY; tileY <= lowerRight.tileY; ++tileY) {
            for (int tileX = upperLeft.tileX; tileX <= lowerRight.tileX; ++tileX) {
                tiles.add(new Tile(tileX, tileY, upperLeft.zoomLevel, upperLeft.tileSize));
            }
        }
        return tiles;
    }

    public static Set<Tile> getTiles(BoundingBox boundingBox, byte zoomLevel, int tileSize) {
        int tileLeft = MercatorProjection.longitudeToTileX(boundingBox.minLongitude, zoomLevel);
        int tileTop = MercatorProjection.latitudeToTileY(boundingBox.maxLatitude, zoomLevel);
        int tileRight = MercatorProjection.longitudeToTileX(boundingBox.maxLongitude, zoomLevel);
        int tileBottom = MercatorProjection.latitudeToTileY(boundingBox.minLatitude, zoomLevel);

        Set<Tile> tiles = new HashSet<>();

        for (int tileY = tileTop; tileY <= tileBottom; ++tileY) {
            for (int tileX = tileLeft; tileX <= tileRight; ++tileX) {
                tiles.add(new Tile(tileX, tileY, zoomLevel, tileSize));
            }
        }
        return tiles;
    }

    /**
     * Transforms a list of MapElements, orders it and removes those elements that overlap.
     * This operation is useful for an early elimination of elements in a list that will never
     * be drawn because they overlap.
     *
     * @param input list of MapElements
     * @return collision-free, ordered list, a subset of the input.
     */
    public static List<MapElementContainer> collisionFreeOrdered(List<MapElementContainer> input, Rotation rotation, boolean ascendingOrder) {
        final List<MapElementContainer> output = new LinkedList<>();

        // sort items by priority (highest first)
        Collections.sort(input, Collections.reverseOrder());

        // in order of display and priority, see if an item can be drawn, i.e. none of the items
        // in the currentItemsToDraw list clashes with it.
        for (MapElementContainer item : input) {
            boolean hasSpace = true;
            for (MapElementContainer outputElement : output) {
                if (outputElement.clashesWith(item, rotation)) {
                    hasSpace = false;
                    break;
                }
            }
            if (hasSpace) {
                output.add(item);
            }
        }

        if (ascendingOrder) {
            Collections.reverse(output);
        }

        return output;
    }

    /**
     * Transforms a list of MapElements, orders it and removes those elements that overlap.
     * This operation is useful for an early elimination of elements in a list that will never
     * be drawn because they overlap.
     *
     * @param input list of MapElements
     * @return collision-free, ordered list, a subset of the input.
     */
    public static List<MapElementContainer> collisionFreeOrdered(List<MapElementContainer> input, Rotation rotation) {
        return collisionFreeOrdered(input, rotation, true);
    }

    private LayerUtil() {
        throw new IllegalStateException();
    }
}
