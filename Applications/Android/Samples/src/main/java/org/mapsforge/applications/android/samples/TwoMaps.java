/*
 * Copyright 2013-2014 Ludwig M Brinckmann
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

import android.os.Environment;

import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.reader.MapFile;

import java.io.File;

/**
 * Two rendered maps overlaid in the same map view, e.g. for maps can be for different areas.
 * The lower map is rendered as non-transparent, the higher transparent. There is now a better way of doing
 * this, with the new MultiMapDataStore class.
 */
public class TwoMaps extends RenderTheme4 {
    @Override
    protected void createLayers() {
        TileRendererLayer tileRendererLayer = AndroidUtil.createTileRendererLayer(this.tileCaches.get(0),
                mapView.getModel().mapViewPosition, getMapFile(), getRenderTheme(), false, true, false);
        this.mapView.getLayerManager().getLayers().add(tileRendererLayer);
        TileRendererLayer tileRendererLayer2 = AndroidUtil.createTileRendererLayer(this.tileCaches.get(1),
                mapView.getModel().mapViewPosition, getMapFile2(), getRenderTheme(), true, true, false);
        this.mapView.getLayerManager().getLayers().add(tileRendererLayer2);

        // needed only for samples to hook into Settings.
        setMaxTextWidthFactor();
    }

    protected TileCache createTileCache2() {
        int tileSize = this.mapView.getModel().displayModel.getTileSize();
        return AndroidUtil.createTileCache(this, getPersistableId2(), tileSize,
                getScreenRatio(), this.mapView.getModel().frameBufferModel.getOverdrawFactor());
    }

    @Override
    protected void createTileCaches() {
        super.createTileCaches();
        this.tileCaches.add(createTileCache2());
    }

    /**
     * @return the map file for the second view
     */
    protected MapFile getMapFile2() {
        return new MapFile(new File(Environment.getExternalStorageDirectory(), this.getMapFileName2()));
    }

    /**
     * @return the map file name for the second view
     */
    protected String getMapFileName2() {
        return "second.map";
    }

    protected String getPersistableId2() {
        return this.getPersistableId() + "-2";
    }
}
