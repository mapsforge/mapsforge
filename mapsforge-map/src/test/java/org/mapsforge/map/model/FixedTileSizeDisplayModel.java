/*
 * Copyright 2014 Ludwig M Brinckmann
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
package org.mapsforge.map.model;

/**
 * Dummy DisplayModel that always delivers a fixed tile size for testing.
 */

public class FixedTileSizeDisplayModel extends DisplayModel {

	private int tileSize;

	public FixedTileSizeDisplayModel(int tileSize) {
		this.tileSize = tileSize;
	}

	@Override
	public int getTileSize() {
		return tileSize;
	}
}
