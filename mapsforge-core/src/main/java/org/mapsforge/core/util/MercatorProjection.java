/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2013 Stephan Brandt <stephan@contagt.com>
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
package org.mapsforge.core.util;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Tile;

/**
 * An implementation of the spherical Mercator projection.
 * <p/>
 * There are generally two methods for each operation: one taking a byte zoomlevel and
 * a parallel one taking a double scaleFactor. The scaleFactor is Math.pow(2, zoomLevel)
 * for a given zoomlevel, but it the operations take intermediate values as well.
 * The zoomLevel operation is a little faster as it can make use of shift operations,
 * the scaleFactor operation offers greater flexibility in computing the values for
 * intermediate zoomlevels.
 */
public final class MercatorProjection {
    /**
     * The circumference of the earth at the equator in meters.
     */
    public static final double EARTH_CIRCUMFERENCE = 40075016.686;
    /**
     * Maximum possible latitude coordinate of the map.
     */
    public static final double LATITUDE_MAX = 85.05112877980659;

    /**
     * Minimum possible latitude coordinate of the map.
     */
    public static final double LATITUDE_MIN = -LATITUDE_MAX;

    // TODO some operations actually do not rely on the tile size, but are composited
    // from operations that require a tileSize parameter (which is effectively cancelled
    // out). A shortcut version of those operations should be implemented and then this
    // variable be removed.
    private static final int DUMMY_TILE_SIZE = 256;

    /**
     * Calculates the distance on the ground that is represented by a single pixel on the map.
     *
     * @param latitude    the latitude coordinate at which the resolution should be calculated.
     * @param scaleFactor the zoom level at which the resolution should be calculated.
     * @return the ground resolution at the given latitude and zoom level.
     */
    public static double calculateGroundResolutionWithScaleFactor(double latitude, double scaleFactor, int tileSize) {
        long mapSize = getMapSizeWithScaleFactor(scaleFactor, tileSize);
        return Math.cos(latitude * (Math.PI / 180)) * EARTH_CIRCUMFERENCE / mapSize;
    }

    /**
     * Calculates the distance on the ground that is represented by a single pixel on the map.
     *
     * @param latitude the latitude coordinate at which the resolution should be calculated.
     * @param mapSize  precomputed size of map.
     * @return the ground resolution at the given latitude and zoom level.
     */
    public static double calculateGroundResolution(double latitude, long mapSize) {
        return Math.cos(latitude * (Math.PI / 180)) * EARTH_CIRCUMFERENCE / mapSize;
    }


    /**
     * Get LatLong from Pixels.
     */
    public static LatLong fromPixelsWithScaleFactor(double pixelX, double pixelY, double scaleFactor, int tileSize) {
        return new LatLong(pixelYToLatitudeWithScaleFactor(pixelY, scaleFactor, tileSize),
                pixelXToLongitudeWithScaleFactor(pixelX, scaleFactor, tileSize));
    }

    /**
     * Get LatLong from Pixels.
     */
    public static LatLong fromPixels(double pixelX, double pixelY, long mapSize) {
        return new LatLong(pixelYToLatitude(pixelY, mapSize),
                pixelXToLongitude(pixelX, mapSize));
    }

    /**
     * @param scaleFactor the scale factor for which the size of the world map should be returned.
     * @return the horizontal and vertical size of the map in pixel at the given zoom level.
     * @throws IllegalArgumentException if the given scale factor is < 1
     */
    public static long getMapSizeWithScaleFactor(double scaleFactor, int tileSize) {
        if (scaleFactor < 1) {
            throw new IllegalArgumentException("scale factor must not < 1 " + scaleFactor);
        }
        return (long) (tileSize * (Math.pow(2, scaleFactorToZoomLevel(scaleFactor))));
    }

    /**
     * @param zoomLevel the zoom level for which the size of the world map should be returned.
     * @return the horizontal and vertical size of the map in pixel at the given zoom level.
     * @throws IllegalArgumentException if the given zoom level is negative.
     */
    public static long getMapSize(byte zoomLevel, int tileSize) {
        if (zoomLevel < 0) {
            throw new IllegalArgumentException("zoom level must not be negative: " + zoomLevel);
        }
        return (long) tileSize << zoomLevel;
    }

    public static Point getPixelWithScaleFactor(LatLong latLong, double scaleFactor, int tileSize) {
        double pixelX = MercatorProjection.longitudeToPixelXWithScaleFactor(latLong.longitude, scaleFactor, tileSize);
        double pixelY = MercatorProjection.latitudeToPixelYWithScaleFactor(latLong.latitude, scaleFactor, tileSize);
        return new Point(pixelX, pixelY);
    }

    public static Point getPixel(LatLong latLong, long mapSize) {
        double pixelX = MercatorProjection.longitudeToPixelX(latLong.longitude, mapSize);
        double pixelY = MercatorProjection.latitudeToPixelY(latLong.latitude, mapSize);
        return new Point(pixelX, pixelY);
    }

    /**
     * Calculates the absolute pixel position for a zoom level and tile size
     *
     * @param latLong the geographic position.
     * @param mapSize precomputed size of map.
     * @return the absolute pixel coordinates (for world)
     */

    public static Point getPixelAbsolute(LatLong latLong, long mapSize) {
        return getPixelRelative(latLong, mapSize, 0, 0);
    }

    /**
     * Calculates the absolute pixel position for a zoom level and tile size relative to origin
     *
     * @param latLong
     * @param mapSize precomputed size of map.
     * @return the relative pixel position to the origin values (e.g. for a tile)
     */
    public static Point getPixelRelative(LatLong latLong, long mapSize, double x, double y) {
        double pixelX = MercatorProjection.longitudeToPixelX(latLong.longitude, mapSize) - x;
        double pixelY = MercatorProjection.latitudeToPixelY(latLong.latitude, mapSize) - y;
        return new Point(pixelX, pixelY);
    }


    /**
     * Calculates the absolute pixel position for a zoom level and tile size relative to origin
     *
     * @param latLong
     * @param mapSize precomputed size of map.
     * @return the relative pixel position to the origin values (e.g. for a tile)
     */
    public static Point getPixelRelative(LatLong latLong, long mapSize, Point origin) {
        return getPixelRelative(latLong, mapSize, origin.x, origin.y);
    }

    /**
     * Calculates the absolute pixel position for a zoom level and tile size relative to origin
     *
     * @param latLong
     * @param tile    tile
     * @return the relative pixel position to the origin values (e.g. for a tile)
     */
    public static Point getPixelRelativeToTile(LatLong latLong, Tile tile) {
        return getPixelRelative(latLong, tile.mapSize, tile.getOrigin());
    }

    /**
     * Converts a latitude coordinate (in degrees) to a pixel Y coordinate at a certain zoom level.
     *
     * @param latitude    the latitude coordinate that should be converted.
     * @param scaleFactor the scale factor at which the coordinate should be converted.
     * @return the pixel Y coordinate of the latitude value.
     */
    public static double latitudeToPixelYWithScaleFactor(double latitude, double scaleFactor, int tileSize) {
        double sinLatitude = Math.sin(latitude * (Math.PI / 180));
        long mapSize = getMapSizeWithScaleFactor(scaleFactor, tileSize);
        // FIXME improve this formula so that it works correctly without the clipping
        double pixelY = (0.5 - Math.log((1 + sinLatitude) / (1 - sinLatitude)) / (4 * Math.PI)) * mapSize;
        return Math.min(Math.max(0, pixelY), mapSize);
    }

    /**
     * Converts a latitude coordinate (in degrees) to a pixel Y coordinate at a certain zoom level.
     *
     * @param latitude  the latitude coordinate that should be converted.
     * @param zoomLevel the zoom level at which the coordinate should be converted.
     * @return the pixel Y coordinate of the latitude value.
     */
    public static double latitudeToPixelY(double latitude, byte zoomLevel, int tileSize) {
        double sinLatitude = Math.sin(latitude * (Math.PI / 180));
        long mapSize = getMapSize(zoomLevel, tileSize);
        // FIXME improve this formula so that it works correctly without the clipping
        double pixelY = (0.5 - Math.log((1 + sinLatitude) / (1 - sinLatitude)) / (4 * Math.PI)) * mapSize;
        return Math.min(Math.max(0, pixelY), mapSize);
    }

    /**
     * Converts a latitude coordinate (in degrees) to a pixel Y coordinate at a certain zoom level.
     *
     * @param latitude the latitude coordinate that should be converted.
     * @param mapSize  precomputed size of map.
     * @return the pixel Y coordinate of the latitude value.
     */
    public static double latitudeToPixelY(double latitude, long mapSize) {
        double sinLatitude = Math.sin(latitude * (Math.PI / 180));
        // FIXME improve this formula so that it works correctly without the clipping
        double pixelY = (0.5 - Math.log((1 + sinLatitude) / (1 - sinLatitude)) / (4 * Math.PI)) * mapSize;
        return Math.min(Math.max(0, pixelY), mapSize);
    }


    /**
     * Converts a latitude coordinate (in degrees) to a tile Y number at a certain zoom level.
     *
     * @param latitude    the latitude coordinate that should be converted.
     * @param scaleFactor the scale factor at which the coordinate should be converted.
     * @return the tile Y number of the latitude value.
     */
    public static int latitudeToTileY(double latitude, double scaleFactor) {
        return pixelYToTileY(latitudeToPixelYWithScaleFactor(latitude, scaleFactor, DUMMY_TILE_SIZE), scaleFactor, DUMMY_TILE_SIZE);
    }

    /**
     * Converts a latitude coordinate (in degrees) to a tile Y number at a certain zoom level.
     *
     * @param latitude  the latitude coordinate that should be converted.
     * @param zoomLevel the zoom level at which the coordinate should be converted.
     * @return the tile Y number of the latitude value.
     */
    public static int latitudeToTileY(double latitude, byte zoomLevel) {
        return pixelYToTileY(latitudeToPixelY(latitude, zoomLevel, DUMMY_TILE_SIZE), zoomLevel, DUMMY_TILE_SIZE);
    }

    /**
     * Converts a longitude coordinate (in degrees) to a pixel X coordinate at a certain scale factor.
     *
     * @param longitude   the longitude coordinate that should be converted.
     * @param scaleFactor the scale factor at which the coordinate should be converted.
     * @return the pixel X coordinate of the longitude value.
     */
    public static double longitudeToPixelXWithScaleFactor(double longitude, double scaleFactor, int tileSize) {
        long mapSize = getMapSizeWithScaleFactor(scaleFactor, tileSize);
        return (longitude + 180) / 360 * mapSize;
    }

    /**
     * Converts a longitude coordinate (in degrees) to a pixel X coordinate at a certain zoom level.
     *
     * @param longitude the longitude coordinate that should be converted.
     * @param zoomLevel the zoom level at which the coordinate should be converted.
     * @param tileSize  the tile size
     * @return the pixel X coordinate of the longitude value.
     */
    public static double longitudeToPixelX(double longitude, byte zoomLevel, int tileSize) {
        long mapSize = getMapSize(zoomLevel, tileSize);
        return (longitude + 180) / 360 * mapSize;
    }

    /**
     * Converts a longitude coordinate (in degrees) to a pixel X coordinate at a certain zoom level.
     *
     * @param longitude the longitude coordinate that should be converted.
     * @param mapSize   precomputed size of map.
     * @return the pixel X coordinate of the longitude value.
     */
    public static double longitudeToPixelX(double longitude, long mapSize) {
        return (longitude + 180) / 360 * mapSize;
    }

    /**
     * Converts a longitude coordinate (in degrees) to the tile X number at a certain scale factor.
     *
     * @param longitude   the longitude coordinate that should be converted.
     * @param scaleFactor the scale factor at which the coordinate should be converted.
     * @return the tile X number of the longitude value.
     */
    public static int longitudeToTileX(double longitude, double scaleFactor) {
        return pixelXToTileX(longitudeToPixelXWithScaleFactor(longitude, scaleFactor, DUMMY_TILE_SIZE), scaleFactor, DUMMY_TILE_SIZE);
    }

    /**
     * Converts a longitude coordinate (in degrees) to the tile X number at a certain zoom level.
     *
     * @param longitude the longitude coordinate that should be converted.
     * @param zoomLevel the zoom level at which the coordinate should be converted.
     * @return the tile X number of the longitude value.
     */
    public static int longitudeToTileX(double longitude, byte zoomLevel) {
        return pixelXToTileX(longitudeToPixelX(longitude, zoomLevel, DUMMY_TILE_SIZE), zoomLevel, DUMMY_TILE_SIZE);
    }

    /**
     * Converts meters to pixels at latitude for zoom-level.
     *
     * @param meters      the meters to convert
     * @param latitude    the latitude for the conversion.
     * @param scaleFactor the scale factor for the conversion.
     * @return pixels that represent the meters at the given zoom-level and latitude.
     */
    public static double metersToPixelsWithScaleFactor(float meters, double latitude, double scaleFactor, int tileSize) {
        return meters / MercatorProjection.calculateGroundResolutionWithScaleFactor(latitude, scaleFactor, tileSize);
    }

    /**
     * Converts meters to pixels at latitude for zoom-level.
     *
     * @param meters   the meters to convert
     * @param latitude the latitude for the conversion.
     * @param mapSize  precomputed size of map.
     * @return pixels that represent the meters at the given zoom-level and latitude.
     */
    public static double metersToPixels(float meters, double latitude, long mapSize) {
        return meters / MercatorProjection.calculateGroundResolution(latitude, mapSize);
    }

    /**
     * Converts a pixel X coordinate at a certain zoom level to a longitude coordinate.
     *
     * @param pixelX      the pixel X coordinate that should be converted.
     * @param scaleFactor the scale factor at which the coordinate should be converted.
     * @return the longitude value of the pixel X coordinate.
     * @throws IllegalArgumentException if the given pixelX coordinate is invalid.
     */
    public static double pixelXToLongitudeWithScaleFactor(double pixelX, double scaleFactor, int tileSize) {
        long mapSize = getMapSizeWithScaleFactor(scaleFactor, tileSize);
        if (pixelX < 0 || pixelX > mapSize) {
            throw new IllegalArgumentException("invalid pixelX coordinate at zoom level " + scaleFactor + ": " + pixelX);
        }
        return 360 * ((pixelX / mapSize) - 0.5);
    }

    /**
     * Converts a pixel X coordinate at a certain zoom level to a longitude coordinate.
     *
     * @param pixelX  the pixel X coordinate that should be converted.
     * @param mapSize precomputed size of map.
     * @return the longitude value of the pixel X coordinate.
     * @throws IllegalArgumentException if the given pixelX coordinate is invalid.
     */

    public static double pixelXToLongitude(double pixelX, long mapSize) {
        if (pixelX < 0 || pixelX > mapSize) {
            throw new IllegalArgumentException("invalid pixelX coordinate " + mapSize + ": " + pixelX);
        }
        return 360 * ((pixelX / mapSize) - 0.5);
    }

    /**
     * Converts a pixel X coordinate to the tile X number.
     *
     * @param pixelX      the pixel X coordinate that should be converted.
     * @param scaleFactor the scale factor at which the coordinate should be converted.
     * @return the tile X number.
     */
    public static int pixelXToTileX(double pixelX, double scaleFactor, int tileSize) {
        return (int) Math.min(Math.max(pixelX / tileSize, 0), scaleFactor - 1);
    }

    /**
     * Converts a pixel X coordinate to the tile X number.
     *
     * @param pixelX    the pixel X coordinate that should be converted.
     * @param zoomLevel the zoom level at which the coordinate should be converted.
     * @return the tile X number.
     */
    public static int pixelXToTileX(double pixelX, byte zoomLevel, int tileSize) {
        return (int) Math.min(Math.max(pixelX / tileSize, 0), Math.pow(2, zoomLevel) - 1);
    }

    /**
     * Converts a pixel Y coordinate at a certain zoom level to a latitude coordinate.
     *
     * @param pixelY      the pixel Y coordinate that should be converted.
     * @param scaleFactor the scale factor at which the coordinate should be converted.
     * @return the latitude value of the pixel Y coordinate.
     * @throws IllegalArgumentException if the given pixelY coordinate is invalid.
     */
    public static double pixelYToLatitudeWithScaleFactor(double pixelY, double scaleFactor, int tileSize) {
        long mapSize = getMapSizeWithScaleFactor(scaleFactor, tileSize);
        if (pixelY < 0 || pixelY > mapSize) {
            throw new IllegalArgumentException("invalid pixelY coordinate at zoom level " + scaleFactor + ": " + pixelY);
        }
        double y = 0.5 - (pixelY / mapSize);
        return 90 - 360 * Math.atan(Math.exp(-y * (2 * Math.PI))) / Math.PI;
    }

    /**
     * Converts a pixel Y coordinate at a certain zoom level to a latitude coordinate.
     *
     * @param pixelY  the pixel Y coordinate that should be converted.
     * @param mapSize precomputed size of map.
     * @return the latitude value of the pixel Y coordinate.
     * @throws IllegalArgumentException if the given pixelY coordinate is invalid.
     */
    public static double pixelYToLatitude(double pixelY, long mapSize) {
        if (pixelY < 0 || pixelY > mapSize) {
            throw new IllegalArgumentException("invalid pixelY coordinate " + mapSize + ": " + pixelY);
        }
        double y = 0.5 - (pixelY / mapSize);
        return 90 - 360 * Math.atan(Math.exp(-y * (2 * Math.PI))) / Math.PI;
    }

    /**
     * Converts a pixel Y coordinate to the tile Y number.
     *
     * @param pixelY      the pixel Y coordinate that should be converted.
     * @param scaleFactor the scale factor at which the coordinate should be converted.
     * @return the tile Y number.
     */
    public static int pixelYToTileY(double pixelY, double scaleFactor, int tileSize) {
        return (int) Math.min(Math.max(pixelY / tileSize, 0), scaleFactor - 1);
    }

    /**
     * Converts a pixel Y coordinate to the tile Y number.
     *
     * @param pixelY    the pixel Y coordinate that should be converted.
     * @param zoomLevel the zoom level at which the coordinate should be converted.
     * @return the tile Y number.
     */
    public static int pixelYToTileY(double pixelY, byte zoomLevel, int tileSize) {
        return (int) Math.min(Math.max(pixelY / tileSize, 0), Math.pow(2, zoomLevel) - 1);
    }

    /**
     * Converts a scaleFactor to a zoomLevel.
     * Note that this will return a double, as the scale factors cover the
     * intermediate zoom levels as well.
     *
     * @param scaleFactor the scale factor to convert to a zoom level.
     * @return the zoom level.
     */
    public static double scaleFactorToZoomLevel(double scaleFactor) {
        return Math.log(scaleFactor) / Math.log(2);
    }

    /**
     * @param tileNumber the tile number that should be converted.
     * @return the pixel coordinate for the given tile number.
     */
    public static long tileToPixel(long tileNumber, int tileSize) {
        return tileNumber * tileSize;
    }

    /**
     * Converts a tile X number at a certain zoom level to a longitude coordinate.
     *
     * @param tileX       the tile X number that should be converted.
     * @param scaleFactor the scale factor at which the number should be converted.
     * @return the longitude value of the tile X number.
     */
    public static double tileXToLongitude(long tileX, double scaleFactor) {
        return pixelXToLongitudeWithScaleFactor(tileX * DUMMY_TILE_SIZE, scaleFactor, DUMMY_TILE_SIZE);
    }

    /**
     * Converts a tile X number at a certain zoom level to a longitude coordinate.
     *
     * @param tileX     the tile X number that should be converted.
     * @param zoomLevel the zoom level at which the number should be converted.
     * @return the longitude value of the tile X number.
     */
    public static double tileXToLongitude(long tileX, byte zoomLevel) {
        return pixelXToLongitude(tileX * DUMMY_TILE_SIZE, getMapSize(zoomLevel, DUMMY_TILE_SIZE));
    }

    /**
     * Converts a tile Y number at a certain zoom level to a latitude coordinate.
     *
     * @param tileY       the tile Y number that should be converted.
     * @param scaleFactor the scale factor at which the number should be converted.
     * @return the latitude value of the tile Y number.
     */
    public static double tileYToLatitude(long tileY, double scaleFactor) {
        return pixelYToLatitudeWithScaleFactor(tileY * DUMMY_TILE_SIZE, scaleFactor, DUMMY_TILE_SIZE);
    }

    /**
     * Converts a tile Y number at a certain zoom level to a latitude coordinate.
     *
     * @param tileY     the tile Y number that should be converted.
     * @param zoomLevel the zoom level at which the number should be converted.
     * @return the latitude value of the tile Y number.
     */
    public static double tileYToLatitude(long tileY, byte zoomLevel) {
        return pixelYToLatitude(tileY * DUMMY_TILE_SIZE, getMapSize(zoomLevel, DUMMY_TILE_SIZE));
    }

    /**
     * Converts a zoom level to a scale factor.
     *
     * @param zoomLevel the zoom level to convert.
     * @return the corresponding scale factor.
     */
    public static double zoomLevelToScaleFactor(byte zoomLevel) {
        return Math.pow(2, zoomLevel);
    }

    private MercatorProjection() {
        throw new IllegalStateException();
    }
}
