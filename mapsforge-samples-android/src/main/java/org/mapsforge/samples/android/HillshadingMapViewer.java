/*
 * Copyright 2017 usrusr
 * Copyright 2017-2019 devemux86
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
import org.mapsforge.map.layer.hills.HillsRenderConfig;
import org.mapsforge.map.layer.hills.MemoryCachingHgtReaderTileSource;
import org.mapsforge.map.layer.hills.SimpleShadingAlgorithm;

import java.io.File;

/**
 * Standard map view with hill shading.
 */
public class HillshadingMapViewer extends DefaultTheme {
    private File demFolder;
    private HillsRenderConfig hillsConfig;

    @Override
    protected void createLayers() {
        demFolder = new File(getMapFileDirectory(), "dem");

        if (!(demFolder.exists() && demFolder.isDirectory() && demFolder.canRead() && demFolder.listFiles().length > 0)) {
            hillsConfig = null;
        } else {
            // minimum setup for hillshading
            MemoryCachingHgtReaderTileSource hillTileSource = new MemoryCachingHgtReaderTileSource(demFolder, new SimpleShadingAlgorithm(), AndroidGraphicFactory.INSTANCE);
            customizeConfig(hillTileSource);
            hillsConfig = new HillsRenderConfig(hillTileSource);

            // call after setting/changing parameters, walks filesystem for DEM metadata
            hillsConfig.indexOnThread();
        }

        super.createLayers();
    }

    private void customizeConfig(MemoryCachingHgtReaderTileSource hillTileSource) {
        hillTileSource.setEnableInterpolationOverlap(true);
    }

    @Override
    protected HillsRenderConfig getHillsRenderConfig() {
        return hillsConfig;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (hillsConfig == null) {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle("Hillshading demo needs SRTM hgt files");
            alert.setMessage("Currently looking in: " + demFolder);
            alert.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    finish();
                }
            });
            alert.show();
        }
    }
}
