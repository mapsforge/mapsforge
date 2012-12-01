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
package org.mapsforge.android.maps;

import java.io.Serializable;

/**
 * A simple DTO to stores flags for debugging rendered map tiles.
 */
public class DebugSettings implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * True if drawing of tile coordinates is enabled, false otherwise.
	 */
	public final boolean drawTileCoordinates;

	/**
	 * True if drawing of tile frames is enabled, false otherwise.
	 */
	public final boolean drawTileFrames;

	/**
	 * True if highlighting of water tiles is enabled, false otherwise.
	 */
	public final boolean highlightWaterTiles;

	private final int hashCodeValue;

	/**
	 * @param drawTileCoordinates
	 *            if drawing of tile coordinates is enabled.
	 * @param drawTileFrames
	 *            if drawing of tile frames is enabled.
	 * @param highlightWaterTiles
	 *            if highlighting of water tiles is enabled.
	 */
	public DebugSettings(boolean drawTileCoordinates, boolean drawTileFrames, boolean highlightWaterTiles) {
		this.drawTileCoordinates = drawTileCoordinates;
		this.drawTileFrames = drawTileFrames;
		this.highlightWaterTiles = highlightWaterTiles;
		this.hashCodeValue = calculateHashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof DebugSettings)) {
			return false;
		}
		DebugSettings other = (DebugSettings) obj;
		if (this.drawTileCoordinates != other.drawTileCoordinates) {
			return false;
		}
		if (this.drawTileFrames != other.drawTileFrames) {
			return false;
		}
		if (this.highlightWaterTiles != other.highlightWaterTiles) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		return this.hashCodeValue;
	}

	/**
	 * @return the hash code of this object.
	 */
	private int calculateHashCode() {
		int result = 1;
		result = 31 * result + (this.drawTileCoordinates ? 1231 : 1237);
		result = 31 * result + (this.drawTileFrames ? 1231 : 1237);
		result = 31 * result + (this.highlightWaterTiles ? 1231 : 1237);
		return result;
	}
}
