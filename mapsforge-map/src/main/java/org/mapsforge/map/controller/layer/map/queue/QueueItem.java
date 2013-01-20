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
package org.mapsforge.map.controller.layer.map.queue;

import org.mapsforge.core.model.Tile;

class QueueItem {
	double priority;
	final Tile tile;

	QueueItem(Tile tile) {
		this.tile = tile;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (!(obj instanceof QueueItem)) {
			return false;
		}
		QueueItem other = (QueueItem) obj;
		return this.tile.equals(other.tile);
	}

	@Override
	public int hashCode() {
		return this.tile.hashCode();
	}
}
