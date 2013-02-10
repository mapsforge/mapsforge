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

import org.mapsforge.map.layer.queue.Job;

import android.graphics.Bitmap;

public class TwoLevelTileCache<T extends Job> implements TileCache<T> {
	private final TileCache<T> firstLevelTileCache;
	private final TileCache<T> secondLevelTileCache;

	public TwoLevelTileCache(TileCache<T> firstLevelTileCache, TileCache<T> secondLevelTileCache) {
		this.firstLevelTileCache = firstLevelTileCache;
		this.secondLevelTileCache = secondLevelTileCache;
	}

	@Override
	public synchronized boolean containsKey(T key) {
		return this.firstLevelTileCache.containsKey(key) || this.secondLevelTileCache.containsKey(key);
	}

	@Override
	public synchronized void destroy() {
		this.firstLevelTileCache.destroy();
		this.secondLevelTileCache.destroy();
	}

	@Override
	public synchronized Bitmap get(T key, Bitmap bitmap) {
		Bitmap returnBitmap = this.firstLevelTileCache.get(key, bitmap);
		if (returnBitmap != null) {
			return returnBitmap;
		}

		returnBitmap = this.secondLevelTileCache.get(key, bitmap);
		if (returnBitmap != null) {
			this.firstLevelTileCache.put(key, returnBitmap.copy(returnBitmap.getConfig(), false));
			return returnBitmap;
		}

		return null;
	}

	@Override
	public synchronized int getCapacity() {
		return Math.max(this.firstLevelTileCache.getCapacity(), this.secondLevelTileCache.getCapacity());
	}

	@Override
	public synchronized void put(T key, Bitmap bitmap) {
		this.secondLevelTileCache.put(key, bitmap);
	}
}
