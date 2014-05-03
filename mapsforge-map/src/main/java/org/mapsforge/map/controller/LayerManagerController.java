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

import org.mapsforge.map.layer.LayerManager;
import org.mapsforge.map.model.Model;
import org.mapsforge.map.model.common.Observer;

public final class LayerManagerController implements Observer {
	public static LayerManagerController create(LayerManager layerManager, Model model) {
		LayerManagerController layerManagerController = new LayerManagerController(layerManager);

		model.mapViewDimension.addObserver(layerManagerController);
		model.mapViewPosition.addObserver(layerManagerController);

		return layerManagerController;
	}

	private final LayerManager layerManager;

	private LayerManagerController(LayerManager layerManager) {
		this.layerManager = layerManager;
	}

	@Override
	public void onChange() {
		this.layerManager.redrawLayers();
	}
}
