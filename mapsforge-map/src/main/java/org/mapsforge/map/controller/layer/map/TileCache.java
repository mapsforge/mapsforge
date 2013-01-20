/*
 * Copyright 2010, 2011, 2012 mapsforge.org
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
package org.mapsforge.map.controller.layer.map;

import org.mapsforge.core.model.Tile;

import android.graphics.Bitmap;

/**
 * Interface for tile image caches.
 */
public interface TileCache {
	/**
	 * @return true if this cache contains a tile image for the given key, false otherwise.
	 */
	boolean containsKey(Tile tile);

	/**
	 * Destroys this cache.
	 */
	void destroy();

	/**
	 * @return the tile image for the given key or null, if this cache contains no tile image for the key.
	 */
	Bitmap get(Tile tile, Bitmap bitmap);

	/**
	 * @return the capacity of this cache.
	 */
	int getCapacity();

	/**
	 * Adds another tile image to this cache.
	 * 
	 * @param bitmap
	 *            the tile image.
	 */
	void put(Tile tile, Bitmap bitmap);
}
