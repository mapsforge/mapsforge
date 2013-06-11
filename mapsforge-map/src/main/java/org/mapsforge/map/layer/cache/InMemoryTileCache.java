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

import java.util.logging.Logger;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.util.LRUCache;
import org.mapsforge.map.layer.queue.Job;

/**
 * A thread-safe cache for tile images with a variable size and LRU policy.
 */
public class InMemoryTileCache implements TileCache {
	private static final Logger LOGGER = Logger.getLogger(InMemoryTileCache.class.getName());

	private LRUCache<Job, Bitmap> lruCache;

	/**
	 * @param capacity
	 *            the maximum number of entries in this cache.
	 * @throws IllegalArgumentException
	 *             if the capacity is negative.
	 */
	public InMemoryTileCache(int capacity) {
		this.lruCache = new LRUCache<Job, Bitmap>(capacity);
	}

	@Override
	public synchronized boolean containsKey(Job key) {
		return this.lruCache.containsKey(key);
	}

	@Override
	public synchronized void destroy() {
		this.lruCache.clear();
	}

	@Override
	public synchronized Bitmap get(Job key) {
		return this.lruCache.get(key);
	}

	@Override
	public synchronized int getCapacity() {
		return this.lruCache.capacity;
	}

	@Override
	public synchronized void put(Job key, Bitmap bitmap) {
		if (key == null) {
			throw new IllegalArgumentException("key must not be null");
		} else if (bitmap == null) {
			throw new IllegalArgumentException("bitmap must not be null");
		}

		if (this.lruCache.put(key, bitmap) != null) {
			LOGGER.warning("overwriting cached entry: " + key);
		}
	}

	/**
	 * Sets the new size of this cache. If this cache already contains more items than the new capacity allows, items
	 * are discarded based on the cache policy.
	 * 
	 * @param capacity
	 *            the new maximum number of entries in this cache.
	 * @throws IllegalArgumentException
	 *             if the capacity is negative.
	 */
	public synchronized void setCapacity(int capacity) {
		LRUCache<Job, Bitmap> lruCacheNew = new LRUCache<Job, Bitmap>(capacity);
		lruCacheNew.putAll(this.lruCache);
		this.lruCache = lruCacheNew;
	}
}
