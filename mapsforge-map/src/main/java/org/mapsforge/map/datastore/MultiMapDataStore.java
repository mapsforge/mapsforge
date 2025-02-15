/*
 * Copyright 2014-2015 Ludwig M Brinckmann
 * Copyright 2015-2022 devemux86
 * Copyright 2024-2025 Sublimis
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
import org.mapsforge.core.model.Tile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A {@link MapDataStore} that reads and combines data from multiple map files.
 * It supports the following modes for reading from multiple files:
 * <p>
 * - RETURN_FIRST: Data from the first file to support a tile will be returned. This is the
 * fastest operation suitable when you know there is no overlap between map files.
 * <p>
 * - RETURN_ALL: Data from all files will be returned and combined. This is suitable
 * if more than one file can contain data for a tile, but you know there is no semantic overlap, e.g.
 * one file contains contour lines, another road data. Use {@link #setPriority(int)} to prioritize your maps.
 * <p>
 * - DEDUPLICATE: Data from all files will be returned but duplicates will be removed. This is
 * suitable when multiple maps cover different areas, but there is some overlap at boundaries.
 * Use {@link #setPriority(int)} to prioritize your maps.
 */
public class MultiMapDataStore extends MapDataStore {

    public enum DataPolicy {
        /** Return the first set of data */
        RETURN_FIRST,
        /** Return all data */
        RETURN_ALL,
        /** Return all data but remove duplicates */
        DEDUPLICATE
    }

    private BoundingBox boundingBox;
    private final DataPolicy dataPolicy;
    private final List<MapDataStore> mapDatabases;
    private LatLong startPosition;
    private byte startZoomLevel;

    /**
     * Create {@link MultiMapDataStore} with {@link DataPolicy#DEDUPLICATE} behavior.
     */
    public MultiMapDataStore() {
        this(DataPolicy.DEDUPLICATE);
    }

    /**
     * Create {@link MultiMapDataStore} with the selected {@link DataPolicy} behavior.
     */
    public MultiMapDataStore(DataPolicy dataPolicy) {
        this.dataPolicy = dataPolicy;
        this.mapDatabases = new ArrayList<>();
    }

    /**
     * adds another mapDataStore
     *
     * @param mapDataStore      the mapDataStore to add
     * @param useStartZoomLevel if true, use the start zoom level of this mapDataStore as the start zoom level
     * @param useStartPosition  if true, use the start position of this mapDataStore as the start position
     */

    public void addMapDataStore(MapDataStore mapDataStore, boolean useStartZoomLevel, boolean useStartPosition) {
        if (this.mapDatabases.contains(mapDataStore)) {
            throw new IllegalArgumentException("Duplicate map database");
        }
        this.mapDatabases.add(mapDataStore);
        if (useStartZoomLevel) {
            this.startZoomLevel = mapDataStore.startZoomLevel();
        }
        if (useStartPosition) {
            this.startPosition = mapDataStore.startPosition();
        }
        if (null == this.boundingBox) {
            this.boundingBox = mapDataStore.boundingBox();
        } else {
            this.boundingBox = this.boundingBox.extendBoundingBox(mapDataStore.boundingBox());
        }

        Collections.sort(this.mapDatabases, new Comparator<MapDataStore>() {
            @Override
            public int compare(MapDataStore mds1, MapDataStore mds2) {
                // Reverse order
                return -Integer.compare(mds1.getPriority(), mds2.getPriority());
            }
        });
    }

    @Override
    public BoundingBox boundingBox() {
        return this.boundingBox;
    }

    @Override
    public void close() {
        for (MapDataStore mdb : mapDatabases) {
            mdb.close();
        }
    }

    /**
     * Returns the timestamp of the data used to render a specific tile.
     * <p/>
     * If the tile uses data from multiple data stores, the most recent timestamp is returned.
     *
     * @param tile A tile.
     * @return the timestamp of the data used to render the tile
     */
    @Override
    public long getDataTimestamp(Tile tile) {
        switch (this.dataPolicy) {
            case RETURN_FIRST:
                for (MapDataStore mdb : mapDatabases) {
                    if (mdb.supportsTile(tile)) {
                        return mdb.getDataTimestamp(tile);
                    }
                }
                return 0;
            case RETURN_ALL:
            case DEDUPLICATE:
                long result = 0;
                for (MapDataStore mdb : mapDatabases) {
                    if (mdb.supportsTile(tile)) {
                        result = Math.max(result, mdb.getDataTimestamp(tile));
                    }
                }
                return result;
        }
        throw new IllegalStateException("Invalid data policy for multi map database");
    }

    @Override
    public MapReadResult readNamedItems(Tile tile) {
        switch (this.dataPolicy) {
            case RETURN_FIRST:
                for (MapDataStore mdb : mapDatabases) {
                    if (mdb.supportsTile(tile)) {
                        return mdb.readNamedItems(tile);
                    }
                }
                return null;
            case RETURN_ALL:
                return readAllNamedItems(tile);
            case DEDUPLICATE:
                return readAllNamedItems(tile).deduplicate();
        }
        throw new IllegalStateException("Invalid data policy for multi map database");

    }

    private MapReadResult readAllNamedItems(Tile tile) {
        MapReadResult mapReadResult = new MapReadResult();
        boolean isTileFilled = false;
        for (MapDataStore mdb : mapDatabases) {
            if (isTileFilled && mdb.getPriority() < 0) {
                break;
            }

            if (mdb.supportsTile(tile)) {
                MapReadResult result = mdb.readNamedItems(tile);
                if (result == null) {
                    continue;
                }
                boolean isWater = mapReadResult.isWater & result.isWater;
                mapReadResult.isWater = isWater;
                mapReadResult.add(result);
            }

            if (mdb.supportsFullTile(tile)) {
                isTileFilled = true;
            }
        }
        return mapReadResult;
    }

    @Override
    public MapReadResult readNamedItems(Tile upperLeft, Tile lowerRight) {
        switch (this.dataPolicy) {
            case RETURN_FIRST:
                for (MapDataStore mdb : mapDatabases) {
                    if (mdb.supportsArea(
                            upperLeft.getBoundingBox().extendBoundingBox(lowerRight.getBoundingBox()),
                            upperLeft.zoomLevel
                    )) {
                        return mdb.readNamedItems(upperLeft, lowerRight);
                    }
                }
                return null;
            case RETURN_ALL:
                return readAllNamedItems(upperLeft, lowerRight);
            case DEDUPLICATE:
                return readAllNamedItems(upperLeft, lowerRight).deduplicate();
        }
        throw new IllegalStateException("Invalid data policy for multi map database");

    }

    private MapReadResult readAllNamedItems(Tile upperLeft, Tile lowerRight) {
        MapReadResult mapReadResult = new MapReadResult();
        boolean isTileFilled = false;
        for (MapDataStore mdb : mapDatabases) {
            if (isTileFilled && mdb.getPriority() < 0) {
                break;
            }

            if (mdb.supportsArea(
                    upperLeft.getBoundingBox().extendBoundingBox(lowerRight.getBoundingBox()),
                    upperLeft.zoomLevel
            )) {
                MapReadResult result = mdb.readNamedItems(upperLeft, lowerRight);
                if (result == null) {
                    continue;
                }
                boolean isWater = mapReadResult.isWater & result.isWater;
                mapReadResult.isWater = isWater;
                mapReadResult.add(result);
            }

            if (mdb.supportsFullArea(
                    upperLeft.getBoundingBox().extendBoundingBox(lowerRight.getBoundingBox()),
                    upperLeft.zoomLevel)
            ) {
                isTileFilled = true;
            }
        }
        return mapReadResult;
    }

    @Override
    public MapReadResult readMapData(Tile tile) {
        switch (this.dataPolicy) {
            case RETURN_FIRST:
                for (MapDataStore mdb : mapDatabases) {
                    if (mdb.supportsTile(tile)) {
                        return mdb.readMapData(tile);
                    }
                }
                return null;
            case RETURN_ALL:
                return readAllMapData(tile);
            case DEDUPLICATE:
                return readAllMapData(tile).deduplicate();
        }
        throw new IllegalStateException("Invalid data policy for multi map database");
    }

    private MapReadResult readAllMapData(Tile tile) {
        MapReadResult mapReadResult = new MapReadResult();
        boolean isTileFilled = false;
        for (MapDataStore mdb : mapDatabases) {
            if (isTileFilled && mdb.getPriority() < 0) {
                break;
            }

            if (mdb.supportsTile(tile)) {
                MapReadResult result = mdb.readMapData(tile);
                if (result == null) {
                    continue;
                }
                boolean isWater = mapReadResult.isWater & result.isWater;
                mapReadResult.isWater = isWater;
                mapReadResult.add(result);
            }

            if (mdb.supportsFullTile(tile)) {
                isTileFilled = true;
            }
        }
        return mapReadResult;
    }

    @Override
    public MapReadResult readMapData(Tile upperLeft, Tile lowerRight) {
        switch (this.dataPolicy) {
            case RETURN_FIRST:
                for (MapDataStore mdb : mapDatabases) {
                    if (mdb.supportsArea(
                            upperLeft.getBoundingBox().extendBoundingBox(lowerRight.getBoundingBox()),
                            upperLeft.zoomLevel
                    )) {
                        return mdb.readMapData(upperLeft, lowerRight);
                    }
                }
                return null;
            case RETURN_ALL:
                return readAllMapData(upperLeft, lowerRight);
            case DEDUPLICATE:
                return readAllMapData(upperLeft, lowerRight).deduplicate();
        }
        throw new IllegalStateException("Invalid data policy for multi map database");
    }

    private MapReadResult readAllMapData(Tile upperLeft, Tile lowerRight) {
        MapReadResult mapReadResult = new MapReadResult();
        boolean isTileFilled = false;
        for (MapDataStore mdb : mapDatabases) {
            if (isTileFilled && mdb.getPriority() < 0) {
                break;
            }

            if (mdb.supportsArea(
                    upperLeft.getBoundingBox().extendBoundingBox(lowerRight.getBoundingBox()),
                    upperLeft.zoomLevel)
            ) {
                MapReadResult result = mdb.readMapData(upperLeft, lowerRight);
                if (result == null) {
                    continue;
                }
                boolean isWater = mapReadResult.isWater & result.isWater;
                mapReadResult.isWater = isWater;
                mapReadResult.add(result);
            }

            if (mdb.supportsFullArea(
                    upperLeft.getBoundingBox().extendBoundingBox(lowerRight.getBoundingBox()),
                    upperLeft.zoomLevel)
            ) {
                isTileFilled = true;
            }
        }
        return mapReadResult;
    }

    @Override
    public MapReadResult readPoiData(Tile tile) {
        switch (this.dataPolicy) {
            case RETURN_FIRST:
                for (MapDataStore mdb : mapDatabases) {
                    if (mdb.supportsTile(tile)) {
                        return mdb.readPoiData(tile);
                    }
                }
                return null;
            case RETURN_ALL:
                return readAllPoiData(tile);
            case DEDUPLICATE:
                return readAllPoiData(tile).deduplicate();
        }
        throw new IllegalStateException("Invalid data policy for multi map database");

    }

    private MapReadResult readAllPoiData(Tile tile) {
        MapReadResult mapReadResult = new MapReadResult();
        boolean isTileFilled = false;
        for (MapDataStore mdb : mapDatabases) {
            if (isTileFilled && mdb.getPriority() < 0) {
                break;
            }

            if (mdb.supportsTile(tile)) {
                MapReadResult result = mdb.readPoiData(tile);
                if (result == null) {
                    continue;
                }
                boolean isWater = mapReadResult.isWater & result.isWater;
                mapReadResult.isWater = isWater;
                mapReadResult.add(result);
            }

            if (mdb.supportsFullTile(tile)) {
                isTileFilled = true;
            }
        }
        return mapReadResult;
    }

    @Override
    public MapReadResult readPoiData(Tile upperLeft, Tile lowerRight) {
        switch (this.dataPolicy) {
            case RETURN_FIRST:
                for (MapDataStore mdb : mapDatabases) {
                    if (mdb.supportsArea(
                            upperLeft.getBoundingBox().extendBoundingBox(lowerRight.getBoundingBox()),
                            upperLeft.zoomLevel)
                    ) {
                        return mdb.readPoiData(upperLeft, lowerRight);
                    }
                }
                return null;
            case RETURN_ALL:
                return readAllPoiData(upperLeft, lowerRight);
            case DEDUPLICATE:
                return readAllPoiData(upperLeft, lowerRight).deduplicate();
        }
        throw new IllegalStateException("Invalid data policy for multi map database");

    }

    private MapReadResult readAllPoiData(Tile upperLeft, Tile lowerRight) {
        MapReadResult mapReadResult = new MapReadResult();
        boolean isTileFilled = false;
        for (MapDataStore mdb : mapDatabases) {
            if (isTileFilled && mdb.getPriority() < 0) {
                break;
            }

            if (mdb.supportsArea(
                    upperLeft.getBoundingBox().extendBoundingBox(lowerRight.getBoundingBox()),
                    upperLeft.zoomLevel)
            ) {
                MapReadResult result = mdb.readPoiData(upperLeft, lowerRight);
                if (result == null) {
                    continue;
                }
                boolean isWater = mapReadResult.isWater & result.isWater;
                mapReadResult.isWater = isWater;
                mapReadResult.add(result);
            }

            if (mdb.supportsFullArea(
                    upperLeft.getBoundingBox().extendBoundingBox(lowerRight.getBoundingBox()),
                    upperLeft.zoomLevel)
            ) {
                isTileFilled = true;
            }
        }
        return mapReadResult;
    }

    public void setStartPosition(LatLong startPosition) {
        this.startPosition = startPosition;
    }

    public void setStartZoomLevel(byte startZoomLevel) {
        this.startZoomLevel = startZoomLevel;
    }

    @Override
    public LatLong startPosition() {
        if (null != this.startPosition) {
            return this.startPosition;
        }
        if (null != this.boundingBox) {
            return this.boundingBox.getCenterPoint();
        }
        return null;
    }

    @Override
    public Byte startZoomLevel() {
        return startZoomLevel;
    }

    @Override
    public boolean supportsTile(Tile tile) {
        for (MapDataStore mdb : mapDatabases) {
            if (mdb.supportsTile(tile)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean supportsFullTile(Tile tile) {
        for (MapDataStore mdb : mapDatabases) {
            if (mdb.supportsFullTile(tile)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean supportsArea(BoundingBox boundingBox, byte zoomLevel) {
        for (MapDataStore mdb : mapDatabases) {
            if (mdb.supportsArea(boundingBox, zoomLevel)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean supportsFullArea(BoundingBox boundingBox, byte zoomLevel) {
        for (MapDataStore mdb : mapDatabases) {
            if (mdb.supportsFullArea(boundingBox, zoomLevel)) {
                return true;
            }
        }
        return false;
    }
}
