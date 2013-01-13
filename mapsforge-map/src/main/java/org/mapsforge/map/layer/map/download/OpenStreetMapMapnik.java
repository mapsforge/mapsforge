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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.mapsforge.core.model.Tile;

public class OpenStreetMapMapnik implements TileSource {
	private static final String HOST_NAME = ".tile.openstreetmap.org";
	private static final int PORT = 80;
	private static final String PROTOCOL = "http";
	private static final int ZOOM_LEVEL_MAX = 18;
	private static final int ZOOM_LEVEL_MIN = 0;

	/**
	 * Factory method to create a new {@link OpenStreetMapMapnik} instance with the default parameters.
	 */
	public static OpenStreetMapMapnik create() {
		Set<String> hostNames = new HashSet<String>();
		hostNames.add('a' + HOST_NAME);
		hostNames.add('b' + HOST_NAME);
		hostNames.add('c' + HOST_NAME);
		return new OpenStreetMapMapnik(hostNames, PORT);
	}

	private int hosteNameIndex;
	private final String[] hostNames;
	private final int port;

	public OpenStreetMapMapnik(Set<String> hostNames, int port) {
		if (hostNames == null || hostNames.isEmpty()) {
			throw new IllegalArgumentException("no host name specified");
		} else if (port < 0 || port > 65535) {
			throw new IllegalArgumentException("invalid port number: " + port);
		}

		this.hostNames = hostNames.toArray(new String[0]);
		this.port = port;

		Arrays.sort(this.hostNames);
		this.hosteNameIndex = -1;
	}

	@Override
	public int getParallelRequestsLimit() {
		// the HTTP specification recommends a maximum of two parallel requests per host name
		return this.hostNames.length * 2;
	}

	@Override
	public URL getTileUrl(Tile tile) throws MalformedURLException {
		StringBuilder stringBuilder = new StringBuilder();

		stringBuilder.append('/');
		stringBuilder.append(tile.zoomLevel);
		stringBuilder.append('/');
		stringBuilder.append(tile.tileX);
		stringBuilder.append('/');
		stringBuilder.append(tile.tileY);
		stringBuilder.append(".png");

		return new URL(PROTOCOL, getHostname(), this.port, stringBuilder.toString());
	}

	@Override
	public byte getZoomLevelMax() {
		return ZOOM_LEVEL_MAX;
	}

	@Override
	public byte getZoomLevelMin() {
		return ZOOM_LEVEL_MIN;
	}

	/**
	 * @return an entry from the host name array. The actual entry is determined via Round-robin.
	 */
	private String getHostname() {
		this.hosteNameIndex = ++this.hosteNameIndex % this.hostNames.length;
		return this.hostNames[this.hosteNameIndex];
	}
}
