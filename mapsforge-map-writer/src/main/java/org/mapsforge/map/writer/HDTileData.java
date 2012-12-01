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
package org.mapsforge.map.writer;

import gnu.trove.list.array.TLongArrayList;

import java.util.List;
import java.util.Map;

import org.mapsforge.map.writer.model.TDNode;
import org.mapsforge.map.writer.model.TDWay;
import org.mapsforge.map.writer.model.TileData;

/**
 * @author bross
 */
public class HDTileData extends TileData {
	private final TLongArrayList pois;
	private final TLongArrayList ways;

	HDTileData() {
		super();
		this.pois = new TLongArrayList();
		this.ways = new TLongArrayList();
	}

	final TLongArrayList getPois() {
		return this.pois;
	}

	final TLongArrayList getWays() {
		return this.ways;
	}

	@Override
	public final void addPOI(TDNode poi) {
		this.pois.add(poi.getId());
	}

	@Override
	public final void addWay(TDWay way) {
		this.ways.add(way.getId());
	}

	@Override
	public Map<Byte, List<TDNode>> poisByZoomlevel(byte minValidZoomlevel, byte maxValidZoomlevel) {
		throw new UnsupportedOperationException(HDTileData.class.getName() + "does not support this operation");
	}

	@Override
	public Map<Byte, List<TDWay>> waysByZoomlevel(byte minValidZoomlevel, byte maxValidZoomlevel) {
		throw new UnsupportedOperationException(HDTileData.class.getName() + "does not support this operation");
	}
}
