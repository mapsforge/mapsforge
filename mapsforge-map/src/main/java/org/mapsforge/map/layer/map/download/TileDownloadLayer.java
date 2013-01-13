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
package org.mapsforge.map.layer.map.download;

import java.util.ArrayList;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.map.InMemoryTileCache;
import org.mapsforge.map.layer.map.LayerUtil;
import org.mapsforge.map.layer.map.TileCache;
import org.mapsforge.map.layer.map.TilePosition;
import org.mapsforge.map.layer.map.queue.TileQueue;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.view.LayerManager;

import android.graphics.Bitmap;
import android.graphics.Canvas;

public class TileDownloadLayer extends Layer {
	private static final int DOWNLOAD_THREADS_MAX = 8;

	private final TileCache tileCache;
	private final TileDownloadThread[] tileDownloadThreads;
	private final TileQueue tileQueue;

	public TileDownloadLayer(TileSource tileSource, MapViewPosition mapViewPosition, LayerManager layerManager) {
		this.tileCache = new InMemoryTileCache(30);
		this.tileQueue = new TileQueue(mapViewPosition);

		int numberOfDownloadThreads = Math.min(tileSource.getParallelRequestsLimit(), DOWNLOAD_THREADS_MAX);
		this.tileDownloadThreads = new TileDownloadThread[numberOfDownloadThreads];
		for (int i = 0; i < numberOfDownloadThreads; ++i) {
			TileDownloadThread tileDownloadThread = new TileDownloadThread(this.tileCache, this.tileQueue, tileSource,
					layerManager);
			tileDownloadThread.start();
			this.tileDownloadThreads[i] = tileDownloadThread;
		}
	}

	@Override
	public void destroy() {
		for (TileDownloadThread tileDownloadThread : this.tileDownloadThreads) {
			tileDownloadThread.interrupt();
		}
	}

	@Override
	public void draw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point canvasPosition) {
		ArrayList<TilePosition> tilePositions = LayerUtil.getTilePositions(boundingBox, zoomLevel, canvasPosition);

		for (int i = tilePositions.size() - 1; i >= 0; --i) {
			TilePosition tilePosition = tilePositions.get(i);
			Bitmap bitmap = this.tileCache.get(tilePosition.tile);
			if (bitmap == null) {
				this.tileQueue.add(tilePosition.tile);
			} else {
				Point point = tilePosition.point;
				canvas.drawBitmap(bitmap, (float) point.x, (float) point.y, null);
			}
		}

		synchronized (this.tileQueue) {
			this.tileQueue.notifyAll();
		}
	}
}
