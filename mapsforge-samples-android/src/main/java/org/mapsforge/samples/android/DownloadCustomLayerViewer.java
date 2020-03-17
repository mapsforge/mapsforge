/*
 * Copyright 2014-2020 devemux86
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

import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.download.TileDownloadLayer;
import org.mapsforge.map.layer.download.tilesource.OnlineTileSource;

/**
 * Shows how to use a custom tile download layer.
 */
public class DownloadCustomLayerViewer extends DownloadLayerViewer {
    @Override
    protected void createLayers() {
        OnlineTileSource onlineTileSource = new OnlineTileSource(new String[]{
                "a.tile.openstreetmap.fr", "b.tile.openstreetmap.fr", "c.tile.openstreetmap.fr"},
                443);
        onlineTileSource.setName("Humanitarian").setAlpha(false)
                .setBaseUrl("/hot/")
                .setParallelRequestsLimit(8).setProtocol("https").setTileSize(256)
                .setZoomLevelMax((byte) 18).setZoomLevelMin((byte) 0);
        onlineTileSource.setUserAgent("mapsforge-samples-android");
        this.downloadLayer = new TileDownloadLayer(this.tileCaches.get(0),
                this.mapView.getModel().mapViewPosition, onlineTileSource,
                AndroidGraphicFactory.INSTANCE);
        mapView.getLayerManager().getLayers().add(this.downloadLayer);

        mapView.setZoomLevelMin(onlineTileSource.getZoomLevelMin());
        mapView.setZoomLevelMax(onlineTileSource.getZoomLevelMax());
    }

    @Override
    protected void createMapViews() {
        super.createMapViews();
        // we need to set a fixed size tile as the raster tiles come at a fixed size and not being blurry
        //this.mapView.getModel().displayModel.setFixedTileSize(256);
    }
}
