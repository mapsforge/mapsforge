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
package org.mapsforge.map.layer.download;

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.layer.TileLayer;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.download.tilesource.TileSource;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.model.common.Observer;

public class TileDownloadLayer extends TileLayer<DownloadJob> implements Observer {
    private static final int DOWNLOAD_THREADS_MAX = 8;

    private long cacheTimeToLive = 0;
    private final GraphicFactory graphicFactory;
    private boolean started;
    private final TileCache tileCache;
    private TileDownloadThread[] tileDownloadThreads;
    private final TileSource tileSource;

    public TileDownloadLayer(TileCache tileCache, MapViewPosition mapViewPosition, TileSource tileSource,
                             GraphicFactory graphicFactory) {
        super(tileCache, mapViewPosition, graphicFactory.createMatrix(), tileSource.hasAlpha());

        this.tileCache = tileCache;
        this.tileSource = tileSource;
        this.cacheTimeToLive = tileSource.getDefaultTimeToLive();
        this.graphicFactory = graphicFactory;
    }

    @Override
    public void draw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point topLeftPoint) {
        if (zoomLevel < this.tileSource.getZoomLevelMin() || zoomLevel > this.tileSource.getZoomLevelMax()) {
            return;
        }

        super.draw(boundingBox, zoomLevel, canvas, topLeftPoint);
    }

    /**
     * Returns the time-to-live (TTL) for tiles in the cache, or 0 if not set.
     * <p/>
     * Refer to {@link #isTileStale(Tile, TileBitmap)} for information on how the TTL is enforced.
     */
    public long getCacheTimeToLive() {
        return cacheTimeToLive;
    }

    @Override
    public void onDestroy() {
        for (TileDownloadThread tileDownloadThread : this.tileDownloadThreads) {
            tileDownloadThread.interrupt();
        }

        super.onDestroy();
    }

    public void onPause() {
        for (TileDownloadThread tileDownloadThread : this.tileDownloadThreads) {
            tileDownloadThread.pause();
        }
    }

    public void onResume() {
        if (!started) {
            start();
        }
        for (TileDownloadThread tileDownloadThread : this.tileDownloadThreads) {
            tileDownloadThread.proceed();
        }
    }

    /**
     * Sets the time-to-live (TTL) for tiles in the cache.
     * <p/>
     * The initial TTL is obtained by calling the {@link org.mapsforge.map.layer.download.tilesource.TileSource}'s
     * {@link TileSource#getDefaultTimeToLive()} ()} method. Refer to
     * {@link #isTileStale(Tile, TileBitmap)} for information on how the TTL is enforced.
     *
     * @param ttl The TTL. If set to 0, no TTL will be enforced.
     */
    public void setCacheTimeToLive(long ttl) {
        cacheTimeToLive = ttl;
    }

    @Override
    public synchronized void setDisplayModel(DisplayModel displayModel) {
        super.setDisplayModel(displayModel);
        int numberOfDownloadThreads = Math.min(tileSource.getParallelRequestsLimit(), DOWNLOAD_THREADS_MAX);
        if (this.displayModel != null) {
            this.tileDownloadThreads = new TileDownloadThread[numberOfDownloadThreads];
            for (int i = 0; i < numberOfDownloadThreads; ++i) {
                this.tileDownloadThreads[i] = new TileDownloadThread(this.tileCache, this.jobQueue, this,
                        this.graphicFactory, this.displayModel);
            }
        } else {
            if (this.tileDownloadThreads != null) {
                for (final TileDownloadThread tileDownloadThread : tileDownloadThreads) {
                    tileDownloadThread.interrupt();
                }
            }
        }

    }

    public void start() {
        for (TileDownloadThread tileDownloadThread : this.tileDownloadThreads) {
            tileDownloadThread.start();
        }
        started = true;
    }

    @Override
    protected DownloadJob createJob(Tile tile) {
        return new DownloadJob(tile, this.tileSource);
    }

    /**
     * Whether the tile is stale and should be refreshed.
     * <p/>
     * This method is called from {@link #draw(BoundingBox, byte, Canvas, Point)} to determine whether the tile needs to
     * be refreshed.
     * <p/>
     * A tile is considered stale if one or more of the following two conditions apply:
     * <ul>
     * <li>The {@code bitmap}'s {@link org.mapsforge.core.graphics.TileBitmap#isExpired()} method returns {@code True}.</li>
     * <li>The layer has a time-to-live (TTL) set ({@link #getCacheTimeToLive()} returns a nonzero value) and the sum of
     * the {@code bitmap}'s {@link org.mapsforge.core.graphics.TileBitmap#getTimestamp()} and TTL is less than current
     * time (as returned by {@link java.lang.System#currentTimeMillis()}).</li>
     * </ul>
     * <p/>
     * When a tile has become stale, the layer will first display the tile referenced by {@code bitmap} and attempt to
     * obtain a fresh copy in the background. When a fresh copy becomes available, the layer will replace it and update
     * the cache. If a fresh copy cannot be obtained (e.g. because the tile is obtained from an online source which
     * cannot be reached), the stale tile will continue to be used until another
     * {@code #draw(BoundingBox, byte, Canvas, Point)} operation requests it again.
     *
     * @param tile   A tile. This parameter is not used for a {@code TileDownloadLayer} and can be null.
     * @param bitmap The bitmap for {@code tile} currently held in the layer's cache.
     */
    @Override
    protected boolean isTileStale(Tile tile, TileBitmap bitmap) {
        if (bitmap.isExpired())
            return true;
        return cacheTimeToLive != 0 && ((bitmap.getTimestamp() + cacheTimeToLive) < System.currentTimeMillis());
    }

    @Override
    protected void onAdd() {
        if (tileCache != null) {
            tileCache.addObserver(this);
        }

        super.onAdd();
    }

    @Override
    protected void onRemove() {
        if (tileCache != null) {
            tileCache.removeObserver(this);
        }
        super.onRemove();
    }

    @Override
    public void onChange() {
        this.requestRedraw();
    }

}
