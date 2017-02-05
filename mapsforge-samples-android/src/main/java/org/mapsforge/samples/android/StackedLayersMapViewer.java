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
package org.mapsforge.samples.android;

import android.util.Log;
import org.mapsforge.map.android.rendertheme.AssetsRenderTheme;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.rendertheme.XmlRenderTheme;

import java.io.IOException;

/**
 * An activity with two tile renderer layers stacked on top of each other using
 * a partially transparent render theme to show the lower layer. This will show
 * as buildings on top of labels and other stuff, so the display is wrong, but
 * that is intentional.
 */

public class StackedLayersMapViewer extends DefaultTheme {

    @Override
    protected void createLayers() {
        super.createLayers();
        try {

            XmlRenderTheme secondRenderTheme = new AssetsRenderTheme(this, "",
                    "mapsforge/onlybuildings.xml", null);

            this.mapView.getLayerManager()
                    .getLayers()
                    .add(AndroidUtil.createTileRendererLayer(this.tileCaches.get(1),
                            this.mapView.getModel().mapViewPosition, getMapFile(),
                            secondRenderTheme, true, true, false));

        } catch (IOException e) {
            Log.e(SamplesApplication.TAG, "Rendertheme not found");
        }

    }

    @Override
    protected void createTileCaches() {
        super.createTileCaches();
        TileCache tileCache2 = AndroidUtil.createTileCache(this,
                getPersistableId2(),
                this.mapView.getModel().displayModel.getTileSize(),
                this.getScreenRatio(),
                this.mapView.getModel().frameBufferModel
                        .getOverdrawFactor());
        this.tileCaches.add(tileCache2);
    }


    protected String getPersistableId2() {
        return this.getPersistableId() + "-2";
    }

}
