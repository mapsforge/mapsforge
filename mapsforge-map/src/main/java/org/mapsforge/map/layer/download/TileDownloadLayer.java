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
package org.mapsforge.map.layer.download;

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.layer.TileLayer;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.download.tilesource.TileSource;
import org.mapsforge.map.model.MapViewPosition;

public class TileDownloadLayer extends TileLayer<DownloadJob> {
	private static final int DOWNLOAD_THREADS_MAX = 8;

	private final TileDownloadThread[] tileDownloadThreads;
	private final TileSource tileSource;
    private boolean started;

	public TileDownloadLayer(TileCache tileCache, MapViewPosition mapViewPosition, TileSource tileSource,
			GraphicFactory graphicFactory) {
		super(tileCache, mapViewPosition, graphicFactory);

		if (tileSource == null) {
			throw new IllegalArgumentException("tileSource must not be null");
		}

		this.tileSource = tileSource;

		int numberOfDownloadThreads = Math.min(tileSource.getParallelRequestsLimit(), DOWNLOAD_THREADS_MAX);
		this.tileDownloadThreads = new TileDownloadThread[numberOfDownloadThreads];
		for (int i = 0; i < numberOfDownloadThreads; ++i) {
			this.tileDownloadThreads[i] = new TileDownloadThread(tileCache, this.jobQueue, this, graphicFactory);
		}
	}

	@Override
	public void draw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point topLeftPoint) {
		if (zoomLevel < this.tileSource.getZoomLevelMin() || zoomLevel > this.tileSource.getZoomLevelMax()) {
			return;
		}

		super.draw(boundingBox, zoomLevel, canvas, topLeftPoint);
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

	public void onResume() {
        if (!started) {
            start();
        }
		for (TileDownloadThread tileDownloadThread : this.tileDownloadThreads) {
			tileDownloadThread.proceed();
		}
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
}
