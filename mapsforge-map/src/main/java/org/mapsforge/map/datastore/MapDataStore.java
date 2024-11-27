/*
 * Copyright 2014-2015 Ludwig M Brinckmann
 * Copyright 2015 devemux86
 * Copyright 2015 lincomatic
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
package org.mapsforge.map.datastore;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Tag;
import org.mapsforge.core.model.Tile;

import java.util.List;
import java.util.Locale;

/**
 * Base class for map data retrieval.
 */
public abstract class MapDataStore implements Comparable<MapDataStore> {

    protected int priority = 0;

    /**
     * Extracts substring of preferred language from multilingual string.<br/>
     * Example multilingual string: "Base\ren\bEnglish\rjp\bJapan\rzh_py\bPin-yin".
     * <p/>
     * Use '\r' delimiter among names and '\b' delimiter between each language and name.
     */
    public static String extract(String s, String language) {
        if (s == null || s.trim().isEmpty()) {
            return null;
        }

        String[] langNames = s.split("\r");
        if (language == null || language.trim().isEmpty()) {
            return langNames[0];
        }

        String fallback = null;
        for (int i = 1; i < langNames.length; i++) {
            String[] langName = langNames[i].split("\b");
            if (langName.length != 2) {
                continue;
            }

            // Perfect match
            if (langName[0].equalsIgnoreCase(language)) {
                return langName[1];
            }

            // Fall back to base, e.g. zh-min-lan -> zh
            if (fallback == null && !langName[0].contains("-") && (language.contains("-") || language.contains("_"))
                    && language.toLowerCase(Locale.ENGLISH).startsWith(langName[0].toLowerCase(Locale.ENGLISH))) {
                fallback = langName[1];
            }
        }
        return (fallback != null) ? fallback : langNames[0];
    }

    /**
     * Callback methods for map reader.
     */
    private MapCallback mapCallback;

    /**
     * the preferred language when extracting labels from this data store. The actual
     * implementation is up to the concrete implementation, which can also simply ignore
     * this setting.
     */
    protected String preferredLanguage;

    /**
     * Ctor for MapDataStore that will use default language.
     */
    public MapDataStore() {
        this(null);
    }

    /**
     * Ctor for MapDataStore setting preferred language.
     *
     * @param language the preferred language or null if default language is used.
     */
    public MapDataStore(String language) {
        this.preferredLanguage = language;
    }

    /**
     * Returns the area for which data is supplied.
     *
     * @return bounding box of area.
     */
    public abstract BoundingBox boundingBox();

    /*
     * Closes the map database.
     */
    public abstract void close();

    /**
     * Extracts substring of preferred language from multilingual string using
     * the preferredLanguage setting.
     */
    protected String extractLocalized(String s) {
        return MapDataStore.extract(s, preferredLanguage);
    }

    /**
     * Returns the timestamp of the data used to render a specific tile.
     *
     * @param tile A tile.
     * @return the timestamp of the data used to render the tile
     */
    public abstract long getDataTimestamp(Tile tile);

    public MapCallback getMapCallback() {
        return mapCallback;
    }

    /**
     * Reads only labels for tile. Labels are pois as well as ways that carry a name tag.
     * It is permissible for the MapDataStore to return more data.
     * This default implementation returns all map data, which is inefficient, but works.
     *
     * @param tile tile for which data is requested.
     * @return label data for the tile.
     */
    public MapReadResult readLabels(Tile tile) {
        return readMapData(tile);
    }

    /**
     * Reads data for an area defined by the tile in the upper left and the tile in
     * the lower right corner. The default implementation combines the results from
     * all tiles, a possibly inefficient solution.
     * Precondition: upperLeft.tileX <= lowerRight.tileX && upperLeft.tileY <= lowerRight.tileY
     *
     * @param upperLeft  tile that defines the upper left corner of the requested area.
     * @param lowerRight tile that defines the lower right corner of the requested area.
     * @return map data for the tile.
     */
    public MapReadResult readLabels(Tile upperLeft, Tile lowerRight) {
        if (upperLeft.tileX > lowerRight.tileX || upperLeft.tileY > lowerRight.tileY) {
            new IllegalArgumentException("upperLeft tile must be above and left of lowerRight tile");
        }
        MapReadResult result = new MapReadResult();
        for (int x = upperLeft.tileX; x <= lowerRight.tileX; x++) {
            for (int y = upperLeft.tileY; y <= lowerRight.tileY; y++) {
                Tile current = new Tile(x, y, upperLeft.zoomLevel, upperLeft.tileSize);
                result.add(readLabels(current), false);
            }
        }
        return result;
    }

    /**
     * Reads data for tile.
     *
     * @param tile tile for which data is requested.
     * @return map data for the tile.
     */
    public abstract MapReadResult readMapData(Tile tile);

    /**
     * Reads data for an area defined by the tile in the upper left and the tile in
     * the lower right corner. The default implementation combines the results from
     * all tiles, a possibly inefficient solution.
     * Precondition: upperLeft.tileX <= lowerRight.tileX && upperLeft.tileY <= lowerRight.tileY
     *
     * @param upperLeft  tile that defines the upper left corner of the requested area.
     * @param lowerRight tile that defines the lower right corner of the requested area.
     * @return map data for the tile.
     */
    public MapReadResult readMapData(Tile upperLeft, Tile lowerRight) {
        if (upperLeft.tileX > lowerRight.tileX || upperLeft.tileY > lowerRight.tileY) {
            new IllegalArgumentException("upperLeft tile must be above and left of lowerRight tile");
        }
        MapReadResult result = new MapReadResult();
        for (int x = upperLeft.tileX; x <= lowerRight.tileX; x++) {
            for (int y = upperLeft.tileY; y <= lowerRight.tileY; y++) {
                Tile current = new Tile(x, y, upperLeft.zoomLevel, upperLeft.tileSize);
                result.add(readMapData(current), false);
            }
        }
        return result;
    }

    /**
     * Reads only POI data for tile.
     *
     * @param tile tile for which data is requested.
     * @return poi data for the tile.
     */
    public abstract MapReadResult readPoiData(Tile tile);

    /**
     * Reads POI data for an area defined by the tile in the upper left and the tile in
     * the lower right corner. The default implementation combines the results from
     * all tiles, a possibly inefficient solution.
     * Precondition: upperLeft.tileX <= lowerRight.tileX && upperLeft.tileY <= lowerRight.tileY
     *
     * @param upperLeft  tile that defines the upper left corner of the requested area.
     * @param lowerRight tile that defines the lower right corner of the requested area.
     * @return map data for the tile.
     */
    public MapReadResult readPoiData(Tile upperLeft, Tile lowerRight) {
        if (upperLeft.tileX > lowerRight.tileX || upperLeft.tileY > lowerRight.tileY) {
            new IllegalArgumentException("upperLeft tile must be above and left of lowerRight tile");
        }
        MapReadResult result = new MapReadResult();
        for (int x = upperLeft.tileX; x <= lowerRight.tileX; x++) {
            for (int y = upperLeft.tileY; y <= lowerRight.tileY; y++) {
                Tile current = new Tile(x, y, upperLeft.zoomLevel, upperLeft.tileSize);
                result.add(readPoiData(current), false);
            }
        }
        return result;
    }

    public void setMapCallback(MapCallback mapCallback) {
        this.mapCallback = mapCallback;
    }

    public int getPriority() {
        return priority;
    }

    public MapDataStore setPriority(int priority) {
        this.priority = priority;
        return this;
    }

    @Override
    public int compareTo(MapDataStore other) {
        // Reverse order
        return -Integer.compare(this.getPriority(), other.getPriority());
    }

    /**
     * Gets the initial map position.
     *
     * @return the start position, if available.
     */
    public abstract LatLong startPosition();

    /**
     * Gets the initial zoom level.
     *
     * @return the start zoom level.
     */
    public abstract Byte startZoomLevel();

    /**
     * Returns true if MapDatabase contains tile.
     *
     * @param tile tile to be rendered.
     * @return true if tile is part of database.
     */
    public abstract boolean supportsTile(Tile tile);

    /**
     * Returns true if MapDatabase contains a complete tile.
     *
     * @param tile tile to be rendered.
     * @return true if complete tile is part of database.
     */
    public abstract boolean supportsFullTile(Tile tile);

    /**
     * Returns true if MapDatabase covers (even partially) certain area in required zoom level.
     *
     * @param boundingBox area we test
     * @param zoomLevel zoom level we test
     * @return true if area is part of the database.
     */
    public abstract boolean supportsArea(BoundingBox boundingBox, byte zoomLevel);

    /**
     * Returns true if MapDatabase covers certain area completely in required zoom level.
     *
     * @param boundingBox area we test
     * @param zoomLevel   zoom level we test
     * @return true if complete area is part of the database.
     */
    public abstract boolean supportsFullArea(BoundingBox boundingBox, byte zoomLevel);

    /**
     * Returns true if a way should be included in the result set for readLabels()
     * By default only ways with names, house numbers or a ref are included in the result set
     * of readLabels(). This is to reduce the set of ways as much as possible to save memory.
     * @param tags the tags associated with the way
     * @return true if the way should be included in the result set
     */
    public boolean wayAsLabelTagFilter(List<Tag> tags) {
        return false;
    }

}
