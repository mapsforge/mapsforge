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

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Tag;

/**
 * An immutable container for all data associated with a single point of interest node (POI).
 */
public class PointOfInterest {
	/**
	 * The layer of this POI + 5 (to avoid negative values).
	 */
	public final byte layer;

	/**
	 * The position of this POI.
	 */
	public final LatLong position;

	/**
	 * The tags of this POI.
	 */
	public final List<Tag> tags;

	PointOfInterest(byte layer, List<Tag> tags, LatLong position) {
		this.layer = layer;
		this.tags = tags;
		this.position = position;
	}
}
