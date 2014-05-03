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
package org.mapsforge.map.model.common;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Observable {
	private static final String OBSERVER_MUST_NOT_BE_NULL = "observer must not be null";
	private final List<Observer> observers = new CopyOnWriteArrayList<Observer>();

	protected Observable() {
		// do nothing
	}

	public final void addObserver(Observer observer) {
		if (observer == null) {
			throw new IllegalArgumentException(OBSERVER_MUST_NOT_BE_NULL);
		} else if (this.observers.contains(observer)) {
			throw new IllegalArgumentException("observer is already registered: " + observer);
		}
		this.observers.add(observer);
	}

	public final void removeObserver(Observer observer) {
		if (observer == null) {
			throw new IllegalArgumentException(OBSERVER_MUST_NOT_BE_NULL);
		} else if (!this.observers.contains(observer)) {
			throw new IllegalArgumentException("observer is not registered: " + observer);
		}
		this.observers.remove(observer);
	}

	protected final void notifyObservers() {
		for (Observer observer : this.observers) {
			observer.onChange();
		}
	}
}
