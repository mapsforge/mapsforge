/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2016 devemux86
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

import org.mapsforge.core.model.*;
import org.mapsforge.core.util.MercatorProjection;

public final class MapPositionUtil {
    public static BoundingBox getBoundingBox(MapPosition mapPosition, Rotation rotation, int tileSize,
                                             Dimension canvasDimension, float mapViewCenterX, float mapViewCenterY) {
        long mapSize = MercatorProjection.getMapSize(mapPosition.zoomLevel, tileSize);
        double pixelX = MercatorProjection.longitudeToPixelX(mapPosition.latLong.longitude, mapSize);
        double pixelY = MercatorProjection.latitudeToPixelY(mapPosition.latLong.latitude, mapSize);

        float halfCanvasWidth = canvasDimension.width * 0.5f;
        float halfCanvasHeight = canvasDimension.height * 0.5f;
        float offsetX = canvasDimension.width * (mapViewCenterX - 0.5f);
        float offsetY = canvasDimension.height * (mapViewCenterY - 0.5f);

        double left = pixelX - halfCanvasWidth - offsetX;
        double top = pixelY - halfCanvasHeight - offsetY;
        double right = pixelX + halfCanvasWidth - offsetX;
        double bottom = pixelY + halfCanvasHeight - offsetY;

        double pixelXMin = Math.max(0, left);
        double pixelYMin = Math.max(0, top);
        double pixelXMax = Math.min(mapSize, right);
        double pixelYMax = Math.min(mapSize, bottom);

        double minLatitude;
        double minLongitude;
        double maxLatitude;
        double maxLongitude;
        if (!Rotation.noRotation(rotation)) {
            Rotation mapRotation = new Rotation(rotation.degrees, (float) pixelX, (float) pixelY);
            Point lowerRight = mapRotation.rotate(right, bottom);
            Point upperLeft = mapRotation.rotate(left, top);
            Point lowerLeft = mapRotation.rotate(left, bottom);
            Point upperRight = mapRotation.rotate(right, top);
            Rectangle envelope = new Rectangle(lowerLeft, upperLeft, lowerRight, upperRight);
            minLatitude = MercatorProjection.pixelYToLatitude(Math.max(0, Math.min(mapSize, envelope.bottom)), mapSize);
            minLongitude = MercatorProjection.pixelXToLongitude(Math.max(0, Math.min(mapSize, envelope.left)), mapSize);
            maxLatitude = MercatorProjection.pixelYToLatitude(Math.max(0, Math.min(mapSize, envelope.top)), mapSize);
            maxLongitude = MercatorProjection.pixelXToLongitude(Math.max(0, Math.min(mapSize, envelope.right)), mapSize);
        } else {
            minLatitude = MercatorProjection.pixelYToLatitude(pixelYMax, mapSize);
            minLongitude = MercatorProjection.pixelXToLongitude(pixelXMin, mapSize);
            maxLatitude = MercatorProjection.pixelYToLatitude(pixelYMin, mapSize);
            maxLongitude = MercatorProjection.pixelXToLongitude(pixelXMax, mapSize);
        }

        return new BoundingBox(minLatitude, minLongitude, maxLatitude, maxLongitude);
    }

    public static Point getTopLeftPoint(MapPosition mapPosition, Dimension canvasDimension, int tileSize) {
        LatLong centerPoint = mapPosition.latLong;

        int halfCanvasWidth = canvasDimension.width / 2;
        int halfCanvasHeight = canvasDimension.height / 2;

        long mapSize = MercatorProjection.getMapSize(mapPosition.zoomLevel, tileSize);
        double pixelX = Math.round(MercatorProjection.longitudeToPixelX(centerPoint.longitude, mapSize));
        double pixelY = Math.round(MercatorProjection.latitudeToPixelY(centerPoint.latitude, mapSize));
        return new Point((long) pixelX - halfCanvasWidth, (long) pixelY - halfCanvasHeight);
    }

    private MapPositionUtil() {
        throw new IllegalStateException();
    }
}
