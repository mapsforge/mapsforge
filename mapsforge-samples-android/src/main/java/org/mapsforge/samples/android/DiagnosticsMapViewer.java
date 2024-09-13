/*
 * Copyright 2013-2014 Ludwig M Brinckmann
 * Copyright 2015-2016 devemux86
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
package org.mapsforge.samples.android;

import android.view.View;
import org.mapsforge.core.util.Parameters;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.debug.TileCoordinatesLayer;
import org.mapsforge.map.layer.debug.TileGridLayer;

/**
 * Viewer with tile grid and coordinates visible and frame counter displayed.
 */
public class DiagnosticsMapViewer extends DownloadLayerViewer {

    private static final double ZOOM_OFFSET = 0.5;

    @Override
    protected void createControls() {
        super.createControls();

        if (Parameters.FRACTIONAL_ZOOM) {
            mapView.getMapZoomControls().getButtonZoomIn().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mapView.onZoomEvent();
                    mapView.getModel().mapViewPosition.setZoom(mapView.getModel().mapViewPosition.getZoom() + ZOOM_OFFSET);
                }
            });
            mapView.getMapZoomControls().getButtonZoomOut().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mapView.onZoomEvent();
                    mapView.getModel().mapViewPosition.setZoom(Math.max(0, mapView.getModel().mapViewPosition.getZoom() - ZOOM_OFFSET));
                }
            });
        }
    }

    @Override
    protected void createLayers() {
        super.createLayers();

        // Add a grid layer and a layer showing tile coordinates
        mapView.getLayerManager().getLayers().add(new TileGridLayer(AndroidGraphicFactory.INSTANCE, this.mapView.getModel().displayModel));
        TileCoordinatesLayer tileCoordinatesLayer = new TileCoordinatesLayer(AndroidGraphicFactory.INSTANCE, this.mapView.getModel().displayModel);
        tileCoordinatesLayer.setDrawSimple(true);
        mapView.getLayerManager().getLayers().add(tileCoordinatesLayer);

        // Enable frame counter
        mapView.getFpsCounter().setVisible(true);

        // Enable rotation gesture
        mapView.getTouchGestureHandler().setRotationEnabled(true);
    }

    @Override
    protected float getScreenRatio() {
        // just to get the cache bigger right now.
        return 2f;
    }

    @Override
    protected boolean isZoomControlsAutoHide() {
        return false;
    }
}
