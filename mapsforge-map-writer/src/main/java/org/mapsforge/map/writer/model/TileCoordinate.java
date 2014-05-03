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
package org.mapsforge.map.writer.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a coordinate in the tile space.
 */
public class TileCoordinate {
	private final int x;
	private final int y;
	private final byte zoomlevel;

	/**
	 * Constructor.
	 * 
	 * @param x
	 *            the x value of the tile on the given zoom level
	 * @param y
	 *            the y value of the tile on the given zoom level
	 * @param zoomlevel
	 *            the zoom level
	 */
	public TileCoordinate(int x, int y, byte zoomlevel) {
		super();
		this.x = x;
		this.y = y;
		this.zoomlevel = zoomlevel;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		TileCoordinate other = (TileCoordinate) obj;
		if (this.x != other.x) {
			return false;
		}
		if (this.y != other.y) {
			return false;
		}
		if (this.zoomlevel != other.zoomlevel) {
			return false;
		}
		return true;
	}

	/**
	 * @return the x
	 */
	public int getX() {
		return this.x;
	}

	/**
	 * @return the y
	 */
	public int getY() {
		return this.y;
	}

	/**
	 * @return the zoomlevel
	 */
	public byte getZoomlevel() {
		return this.zoomlevel;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + this.x;
		result = prime * result + this.y;
		result = prime * result + this.zoomlevel;
		return result;
	}

	@Override
	public String toString() {
		return "TileCoordinate [x=" + this.x + ", y=" + this.y + ", zoomlevel=" + this.zoomlevel + "]";
	}

	/**
	 * Computes which tile on a lower zoom level covers this given tile or which tiles on a higher zoom level together
	 * cover this tile.
	 * 
	 * @param zoomlevelNew
	 *            the zoom level
	 * @return a list of tiles (represented by tile coordinates) which cover this tile
	 */
	public List<TileCoordinate> translateToZoomLevel(byte zoomlevelNew) {
		List<TileCoordinate> tiles = null;
		int zoomlevelDistance = zoomlevelNew - this.zoomlevel;

		int factor = (int) Math.pow(2, Math.abs(zoomlevelDistance));
		if (zoomlevelDistance > 0) {
			tiles = new ArrayList<>((int) Math.pow(4, Math.abs(zoomlevelDistance)));
			int tileUpperLeftX = this.x * factor;
			int tileUpperLeftY = this.y * factor;
			for (int i = 0; i < factor; i++) {
				for (int j = 0; j < factor; j++) {
					tiles.add(new TileCoordinate(tileUpperLeftX + j, tileUpperLeftY + i, zoomlevelNew));
				}
			}
		} else {
			tiles = new ArrayList<>(1);
			tiles.add(new TileCoordinate(this.x / factor, this.y / factor, zoomlevelNew));
		}
		return tiles;
	}
}
