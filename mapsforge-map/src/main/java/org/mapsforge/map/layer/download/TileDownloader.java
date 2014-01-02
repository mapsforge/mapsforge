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
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.GZIPInputStream;

import org.mapsforge.core.graphics.CorruptedInputStream;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.core.util.IOUtils;

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
	private final GraphicFactory graphicFactory;

	TileDownloader(DownloadJob downloadJob, GraphicFactory graphicFactory) {
		if (downloadJob == null) {
			throw new IllegalArgumentException("downloadJob must not be null");
		} else if (graphicFactory == null) {
			throw new IllegalArgumentException("graphicFactory must not be null");
		}

		this.downloadJob = downloadJob;
		this.graphicFactory = graphicFactory;
	}

	TileBitmap downloadImage() throws IOException {
		URL url = this.downloadJob.tileSource.getTileUrl(this.downloadJob.tile);
		URLConnection urlConnection = getURLConnection(url);
		InputStream inputStream = getInputStream(urlConnection);

		try {
			return this.graphicFactory.createTileBitmap(inputStream, this.downloadJob.tileSize);
        } catch (CorruptedInputStream e) {
            // the creation of the tile bit map can fail at, at least on Android,
            // when the connection is slow or busy, returning null here ensures that
            // the tile will be downloaded again
            return null;
 		} finally {
			IOUtils.closeQuietly(inputStream);
		}
	}
}
