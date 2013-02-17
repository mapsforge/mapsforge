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
import java.util.zip.GZIPInputStream;

import org.mapsforge.core.util.IOUtils;
import org.mapsforge.map.graphics.Bitmap;
import org.mapsforge.map.rendertheme.GraphicAdapter;

class TileDownloader {
	private static final int TIMEOUT_CONNECT = 5000;
	private static final int TIMEOUT_READ = 10000;

	private static InputStream getInputStream(URLConnection urlConnection) throws IOException {
		if ("gzip".equals(urlConnection.getContentEncoding())) {
			return new GZIPInputStream(urlConnection.getInputStream());
		}
		return urlConnection.getInputStream();
	}

	private static URLConnection getURLConnection(URL url) throws IOException {
		URLConnection urlConnection = url.openConnection();
		urlConnection.setConnectTimeout(TIMEOUT_CONNECT);
		urlConnection.setReadTimeout(TIMEOUT_READ);
		return urlConnection;
	}

	private final DownloadJob downloadJob;
	private final GraphicAdapter graphicAdapter;

	TileDownloader(DownloadJob downloadJob, GraphicAdapter graphicAdapter) {
		if (downloadJob == null) {
			throw new IllegalArgumentException("downloadJob must not be null");
		} else if (graphicAdapter == null) {
			throw new IllegalArgumentException("graphicAdapter must not be null");
		}

		this.downloadJob = downloadJob;
		this.graphicAdapter = graphicAdapter;
	}

	Bitmap downloadImage() throws IOException {
		URL url = this.downloadJob.tileSource.getTileUrl(this.downloadJob.tile);
		URLConnection urlConnection = getURLConnection(url);
		InputStream inputStream = getInputStream(urlConnection);

		try {
			return this.graphicAdapter.decodeStream(inputStream);
		} finally {
			IOUtils.closeQuietly(inputStream);
		}
	}
}
