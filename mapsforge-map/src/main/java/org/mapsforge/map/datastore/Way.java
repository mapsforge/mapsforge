/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014-2015 Ludwig M Brinckmann
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
package org.mapsforge.map.datastore;

import java.util.Arrays;
import java.util.List;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Tag;

/**
 * An immutable container for all data associated with a single way or area (closed way).
 */
public class Way {
	/**
	 * The position of the area label (may be null).
	 */
	public final LatLong labelPosition;

	/**
	 * The geographical coordinates of the way nodes.
	 */
	public final LatLong[][] latLongs;

	/**
	 * The layer of this way + 5 (to avoid negative values).
	 */
	public final byte layer;

	/**
	 * The tags of this way.
	 */
	public final List<Tag> tags;

	public Way(byte layer, List<Tag> tags, LatLong[][] latLongs, LatLong labelPosition) {
		this.layer = layer;
		this.tags = tags;
		this.latLongs = latLongs;
		this.labelPosition = labelPosition;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (!(obj instanceof Way)) {
			return false;
		}
		Way other = (Way) obj;
		if (this.layer != other.layer) {
			return false;
		} else if (!this.tags.equals(other.tags)) {
			return false;
		} else if (this.labelPosition == null && other.labelPosition != null) {
			return false;
		} else if (this.labelPosition!= null && !this.labelPosition.equals(other.labelPosition)) {
			return false;
		} else if (this.latLongs.length != other.latLongs.length) {
			return false;
		} else {
			for (int i = 0; i < this.latLongs.length; i++) {
				if (this.latLongs[i].length != other.latLongs[i].length) {
					return false;
				} else {
					for (int j = 0; j < this.latLongs[i].length; j++) {
						if (!latLongs[i][j].equals(other.latLongs[i][j])) {
							return false;
						}
					}
				}
			}
		}
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + layer;
		result = prime * result + tags.hashCode();
		result = prime * result + Arrays.deepHashCode(latLongs);
		if (labelPosition != null) {
			result = prime * result + labelPosition.hashCode();
		}
		return result;
	}

}
