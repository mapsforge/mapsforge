/*
 * Copyright 2015 Ludwig M Brinckmann
 * Copyright 2024 devemux86
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

import android.view.View;
import android.widget.Button;
import org.mapsforge.core.model.Rotation;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.layer.debug.TileCoordinatesLayer;
import org.mapsforge.map.layer.debug.TileGridLayer;
import org.mapsforge.map.layer.labels.LabelLayer;
import org.mapsforge.map.layer.renderer.TileRendererLayer;

/**
 * Viewer supporting map rotation.
 */
public class RotationMapViewer extends DefaultTheme {

    private float rotationAngle;

    @Override
    protected void createControls() {
        super.createControls();

        // Three rotation buttons: rotate counterclockwise, reset, clockwise
        Button rotateCCWButton = findViewById(R.id.rotateCounterClockWiseButton);
        rotateCCWButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rotationAngle -= 15;
                mapView.rotate(new Rotation(rotationAngle, mapView.getWidth() * 0.5f, mapView.getHeight() * 0.5f));
                redrawLayers();
            }
        });

        Button rotateResetButton = findViewById(R.id.rotateResetButton);
        rotateResetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rotationAngle = 0;
                mapView.rotate(new Rotation(rotationAngle, mapView.getWidth() * 0.5f, mapView.getHeight() * 0.5f));
                redrawLayers();
            }
        });

        Button rotateCWButton = findViewById(R.id.rotateClockwiseButton);
        rotateCWButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rotationAngle += 15;
                mapView.rotate(new Rotation(rotationAngle, mapView.getWidth() * 0.5f, mapView.getHeight() * 0.5f));
                redrawLayers();
            }
        });
    }

    @Override
    protected void createLayers() {
        TileRendererLayer tileRendererLayer = AndroidUtil.createTileRendererLayer(tileCaches.get(0),
                mapView.getModel().mapViewPosition, getMapFile(), getRenderTheme(), false, false, true);
        mapView.getLayerManager().getLayers().add(tileRendererLayer);
        LabelLayer labelLayer = new LabelLayer(AndroidGraphicFactory.INSTANCE, tileRendererLayer.getLabelStore());
        mapView.getLayerManager().getLayers().add(labelLayer);

        // add a grid layer and a layer showing tile coordinates
        mapView.getLayerManager().getLayers()
                .add(new TileGridLayer(AndroidGraphicFactory.INSTANCE, mapView.getModel().displayModel));
        mapView.getLayerManager().getLayers()
                .add(new TileCoordinatesLayer(AndroidGraphicFactory.INSTANCE, mapView.getModel().displayModel));

        rotationAngle = mapView.getMapRotation().degrees;
    }

    @Override
    protected void createMapViews() {
        super.createMapViews();
        mapView.setMapViewCenterY(0.75f);
        mapView.getModel().frameBufferModel.setOverdrawFactor(1.5);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.rotation;
    }

    @Override
    protected float getScreenRatio() {
        // just to get the cache bigger right now.
        return 2f;
    }
}
