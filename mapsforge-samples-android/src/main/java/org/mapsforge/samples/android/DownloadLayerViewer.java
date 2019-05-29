/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2013-2014 Ludwig M Brinckmann
 * Copyright 2015-2019 devemux86
 * Copyright 2015 Andreas Schildbach
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

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.download.TileDownloadLayer;
import org.mapsforge.map.layer.download.tilesource.OpenStreetMapMapnik;
import org.mapsforge.map.rendertheme.XmlRenderTheme;

/**
 * Shows how to use a tile download layer.
 * The important thing here is that the downloadLayer needs
 * to be paused and resumed to fit into the Android life cycle.
 */
public class DownloadLayerViewer extends SamplesBaseActivity {
    protected TileDownloadLayer downloadLayer;

    @Override
    protected void createLayers() {
        OpenStreetMapMapnik tileSource = OpenStreetMapMapnik.INSTANCE;
        tileSource.setUserAgent("mapsforge-samples-android");
        this.downloadLayer = new TileDownloadLayer(this.tileCaches.get(0),
                this.mapView.getModel().mapViewPosition, tileSource,
                AndroidGraphicFactory.INSTANCE);
        mapView.getLayerManager().getLayers().add(this.downloadLayer);

        mapView.setZoomLevelMin(tileSource.getZoomLevelMin());
        mapView.setZoomLevelMax(tileSource.getZoomLevelMax());
    }

    @Override
    protected void createMapViews() {
        super.createMapViews();
        // we need to set a fixed size tile as the raster tiles come at a fixed size and not being blurry
        //this.mapView.getModel().displayModel.setFixedTileSize(256);
    }

    @Override
    protected MapPosition getInitialPosition() {
        return new MapPosition(new LatLong(0, 0), (byte) 2);
    }

    @Override
    protected XmlRenderTheme getRenderTheme() {
        // no render theme needed here
        return null;
    }

    @Override
    protected byte getZoomLevelMax() {
        return mapView.getModel().mapViewPosition.getZoomLevelMax();
    }

    @Override
    protected byte getZoomLevelMin() {
        return mapView.getModel().mapViewPosition.getZoomLevelMin();
    }

    @Override
    public void onPause() {
        this.downloadLayer.onPause();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        this.downloadLayer.onResume();
    }
}
