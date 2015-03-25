/*
 * Copyright 2013-2014 Ludwig M Brinckmann
 * Copyright 2015 devemux86
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

import java.util.HashMap;
import java.util.Map;

import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.overlay.Grid;

/**
 * Map viewer with a Grid layer added.
 */
public class GridMapViewer extends RenderTheme4 {

	@Override
	protected void createLayers() {
		super.createLayers();

		Map<Byte, Double> spacingConfig = new HashMap<Byte, Double>(23);

		spacingConfig.put((byte)(0), 10d);
		spacingConfig.put((byte)(1), 10d);
		spacingConfig.put((byte)(2), 10d);
		spacingConfig.put((byte)(3), 10d);
		spacingConfig.put((byte)(4), 5d);
		spacingConfig.put((byte)(5), 2.5d);
		spacingConfig.put((byte)(6), 2.5d);
		spacingConfig.put((byte)(7), 1d);
		spacingConfig.put((byte)(8), .5d);
		spacingConfig.put((byte)(9), .5d);
		spacingConfig.put((byte)(10), .2d);
		spacingConfig.put((byte)(11), .1d);
		spacingConfig.put((byte)(12), .1d);
		spacingConfig.put((byte)(13), .05d);
		spacingConfig.put((byte)(14), .02d);
		spacingConfig.put((byte)(15), .01d);
		spacingConfig.put((byte)(16), .01d);
		spacingConfig.put((byte)(17), .005d);
		spacingConfig.put((byte)(18), .002d);
		spacingConfig.put((byte)(19), .001d);
		spacingConfig.put((byte)(20), .0005d);
		spacingConfig.put((byte)(21), .0002d);
		spacingConfig.put((byte)(22), .0001d);

		mapView.getLayerManager().getLayers().add(new Grid(
				AndroidGraphicFactory.INSTANCE, this.mapView.getModel().displayModel, spacingConfig));
	}
}
