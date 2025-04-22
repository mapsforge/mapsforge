/*
 * Copyright 2025 moving-bits
 * Copyright 2025 devemux86
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
import android.os.Bundle;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Dimension;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.core.util.LatLongUtils;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.mbtiles.MBTilesFile;
import org.mapsforge.map.android.mbtiles.TileMBTilesLayer;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.cache.InMemoryTileCache;

import java.io.File;

/**
 * An example activity making use of raster MBTiles.
 */
public class MBTilesBitmapActivity extends Activity {

    private MapView mapView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AndroidGraphicFactory.createInstance(getApplication());

        setContentView(R.layout.mapviewer);
        mapView = findViewById(R.id.mapView);

        File[] files = getExternalMediaDirs()[0].listFiles((dir, name) -> name.endsWith(".mbtiles"));
        if (files == null || files.length == 0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setTitle(R.string.warning)
                    .setMessage(getResources().getString(R.string.startup_message_mbtiles))
                    .setPositiveButton(R.string.exit, (dialog, which) -> finish());
            builder.show();
            return;
        }

        BoundingBox bbox = null;
        for (File file : files) {
            MBTilesFile mbtilesFile = new MBTilesFile(file);
            BoundingBox boundingBox = mbtilesFile.getBoundingBox();
            bbox = bbox == null ? boundingBox : bbox.extendBoundingBox(boundingBox);
            TileMBTilesLayer tilesLayer = new TileMBTilesLayer(new InMemoryTileCache(200), mapView.getModel().mapViewPosition, true, mbtilesFile, AndroidGraphicFactory.INSTANCE);
            mapView.getLayerManager().getLayers().add(tilesLayer);
        }
        if (bbox != null) {
            int tileSize = mapView.getModel().displayModel.getTileSize();
            byte zoomForBounds = LatLongUtils.zoomForBounds(new Dimension(tileSize * 4, tileSize * 4), bbox, tileSize);
            mapView.getModel().mapViewPosition.setMapPosition(new MapPosition(bbox.getCenterPoint(), zoomForBounds));
        }
    }

    @Override
    protected void onDestroy() {
        mapView.destroyAll();
        AndroidGraphicFactory.clearResourceMemoryCache();
        super.onDestroy();
    }
}
