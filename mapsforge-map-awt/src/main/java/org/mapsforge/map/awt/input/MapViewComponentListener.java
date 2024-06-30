/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
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
package org.mapsforge.map.awt.input;

import org.mapsforge.core.model.Rotation;
import org.mapsforge.map.awt.view.MapView;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class MapViewComponentListener extends ComponentAdapter {
    private final MapView mapView;

    public MapViewComponentListener(MapView mapView) {
        this.mapView = mapView;
    }

    @Override
    public void componentResized(ComponentEvent componentEvent) {
        if (this.mapView.getWidth() < 0 || this.mapView.getHeight() < 0) {
            return;
        }
        this.mapView.getModel().mapViewDimension.setDimension(new org.mapsforge.core.model.Dimension(this.mapView.getWidth(), this.mapView.getHeight()));

        if (!Rotation.noRotation(this.mapView.getMapRotation())) {
            this.mapView.rotate(new Rotation(this.mapView.getMapRotation().degrees, this.mapView.getWidth() * 0.5f, this.mapView.getHeight() * 0.5f));
            this.mapView.getLayerManager().redrawLayers();
        }
    }
}
