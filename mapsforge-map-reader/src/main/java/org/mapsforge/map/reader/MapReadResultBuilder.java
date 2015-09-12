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
package org.mapsforge.map.reader;

import org.mapsforge.map.datastore.PointOfInterest;
import org.mapsforge.map.datastore.Way;

import java.util.ArrayList;
import java.util.List;

public class MapReadResultBuilder {
	private boolean isWater;
	private final List<PointOfInterest> pointOfInterests;
	private final List<Way> ways;

	public MapReadResultBuilder() {
		this.pointOfInterests = new ArrayList<>();
		this.ways = new ArrayList<Way>();
	}

	public void add(PoiWayBundle poiWayBundle) {
		this.pointOfInterests.addAll(poiWayBundle.pois);
		this.ways.addAll(poiWayBundle.ways);
	}

	public List<PointOfInterest> getPointOfInterests() {
		return pointOfInterests;
	}

	public List<Way> getWays() {
		return ways;
	}

	public boolean isWater() {
		return isWater;
	}

	public void setWater(boolean isWater) {
		this.isWater = isWater;
	}

	public MapFileReadResult build() {
		return new MapFileReadResult(this);
	}
}
