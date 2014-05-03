/*
 * Copyright © 2013-2014 Ludwig M Brinckmann
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

import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.debug.TileCoordinatesLayer;
import org.mapsforge.map.layer.debug.TileGridLayer;

/**
 * Viewer with tile grid and coordinates visible and frame counter displayed.
 */
public class DiagnosticsMapViewer extends BasicMapViewerXml {

	@Override
	protected void createLayers() {
		super.createLayers();
		this.layerManagers
				.get(0)
				.getLayers()
				.add(new TileGridLayer(AndroidGraphicFactory.INSTANCE,
						this.mapViews.get(0).getModel().displayModel));
		this.layerManagers
				.get(0)
				.getLayers()
				.add(new TileCoordinatesLayer(AndroidGraphicFactory.INSTANCE,
						this.mapViews.get(0).getModel().displayModel));
	}

	@Override
	protected void createMapViews() {
		super.createMapViews();
		for (MapView mapView : mapViews) {
			mapView.getFpsCounter().setVisible(true);
		}
	}
}
