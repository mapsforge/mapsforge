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
package org.mapsforge.map.layer.map;

import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.LRUCache;

import android.graphics.Bitmap;

/**
 * A thread-safe cache for tile images with a variable size and LRU policy.
 */
public class InMemoryTileCache implements TileCache {
	private LRUCache<Tile, Bitmap> lruCache;

	/**
	 * @param capacity
	 *            the maximum number of entries in this cache.
	 * @throws IllegalArgumentException
	 *             if the capacity is negative.
	 */
	public InMemoryTileCache(int capacity) {
		this.lruCache = new LRUCache<Tile, Bitmap>(capacity);
	}

	@Override
	public synchronized boolean containsKey(Tile tile) {
		return this.lruCache.containsKey(tile);
	}

	@Override
	public synchronized void destroy() {
		this.lruCache.clear();
	}

	@Override
	public synchronized Bitmap get(Tile tile) {
		return this.lruCache.get(tile);
	}

	@Override
	public synchronized int getCapacity() {
		return this.lruCache.getCapacity();
	}

	@Override
	public synchronized void put(Tile tile, Bitmap bitmap) {
		this.lruCache.put(tile, bitmap);
	}

	@Override
	public synchronized void setCapacity(int capacity) {
		LRUCache<Tile, Bitmap> lruCacheOld = this.lruCache;
		this.lruCache = new LRUCache<Tile, Bitmap>(capacity);
		this.lruCache.putAll(lruCacheOld);
	}
}
