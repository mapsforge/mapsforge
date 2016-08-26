/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
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
package org.mapsforge.map.layer.cache;

import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.map.layer.queue.Job;
import org.mapsforge.map.model.common.Observer;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class TwoLevelTileCache implements TileCache {

    private final TileCache firstLevelTileCache;
    private final TileCache secondLevelTileCache;
    private final Set<Job> workingSet;

    public TwoLevelTileCache(TileCache firstLevelTileCache, TileCache secondLevelTileCache) {
        this.firstLevelTileCache = firstLevelTileCache;
        this.secondLevelTileCache = secondLevelTileCache;
        this.workingSet = Collections.synchronizedSet(new HashSet<Job>());
    }

    @Override
    public boolean containsKey(Job key) {
        return this.firstLevelTileCache.containsKey(key) || this.secondLevelTileCache.containsKey(key);
    }

    @Override
    public void destroy() {
        this.firstLevelTileCache.destroy();
        this.secondLevelTileCache.destroy();
    }

    @Override
    public TileBitmap get(Job key) {
        TileBitmap returnBitmap = this.firstLevelTileCache.get(key);
        if (returnBitmap != null) {
            return returnBitmap;
        }
        returnBitmap = this.secondLevelTileCache.get(key);
        if (returnBitmap != null) {
            this.firstLevelTileCache.put(key, returnBitmap);
            return returnBitmap;
        }
        return null;
    }

    @Override
    public int getCapacity() {
        return Math.max(this.firstLevelTileCache.getCapacity(), this.secondLevelTileCache.getCapacity());
    }

    @Override
    public int getCapacityFirstLevel() {
        return this.firstLevelTileCache.getCapacity();
    }

    @Override
    public TileBitmap getImmediately(Job key) {
        return firstLevelTileCache.get(key);
    }

    @Override
    public void purge() {
        this.firstLevelTileCache.purge();
        this.secondLevelTileCache.purge();
    }

    @Override
    public void put(Job key, TileBitmap bitmap) {
        if (this.workingSet.contains(key)) {
            this.firstLevelTileCache.put(key, bitmap);
        }
        this.secondLevelTileCache.put(key, bitmap);
    }

    @Override
    public void setWorkingSet(Set<Job> newWorkingSet) {
        this.workingSet.clear();
        this.workingSet.addAll(newWorkingSet);
        this.firstLevelTileCache.setWorkingSet(this.workingSet);
        this.secondLevelTileCache.setWorkingSet(this.workingSet);
        synchronized(this.workingSet) {
            for (Job job : workingSet) {
                if (!firstLevelTileCache.containsKey(job) && secondLevelTileCache.containsKey(job)) {
                    TileBitmap tileBitmap = secondLevelTileCache.get(job);
                    if (tileBitmap != null) {
                        firstLevelTileCache.put(job, tileBitmap);
                    }
                }
            }
        }
    }

    @Override
    public void addObserver(final Observer observer) {
        this.firstLevelTileCache.addObserver(observer);
        this.secondLevelTileCache.addObserver(observer);
    }

    @Override
    public void removeObserver(final Observer observer) {
        this.secondLevelTileCache.removeObserver(observer);
        this.firstLevelTileCache.removeObserver(observer);
    }

}
