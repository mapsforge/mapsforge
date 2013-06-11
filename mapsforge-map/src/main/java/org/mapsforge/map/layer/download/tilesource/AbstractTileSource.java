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
package org.mapsforge.map.layer.download.tilesource;

public abstract class AbstractTileSource implements TileSource {
	protected final String hostName;
	protected final int port;

	protected AbstractTileSource(String hostName, int port) {
		if (hostName == null || hostName.isEmpty()) {
			throw new IllegalArgumentException("no host name specified");
		} else if (port < 0 || port > 65535) {
			throw new IllegalArgumentException("invalid port number: " + port);
		}

		this.hostName = hostName;
		this.port = port;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (!(obj instanceof AbstractTileSource)) {
			return false;
		}
		AbstractTileSource other = (AbstractTileSource) obj;
		if (!this.hostName.equals(other.hostName)) {
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
		result = prime * result + this.hostName.hashCode();
		result = prime * result + this.port;
		return result;
	}
}
