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
package org.mapsforge.map.swing.controller;

import java.awt.Dimension;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import org.mapsforge.map.model.MapViewDimension;
import org.mapsforge.map.swing.view.MapView;

public class MapViewComponentListener implements ComponentListener {
	private final MapView mapView;
	private final MapViewDimension mapViewDimension;

	public MapViewComponentListener(MapView mapView, MapViewDimension mapViewDimension) {
		this.mapView = mapView;
		this.mapViewDimension = mapViewDimension;
	}

	@Override
	public void componentHidden(ComponentEvent componentEvent) {
		// do nothing
	}

	@Override
	public void componentMoved(ComponentEvent componentEvent) {
		// do nothing
	}

	@Override
	public void componentResized(ComponentEvent componentEvent) {
		Dimension size = this.mapView.getSize();
		this.mapViewDimension.setDimension(new org.mapsforge.core.model.Dimension(size.width, size.height));
	}

	@Override
	public void componentShown(ComponentEvent componentEvent) {
		// do nothing
	}
}
