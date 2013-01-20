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
package org.mapsforge.map.controller.layer.map;

import org.mapsforge.core.model.Tile;

import android.graphics.Bitmap;

public class TwoLevelTileCache implements TileCache {
	private final TileCache tileCache1;
	private final TileCache tileCache2;

	public TwoLevelTileCache(TileCache tileCache1, TileCache tileCache2) {
		this.tileCache1 = tileCache1;
		this.tileCache2 = tileCache2;
	}

	@Override
	public synchronized boolean containsKey(Tile tile) {
		return this.tileCache1.containsKey(tile) || this.tileCache2.containsKey(tile);
	}

	@Override
	public synchronized void destroy() {
		this.tileCache1.destroy();
		this.tileCache2.destroy();
	}

	@Override
	public synchronized Bitmap get(Tile tile, Bitmap bitmap) {
		Bitmap returnBitmap = this.tileCache1.get(tile, bitmap);
		if (returnBitmap != null) {
			return returnBitmap;
		}

		returnBitmap = this.tileCache2.get(tile, bitmap);
		if (returnBitmap != null) {
			this.tileCache1.put(tile, returnBitmap.copy(returnBitmap.getConfig(), false));
			return returnBitmap;
		}

		return null;
	}

	@Override
	public synchronized int getCapacity() {
		return Math.max(this.tileCache1.getCapacity(), this.tileCache2.getCapacity());
	}

	@Override
	public synchronized void put(Tile tile, Bitmap bitmap) {
		this.tileCache2.put(tile, bitmap);
	}
}
