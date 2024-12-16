/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2015-2019 devemux86
 * Copyright 2019 cpt1gl0
 * Copyright 2019 mg4gh
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
package org.mapsforge.map.layer;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.Matrix;
import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Rotation;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.Parameters;
import org.mapsforge.map.layer.cache.FileSystemTileCache;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.cache.TwoLevelTileCache;
import org.mapsforge.map.layer.queue.Job;
import org.mapsforge.map.layer.queue.JobQueue;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.util.LayerUtil;
import org.mapsforge.map.util.MapPositionUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class TileLayer<T extends Job> extends Layer {
    private float alpha = 1.0f;
    private int cacheTileMargin = 0, cacheZoomMinus = 0, cacheZoomPlus = 0;
    protected final boolean hasJobQueue;
    protected final boolean isTransparent;
    protected JobQueue<T> jobQueue;
    protected final TileCache tileCache;
    private final MapViewPosition mapViewPosition;
    private final Matrix matrix;
    private Parameters.ParentTilesRendering parentTilesRendering = Parameters.PARENT_TILES_RENDERING;

    public TileLayer(TileCache tileCache, MapViewPosition mapViewPosition, Matrix matrix, boolean isTransparent) {
        this(tileCache, mapViewPosition, matrix, isTransparent, true);
    }

    public TileLayer(TileCache tileCache, MapViewPosition mapViewPosition, Matrix matrix, boolean isTransparent, boolean hasJobQueue) {
        super();

        if (tileCache == null) {
            throw new IllegalArgumentException("tileCache must not be null");
        } else if (mapViewPosition == null) {
            throw new IllegalArgumentException("mapViewPosition must not be null");
        }

        this.hasJobQueue = hasJobQueue;
        this.tileCache = tileCache;
        this.mapViewPosition = mapViewPosition;
        this.matrix = matrix;
        this.isTransparent = isTransparent;
    }

    @Override
    public void draw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point topLeftPoint, Rotation rotation) {
        List<TilePosition> tilePositions = LayerUtil.getTilePositions(boundingBox, zoomLevel, topLeftPoint,
                this.displayModel.getTileSize());

        // In a rotation situation it is possible that drawParentTileBitmap sets the
        // clipping bounds to portrait, while the device is just being rotated into
        // landscape: the result is a partially painted screen that only goes away
        // after zooming (which has the effect of resetting the clip bounds if drawParentTileBitmap
        // is called again).
        // Always resetting the clip bounds here seems to avoid the problem,
        // I assume that this is a pretty cheap operation, otherwise it would be better
        // to hook this into the onConfigurationChanged call chain.
        canvas.resetClip();

        if (!isTransparent) {
            canvas.fillColor(this.displayModel.getBackgroundColor());
        }

        Set<Job> jobs = new HashSet<>();
        for (TilePosition tilePosition : tilePositions) {
            jobs.add(createJob(tilePosition.tile));
        }
        this.tileCache.setWorkingSet(jobs);

        for (int i = tilePositions.size() - 1; i >= 0; --i) {
            TilePosition tilePosition = tilePositions.get(i);
            Point point = tilePosition.point;
            Tile tile = tilePosition.tile;
            T job = createJob(tile);
            TileBitmap bitmap = this.tileCache.getImmediately(job);

            if (bitmap == null) {
                if (this.hasJobQueue && !this.tileCache.containsKey(job)) {
                    this.jobQueue.add(job);
                }
                if (this.parentTilesRendering != Parameters.ParentTilesRendering.OFF) {
                    drawParentTileBitmap(canvas, point, tile);
                }
            } else {
                if (isTileStale(tile, bitmap) && this.hasJobQueue && !this.tileCache.containsKey(job)) {
                    this.jobQueue.add(job);
                }
                retrieveLabelsOnly(job);
                canvas.drawBitmap(bitmap, (int) Math.round(point.x), (int) Math.round(point.y), this.alpha);
                bitmap.decrementRefCount();
            }
        }
        if (zoomLevel > 0) {
            // Pre-cache tiles outside the screen
            if (this.cacheTileMargin > 0) {
                processTilePositions(boundingBox, zoomLevel, canvas, topLeftPoint, rotation);
            }

            // Pre-cache tiles for +zoom
            if (this.cacheZoomPlus > 0) {
                for (int i = 1; i <= this.cacheZoomPlus; i++) {
                    byte zoom = (byte) (zoomLevel + i);
                    if (zoom > this.mapViewPosition.getZoomLevelMax()) {
                        break;
                    }
                    processTilePositions(null, zoom, canvas, null, rotation);
                }
            }

            // Pre-cache tiles for -zoom
            if (this.cacheZoomMinus > 0) {
                for (int i = 1; i <= this.cacheZoomMinus; i++) {
                    byte zoom = (byte) (zoomLevel - i);
                    if (zoom < this.mapViewPosition.getZoomLevelMin()) {
                        break;
                    }
                    processTilePositions(null, zoom, canvas, null, rotation);
                }
            }
        }
        if (this.hasJobQueue) {
            this.jobQueue.notifyWorkers();
        }
    }

    private void processTilePositions(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point topLeftPoint, Rotation rotation) {
        if (!(this.tileCache instanceof TwoLevelTileCache)) {
            return;
        }
        TileCache secondLevelTileCache = ((TwoLevelTileCache) this.tileCache).getSecondLevelTileCache();
        if (!(secondLevelTileCache instanceof FileSystemTileCache)) {
            return;
        }
        if (boundingBox == null) {
            boundingBox = MapPositionUtil.getBoundingBox(this.mapViewPosition.getCenter(), zoomLevel, rotation,
                    this.displayModel.getTileSize(), canvas.getDimension(),
                    this.mapViewPosition.getMapViewCenterX(), this.mapViewPosition.getMapViewCenterY());
        }
        if (topLeftPoint == null) {
            topLeftPoint = MapPositionUtil.getTopLeftPoint(this.mapViewPosition.getCenter(), zoomLevel,
                    canvas.getDimension(), this.displayModel.getTileSize());
        }
        List<TilePosition> tilePositions = LayerUtil.getTilePositions(boundingBox, zoomLevel, topLeftPoint,
                this.displayModel.getTileSize(), this.cacheTileMargin);

        for (int i = tilePositions.size() - 1; i >= 0; --i) {
            TilePosition tilePosition = tilePositions.get(i);
            Tile tile = tilePosition.tile;
            T job = createJob(tile);

            if (this.hasJobQueue && !secondLevelTileCache.containsKey(job)) {
                this.jobQueue.add(job);
            }
        }
    }

    @Override
    public synchronized void setDisplayModel(DisplayModel displayModel) {
        super.setDisplayModel(displayModel);
        if (displayModel != null && this.hasJobQueue) {
            this.jobQueue = new JobQueue<>(this.mapViewPosition, this.displayModel);
        } else {
            this.jobQueue = null;
        }
    }

    protected abstract T createJob(Tile tile);

    /**
     * @return the margin to pre-cache tiles (≥ 0).
     */
    public int getCacheTileMargin() {
        return this.cacheTileMargin;
    }

    /**
     * @return the -zoom to pre-cache tiles (≥ 0).
     */
    public int getCacheZoomMinus() {
        return this.cacheZoomMinus;
    }

    /**
     * @return the +zoom to pre-cache tiles (≥ 0).
     */
    public int getCacheZoomPlus() {
        return this.cacheZoomPlus;
    }

    /**
     * Whether the tile is stale and should be refreshed.
     * <p/>
     * This method is called from {@link #draw(BoundingBox, byte, Canvas, Point, Rotation)} to determine whether the
     * tile needs to be refreshed. Subclasses must override this method and implement appropriate checks to determine
     * when a tile is stale.
     * <p/>
     * Return {@code false} to use the cached copy without attempting to refresh it.
     * <p/>
     * Return {@code true} to cause the layer to attempt to obtain a fresh copy of the tile. The layer will first
     * display the tile referenced by {@code bitmap} and attempt to obtain a fresh copy in the background. When a fresh
     * copy becomes available, the layer will replace is and update the cache. If a fresh copy cannot be obtained (e.g.
     * because the tile is obtained from an online source which cannot be reached), the stale tile will continue to be
     * used until another {@code #draw(BoundingBox, byte, Canvas, Point)} operation requests it again.
     *
     * @param tile   A tile.
     * @param bitmap The bitmap for {@code tile} currently held in the layer's cache.
     */
    protected abstract boolean isTileStale(Tile tile, TileBitmap bitmap);

    protected void retrieveLabelsOnly(T job) {
    }

    private void drawParentTileBitmap(Canvas canvas, Point point, Tile tile) {
        Tile cachedParentTile = getCachedParentTile(tile, 4);
        if (cachedParentTile != null) {
            Bitmap bitmap = this.tileCache.getImmediately(createJob(cachedParentTile));
            if (bitmap != null) {
                int tileSize = this.displayModel.getTileSize();
                long translateX = tile.getShiftX(cachedParentTile) * tileSize;
                long translateY = tile.getShiftY(cachedParentTile) * tileSize;
                byte zoomLevelDiff = (byte) (tile.zoomLevel - cachedParentTile.zoomLevel);
                float scaleFactor = (float) Math.pow(2, zoomLevelDiff);

                int x = (int) Math.round(point.x);
                int y = (int) Math.round(point.y);

                if (this.parentTilesRendering == Parameters.ParentTilesRendering.SPEED) {
                    boolean antiAlias = canvas.isAntiAlias();
                    boolean filterBitmap = canvas.isFilterBitmap();

                    canvas.setAntiAlias(false);
                    canvas.setFilterBitmap(false);

                    canvas.drawBitmap(bitmap,
                            (int) (translateX / scaleFactor), (int) (translateY / scaleFactor), (int) ((translateX + tileSize) / scaleFactor), (int) ((translateY + tileSize) / scaleFactor),
                            x, y, x + tileSize, y + tileSize,
                            this.alpha);

                    canvas.setAntiAlias(antiAlias);
                    canvas.setFilterBitmap(filterBitmap);
                } else {
                    this.matrix.reset();
                    this.matrix.translate(x - translateX, y - translateY);
                    this.matrix.scale(scaleFactor, scaleFactor);

                    canvas.setClip(x, y, this.displayModel.getTileSize(), this.displayModel.getTileSize());
                    canvas.drawBitmap(bitmap, this.matrix, this.alpha);
                    canvas.resetClip();
                }

                bitmap.decrementRefCount();
            }
        }
    }

    public float getAlpha() {
        return this.alpha;
    }

    /**
     * @return the first parent object of the given object whose tileCacheBitmap is cached (may be null).
     */
    private Tile getCachedParentTile(Tile tile, int level) {
        if (level == 0) {
            return null;
        }

        Tile parentTile = tile.getParent();
        if (parentTile == null) {
            return null;
        } else if (this.tileCache.containsKey(createJob(parentTile))) {
            return parentTile;
        }

        return getCachedParentTile(parentTile, level - 1);
    }

    public TileCache getTileCache() {
        return this.tileCache;
    }

    public void setAlpha(float alpha) {
        this.alpha = Math.max(0, Math.min(1, alpha));
    }

    public void setParentTilesRendering(Parameters.ParentTilesRendering parentTilesRendering) {
        this.parentTilesRendering = parentTilesRendering;
    }

    /**
     * Set the margin to pre-cache tiles (≥ 0).
     */
    public void setCacheTileMargin(int cacheTileMargin) {
        this.cacheTileMargin = Math.max(0, cacheTileMargin);
    }

    /**
     * Set the -zoom to pre-cache tiles (≥ 0).
     */
    public void setCacheZoomMinus(int cacheZoomMinus) {
        this.cacheZoomMinus = Math.max(0, cacheZoomMinus);
    }

    /**
     * Set the +zoom to pre-cache tiles (≥ 0).
     */
    public void setCacheZoomPlus(int cacheZoomPlus) {
        this.cacheZoomPlus = Math.max(0, cacheZoomPlus);
    }
}
