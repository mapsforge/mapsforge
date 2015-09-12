/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2015 devemux86
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

import java.util.ArrayList;

import org.mapsforge.map.datastore.MapReadResult;
import org.mapsforge.map.datastore.Way;

/**
 * An immutable container for the data returned by the {@link MapFile}.
 */
public class MapFileReadResult extends MapReadResult {

	public MapFileReadResult() {
		this.pointOfInterests = new ArrayList<>();
		this.ways = new ArrayList<Way>();
	}

	public void add(PoiWayBundle poiWayBundle) {
		this.pointOfInterests.addAll(poiWayBundle.pois);
		this.ways.addAll(poiWayBundle.ways);
	}
}
