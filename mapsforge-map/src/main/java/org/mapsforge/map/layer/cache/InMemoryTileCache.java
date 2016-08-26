/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 devemux86
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

import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.core.util.WorkingSetCache;
import org.mapsforge.map.layer.queue.Job;
import org.mapsforge.map.model.common.Observable;
import org.mapsforge.map.model.common.Observer;

import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * A thread-safe cache for tile images with a variable size and LRU policy.
 */
public class InMemoryTileCache implements TileCache {
    private static final Logger LOGGER = Logger.getLogger(InMemoryTileCache.class.getName());

    private BitmapLRUCache lruCache;
    private Observable observable;

    /**
     * @param capacity the maximum number of entries in this cache.
     * @throws IllegalArgumentException if the capacity is negative.
     */
    public InMemoryTileCache(int capacity) {
        this.lruCache = new BitmapLRUCache(capacity);
        this.observable = new Observable();
    }

    @Override
    public synchronized boolean containsKey(Job key) {
        return this.lruCache.containsKey(key);
    }

    @Override
    public synchronized void destroy() {
        purge();
    }

    @Override
    public synchronized TileBitmap get(Job key) {
        TileBitmap bitmap = this.lruCache.get(key);
        if (bitmap != null) {
            bitmap.incrementRefCount();
        }
        return bitmap;
    }

    @Override
    public synchronized int getCapacity() {
        return this.lruCache.capacity;
    }

    @Override
    public int getCapacityFirstLevel() {
        return getCapacity();
    }

    @Override
    public TileBitmap getImmediately(Job key) {
        return get(key);
    }

    @Override
    public void purge() {
        for (TileBitmap bitmap : this.lruCache.values()) {
            bitmap.decrementRefCount();
        }
        this.lruCache.clear();
    }

    @Override
    public synchronized void put(Job key, TileBitmap bitmap) {
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        } else if (bitmap == null) {
            throw new IllegalArgumentException("bitmap must not be null");
        }

        TileBitmap old = this.lruCache.get(key);
        if (old != null) {
            old.decrementRefCount();
        }

        if (this.lruCache.put(key, bitmap) != null) {
            LOGGER.warning("overwriting cached entry: " + key);
        }
        bitmap.incrementRefCount();
        this.observable.notifyObservers();
    }

    /**
     * Sets the new size of this cache. If this cache already contains more items than the new capacity allows, items
     * are discarded based on the cache policy.
     *
     * @param capacity the new maximum number of entries in this cache.
     * @throws IllegalArgumentException if the capacity is negative.
     */
    public synchronized void setCapacity(int capacity) {
        BitmapLRUCache lruCacheNew = new BitmapLRUCache(capacity);
        lruCacheNew.putAll(this.lruCache);
        this.lruCache = lruCacheNew;
    }

    @Override
    public synchronized void setWorkingSet(Set<Job> jobs) {
        this.lruCache.setWorkingSet(jobs);
    }

    @Override
    public void addObserver(final Observer observer) {
        this.observable.addObserver(observer);
    }

    @Override
    public void removeObserver(final Observer observer) {
        this.observable.removeObserver(observer);
    }

}

class BitmapLRUCache extends WorkingSetCache<Job, TileBitmap> {
    private static final long serialVersionUID = 1L;

    public BitmapLRUCache(int capacity) {
        super(capacity);
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<Job, TileBitmap> eldest) {
        if (size() > this.capacity) {
            TileBitmap bitmap = eldest.getValue();
            if (bitmap != null) {
                bitmap.decrementRefCount();
            }
            return true;
        }
        return false;
    }

}
