/*
 * Copyright 2025 moving-bits
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
import android.os.Bundle;

import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.mbtiles.MBTilesFile;
import org.mapsforge.map.android.mbtiles.TileMBTilesLayer;
import org.mapsforge.map.layer.cache.InMemoryTileCache;

import java.io.File;

/**
 * An example activity making use of raster MBTiles.
 */
public class MBTilesBitmapActivity extends DefaultTheme {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        File[] files = getExternalMediaDirs()[0].listFiles((dir, name) -> name.endsWith(".mbtiles"));
        if (files == null || files.length == 0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setTitle(R.string.warning)
                    .setMessage(getResources().getString(R.string.startup_message_mbtiles))
                    .setPositiveButton(R.string.exit, (dialog, which) -> finish());
            builder.show();
            return;
        }

        for (File file : files) {
            TileMBTilesLayer tilesLayer = new TileMBTilesLayer(new InMemoryTileCache(200), mapView.getModel().mapViewPosition, true, new MBTilesFile(file), AndroidGraphicFactory.INSTANCE);
            mapView.getLayerManager().getLayers().add(tilesLayer);
        }
    }
}
