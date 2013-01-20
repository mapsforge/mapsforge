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
package org.mapsforge.map.controller.layer.map.download;

import java.io.File;
import java.util.ArrayList;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.controller.layer.Layer;
import org.mapsforge.map.controller.layer.map.FileSystemTileCache;
import org.mapsforge.map.controller.layer.map.InMemoryTileCache;
import org.mapsforge.map.controller.layer.map.LayerUtil;
import org.mapsforge.map.controller.layer.map.TileCache;
import org.mapsforge.map.controller.layer.map.TilePosition;
import org.mapsforge.map.controller.layer.map.TwoLevelTileCache;
import org.mapsforge.map.controller.layer.map.queue.TileQueue;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.viewinterfaces.LayerManagerInterface;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Matrix;

public class TileDownloadLayer extends Layer {
	// TODO set back to 8
	private static final int DOWNLOAD_THREADS_MAX = 1;

	private final TileCache tileCache;
	private final Bitmap tileCacheBitmap;
	private final TileDownloadThread[] tileDownloadThreads;
	private final TileQueue tileQueue;

	public TileDownloadLayer(File cacheDirectory, TileSource tileSource, MapViewPosition mapViewPosition,
			LayerManagerInterface layerManagerInterface) {
		this.tileCache = new TwoLevelTileCache(new InMemoryTileCache(25), new FileSystemTileCache(256, cacheDirectory));

		this.tileQueue = new TileQueue(mapViewPosition);
		this.tileCacheBitmap = Bitmap.createBitmap(Tile.TILE_SIZE, Tile.TILE_SIZE, Config.ARGB_8888);

		int numberOfDownloadThreads = Math.min(tileSource.getParallelRequestsLimit(), DOWNLOAD_THREADS_MAX);
		this.tileDownloadThreads = new TileDownloadThread[numberOfDownloadThreads];
		for (int i = 0; i < numberOfDownloadThreads; ++i) {
			TileDownloadThread tileDownloadThread = new TileDownloadThread(this.tileCache, this.tileQueue, tileSource,
					layerManagerInterface);
			tileDownloadThread.start();
			this.tileDownloadThreads[i] = tileDownloadThread;
		}
	}

	@Override
	public void destroy() {
		for (TileDownloadThread tileDownloadThread : this.tileDownloadThreads) {
			tileDownloadThread.interrupt();
		}

		this.tileCache.destroy();
	}

	@Override
	public void draw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point canvasPosition) {
		ArrayList<TilePosition> tilePositions = LayerUtil.getTilePositions(boundingBox, zoomLevel, canvasPosition);

		for (int i = tilePositions.size() - 1; i >= 0; --i) {
			TilePosition tilePosition = tilePositions.get(i);
			Point point = tilePosition.point;
			Tile tile = tilePosition.tile;
			Bitmap bitmap = this.tileCache.get(tile, this.tileCacheBitmap);

			if (bitmap == null) {
				this.tileQueue.add(tile);
				drawParentTileBitmap(canvas, point, tile);
			} else {
				canvas.drawBitmap(bitmap, (float) point.x, (float) point.y, null);
			}
		}

		this.tileQueue.notifyWorkers();
	}

	private void drawParentTileBitmap(Canvas canvas, Point point, Tile tile) {
		Tile cachedParentTile = getCachedParentTile(tile, 4);
		if (cachedParentTile != null) {
			Bitmap bitmap = this.tileCache.get(cachedParentTile, this.tileCacheBitmap);
			if (bitmap != null) {
				long translateX = tile.getShiftX(cachedParentTile) * Tile.TILE_SIZE;
				long translateY = tile.getShiftY(cachedParentTile) * Tile.TILE_SIZE;
				byte zoomLevelDiff = (byte) (tile.zoomLevel - cachedParentTile.zoomLevel);
				float scaleFactor = (float) Math.pow(2, zoomLevelDiff);

				Matrix matrix = new Matrix();
				matrix.setScale(scaleFactor, scaleFactor);
				matrix.postTranslate((float) (point.x - translateX), (float) (point.y - translateY));
				canvas.drawBitmap(bitmap, matrix, null);
			}
		}
	}

	/**
	 * @return the first parent tile of the given tile whose tileCacheBitmap is cached (may be null).
	 */
	private Tile getCachedParentTile(Tile tile, int level) {
		if (level == 0) {
			return null;
		}

		Tile parentTile = tile.getParent();
		if (parentTile == null) {
			return null;
		} else if (this.tileCache.containsKey(parentTile)) {
			return parentTile;
		}

		return getCachedParentTile(parentTile, level - 1);
	}
}
