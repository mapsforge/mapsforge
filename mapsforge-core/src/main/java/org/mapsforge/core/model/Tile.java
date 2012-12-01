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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

/**
 * A tile represents a rectangular part of the world map. All tiles can be identified by their X and Y number together
 * with their zoom level. The actual area that a tile covers on a map depends on the underlying map projection.
 */
public class Tile implements Serializable {
	/**
	 * Bytes per pixel required in a map tile bitmap.
	 */
	public static final byte TILE_BYTES_PER_PIXEL = 2;

	/**
	 * Width and height of a map tile in pixel.
	 */
	public static final int TILE_SIZE = 256;

	/**
	 * Size of a single uncompressed map tile bitmap in bytes.
	 */
	public static final int TILE_SIZE_IN_BYTES = TILE_SIZE * TILE_SIZE * TILE_BYTES_PER_PIXEL;

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
	 * The Zoom level of this tile.
	 */
	public final byte zoomLevel;

	private transient int hashCodeValue;
	private transient long pixelX;
	private transient long pixelY;

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
		calculateTransientValues();
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
	 * @return the pixel X coordinate of the upper left corner of this tile.
	 */
	public long getPixelX() {
		return this.pixelX;
	}

	/**
	 * @return the pixel Y coordinate of the upper left corner of this tile.
	 */
	public long getPixelY() {
		return this.pixelY;
	}

	@Override
	public int hashCode() {
		return this.hashCodeValue;
	}

	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("Tile [tileX=");
		stringBuilder.append(this.tileX);
		stringBuilder.append(", tileY=");
		stringBuilder.append(this.tileY);
		stringBuilder.append(", zoomLevel=");
		stringBuilder.append(this.zoomLevel);
		stringBuilder.append("]");
		return stringBuilder.toString();
	}

	/**
	 * @return the hash code of this object.
	 */
	private int calculateHashCode() {
		int result = 7;
		result = 31 * result + (int) (this.tileX ^ (this.tileX >>> 32));
		result = 31 * result + (int) (this.tileY ^ (this.tileY >>> 32));
		result = 31 * result + this.zoomLevel;
		return result;
	}

	/**
	 * Calculates the values of some transient variables.
	 */
	private void calculateTransientValues() {
		this.pixelX = this.tileX * TILE_SIZE;
		this.pixelY = this.tileY * TILE_SIZE;
		this.hashCodeValue = calculateHashCode();
	}

	private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
		objectInputStream.defaultReadObject();
		calculateTransientValues();
	}
}
