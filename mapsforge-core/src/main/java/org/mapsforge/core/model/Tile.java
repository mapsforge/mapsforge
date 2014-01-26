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
package org.mapsforge.core.model;

import java.io.Serializable;

/**
 * A tile represents a rectangular part of the world map. All tiles can be identified by their X and Y number together
 * with their zoom level. The actual area that a tile covers on a map depends on the underlying map projection.
 */
public class Tile implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * @return the maximum valid tile number for the given zoom level, 2<sup>zoomLevel</sup> -1.
	 */
	public static long getMaxTileNumber(byte zoomLevel) {
		if (zoomLevel < 0) {
			throw new IllegalArgumentException("zoomLevel must not be negative: " + zoomLevel);
		} else if (zoomLevel == 0) {
			return 0;
		}
		return (2 << zoomLevel - 1) - 1;
	}

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
	 * @throws IllegalArgumentException
	 *             if any of the parameters is invalid.
	 */
	public Tile(long tileX, long tileY, byte zoomLevel) {
		if (tileX < 0) {
			throw new IllegalArgumentException("tileX must not be negative: " + tileX);
		} else if (tileY < 0) {
			throw new IllegalArgumentException("tileY must not be negative: " + tileY);
		} else if (zoomLevel < 0) {
			throw new IllegalArgumentException("zoomLevel must not be negative: " + zoomLevel);
		}

		long maxTileNumber = getMaxTileNumber(zoomLevel);
		if (tileX > maxTileNumber) {
			throw new IllegalArgumentException("invalid tileX number on zoom level " + zoomLevel + ": " + tileX);
		} else if (tileY > maxTileNumber) {
			throw new IllegalArgumentException("invalid tileY number on zoom level " + zoomLevel + ": " + tileY);
		}

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

	/**
	 * @return the parent tile of this tile or null, if the zoom level of this tile is 0.
	 */
	public Tile getParent() {
		if (this.zoomLevel == 0) {
			return null;
		}

		return new Tile(this.tileX / 2, this.tileY / 2, (byte) (this.zoomLevel - 1));
	}

	public long getShiftX(Tile otherTile) {
		if (this.equals(otherTile)) {
			return 0;
		}

		return this.tileX % 2 + 2 * getParent().getShiftX(otherTile);
	}

	public long getShiftY(Tile otherTile) {
		if (this.equals(otherTile)) {
			return 0;
		}

		return this.tileY % 2 + 2 * getParent().getShiftY(otherTile);
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
