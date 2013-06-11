/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
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
package org.mapsforge.map.writer.model;

import java.util.List;
import java.util.Map;

public abstract class TileData {
	/**
	 * Add a POI to the tile.
	 * 
	 * @param poi
	 *            the POI
	 */
	public abstract void addPOI(TDNode poi);

	/**
	 * Add a way to the tile.
	 * 
	 * @param way
	 *            the way
	 */
	public abstract void addWay(TDWay way);

	/**
	 * Gets all POIs of this tile that are seen in the given zoom interval.
	 * 
	 * @param minValidZoomlevel
	 *            the minimum zoom level (inclusive)
	 * @param maxValidZoomlevel
	 *            the maximum zoom level (inclusive)
	 * @return a map that maps from zoom levels to list of nodes
	 */
	public abstract Map<Byte, List<TDNode>> poisByZoomlevel(byte minValidZoomlevel, byte maxValidZoomlevel);

	/**
	 * Gets all ways of this tile that are seen in the given zoom interval.
	 * 
	 * @param minValidZoomlevel
	 *            the minimum zoom level (inclusive)
	 * @param maxValidZoomlevel
	 *            the maximum zoom level (inclusive)
	 * @return a map that maps from zoom levels to list of ways
	 */
	public abstract Map<Byte, List<TDWay>> waysByZoomlevel(byte minValidZoomlevel, byte maxValidZoomlevel);
}
