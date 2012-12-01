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
package org.mapsforge.android.maps.mapgenerator;

import android.graphics.Bitmap;

/**
 * Interface for tile image caches.
 */
public interface TileCache {
	/**
	 * @param mapGeneratorJob
	 *            the key of the image.
	 * @return true if this cache contains a tile image for the given key, false otherwise.
	 */
	boolean containsKey(MapGeneratorJob mapGeneratorJob);

	/**
	 * Destroys this cache.
	 */
	void destroy();

	/**
	 * @param mapGeneratorJob
	 *            the key of the tile image.
	 * @return the tile image for the given key or null, if this cache contains no tile image for the key.
	 */
	Bitmap get(MapGeneratorJob mapGeneratorJob);

	/**
	 * @return the current capacity of this cache.
	 */
	int getCapacity();

	/**
	 * @return true if this cache is persistent, false otherwise.
	 */
	boolean isPersistent();

	/**
	 * Adds another tile image to this cache.
	 * 
	 * @param mapGeneratorJob
	 *            the key of the tile image.
	 * @param bitmap
	 *            the tile image.
	 */
	void put(MapGeneratorJob mapGeneratorJob, Bitmap bitmap);

	/**
	 * Sets the new size of this cache. If this cache already contains more items than the new capacity allows, items
	 * are discarded based on the cache policy.
	 * 
	 * @param capacity
	 *            the maximum number of entries in this cache.
	 * @throws IllegalArgumentException
	 *             if the capacity is negative.
	 * @throws UnsupportedOperationException
	 *             if this cache has a fixed size and does not support changing its capacity.
	 */
	void setCapacity(int capacity);

	/**
	 * Sets the persistence of this cache.
	 * 
	 * @param persistent
	 *            the new persistence of this cache.
	 * @throws UnsupportedOperationException
	 *             if this cache does not support persistence.
	 */
	void setPersistent(boolean persistent);
}
