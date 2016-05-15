/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
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
package org.mapsforge.map.layer.renderer;

import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Tag;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.LatLongUtils;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.datastore.Way;

import java.util.List;

/**
 * A PolylineContainer encapsulates the way data retrieved from a map file.
 * <p/>
 * The class uses deferred evaluation for computing the absolute and relative
 * pixel coordinates of the way as many ways will not actually be rendered on a
 * map. In order to save memory, after evaluation, the internally stored way is
 * released.
 */

public class PolylineContainer implements ShapeContainer {

    private Point center;
    private Point[][] coordinatesAbsolute;
    private Point[][] coordinatesRelativeToTile;
    private final List<Tag> tags;
    private final byte layer;
    private final Tile upperLeft;
    private final Tile lowerRight;
    private final boolean isClosedWay;
    private Way way;

    public PolylineContainer(Way way, Tile upperLeft, Tile lowerRight) {
        this.coordinatesAbsolute = null;
        this.coordinatesRelativeToTile = null;
        this.tags = way.tags;
        this.upperLeft = upperLeft;
        this.lowerRight = lowerRight;
        layer = way.layer;
        this.way = way;
        this.isClosedWay = LatLongUtils.isClosedWay(way.latLongs[0]);
    }

    public PolylineContainer(Point[] coordinates, final Tile upperLeft, final Tile lowerRight, List<Tag> tags) {
        this.coordinatesAbsolute = new Point[1][];
        this.coordinatesRelativeToTile = null;
        this.coordinatesAbsolute[0] = new Point[coordinates.length];
        System.arraycopy(coordinates, 0, coordinatesAbsolute[0], 0, coordinates.length);
        this.tags = tags;
        this.upperLeft = upperLeft;
        this.lowerRight = lowerRight;
        this.layer = 0;
        isClosedWay = coordinates[0].equals(coordinates[coordinates.length - 1]);
    }

    public Point getCenterAbsolute() {
        if (null == center) {
            this.center = GeometryUtils.calculateCenterOfBoundingBox(getCoordinatesAbsolute()[0]);
        }
        return this.center;
    }

    public Point[][] getCoordinatesAbsolute() {
        // deferred evaluation as some PolyLineContainers will never be drawn. However,
        // to save memory, after computing the absolute coordinates, the way is released.
        if (coordinatesAbsolute == null) {
            coordinatesAbsolute = new Point[way.latLongs.length][];
            for (int i = 0; i < way.latLongs.length; ++i) {
                coordinatesAbsolute[i] = new Point[way.latLongs[i].length];
                for (int j = 0; j < way.latLongs[i].length; ++j) {
                    coordinatesAbsolute[i][j] = MercatorProjection.getPixelAbsolute(way.latLongs[i][j], upperLeft.mapSize);
                }
            }
            this.way = null;
        }
        return coordinatesAbsolute;
    }

    public Point[][] getCoordinatesRelativeToOrigin() {
        if (coordinatesRelativeToTile == null) {
            Point tileOrigin = upperLeft.getOrigin();
            coordinatesRelativeToTile = new Point[getCoordinatesAbsolute().length][];
            for (int i = 0; i < coordinatesRelativeToTile.length; ++i) {
                coordinatesRelativeToTile[i] = new Point[coordinatesAbsolute[i].length];
                for (int j = 0; j < coordinatesRelativeToTile[i].length; ++j) {
                    coordinatesRelativeToTile[i][j] = coordinatesAbsolute[i][j].offset(-tileOrigin.x, -tileOrigin.y);
                }
            }
        }
        return coordinatesRelativeToTile;
    }

    public byte getLayer() {
        return layer;
    }

    @Override
    public ShapeType getShapeType() {
        return ShapeType.POLYLINE;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public boolean isClosedWay() {
        return isClosedWay;
    }

    public Tile getUpperLeft() {
        return this.upperLeft;
    }

    public Tile getLowerRight() {
        return this.lowerRight;
    }
}
