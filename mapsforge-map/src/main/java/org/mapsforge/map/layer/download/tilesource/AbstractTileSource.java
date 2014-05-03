/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2014 devemux86
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
package org.mapsforge.map.layer.download.tilesource;

import java.util.Random;

public abstract class AbstractTileSource implements TileSource {
	protected final String[] hostNames;
	protected final int port;
	protected final Random random = new Random();

	protected AbstractTileSource(String[] hostNames, int port) {
		if (hostNames == null || hostNames.length == 0) {
			throw new IllegalArgumentException("no host names specified");
		}
		if (port < 0 || port > 65535) {
			throw new IllegalArgumentException("invalid port number: " + port);
		}
		for (String hostname : hostNames) {
			if (hostname.isEmpty()) {
				throw new IllegalArgumentException("empty host name in host name list");
			}
		}


		this.hostNames = hostNames;
		this.port = port;
	}

	protected String getHostName() {
		return this.hostNames[random.nextInt(this.hostNames.length)];
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (!(obj instanceof AbstractTileSource)) {
			return false;
		}
		AbstractTileSource other = (AbstractTileSource) obj;
		if (!java.util.Arrays.equals(this.hostNames, other.hostNames)) {
			return false;
		} else if (this.port != other.port) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + java.util.Arrays.hashCode(this.hostNames);
		result = prime * result + this.port;
		return result;
	}
}
