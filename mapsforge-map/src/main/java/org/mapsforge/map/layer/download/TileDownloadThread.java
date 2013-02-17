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

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mapsforge.map.PausableThread;
import org.mapsforge.map.graphics.Bitmap;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.queue.JobQueue;
import org.mapsforge.map.rendertheme.GraphicAdapter;
import org.mapsforge.map.viewinterfaces.LayerManagerInterface;

class TileDownloadThread extends PausableThread {
	private static final Logger LOGGER = Logger.getLogger(TileDownloadThread.class.getName());

	private final GraphicAdapter graphicAdapter;
	private final JobQueue<DownloadJob> jobQueue;
	private final LayerManagerInterface layerManagerInterface;
	private final TileCache<DownloadJob> tileCache;

	TileDownloadThread(TileCache<DownloadJob> tileCache, JobQueue<DownloadJob> jobQueue,
			LayerManagerInterface layerManagerInterface, GraphicAdapter graphicAdapter) {
		super();

		this.tileCache = tileCache;
		this.jobQueue = jobQueue;
		this.layerManagerInterface = layerManagerInterface;
		this.graphicAdapter = graphicAdapter;
	}

	@Override
	protected void doWork() throws InterruptedException {
		DownloadJob downloadJob = this.jobQueue.remove();

		try {
			TileDownloader tileDownloader = new TileDownloader(downloadJob, this.graphicAdapter);
			Bitmap bitmap = tileDownloader.downloadImage();

			this.tileCache.put(downloadJob, bitmap);
			this.layerManagerInterface.redrawLayers();
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
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
}
