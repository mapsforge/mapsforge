/*
 * Copyright 2013-2014 Ludwig M Brinckmann
 * Copyright 2016 devemux86
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

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.overlay.FixedPixelCircle;
import org.mapsforge.map.layer.renderer.TileRendererLayer;

/**
 * Demonstrates how to enable a LongPress on a layer.
 * <p/>
 * In this example a long press creates/removes
 * circles, a tap on a circle toggles the colour between red and green.
 */
public class LongPressAction extends RenderTheme4 {

    private static final Paint GREEN = Utils.createPaint(
            AndroidGraphicFactory.INSTANCE.createColor(Color.GREEN), 0,
            Style.FILL);
    private static final Paint RED = Utils.createPaint(
            AndroidGraphicFactory.INSTANCE.createColor(Color.RED), 0,
            Style.FILL);
    private static final Paint BLACK = Utils.createPaint(
            AndroidGraphicFactory.INSTANCE.createColor(Color.BLACK), 0,
            Style.FILL);

    private int i;

    @Override
    protected void createLayers() {
        TileRendererLayer tileRendererLayer = new TileRendererLayer(
                this.tileCaches.get(0), getMapFile(),
                this.mapView.getModel().mapViewPosition,
                false, true, false,
                org.mapsforge.map.android.graphics.AndroidGraphicFactory.INSTANCE) {
            @Override
            public boolean onLongPress(LatLong tapLatLong, Point thisXY,
                                       Point tapXY) {
                LongPressAction.this.onLongPress(tapLatLong);
                return true;
            }
        };
        tileRendererLayer.setXmlRenderTheme(this.getRenderTheme());
        mapView.getLayerManager().getLayers().add(tileRendererLayer);
        BLACK.setTextSize(22 * this.mapView.getModel().displayModel.getScaleFactor());
    }

    protected void onLongPress(final LatLong position) {
        float circleSize = 20 * this.mapView.getModel().displayModel.getScaleFactor();

        i += 1;

        FixedPixelCircle tappableCircle = new FixedPixelCircle(position,
                circleSize, GREEN, null) {

            int count = i;

            @Override
            public void draw(BoundingBox boundingBox, byte zoomLevel, Canvas
                    canvas, Point topLeftPoint) {
                super.draw(boundingBox, zoomLevel, canvas, topLeftPoint);

                long mapSize = MercatorProjection.getMapSize(zoomLevel, this.displayModel.getTileSize());

                int pixelX = (int) (MercatorProjection.longitudeToPixelX(position.longitude, mapSize) - topLeftPoint.x);
                int pixelY = (int) (MercatorProjection.latitudeToPixelY(position.latitude, mapSize) - topLeftPoint.y);
                String text = Integer.toString(count);
                canvas.drawText(text, pixelX - BLACK.getTextWidth(text) / 2, pixelY + BLACK.getTextHeight(text) / 2, BLACK);
            }

            @Override
            public boolean onLongPress(LatLong geoPoint, Point viewPosition,
                                       Point tapPoint) {
                if (this.contains(viewPosition, tapPoint)) {
                    LongPressAction.this.mapView.getLayerManager()
                            .getLayers().remove(this);
                    LongPressAction.this.mapView.getLayerManager()
                            .redrawLayers();
                    return true;
                }
                return false;
            }

            @Override
            public boolean onTap(LatLong geoPoint, Point viewPosition,
                                 Point tapPoint) {
                if (this.contains(viewPosition, tapPoint)) {
                    toggleColor();
                    this.requestRedraw();
                    return true;
                }
                return false;
            }

            private void toggleColor() {
                if (this.getPaintFill().equals(LongPressAction.GREEN)) {
                    this.setPaintFill(LongPressAction.RED);
                } else {
                    this.setPaintFill(LongPressAction.GREEN);
                }
            }
        };
        this.mapView.getLayerManager().getLayers().add(tappableCircle);
        tappableCircle.requestRedraw();

    }
}
