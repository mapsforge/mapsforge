/*
 * Copyright 2017-2022 usrusr
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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.hills.DemFolderAndroidContent;
import org.mapsforge.map.layer.hills.*;

import java.io.File;

/**
 * Standard map view with hill shading.
 */
public class HillshadingMapViewer extends DefaultTheme {
    private HillsRenderConfig hillsConfig;
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
        }
        if (anyDems != null) {
            // minimum setup for hillshading
            MemoryCachingHgtReaderTileSource hillTileSource = new MemoryCachingHgtReaderTileSource(anyDems, new StandardClasyHillShading(), AndroidGraphicFactory.INSTANCE);
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


    private void startSelect() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                        | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
        );

        startActivityForResult(intent, SELECT_DEM_FOLDER);

        hillsConfig = null;
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
