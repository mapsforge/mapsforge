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
import org.mapsforge.map.layer.overlay.Grid;

import java.util.HashMap;
import java.util.Map;

/**
 * Map viewer with a Grid layer added.
 */
public class GridMapViewer extends RenderTheme4 {

    @Override
    protected void createLayers() {
        super.createLayers();

        Map<Byte, Double> spacingConfig = new HashMap<Byte, Double>(23);

        spacingConfig.put((byte) 0, 90d); // 90°
        spacingConfig.put((byte) 1, 45d); // 45°
        spacingConfig.put((byte) 2, 30d); // 30°
        spacingConfig.put((byte) 3, 15d); // 15°
        spacingConfig.put((byte) 4, 10d); // 10°
        spacingConfig.put((byte) 5, 5d); // 5°
        spacingConfig.put((byte) 6, 2d); // 2°
        spacingConfig.put((byte) 7, 1d); // 1°
        spacingConfig.put((byte) 8, 30d / 60); // 30′
        spacingConfig.put((byte) 9, 20d / 60); // 20′
        spacingConfig.put((byte) 10, 10d / 60); // 10′
        spacingConfig.put((byte) 11, 5d / 60); // 5′
        spacingConfig.put((byte) 12, 2d / 60); // 2′
        spacingConfig.put((byte) 13, 1d / 60); // 1′
        spacingConfig.put((byte) 14, 30d / 60 / 60); // 30″
        spacingConfig.put((byte) 15, 20d / 60 / 60); // 20″
        spacingConfig.put((byte) 16, 10d / 60 / 60); // 10″
        spacingConfig.put((byte) 17, 5d / 60 / 60); // 5″
        spacingConfig.put((byte) 18, 2d / 60 / 60); // 2″
        spacingConfig.put((byte) 19, 1d / 60 / 60); // 1″
        spacingConfig.put((byte) 20, 1d / 60 / 60); // 1″
        spacingConfig.put((byte) 21, 1d / 60 / 60); // 1″
        spacingConfig.put((byte) 22, 1d / 60 / 60); // 1″

        mapView.getLayerManager().getLayers().add(new Grid(
                AndroidGraphicFactory.INSTANCE, this.mapView.getModel().displayModel, spacingConfig));
    }
}
