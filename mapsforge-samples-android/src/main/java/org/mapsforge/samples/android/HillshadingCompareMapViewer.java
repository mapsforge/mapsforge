/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2013 - 2014 Ludwig M Brinckmann
 * Copyright 2015-2017 devemux86
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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;

import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.layer.hills.DiffuseLightShadingAlgorithm;
import org.mapsforge.map.layer.hills.HillsRenderConfig;
import org.mapsforge.map.layer.hills.MemoryCachingHgtReaderTileSource;
import org.mapsforge.map.layer.hills.ShadingAlgorithm;
import org.mapsforge.map.layer.hills.SimpleShadingAlgorithm;
import org.mapsforge.map.layer.renderer.TileRendererLayer;

import java.io.File;

/**
 * Compare two hillshading configurations, bring RAM and patience
 */
public class HillshadingCompareMapViewer extends DualSyncMapViewer {
    @Override
    protected HillsRenderConfig getHillsRenderConfig() {
        ShadingAlgorithm algorithm = new SimpleShadingAlgorithm();
        setMapTitle(algorithm.toString());
        return commonHillshading(algorithm);
    }

    protected HillsRenderConfig getHillsRenderConfig2() {
        ShadingAlgorithm algorithm = new DiffuseLightShadingAlgorithm();
        setMapTitle2(algorithm.toString());
        HillsRenderConfig hillsRenderConfig = commonHillshading(algorithm);
        hillsRenderConfig.setMaginuteScaleFactor(1.5f);
        return hillsRenderConfig;
    }


    @Override
    protected void createLayers2() {
        TileRendererLayer tileRendererLayer = AndroidUtil.createTileRendererLayer(this.tileCaches.get(1),
                mapView2.getModel().mapViewPosition, getMapFile(), getRenderTheme(), false, true, false,
                getHillsRenderConfig2());
        this.mapView2.getLayerManager().getLayers().add(tileRendererLayer);
    }

    protected File getDemFolder() {
        return new File(getMapFileDirectory(), "dem");
    }



    private HillsRenderConfig commonHillshading(ShadingAlgorithm algorithm) {
        MemoryCachingHgtReaderTileSource tileSource = new MemoryCachingHgtReaderTileSource(getDemFolder(), algorithm, AndroidGraphicFactory.INSTANCE);

        tileSource.setMainCacheSize(8);
        tileSource.setNeighborCacheSize(8);
        tileSource.setEnableInterpolationOverlap(true);

        HillsRenderConfig hillsRenderConfig = new HillsRenderConfig(tileSource);

        return hillsRenderConfig;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        File demFolder = getDemFolder();

        if (!(demFolder.exists() && demFolder.isDirectory() && demFolder.canRead() && demFolder.listFiles().length > 0)) {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle("Hillshading demo needs SRTM hgt files");
            alert.setMessage("Currently looking in " + demFolder + "\noverride in " + this.getClass().getCanonicalName());
            alert.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    finish();
                }
            });
        }
    }
}
