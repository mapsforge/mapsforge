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
package org.mapsforge.map.controller;

import org.mapsforge.map.model.Model;
import org.mapsforge.map.model.common.Observer;
import org.mapsforge.map.view.MapView;

public final class MapViewController implements Observer {
	public static MapViewController create(MapView mapView, Model model) {
		MapViewController mapViewController = new MapViewController(mapView);

		model.mapViewPosition.addObserver(mapViewController);

		return mapViewController;
	}

	private final MapView mapView;

	private MapViewController(MapView mapView) {
		this.mapView = mapView;
	}

	@Override
	public void onChange() {
		this.mapView.repaint();
	}
}
