/*
 * Copyright 2014-2015 Ludwig M Brinckmann
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

/**
 * Base class for map data retrieval.
 */

public abstract class MapDataStore {

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
	 * @param language the preferred language or null if default language is used.
	 */
	public MapDataStore(String language) {
		this.preferredLanguage = language;
	}

	/**
	 * Returns the area for which data is supplied.
	 * @return bounding box of area.
	 */
	public abstract BoundingBox boundingBox();

	/*
	 * Closes the map database.
	 */
	public abstract void close();

	/**
	 * Returns the timestamp of the data used to render a specific tile.
	 *
	 * @param tile
	 *            A tile.
	 * @return the timestamp of the data used to render the tile
	 */
	public abstract long getDataTimestamp(Tile tile);

	/**
	 * Gets the initial map position.
	 * @return the start position, if available.
	 */
	public abstract LatLong startPosition();

	/**
	 * Gets the initial zoom level.
	 * @return the start zoom level.
	 */
	public abstract Byte startZoomLevel();

	/**
	 * Reads data for tile.
	 * @param tile tile for which data is requested.
	 * @return map data for the tile.
	 */
	public abstract MapReadResult readMapData(Tile tile);

	/**
	 * Returns true if MapDatabase contains tile.
	 * @param tile tile to be rendered.
	 * @return true if tile is part of database.
	 */
	public abstract boolean supportsTile(Tile tile);

}