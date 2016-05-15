/*
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2014, 2015 devemux86
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

import android.os.Environment;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.layer.cache.InMemoryTileCache;
import org.mapsforge.map.layer.cache.TileStore;
import org.mapsforge.map.layer.cache.TwoLevelTileCache;
import org.mapsforge.map.layer.tilestore.TileStoreLayer;

import java.io.File;

/**
 * Shows how to use a tile store layer.
 */
public class TileStoreLayerViewer extends RenderTheme4 {
    @Override
    protected void createLayers() {
        TileStoreLayer tileStoreLayer = new TileStoreLayer(this.tileCaches.get(0),
                this.mapView.getModel().mapViewPosition, AndroidGraphicFactory.INSTANCE, false);
        mapView.getLayerManager().getLayers().add(tileStoreLayer);
    }

    @Override
    protected void createMapViews() {
        super.createMapViews();
        // we need to set a fixed size tile as the tiles in the store come at a fixed size
        this.mapView.getModel().displayModel.setFixedTileSize(256);
        mapView.getModel().mapViewPosition.setMapPosition(new MapPosition(new LatLong(52.517037, 13.38886), (byte) 12));
    }

    @Override
    protected void createTileCaches() {
        // to use a tile store you provide it as a cache (which is pre-filled and never purges any files.
        // additionally you should use a memory tile store for faster refresh.
        TileStore tileStore = new TileStore(new File(Environment.getExternalStorageDirectory(), "tilestore"), ".png", AndroidGraphicFactory.INSTANCE);
        InMemoryTileCache memoryTileCache = new InMemoryTileCache(AndroidUtil.getMinimumCacheSize(this,
                this.mapView.getModel().displayModel.getTileSize(),
                this.mapView.getModel().frameBufferModel.getOverdrawFactor(), this.getScreenRatio()));
        this.tileCaches.add(new TwoLevelTileCache(memoryTileCache, tileStore));
    }
}
