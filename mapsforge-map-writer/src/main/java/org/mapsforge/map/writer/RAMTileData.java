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
package org.mapsforge.map.writer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mapsforge.map.writer.model.TDNode;
import org.mapsforge.map.writer.model.TDWay;
import org.mapsforge.map.writer.model.TileData;

public class RAMTileData extends TileData {
	private final Set<TDNode> pois;
	private final Set<TDWay> ways;

	RAMTileData() {
		super();
		this.pois = new HashSet<>();
		this.ways = new HashSet<>();
	}

	@Override
	public final void addPOI(TDNode poi) {
		this.pois.add(poi);
	}

	@Override
	public final void addWay(TDWay way) {
		this.ways.add(way);
	}

	@Override
	public final Map<Byte, List<TDNode>> poisByZoomlevel(byte minValidZoomlevel, byte maxValidZoomlevel) {
		HashMap<Byte, List<TDNode>> poisByZoomlevel = new HashMap<>();
		for (TDNode poi : this.pois) {
			byte zoomlevel = poi.getZoomAppear();
			if (zoomlevel > maxValidZoomlevel) {
				continue;
			}
			if (zoomlevel < minValidZoomlevel) {
				zoomlevel = minValidZoomlevel;
			}
			List<TDNode> group = poisByZoomlevel.get(Byte.valueOf(zoomlevel));
			if (group == null) {
				group = new ArrayList<>();
			}
			group.add(poi);
			poisByZoomlevel.put(Byte.valueOf(zoomlevel), group);
		}

		return poisByZoomlevel;
	}

	@Override
	public final Map<Byte, List<TDWay>> waysByZoomlevel(byte minValidZoomlevel, byte maxValidZoomlevel) {
		HashMap<Byte, List<TDWay>> waysByZoomlevel = new HashMap<>();
		for (TDWay way : this.ways) {
			byte zoomlevel = way.getMinimumZoomLevel();
			if (zoomlevel > maxValidZoomlevel) {
				continue;
			}
			if (zoomlevel < minValidZoomlevel) {
				zoomlevel = minValidZoomlevel;
			}
			List<TDWay> group = waysByZoomlevel.get(Byte.valueOf(zoomlevel));
			if (group == null) {
				group = new ArrayList<>();
			}
			group.add(way);
			waysByZoomlevel.put(Byte.valueOf(zoomlevel), group);
		}

		return waysByZoomlevel;
	}
}
