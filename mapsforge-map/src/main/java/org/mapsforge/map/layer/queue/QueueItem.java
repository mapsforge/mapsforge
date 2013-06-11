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

class QueueItem<T extends Job> {
	final T object;
	private double priority;

	QueueItem(T object) {
		this.object = object;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (!(obj instanceof QueueItem)) {
			return false;
		}
		QueueItem<?> other = (QueueItem<?>) obj;
		return this.object.equals(other.object);
	}

	@Override
	public int hashCode() {
		return this.object.hashCode();
	}

	/**
	 * @return the current priority of this job, will always be a positive number including zero.
	 */
	double getPriority() {
		return this.priority;
	}

	/**
	 * @throws IllegalArgumentException
	 *             if the given priority is negative or {@link Double#NaN}.
	 */
	void setPriority(double priority) {
		if (priority < 0 || Double.isNaN(priority)) {
			throw new IllegalArgumentException("invalid priority: " + priority);
		}
		this.priority = priority;
	}
}
