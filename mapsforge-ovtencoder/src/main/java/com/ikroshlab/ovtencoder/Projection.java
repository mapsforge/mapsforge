/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 *
 * This file is part of the OpenScienceMap project (http://www.opensciencemap.org).
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
package com.ikroshlab.ovtencoder;

import org.oscim.core.Tile;


public class Projection {

    /**
     * Converts a tile X number at a certain zoom level to a longitude
     * coordinate.
     *
     * @param tileX     the tile X number that should be converted.
     * @param zoomLevel the zoom level at which the number should be converted.
     * @return the longitude value of the tile X number.
     */
    public static double tileXToLongitude(long tileX, int zoomLevel) {
        return pixelXToLongitude(tileX * Tile.SIZE, zoomLevel);
    }

    /**
     * Converts a tile Y number at a certain zoom level to a latitude
     * coordinate.
     *
     * @param tileY     the tile Y number that should be converted.
     * @param zoomLevel the zoom level at which the number should be converted.
     * @return the latitude value of the tile Y number.
     */
    public static double tileYToLatitude(long tileY, int zoomLevel) {
        return pixelYToLatitude(tileY * Tile.SIZE, zoomLevel);
    }

    /**
     * Converts a latitude coordinate (in degrees) to a tile Y number at a
     * certain zoom level.
     *
     * @param latitude  the latitude coordinate that should be converted.
     * @param zoomLevel the zoom level at which the coordinate should be converted.
     * @return the tile Y number of the latitude value.
     */
    public static long latitudeToTileY(double latitude, int zoomLevel) {
        return pixelYToTileY(latitudeToPixelY(latitude, zoomLevel), zoomLevel);
    }

    /**
     * Converts a longitude coordinate (in degrees) to the tile X number at a
     * certain zoom level.
     *
     * @param longitude the longitude coordinate that should be converted.
     * @param zoomLevel the zoom level at which the coordinate should be converted.
     * @return the tile X number of the longitude value.
     */
    public static long longitudeToTileX(double longitude, int zoomLevel) {
        return pixelXToTileX(longitudeToPixelX(longitude, zoomLevel), zoomLevel);
    }

    /**
     * Converts a pixel X coordinate to the tile X number.
     *
     * @param pixelX    the pixel X coordinate that should be converted.
     * @param zoomLevel the zoom level at which the coordinate should be converted.
     * @return the tile X number.
     */
    public static int pixelXToTileX(double pixelX, int zoomLevel) {
        return (int) Math.min(Math.max(pixelX / Tile.SIZE, 0),
                Math.pow(2, zoomLevel) - 1);
    }

    /**
     * Converts a pixel Y coordinate to the tile Y number.
     *
     * @param pixelY    the pixel Y coordinate that should be converted.
     * @param zoomLevel the zoom level at which the coordinate should be converted.
     * @return the tile Y number.
     */
    public static int pixelYToTileY(double pixelY, int zoomLevel) {
        return (int) Math.min(Math.max(pixelY / Tile.SIZE, 0),
                Math.pow(2, zoomLevel) - 1);
    }

    /**
     * Converts a pixel X coordinate at a certain zoom level to a longitude
     * coordinate.
     *
     * @param pixelX    the pixel X coordinate that should be converted.
     * @param zoomLevel the zoom level at which the coordinate should be converted.
     * @return the longitude value of the pixel X coordinate.
     */
    public static double pixelXToLongitude(double pixelX, int zoomLevel) {
        return 360 * ((pixelX / ((long) Tile.SIZE << zoomLevel)) - 0.5);
    }

    /**
     * Converts a longitude coordinate (in degrees) to a pixel X coordinate at a
     * certain zoom level.
     *
     * @param longitude the longitude coordinate that should be converted.
     * @param zoomLevel the zoom level at which the coordinate should be converted.
     * @return the pixel X coordinate of the longitude value.
     */
    public static double longitudeToPixelX(double longitude, int zoomLevel) {
        return (longitude + 180) / 360 * ((long) Tile.SIZE << zoomLevel);
    }

    /**
     * Converts a pixel Y coordinate at a certain zoom level to a latitude
     * coordinate.
     *
     * @param pixelY    the pixel Y coordinate that should be converted.
     * @param zoomLevel the zoom level at which the coordinate should be converted.
     * @return the latitude value of the pixel Y coordinate.
     */
    public static double pixelYToLatitude(double pixelY, int zoomLevel) {
        double y = 0.5 - (pixelY / ((long) Tile.SIZE << zoomLevel));
        return 90 - 360 * Math.atan(Math.exp(-y * (2 * Math.PI))) / Math.PI;
    }

    /**
     * Converts a latitude coordinate (in degrees) to a pixel Y coordinate at a
     * certain zoom level.
     *
     * @param latitude  the latitude coordinate that should be converted.
     * @param zoomLevel the zoom level at which the coordinate should be converted.
     * @return the pixel Y coordinate of the latitude value.
     */
    public static double latitudeToPixelY(double latitude, int zoomLevel) {
        double sinLatitude = Math.sin(latitude * (Math.PI / 180));
        return (0.5 - Math.log((1 + sinLatitude) / (1 - sinLatitude)) / (4 * Math.PI))
                * ((long) Tile.SIZE << zoomLevel);
    }

    private Projection() {

    }
}
