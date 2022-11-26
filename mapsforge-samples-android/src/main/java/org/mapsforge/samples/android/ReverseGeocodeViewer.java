/*
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

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Tag;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.LatLongUtils;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.datastore.MapReadResult;
import org.mapsforge.map.datastore.PointOfInterest;
import org.mapsforge.map.datastore.Way;
import org.mapsforge.map.layer.debug.TileCoordinatesLayer;
import org.mapsforge.map.layer.debug.TileGridLayer;
import org.mapsforge.map.layer.renderer.TileRendererLayer;

import java.util.List;

/**
 * Reverse Geocoding with long press.
 * <p/>
 * - POI in specified radius.<br/>
 * - Ways containing touch point.
 */
public class ReverseGeocodeViewer extends DefaultTheme {
    private static final int TOUCH_RADIUS = 32 / 2;

    @Override
    protected void createLayers() {
        TileRendererLayer tileRendererLayer = new TileRendererLayer(
                this.tileCaches.get(0), getMapFile(),
                this.mapView.getModel().mapViewPosition,
                false, true, false, AndroidGraphicFactory.INSTANCE) {
            @Override
            public boolean onLongPress(LatLong tapLatLong, Point layerXY, Point tapXY) {
                ReverseGeocodeViewer.this.onLongPress(tapLatLong, tapXY);
                return true;
            }
        };
        tileRendererLayer.setXmlRenderTheme(getRenderTheme());
        this.mapView.getLayerManager().getLayers().add(tileRendererLayer);

        // For debug
        this.mapView.getLayerManager().getLayers().add(new TileGridLayer(AndroidGraphicFactory.INSTANCE, this.mapView.getModel().displayModel));
        this.mapView.getLayerManager().getLayers().add(new TileCoordinatesLayer(AndroidGraphicFactory.INSTANCE, this.mapView.getModel().displayModel));
    }

    private void onLongPress(LatLong tapLatLong, Point tapXY) {
        // Read all labeled POI and ways for the area covered by the tiles under touch
        float touchRadius = TOUCH_RADIUS * this.mapView.getModel().displayModel.getScaleFactor();
        long mapSize = MercatorProjection.getMapSize(this.mapView.getModel().mapViewPosition.getZoomLevel(), this.mapView.getModel().displayModel.getTileSize());
        double pixelX = MercatorProjection.longitudeToPixelX(tapLatLong.longitude, mapSize);
        double pixelY = MercatorProjection.latitudeToPixelY(tapLatLong.latitude, mapSize);
        int tileXMin = MercatorProjection.pixelXToTileX(pixelX - touchRadius, this.mapView.getModel().mapViewPosition.getZoomLevel(), this.mapView.getModel().displayModel.getTileSize());
        int tileXMax = MercatorProjection.pixelXToTileX(pixelX + touchRadius, this.mapView.getModel().mapViewPosition.getZoomLevel(), this.mapView.getModel().displayModel.getTileSize());
        int tileYMin = MercatorProjection.pixelYToTileY(pixelY - touchRadius, this.mapView.getModel().mapViewPosition.getZoomLevel(), this.mapView.getModel().displayModel.getTileSize());
        int tileYMax = MercatorProjection.pixelYToTileY(pixelY + touchRadius, this.mapView.getModel().mapViewPosition.getZoomLevel(), this.mapView.getModel().displayModel.getTileSize());
        Tile upperLeft = new Tile(tileXMin, tileYMin, this.mapView.getModel().mapViewPosition.getZoomLevel(), this.mapView.getModel().displayModel.getTileSize());
        Tile lowerRight = new Tile(tileXMax, tileYMax, this.mapView.getModel().mapViewPosition.getZoomLevel(), this.mapView.getModel().displayModel.getTileSize());
        MapReadResult mapReadResult = getMapFile().readLabels(upperLeft, lowerRight);

        StringBuilder sb = new StringBuilder();

        // Filter POI
        sb.append("*** POI ***");
        for (PointOfInterest pointOfInterest : mapReadResult.pointOfInterests) {
            Point layerXY = this.mapView.getMapViewProjection().toPixels(pointOfInterest.position);
            if (layerXY.distance(tapXY) > touchRadius) {
                continue;
            }
            sb.append("\n");
            List<Tag> tags = pointOfInterest.tags;
            for (int i = 0; i < tags.size(); i++) {
                Tag tag = tags.get(i);
                sb.append("\n").append(tag.key).append("=").append(tag.value);
            }
        }

        // Filter ways
        sb.append("\n\n").append("*** WAYS ***");
        for (Way way : mapReadResult.ways) {
            if (!LatLongUtils.isClosedWay(way.latLongs[0])
                    || !LatLongUtils.contains(way.latLongs[0], tapLatLong)) {
                continue;
            }
            sb.append("\n");
            List<Tag> tags = way.tags;
            for (int i = 0; i < tags.size(); i++) {
                Tag tag = tags.get(i);
                sb.append("\n").append(tag.key).append("=").append(tag.value);
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(android.R.drawable.ic_menu_search);
        builder.setTitle(R.string.dialog_reverse_geocoding_title);
        builder.setMessage(sb);
        builder.setPositiveButton(R.string.okbutton, null);
        builder.show();
    }
}
