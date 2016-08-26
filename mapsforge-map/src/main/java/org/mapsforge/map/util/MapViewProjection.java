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
package org.mapsforge.map.util;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.view.MapView;

public class MapViewProjection {
    private static final String INVALID_MAP_VIEW_DIMENSIONS = "invalid MapView dimensions";

    private final MapView mapView;

    public MapViewProjection(MapView mapView) {
        this.mapView = mapView;
    }

    /**
     * Computes the geographic coordinates of a screen point.
     *
     * @return the coordinates of the x/y point
     */
    public LatLong fromPixels(double x, double y) {
        if (this.mapView.getWidth() <= 0 || this.mapView.getHeight() <= 0) {
            return null;
        }

        // this uses the framebuffer position, the mapview position can be out of sync with
        // what the user sees on the screen if an animation is in progress
        MapPosition mapPosition = this.mapView.getModel().frameBufferModel.getMapPosition();

        // this means somehow the mapview is not yet properly set up, see issue #308.
        if (mapPosition == null) {
            return null;
        }

        // calculate the pixel coordinates of the top left corner
        LatLong latLong = mapPosition.latLong;
        long mapSize = MercatorProjection.getMapSize(mapPosition.zoomLevel, this.mapView.getModel().displayModel.getTileSize());
        double pixelX = MercatorProjection.longitudeToPixelX(latLong.longitude, mapSize);
        double pixelY = MercatorProjection.latitudeToPixelY(latLong.latitude, mapSize);
        pixelX -= this.mapView.getWidth() >> 1;
        pixelY -= this.mapView.getHeight() >> 1;

        // catch outer map limits
        try {
            // convert the pixel coordinates to a LatLong and return it
            return new LatLong(MercatorProjection.pixelYToLatitude(pixelY + y, mapSize),
                    MercatorProjection.pixelXToLongitude(pixelX + x, mapSize));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Computes vertical extend of the map view.
     *
     * @return the latitude span of the map in degrees
     */
    public double getLatitudeSpan() {
        if (this.mapView.getWidth() > 0 && this.mapView.getHeight() > 0) {
            LatLong top = fromPixels(0, 0);
            LatLong bottom = fromPixels(0, this.mapView.getHeight());
            return Math.abs(top.latitude - bottom.latitude);
        }
        throw new IllegalStateException(INVALID_MAP_VIEW_DIMENSIONS);
    }

    /**
     * Computes horizontal extend of the map view.
     *
     * @return the longitude span of the map in degrees
     */
    public double getLongitudeSpan() {
        if (this.mapView.getWidth() > 0 && this.mapView.getHeight() > 0) {
            LatLong left = fromPixels(0, 0);
            LatLong right = fromPixels(this.mapView.getWidth(), 0);
            return Math.abs(left.longitude - right.longitude);
        }
        throw new IllegalStateException(INVALID_MAP_VIEW_DIMENSIONS);
    }

    /**
     * Converts geographic coordinates to view x/y coordinates in the map view.
     *
     * @param in the geographic coordinates
     * @return x/y view coordinates for the given location
     */
    public Point toPixels(LatLong in) {
        if (in == null || this.mapView.getWidth() <= 0 || this.mapView.getHeight() <= 0) {
            return null;
        }

        MapPosition mapPosition = this.mapView.getModel().mapViewPosition.getMapPosition();

        // this means somehow the mapview is not yet properly set up, see issue #308.
        if (mapPosition == null) {
            return null;
        }

        // calculate the pixel coordinates of the top left corner
        LatLong latLong = mapPosition.latLong;
        long mapSize = MercatorProjection.getMapSize(mapPosition.zoomLevel, this.mapView.getModel().displayModel.getTileSize());
        double pixelX = MercatorProjection.longitudeToPixelX(latLong.longitude, mapSize);
        double pixelY = MercatorProjection.latitudeToPixelY(latLong.latitude, mapSize);
        pixelX -= this.mapView.getWidth() >> 1;
        pixelY -= this.mapView.getHeight() >> 1;

        // create a new point and return it
        return new Point(
                (int) (MercatorProjection.longitudeToPixelX(in.longitude, mapSize) - pixelX),
                (int) (MercatorProjection.latitudeToPixelY(in.latitude, mapSize) - pixelY));
    }

}
