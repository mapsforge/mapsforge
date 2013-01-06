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
package org.mapsforge.core.model;

import java.io.Serializable;

/**
 * A tile represents a rectangular part of the world map. All tiles can be identified by their X and Y number together
 * with their zoom level. The actual area that a tile covers on a map depends on the underlying map projection.
 */
public class Tile implements Serializable {
	/**
	 * Width and height of a map tile in pixel.
	 */
	public static final int TILE_SIZE = 256;

	private static final long serialVersionUID = 1L;

	/**
	 * The X number of this tile.
	 */
	public final long tileX;

	/**
	 * The Y number of this tile.
	 */
	public final long tileY;

	/**
	 * The zoom level of this tile.
	 */
	public final byte zoomLevel;

	/**
	 * @param tileX
	 *            the X number of the tile.
	 * @param tileY
	 *            the Y number of the tile.
	 * @param zoomLevel
	 *            the zoom level of the tile.
	 */
	public Tile(long tileX, long tileY, byte zoomLevel) {
		this.tileX = tileX;
		this.tileY = tileY;
		this.zoomLevel = zoomLevel;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (!(obj instanceof Tile)) {
			return false;
		}
		Tile other = (Tile) obj;
		if (this.tileX != other.tileX) {
			return false;
		} else if (this.tileY != other.tileY) {
			return false;
		} else if (this.zoomLevel != other.zoomLevel) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		int result = 7;
		result = 31 * result + (int) (this.tileX ^ (this.tileX >>> 32));
		result = 31 * result + (int) (this.tileY ^ (this.tileY >>> 32));
		result = 31 * result + this.zoomLevel;
		return result;
	}

	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("tileX=");
		stringBuilder.append(this.tileX);
		stringBuilder.append(", tileY=");
		stringBuilder.append(this.tileY);
		stringBuilder.append(", zoomLevel=");
		stringBuilder.append(this.zoomLevel);
		return stringBuilder.toString();
	}
}
