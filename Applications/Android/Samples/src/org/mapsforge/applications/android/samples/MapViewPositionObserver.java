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
package org.mapsforge.applications.android.samples;

import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.model.common.Observer;

class MapViewPositionObserver implements Observer {
	private final MapViewPosition observable;
	private final MapViewPosition observer;

	MapViewPositionObserver(MapViewPosition observable, MapViewPosition observer) {
		this.observable = observable;
		this.observer = observer;
		observable.addObserver(this);
	}

	@Override
	public void onChange() {
		setCenter();
		setZoom();
	}

	protected void setCenter() {
		// need to check to avoid circular notifications
		if (!this.observable.getCenter().equals(this.observer.getCenter())) {
			this.observer.setCenter(this.observable.getCenter());
		}
	}

	protected void setZoom() {
		if (this.observable.getZoomLevel() != this.observer.getZoomLevel()) {
			this.observer.setZoomLevel(this.observable.getZoomLevel());
		}
	}

	void removeObserver() {
		this.observable.removeObserver(this);
	}
}
