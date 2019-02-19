/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2015-2019 devemux86
 * Copyright 2019 cpt1gl0
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
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.Parameters;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.queue.Job;
import org.mapsforge.map.layer.queue.JobQueue;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.model.IMapViewPosition;
import org.mapsforge.map.util.LayerUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class TileLayer<T extends Job> extends Layer {
    protected final boolean hasJobQueue;
    protected final boolean isTransparent;
    protected JobQueue<T> jobQueue;
    protected final TileCache tileCache;
    private final IMapViewPosition mapViewPosition;
    private final Matrix matrix;

    public TileLayer(TileCache tileCache, IMapViewPosition mapViewPosition, Matrix matrix, boolean isTransparent) {
        this(tileCache, mapViewPosition, matrix, isTransparent, true);
    }

    public TileLayer(TileCache tileCache, IMapViewPosition mapViewPosition, Matrix matrix, boolean isTransparent, boolean hasJobQueue) {
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
    public void draw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point topLeftPoint) {
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
                if (Parameters.PARENT_TILES_RENDERING != Parameters.ParentTilesRendering.OFF) {
                    drawParentTileBitmap(canvas, point, tile);
                }
            } else {
                if (isTileStale(tile, bitmap) && this.hasJobQueue && !this.tileCache.containsKey(job)) {
                    this.jobQueue.add(job);
                }
                retrieveLabelsOnly(job);
                canvas.drawBitmap(bitmap, (int) Math.round(point.x), (int) Math.round(point.y), this.displayModel.getFilter());
                bitmap.decrementRefCount();
            }
        }
        if (this.hasJobQueue) {
            this.jobQueue.notifyWorkers();
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
     * Whether the tile is stale and should be refreshed.
     * <p/>
     * This method is called from {@link #draw(BoundingBox, byte, Canvas, Point)} to determine whether the tile needs to
     * be refreshed. Subclasses must override this method and implement appropriate checks to determine when a tile is
     * stale.
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

                if (Parameters.PARENT_TILES_RENDERING == Parameters.ParentTilesRendering.SPEED) {
                    boolean antiAlias = canvas.isAntiAlias();
                    boolean filterBitmap = canvas.isFilterBitmap();

                    canvas.setAntiAlias(false);
                    canvas.setFilterBitmap(false);

                    canvas.drawBitmap(bitmap,
                            (int) (translateX / scaleFactor), (int) (translateY / scaleFactor), (int) ((translateX + tileSize) / scaleFactor), (int) ((translateY + tileSize) / scaleFactor),
                            x, y, x + tileSize, y + tileSize,
                            this.displayModel.getFilter());

                    canvas.setAntiAlias(antiAlias);
                    canvas.setFilterBitmap(filterBitmap);
                } else {
                    this.matrix.reset();
                    this.matrix.translate(x - translateX, y - translateY);
                    this.matrix.scale(scaleFactor, scaleFactor);

                    canvas.setClip(x, y, this.displayModel.getTileSize(), this.displayModel.getTileSize());
                    canvas.drawBitmap(bitmap, this.matrix, this.displayModel.getFilter());
                    canvas.resetClip();
                }

                bitmap.decrementRefCount();
            }
        }
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
}
