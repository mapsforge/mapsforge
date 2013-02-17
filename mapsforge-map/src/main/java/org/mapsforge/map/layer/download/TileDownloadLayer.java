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
package org.mapsforge.map.layer.download;

import org.mapsforge.core.model.Tile;
import org.mapsforge.graphics.android.AndroidGraphics;
import org.mapsforge.map.layer.TileLayer;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.download.tilesource.TileSource;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.viewinterfaces.LayerManagerInterface;

public class TileDownloadLayer extends TileLayer<DownloadJob> {
	private static final int DOWNLOAD_THREADS_MAX = 8;

	private final TileDownloadThread[] tileDownloadThreads;
	private final TileSource tileSource;

	public TileDownloadLayer(TileCache<DownloadJob> tileCache, MapViewPosition mapViewPosition, TileSource tileSource,
			LayerManagerInterface layerManagerInterface) {
		super(tileCache, mapViewPosition);

		if (tileSource == null) {
			throw new IllegalArgumentException("tileSource must not be null");
		} else if (layerManagerInterface == null) {
			throw new IllegalArgumentException("layerManagerInterface must not be null");
		}

		this.tileSource = tileSource;

		int numberOfDownloadThreads = Math.min(tileSource.getParallelRequestsLimit(), DOWNLOAD_THREADS_MAX);
		this.tileDownloadThreads = new TileDownloadThread[numberOfDownloadThreads];
		for (int i = 0; i < numberOfDownloadThreads; ++i) {
			TileDownloadThread tileDownloadThread = new TileDownloadThread(tileCache, this.jobQueue,
					layerManagerInterface, AndroidGraphics.INSTANCE);
			tileDownloadThread.start();
			this.tileDownloadThreads[i] = tileDownloadThread;
		}
	}

	@Override
	public void destroy() {
		for (TileDownloadThread tileDownloadThread : this.tileDownloadThreads) {
			tileDownloadThread.interrupt();
		}

		super.destroy();
	}

	@Override
	protected DownloadJob createJob(Tile tile) {
		return new DownloadJob(tile, this.tileSource);
	}
}
