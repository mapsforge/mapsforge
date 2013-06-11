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
package org.mapsforge.map.layer.cache;

import java.util.Map;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.map.layer.queue.Job;

/**
 * Interface for tile image caches.
 */
public interface TileCache {
	/**
	 * @return true if this cache contains an image for the given key, false otherwise.
	 * @see Map#containsKey
	 */
	boolean containsKey(Job key);

	/**
	 * Destroys this cache.
	 */
	void destroy();

	/**
	 * @return the image for the given key or null, if this cache contains no image for the key.
	 * @see Map#get
	 */
	Bitmap get(Job key);

	/**
	 * @return the capacity of this cache.
	 */
	int getCapacity();

	/**
	 * @throws IllegalArgumentException
	 *             if any of the parameters is {@code null}.
	 * @see Map#put
	 */
	void put(Job key, Bitmap bitmap);
}
