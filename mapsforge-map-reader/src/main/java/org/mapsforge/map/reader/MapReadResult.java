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

import java.util.List;

/**
 * An immutable container for the data returned by the {@link MapDatabase}.
 */
public class MapReadResult {
	/**
	 * True if the read area is completely covered by water, false otherwise.
	 */
	public final boolean isWater;

	/**
	 * The read POIs.
	 */
	public final List<PointOfInterest> pointOfInterests;

	/**
	 * The read ways.
	 */
	public final List<Way> ways;

	MapReadResult(MapReadResultBuilder mapReadResultBuilder) {
		this.pointOfInterests = mapReadResultBuilder.pointOfInterests;
		this.ways = mapReadResultBuilder.ways;
		this.isWater = mapReadResultBuilder.isWater;
	}
}
