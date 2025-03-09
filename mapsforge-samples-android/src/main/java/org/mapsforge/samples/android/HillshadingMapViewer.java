/*
 * Copyright 2017-2022 usrusr
 * Copyright 2017-2019 devemux86
 * Copyright 2024 Sublimis
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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.hills.DemFolderAndroidContent;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.hills.*;
import org.mapsforge.map.layer.labels.LabelLayer;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.rendertheme.internal.MapsforgeThemes;

import java.io.File;

/**
 * Standard map view with hill shading.
 */
public class HillshadingMapViewer extends DefaultTheme {
    private static final boolean EXTERNAL_HILLSHADING = false;

    /**
     * holds the DEM folder URI in a quick and dirty "reboot" after selection
     */
    private static final String demFolderKey = "demFolderUri";
    private static final String demUseFiles = "demFolderFiles";
    private static final int SELECT_DEM_FOLDER = 0;

    @SuppressWarnings("deprecation")
    @Override
    protected void createLayers() {
        DemFolder anyDems = null;
        Uri demUri = getIntent().getParcelableExtra(demFolderKey);
        boolean demFiles = getIntent().getBooleanExtra(demUseFiles, false);

        File demFolder = new File(getMapFileDirectory(), "dem");
        if (demFiles) {
            anyDems = new DemFolderFS(demFolder);
        } else if (demUri != null) {
            anyDems = new DemFolderAndroidContent(demUri, this, getContentResolver());
        }

        HillsRenderConfig hillsConfig = null;
        if (anyDems == null) {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle("Select DEM source (srtm hgt files) for hillshading:");
            String fileState = !demFolder.exists() ? "(does not exist)" :
                    !demFolder.isDirectory() ? "(not a directory)" :
                            !demFolder.canRead() ? "(cannot read)" :
                                    demFolder.listFiles() == null ? "(null files)" :
                                            ("(" + demFolder.listFiles().length + " files)");
            alert.setItems(new CharSequence[]{
                    "\nFile access (only older android) " + getMapFileDirectory() + "/dem " + fileState + "\n",
                    "\nSelect DEM folder for ContentResolver\n"
            }, (dialog, which) -> {
                if (which == 0) {
                    // "reboot" with demUseFiles=true
                    Intent intent = (Intent) getIntent().clone();
                    intent.putExtra(demUseFiles, true);
                    setIntent(intent);
                    recreate();
                } else if (which == 1) {
                    startSelect();
                }
            });
            alert.show();
        } else {
            final AdaptiveClasyHillShading algorithm = new AdaptiveClasyHillShading()
                    // You can make additional behavior adjustments
                    .setAdaptiveZoomEnabled(true)
                    // .setZoomMinOverride(0)
                    // .setZoomMaxOverride(17)
                    .setCustomQualityScale(1);

            MemoryCachingHgtReaderTileSource hillTileSource = new MemoryCachingHgtReaderTileSource(anyDems, algorithm, AndroidGraphicFactory.INSTANCE);

            hillsConfig = new HillsRenderConfig(hillTileSource);

            // You can override theme values:
            // hillsConfig.setMagnitudeScaleFactor(1);
            // hillsConfig.setColor(0xff000000);
            hillsConfig.setExternal(EXTERNAL_HILLSHADING);

            // call after setting/changing parameters, walks filesystem for DEM metadata
            hillsConfig.indexOnThread();
        }

        TileRendererLayer tileRendererLayer = AndroidUtil.createTileRendererLayer(tileCaches.get(0),
                mapView.getModel().mapViewPosition, getMapFile(), getRenderTheme(),
                false, false, true, EXTERNAL_HILLSHADING ? null : hillsConfig);
        mapView.getLayerManager().getLayers().add(tileRendererLayer);

        if (EXTERNAL_HILLSHADING) {
            TileRendererLayer hillshadingLayer = new TileRendererLayer(tileCaches.get(1),
                    getMapFile(), mapView.getModel().mapViewPosition, true, false, false,
                    AndroidGraphicFactory.INSTANCE, hillsConfig);
            hillshadingLayer.setXmlRenderTheme(MapsforgeThemes.HILLSHADING);
            mapView.getLayerManager().getLayers().add(hillshadingLayer);
        }

        LabelLayer labelLayer = new LabelLayer(AndroidGraphicFactory.INSTANCE, tileRendererLayer.getLabelStore());
        mapView.getLayerManager().getLayers().add(labelLayer);
    }

    private TileCache createHillshadingCache() {
        return AndroidUtil.createTileCache(this, getPersistableId2(),
                mapView.getModel().displayModel.getTileSize(), getScreenRatio(),
                mapView.getModel().frameBufferModel.getOverdrawFactor());
    }

    @Override
    protected void createTileCaches() {
        super.createTileCaches();
        tileCaches.add(createHillshadingCache());
    }

    private String getPersistableId2() {
        return getPersistableId() + "-2";
    }

    private void startSelect() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                        | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
        );

        startActivityForResult(intent, SELECT_DEM_FOLDER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SELECT_DEM_FOLDER) try {
            if (resultCode != Activity.RESULT_OK || data == null) {
                return;
            }

            // we "reboot" our activity because createLayers runs so early
            Intent ourIntent = getIntent();

            Uri demUri = data.getData();

            getContentResolver().takePersistableUriPermission(demUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

            Intent intent = (Intent) ourIntent.clone();

            intent.putExtra(demFolderKey, demUri);
            setIntent(intent);
            recreate();
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
}
