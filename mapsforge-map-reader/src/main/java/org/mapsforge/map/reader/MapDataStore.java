/*
 * Copyright 2014 Ludwig M Brinckmann
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

/**
 * Abstracted out interface for data retrieval.
 */
public interface MapDataStore {

	/**
	 * Returns the area for which data is supplied.
	 * 
	 * @return bounding box of area.
	 */
	BoundingBox boundingBox();

	/**
	 * Closes the map database.
	 */
	void close();

	/**
	 * Returns the timestamp of the data used to render a specific tile.
	 * 
	 * @param tile
	 *            A tile.
	 * @return the timestamp of the data used to render the tile
	 */
	long getDataTimestamp(Tile tile);

	/**
	 * Gets the initial map position.
	 * 
	 * @return the start position, if available.
	 */
	LatLong startPosition();

	/**
	 * Gets the initial zoom level.
	 * 
	 * @return the start zoom level.
	 */
	Byte startZoomLevel();

	/**
	 * Reads data for tile.
	 * 
	 * @param tile
	 *            tile for which data is requested.
	 * @return map data for the tile.
	 */
	MapReadResult readMapData(Tile tile);

	/**
	 * Returns true if MapDatabase contains tile.
	 * 
	 * @param tile
	 *            tile to be rendered.
	 * @return true if tile is part of database.
	 */
	boolean supportsTile(Tile tile);

}