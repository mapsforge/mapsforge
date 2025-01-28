/*
 * Copyright 2025 Sublimis
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
package org.mapsforge.map.layer.labels;

import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.mapelements.MapElementContainer;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.rendertheme.rule.RenderThemeFuture;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/**
 * A LabelStore that reads labels from its cache, or from a MapDataStore if the data was not found in the cache.
 * <p>
 * Implementation note: The cache is implemented using SoftReference-s, although Android advises against using soft references for cache:
 * <a href="https://developer.android.com/reference/java/lang/ref/SoftReference#avoid-soft-references-for-caching">https://developer.android.com/reference/java/lang/ref/SoftReference#avoid-soft-references-for-caching</a>
 * <p>
 * The reason is that we only need the cache for short periods of time, i.e. while rendering is in progress, to improve its performance.
 * Once rendering is complete, the cache is no longer needed and the memory can be reclaimed at the discretion of the system.
 * <p>
 * For this purpose, our implementation works as expected.
 * Additionally, it frees us from having to decide how much memory the (LRU) cache should use and when it should be returned to the system.
 */
public class CachedMapDataStoreLabelStore extends MapDataStoreLabelStore {

    protected final Object sync = new Object();
    protected SoftReference<Map<Integer, SoftReference<MyFutureTask>>> cache = null;

    public CachedMapDataStoreLabelStore(MapDataStore mapDataStore, RenderThemeFuture renderThemeFuture, float textScale, DisplayModel displayModel, GraphicFactory graphicFactory) {
        super(mapDataStore, renderThemeFuture, textScale, displayModel, graphicFactory);
    }

    @Override
    public void clear() {
        synchronized (this.sync) {
            if (this.cache != null) {
                this.cache.clear();
                this.cache = null;
            }
        }
    }

    @Override
    public List<MapElementContainer> getVisibleItems(Tile upperLeft, Tile lowerRight) {

        final List<MapElementContainer> output = new ArrayList<>();

        // We want to keep these hard references in scope for as long as possible while method is running.
        Map<Integer, SoftReference<MyFutureTask>> map;
        SoftReference<MyFutureTask> futureRef;
        MyFutureTask future;

        synchronized (this.sync) {
            if (this.cache != null) {
                map = this.cache.get();
            } else {
                map = null;
            }

            if (map == null) {
                map = new HashMap<>();
                this.cache = new SoftReference<>(map);
            }
        }

        for (int x = upperLeft.tileX; x <= lowerRight.tileX; x++) {
            for (int y = upperLeft.tileY; y <= lowerRight.tileY; y++) {

                final List<MapElementContainer> tileItems;
                {
                    final Tile tile = new Tile(x, y, upperLeft.zoomLevel, upperLeft.tileSize);
                    final int tileHashCode = tile.hashCode();

                    synchronized (this.sync) {

                        futureRef = map.get(tileHashCode);

                        if (futureRef != null) {
                            future = futureRef.get();
                        } else {
                            future = null;
                        }

                        if (future == null)
                        {
                            future = new MyFutureTask(new Callable<List<MapElementContainer>>() {
                                @Override
                                public List<MapElementContainer> call() {
                                    return CachedMapDataStoreLabelStore.super.getVisibleItems(tile, tile);
                                }
                            });

                            futureRef = new SoftReference<>(future);
                            map.put(tileHashCode, futureRef);
                        }
                    }

                    future.run();
                    tileItems = future.get();
                }

                output.addAll(tileItems);
            }
        }

        return output;
    }

    protected static class MyFutureTask extends FutureTask<List<MapElementContainer>> {
        public MyFutureTask(final Callable<List<MapElementContainer>> callable) {
            super(callable);
        }

        @Override
        public List<MapElementContainer> get() {
            List<MapElementContainer> output;

            try {
                output = super.get();
            } catch (Exception e) {
                output = new ArrayList<>();
            }

            return output;
        }
    }
}
