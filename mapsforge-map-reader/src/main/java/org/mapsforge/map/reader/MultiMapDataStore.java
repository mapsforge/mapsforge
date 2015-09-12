/*
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

package org.mapsforge.map.reader;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.datastore.MapReadResult;
import org.mapsforge.map.datastore.PointOfInterest;
import org.mapsforge.map.datastore.Way;

import java.util.ArrayList;
import java.util.List;

/**
 * A MapDatabase that reads and combines data from multiple map files.
 * The MultiMapDatabase supports the following modes for reading from multiple files:
 * - RETURN_FIRST: the data from the first database to support a tile will be returned. This is the
 * fastest operation suitable when you know there is no overlap between map files.
 * - RETURN_ALL: the data from all files will be returned, the data will be combined. This is suitable
 * if more than one file can contain data for a tile, but you know there is no semantic overlap, e.g.
 * one file contains contour lines, another road data.
 * - DEDUPLICATE: the data from all files will be returned but duplicates will be eliminated. This is
 * suitable when multiple maps cover the different areas, but there is some overlap at boundaries. This
 * is the most expensive operation and often it is actually faster to double paint objects as otherwise
 * all objects have to be compared with all others.
 */
public class MultiMapDataStore implements MapDataStore {

	public enum DataPolicy {
		RETURN_FIRST, // return the first set of data
		RETURN_ALL, // return all data from databases
		DEDUPLICATE // return all data but eliminate duplicates
	}

	private BoundingBox boundingBox;
	private final DataPolicy dataPolicy;
	private final List<MapDataStore> mapDatabases;
	private LatLong startPosition;
	private byte startZoomLevel;

	public MultiMapDataStore(DataPolicy dataPolicy) {
		this.dataPolicy = dataPolicy;
		this.mapDatabases = new ArrayList<MapDataStore>();
	}

	/**
	 * adds another mapDataStore
	 * @param mapDataStore the mapDataStore to add
	 * @param useStartZoomLevel if true, use the start zoom level of this mapDataStore as the start zoom level
	 * @param useStartPosition if true, use the start position of this mapDataStore as the start position
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
	 * <p>
	 * If the tile uses data from multiple data stores, the most recent timestamp is returned.
	 *
	 * @param tile
	 *            A tile.
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
				return readMapData(tile, false);
			case DEDUPLICATE:
				return readMapData(tile, true);
		}
		throw new IllegalStateException("Invalid data policy for multi map database");
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

	public void setStartPosition(LatLong startPosition) {
		this.startPosition = startPosition;
	}

	@Override
	public Byte startZoomLevel() {
		return startZoomLevel;
	}

	public void setStartZoomLevel(byte startZoomLevel) {
		this.startZoomLevel = startZoomLevel;
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

	private MapReadResult readMapData(Tile tile, boolean deduplicate) {
		MapReadResultBuilder mapReadResultBuilder = new MapReadResultBuilder();
		boolean first = true;
		for (MapDataStore mdb : mapDatabases) {
			if (mdb.supportsTile(tile)) {
				MapReadResult result = mdb.readMapData(tile);
				if (result == null) {
					continue;
				}
				boolean isWater = mapReadResultBuilder.isWater() & result.isWater;
				mapReadResultBuilder.setWater(isWater);


				if (first) {
					mapReadResultBuilder.getWays().addAll(result.ways);
				} else {
					if (deduplicate) {
						for (Way way : result.ways) {
							if (!mapReadResultBuilder.getWays().contains(way)) {
								mapReadResultBuilder.getWays().add(way);
							}
						}
					} else {
						mapReadResultBuilder.getWays().addAll(result.ways);
					}
				}
				if (first) {
					mapReadResultBuilder.getPointOfInterests().addAll(result.pointOfInterests);
				} else {
					if (deduplicate) {
						for (PointOfInterest poi : result.pointOfInterests) {
							if (!mapReadResultBuilder.getPointOfInterests().contains(poi)) {
								mapReadResultBuilder.getPointOfInterests().add(poi);
							}
						}
					} else {
						mapReadResultBuilder.getPointOfInterests().addAll(result.pointOfInterests);
					}
				}
				first = false;
			}
		}
		return new MapFileReadResult(mapReadResultBuilder);
	}
}
