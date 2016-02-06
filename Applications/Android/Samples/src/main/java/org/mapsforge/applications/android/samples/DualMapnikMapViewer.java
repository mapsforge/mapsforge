/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
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
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.android.util.MapViewPositionObserver;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.download.TileDownloadLayer;
import org.mapsforge.map.layer.download.tilesource.OpenStreetMapMapnik;

/**
 * An activity with two {@link MapView MapViews} tied to each other.
 */
public class DualMapnikMapViewer extends DualMapViewer {
    private TileDownloadLayer downloadLayer;
    private MapViewPositionObserver observer1;
    private MapViewPositionObserver observer2;

    @Override
    protected void createLayers2() {
        this.downloadLayer = new TileDownloadLayer(this.tileCaches.get(1),
                this.mapView2.getModel().mapViewPosition, OpenStreetMapMapnik.INSTANCE,
                AndroidGraphicFactory.INSTANCE);
        this.mapView2.getLayerManager().getLayers().add(this.downloadLayer);
    }

    @Override
    protected void createMapViews() {
        super.createMapViews();
        // any position change in one view will be reflected in the other
        this.observer1 = new MapViewPositionObserver(
                this.mapView.getModel().mapViewPosition, this.mapView2.getModel().mapViewPosition);
        this.observer2 = new MapViewPositionObserver(
                this.mapView2.getModel().mapViewPosition, this.mapView.getModel().mapViewPosition);
    }

    @Override
    protected TileCache createTileCache2() {
        int tileSize = this.mapView2.getModel().displayModel
                .getTileSize();
        return AndroidUtil.createTileCache(this, getPersistableId2(), tileSize,
                getScreenRatio2(),
                this.mapView2.getModel().frameBufferModel
                        .getOverdrawFactor());
    }

    @Override
    protected void createTileCaches() {
        super.createTileCaches();
        this.tileCaches.add(createTileCache2());
    }

    @Override
    protected void onDestroy() {
        this.observer1.removeObserver();
        this.observer2.removeObserver();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        this.downloadLayer.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.downloadLayer.onResume();
    }
}
