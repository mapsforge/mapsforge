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
package org.mapsforge.map.layer.queue;

import java.io.Serializable;
import java.util.Comparator;

final class QueueItemComparator implements Comparator<QueueItem<?>>, Serializable {
	static final QueueItemComparator INSTANCE = new QueueItemComparator();
	private static final long serialVersionUID = 1L;

	private QueueItemComparator() {
		// do nothing
	}

	@Override
	public int compare(QueueItem<?> queueItem1, QueueItem<?> queueItem2) {
		if (queueItem1.getPriority() < queueItem2.getPriority()) {
			return -1;
		} else if (queueItem1.getPriority() > queueItem2.getPriority()) {
			return 1;
		}
		return 0;
	}
}
