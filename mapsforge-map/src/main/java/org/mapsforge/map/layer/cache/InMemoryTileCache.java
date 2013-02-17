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
package org.mapsforge.map.layer.cache;

import org.mapsforge.core.util.LRUCache;
import org.mapsforge.map.graphics.Bitmap;
import org.mapsforge.map.layer.queue.Job;

/**
 * A thread-safe cache for object images with a variable size and LRU policy.
 */
public class InMemoryTileCache<T extends Job> implements TileCache<T> {
	private LRUCache<T, Bitmap> lruCache;

	/**
	 * @param capacity
	 *            the maximum number of entries in this cache.
	 * @throws IllegalArgumentException
	 *             if the capacity is negative.
	 */
	public InMemoryTileCache(int capacity) {
		this.lruCache = new LRUCache<T, Bitmap>(capacity);
	}

	@Override
	public synchronized boolean containsKey(T key) {
		return this.lruCache.containsKey(key);
	}

	@Override
	public synchronized void destroy() {
		this.lruCache.clear();
	}

	@Override
	public synchronized Bitmap get(T key) {
		return this.lruCache.get(key);
	}

	@Override
	public synchronized int getCapacity() {
		return this.lruCache.getCapacity();
	}

	@Override
	public synchronized void put(T key, Bitmap bitmap) {
		if (key == null) {
			throw new IllegalArgumentException("key must not be null");
		} else if (bitmap == null) {
			throw new IllegalArgumentException("bitmap must not be null");
		}

		this.lruCache.put(key, bitmap);
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
		if (capacity == this.lruCache.getCapacity()) {
			return;
		}

		LRUCache<T, Bitmap> lruCacheNew = new LRUCache<T, Bitmap>(capacity);
		lruCacheNew.putAll(this.lruCache);
		this.lruCache = lruCacheNew;
	}
}
