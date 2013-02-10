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
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mapsforge.core.util.IOUtils;
import org.mapsforge.map.PausableThread;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.queue.JobQueue;
import org.mapsforge.map.viewinterfaces.LayerManagerInterface;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;

class TileDownloadThread extends PausableThread {
	private static final BitmapFactory.Options BITMAP_FACTORY_OPTIONS = createBitmapFactoryOptions();
	private static final Logger LOGGER = Logger.getLogger(TileDownloadThread.class.getName());
	private static final int TIMEOUT_CONNECT = 5000;
	private static final int TIMEOUT_READ = 10000;

	private static BitmapFactory.Options createBitmapFactoryOptions() {
		BitmapFactory.Options bitmapFactoryOptions = new BitmapFactory.Options();
		bitmapFactoryOptions.inPreferredConfig = Config.ARGB_8888;
		return bitmapFactoryOptions;
	}

	private static URLConnection getUrlConnection(URL tileUrl) throws IOException {
		URLConnection urlConnection = tileUrl.openConnection();
		urlConnection.setConnectTimeout(TIMEOUT_CONNECT);
		urlConnection.setReadTimeout(TIMEOUT_READ);
		return urlConnection;
	}

	private final JobQueue<DownloadJob> jobQueue;
	private final LayerManagerInterface layerManagerInterface;
	private final TileCache<DownloadJob> tileCache;

	TileDownloadThread(TileCache<DownloadJob> tileCache, JobQueue<DownloadJob> jobQueue,
			LayerManagerInterface layerManagerInterface) {
		super();

		this.tileCache = tileCache;
		this.jobQueue = jobQueue;
		this.layerManagerInterface = layerManagerInterface;
	}

	@Override
	protected void doWork() throws InterruptedException {
		DownloadJob downloadJob = this.jobQueue.remove();

		try {
			URL tileUrl = downloadJob.tileSource.getTileUrl(downloadJob.tile);
			LOGGER.log(Level.INFO, "downloading: " + tileUrl.toExternalForm());

			URLConnection urlConnection = getUrlConnection(tileUrl);
			InputStream inputStream = urlConnection.getInputStream();
			try {
				Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, BITMAP_FACTORY_OPTIONS);
				this.tileCache.put(downloadJob, bitmap);
				this.layerManagerInterface.redrawLayers();
			} finally {
				IOUtils.closeQuietly(inputStream);
			}
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
