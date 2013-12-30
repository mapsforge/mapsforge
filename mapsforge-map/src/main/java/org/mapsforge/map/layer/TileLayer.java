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
package org.mapsforge.map.layer;

import java.util.List;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.Matrix;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.queue.Job;
import org.mapsforge.map.layer.queue.JobQueue;
import org.mapsforge.map.model.MapViewPosition;

public abstract class TileLayer<T extends Job> extends Layer {
	protected final JobQueue<T> jobQueue;
	private final Matrix matrix;
	private final TileCache tileCache;

	public TileLayer(TileCache tileCache, MapViewPosition mapViewPosition, GraphicFactory graphicFactory) {
		super();

		if (tileCache == null) {
			throw new IllegalArgumentException("tileCache must not be null");
		} else if (mapViewPosition == null) {
			throw new IllegalArgumentException("mapViewPosition must not be null");
		}

		this.tileCache = tileCache;
		this.jobQueue = new JobQueue<T>(mapViewPosition);
		this.matrix = graphicFactory.createMatrix();
	}

	@Override
	public void draw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point topLeftPoint) {
	    List<TilePosition> tilePositions = LayerUtil.getTilePositions(boundingBox, zoomLevel, topLeftPoint);

		for (int i = tilePositions.size() - 1; i >= 0; --i) {
			TilePosition tilePosition = tilePositions.get(i);
			Point point = tilePosition.point;
			Tile tile = tilePosition.tile;
			Bitmap bitmap = this.tileCache.get(createJob(tile));

			if (bitmap == null) {
				this.jobQueue.add(createJob(tile));
				drawParentTileBitmap(canvas, point, tile);
			} else {
				canvas.drawBitmap(bitmap, (int) Math.round(point.x), (int) Math.round(point.y));
				bitmap.decrementRefCount();
			}
		}

		this.jobQueue.notifyWorkers();
	}

	protected abstract T createJob(Tile tile);

	@Override
	public void onDestroy() {
		this.tileCache.destroy();
	}

	private void drawParentTileBitmap(Canvas canvas, Point point, Tile tile) {
		Tile cachedParentTile = getCachedParentTile(tile, 4);
		if (cachedParentTile != null) {
			Bitmap bitmap = this.tileCache.get(createJob(cachedParentTile));
			if (bitmap != null) {
				long translateX = tile.getShiftX(cachedParentTile) * GraphicFactory.getTileSize();
				long translateY = tile.getShiftY(cachedParentTile) * GraphicFactory.getTileSize();
				byte zoomLevelDiff = (byte) (tile.zoomLevel - cachedParentTile.zoomLevel);
				float scaleFactor = (float) Math.pow(2, zoomLevelDiff);

				int x = (int) Math.round(point.x);
				int y = (int) Math.round(point.y);

				this.matrix.reset();
				this.matrix.translate(x - translateX, y - translateY);
				this.matrix.scale(scaleFactor, scaleFactor);

				canvas.setClip(x, y, GraphicFactory.getTileSize(), GraphicFactory.getTileSize());
				canvas.drawBitmap(bitmap, this.matrix);
				canvas.resetClip();
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
}
