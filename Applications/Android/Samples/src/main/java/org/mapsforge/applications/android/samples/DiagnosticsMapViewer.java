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

import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.debug.TileCoordinatesLayer;
import org.mapsforge.map.layer.debug.TileGridLayer;

/**
 * Viewer with tile grid and coordinates visible and frame counter displayed.
 */
public class DiagnosticsMapViewer extends RenderTheme4 {

    @Override
    protected void createLayers() {
        super.createLayers();

        // add a grid layer and a layer showing tile coordinates
        mapView.getLayerManager().getLayers()
                .add(new TileGridLayer(AndroidGraphicFactory.INSTANCE, this.mapView.getModel().displayModel));
        mapView.getLayerManager().getLayers()
                .add(new TileCoordinatesLayer(AndroidGraphicFactory.INSTANCE, this.mapView.getModel().displayModel));
        mapView.getFpsCounter().setVisible(true);
    }

    @Override
    protected boolean isZoomControlsAutoHide() {
        return false;
    }
}
