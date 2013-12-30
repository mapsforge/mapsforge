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

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.map.layer.Layer;

import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.queue.JobQueue;
import org.mapsforge.map.util.PausableThread;

class TileDownloadThread extends PausableThread {
	private static final Logger LOGGER = Logger.getLogger(TileDownloadThread.class.getName());

	private final GraphicFactory graphicFactory;
	private final JobQueue<DownloadJob> jobQueue;
	private final Layer layer;
	private final TileCache tileCache;

	TileDownloadThread(TileCache tileCache, JobQueue<DownloadJob> jobQueue, Layer layer, GraphicFactory graphicFactory) {
		super();

		this.tileCache = tileCache;
		this.jobQueue = jobQueue;
		this.layer = layer;
		this.graphicFactory = graphicFactory;
	}

	@Override
	protected void doWork() throws InterruptedException {
		DownloadJob downloadJob = this.jobQueue.get();

		try {
			if (!this.tileCache.containsKey(downloadJob)) {
				downloadTile(downloadJob);
			}
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		} finally {
			this.jobQueue.remove(downloadJob);
		}
	}

	@Override
	protected ThreadPriority getThreadPriority() {
		return ThreadPriority.BELOW_NORMAL;
	}

	@Override
	protected boolean hasWork() {
		return true;
	}

	private void downloadTile(DownloadJob downloadJob) throws IOException {
		TileDownloader tileDownloader = new TileDownloader(downloadJob, this.graphicFactory);
		TileBitmap bitmap = tileDownloader.downloadImage();

		if (!isInterrupted() && bitmap != null) {
			bitmap.scaleTo(GraphicFactory.getTileSize(), GraphicFactory.getTileSize());
			this.tileCache.put(downloadJob, bitmap);
			this.layer.requestRedraw();
		}
	}
}
