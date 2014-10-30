/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2014 mvglasow <michael -at- vonglasow.com>
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
package org.mapsforge.map.layer.queue;

import java.io.File;

import org.mapsforge.core.model.Tile;

public class Job {
	public final boolean hasAlpha;
	public final Tile tile;
	public final String key;

	public Job(Tile tile, boolean hasAlpha) {
		if (tile == null) {
			throw new IllegalArgumentException("tile must not be null");
		}

		this.tile = tile;
		this.hasAlpha = hasAlpha;
		this.key = new StringBuilder().append(this.tile.zoomLevel).append(File.separatorChar).
				append(this.tile.tileX).append(File.separatorChar).append(this.tile.tileY).toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (!(obj instanceof Job)) {
			return false;
		}
		Job other = (Job) obj;
		if (this.hasAlpha != other.hasAlpha) {
			return false;
		}
		return this.tile.equals(other.tile);
	}

	/**
	 * Returns a unique identifier for the tile.
	 * <p>
	 * The key has the form {@code zoom/x/y}, which is the de-facto standard for tile references. The default path
	 * separator character of the platform is used between {@code zoom}, {@code x} and {@code y}.
	 *
	 * @since 0.5.0
	 */
	public String getKey() {
		return this.key;
	}

	@Override
	public int hashCode() {
		return this.tile.hashCode();
	}
}
