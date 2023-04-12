/*
 * Copyright 2013-2014 Ludwig M Brinckmann
 * Copyright 2015-2019 devemux86
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

import android.graphics.BitmapFactory;
import android.widget.Toast;
import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.android.graphics.AndroidBitmap;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.Layers;
import org.mapsforge.map.layer.overlay.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Map viewer with a few overlays added.
 */
public class OverlayMapViewer extends DownloadLayerViewer {

    protected LatLong latLong1 = new LatLong(52.5, 13.4);
    protected LatLong latLong2 = new LatLong(52.499, 13.402);
    protected LatLong latLong3 = new LatLong(52.503, 13.399);
    protected LatLong latLong4 = new LatLong(52.51, 13.401);
    protected LatLong latLong5 = new LatLong(52.508, 13.408);
    protected LatLong latLong6 = new LatLong(52.515, 13.420);
    protected LatLong latLong7 = new LatLong(52.51, 13.41);
    protected LatLong latLong8 = new LatLong(52.51, 13.42);
    protected LatLong latLong9 = new LatLong(52.52, 13.43);

    protected LatLong latLong10 = new LatLong(52.514, 13.413);
    protected LatLong latLong11 = new LatLong(52.514, 13.423);
    protected LatLong latLong12 = new LatLong(52.524, 13.433);
    protected LatLong latLong13 = new LatLong(52.516, 13.4145);
    protected LatLong latLong14 = new LatLong(52.516, 13.4245);
    protected LatLong latLong15 = new LatLong(52.526, 13.4345);

    protected void addOverlayLayers(Layers layers) {

        Polyline polyline = new Polyline(Utils.createPaint(
                AndroidGraphicFactory.INSTANCE.createColor(Color.BLUE),
                (int) (8 * mapView.getModel().displayModel.getScaleFactor()),
                Style.STROKE), AndroidGraphicFactory.INSTANCE);
        List<LatLong> latLongs = new ArrayList<>();
        latLongs.add(latLong1);
        latLongs.add(latLong2);
        latLongs.add(latLong3);
        polyline.setPoints(latLongs);

        // this illustrates that bitmap shaders can be used on a path, but then any dash effect
        // will not be applied.
        Paint shaderPaint = Utils.createPaint(AndroidGraphicFactory.INSTANCE.createColor(Color.GREEN), 90, Style.STROKE);
        shaderPaint.setBitmapShader(new AndroidBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.marker_green)));

        Polyline polylineWithShader = new Polyline(shaderPaint, AndroidGraphicFactory.INSTANCE, true) {
            @Override
            public boolean onTap(LatLong tapLatLong, Point layerXY, Point tapXY) {
                if (contains(tapXY, mapView.getMapViewProjection())) {
                    Toast.makeText(OverlayMapViewer.this, "Polyline tap\n" + tapLatLong, Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            }
        };
        List<LatLong> latLongs2 = new ArrayList<>();
        latLongs2.add(latLong7);
        latLongs2.add(latLong8);
        latLongs2.add(latLong9);
        polylineWithShader.setPoints(latLongs2);

        Paint paintFill = Utils.createPaint(
                AndroidGraphicFactory.INSTANCE.createColor(Color.GREEN), 2,
                Style.FILL);
        Paint paintStroke = Utils.createPaint(
                AndroidGraphicFactory.INSTANCE.createColor(Color.BLACK), 2,
                Style.STROKE);
        Polygon polygon = new Polygon(paintFill, paintStroke, AndroidGraphicFactory.INSTANCE) {
            @Override
            public boolean onTap(LatLong tapLatLong, Point layerXY, Point tapXY) {
                if (contains(tapLatLong)) {
                    Toast.makeText(OverlayMapViewer.this, "Polygon tap\n" + tapLatLong, Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            }
        };
        List<LatLong> latLongs3 = new ArrayList<>();
        latLongs3.add(latLong2);
        latLongs3.add(latLong3);
        latLongs3.add(latLong4);
        latLongs3.add(latLong5);
        polygon.setPoints(latLongs3);

        // A polygon filled with a shader, where the shader is not aligned
        Paint paintFill2 = Utils.createPaint(
                AndroidGraphicFactory.INSTANCE.createColor(Color.GREEN), 2,
                Style.FILL);
        paintFill2.setBitmapShader(new AndroidBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.marker_green)));

        Paint paintStroke2 = Utils.createPaint(
                AndroidGraphicFactory.INSTANCE.createColor(Color.BLACK), 2,
                Style.STROKE);
        Polygon polygonWithShaderNonAligned = new Polygon(paintFill2, paintStroke2,
                AndroidGraphicFactory.INSTANCE);
        List<LatLong> latLongs4 = new ArrayList<>();
        latLongs4.add(latLong10);
        latLongs4.add(latLong11);
        latLongs4.add(latLong12);
        latLongs4.add(latLong10);
        polygonWithShaderNonAligned.setPoints(latLongs4);

        Paint paintFill3 = Utils.createPaint(
                AndroidGraphicFactory.INSTANCE.createColor(Color.RED), 2,
                Style.FILL);
        paintFill3.setBitmapShader(new AndroidBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.marker_red)));

        Paint paintStroke3 = Utils.createPaint(
                AndroidGraphicFactory.INSTANCE.createColor(Color.BLACK), 2,
                Style.STROKE);
        Polygon polygonWithShaderAligned = new Polygon(paintFill3, paintStroke3,
                AndroidGraphicFactory.INSTANCE, true);
        List<LatLong> latLongs5 = new ArrayList<>();
        latLongs5.add(latLong13);
        latLongs5.add(latLong14);
        latLongs5.add(latLong15);
        latLongs5.add(latLong13);
        polygonWithShaderAligned.setPoints(latLongs5);

        Marker marker1 = Utils.createTappableMarker(this,
                R.drawable.marker_red, latLong1);

        Circle circle = new Circle(latLong3, 100, Utils.createPaint(
                AndroidGraphicFactory.INSTANCE.createColor(Color.WHITE), 0,
                Style.FILL), null) {
            @Override
            public boolean onTap(LatLong geoPoint, Point viewPosition,
                                 Point tapPoint) {
                if (this.contains(viewPosition, tapPoint, geoPoint.latitude,
                        mapView.getModel().mapViewPosition.getZoomLevel())) {
                    Toast.makeText(OverlayMapViewer.this,
                            "The Circle was tapped " + geoPoint.toString(),
                            Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            }
        };

        FixedPixelCircle tappableCircle = new FixedPixelCircle(
                latLong6,
                20,
                Utils.createPaint(
                        AndroidGraphicFactory.INSTANCE.createColor(Color.GREEN),
                        0, Style.FILL), null) {
            @Override
            public boolean onTap(LatLong geoPoint, Point viewPosition,
                                 Point tapPoint) {
                if (this.contains(viewPosition, tapPoint)) {
                    Toast.makeText(OverlayMapViewer.this,
                            "The Circle was tapped " + geoPoint.toString(),
                            Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            }
        };

        layers.add(polyline);
        layers.add(polylineWithShader);
        layers.add(polygon);
        layers.add(polygonWithShaderAligned);
        layers.add(polygonWithShaderNonAligned);
        layers.add(circle);
        layers.add(marker1);
        layers.add(tappableCircle);
    }

    @Override
    protected void createLayers() {
        super.createLayers();

        // we just add a few more overlays
        addOverlayLayers(mapView.getLayerManager().getLayers());
    }
}
