/*
 * Copyright 2014 Ludwig M Brinckmann
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

import android.util.Log;

import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.debug.TileGridLayer;

/**
 * Illustrates the ability to clamp tile sizes to multiples of a fixed value.
 */

public class DualMapViewerWithClampedTileSizes extends DualMapViewerWithDifferentDisplayModels {

    @Override
    protected void createMapViews() {
        super.createMapViews();
        mapView.getModel().displayModel.setTileSizeMultiple(200);
        mapView2.getModel().displayModel.setTileSizeMultiple(100);

        Log.d(SamplesApplication.TAG, "TILESIZE 1: " + Integer.toString(mapView.getModel().displayModel.getTileSize()));
        Log.d(SamplesApplication.TAG, "TILESIZE 2: " + Integer.toString(mapView2.getModel().displayModel.getTileSize()));
    }

    @Override
    protected void createLayers() {
        super.createLayers();
        mapView.getLayerManager().getLayers().add(new TileGridLayer(AndroidGraphicFactory.INSTANCE, this.mapView.getModel().displayModel));
        mapView2.getLayerManager().getLayers().add(new TileGridLayer(AndroidGraphicFactory.INSTANCE, this.mapView2.getModel().displayModel));
    }
}
