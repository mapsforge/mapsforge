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
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Rotation;
import org.mapsforge.map.android.graphics.AndroidBitmap;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.Layers;
import org.mapsforge.map.layer.overlay.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Map viewer with a few overlays added and map rotation.
 */
public class OverlayMapViewer extends DownloadLayerViewer {

    private final LatLong latLong1 = new LatLong(52.5, 13.4);
    private final LatLong latLong2 = new LatLong(52.499, 13.402);
    private final LatLong latLong3 = new LatLong(52.503, 13.399);
    private final LatLong latLong4 = new LatLong(52.51, 13.401);
    private final LatLong latLong5 = new LatLong(52.508, 13.408);
    private final LatLong latLong6 = new LatLong(52.515, 13.420);
    private final LatLong latLong7 = new LatLong(52.51, 13.41);
    private final LatLong latLong8 = new LatLong(52.51, 13.42);
    private final LatLong latLong9 = new LatLong(52.52, 13.43);
    private final LatLong latLong10 = new LatLong(52.514, 13.413);
    private final LatLong latLong11 = new LatLong(52.514, 13.423);
    private final LatLong latLong12 = new LatLong(52.524, 13.433);
    private final LatLong latLong13 = new LatLong(52.516, 13.4145);
    private final LatLong latLong14 = new LatLong(52.516, 13.4245);
    private final LatLong latLong15 = new LatLong(52.526, 13.4345);

    private final LatLong anchorPolygonWithHoles = new LatLong(52.499, 13.430);

    private float rotationAngle;

    private void addOverlayLayers(Layers layers) {

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
            public boolean onLongPress(LatLong tapLatLong, Point layerXY, Point tapXY) {
                if (contains(tapXY, mapView)) {
                    Toast.makeText(OverlayMapViewer.this, "Polyline long press\n" + tapLatLong, Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            }

            @Override
            public boolean onTap(LatLong tapLatLong, Point layerXY, Point tapXY) {
                if (contains(tapXY, mapView)) {
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
            public boolean onLongPress(LatLong tapLatLong, Point layerXY, Point tapXY) {
                if (contains(tapXY, mapView)) {
                    Toast.makeText(OverlayMapViewer.this, "Polygon long press\n" + tapLatLong, Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            }

            @Override
            public boolean onTap(LatLong tapLatLong, Point layerXY, Point tapXY) {
                if (contains(tapXY, mapView)) {
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

        Marker marker1 = Utils.createTappableMarker(this, R.drawable.marker_red, latLong1, mapView);

        Circle circle = new Circle(latLong3, 100, Utils.createPaint(AndroidGraphicFactory.INSTANCE.createColor(Color.WHITE), 0, Style.FILL), null) {
            @Override
            public boolean onLongPress(LatLong tapLatLong, Point layerXY, Point tapXY) {
                if (this.contains(layerXY, tapXY, tapLatLong.latitude, mapView)) {
                    Toast.makeText(OverlayMapViewer.this, "Circle long press\n" + tapLatLong, Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            }

            @Override
            public boolean onTap(LatLong tapLatLong, Point layerXY, Point tapXY) {
                if (this.contains(layerXY, tapXY, tapLatLong.latitude, mapView)) {
                    Toast.makeText(OverlayMapViewer.this, "Circle tap\n" + tapLatLong, Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            }
        };

        FixedPixelCircle fixedPixelCircle = new FixedPixelCircle(latLong6, 20, Utils.createPaint(AndroidGraphicFactory.INSTANCE.createColor(Color.GREEN), 0, Style.FILL), null) {
            @Override
            public boolean onLongPress(LatLong tapLatLong, Point layerXY, Point tapXY) {
                if (this.contains(layerXY, tapXY, mapView)) {
                    Toast.makeText(OverlayMapViewer.this, "Circle long press\n" + tapLatLong.toString(), Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            }

            @Override
            public boolean onTap(LatLong tapLatLong, Point layerXY, Point tapXY) {
                if (this.contains(layerXY, tapXY, mapView)) {
                    Toast.makeText(OverlayMapViewer.this, "Circle tap\n" + tapLatLong.toString(), Toast.LENGTH_SHORT).show();
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
        layers.add(fixedPixelCircle);
        layers.add(createPolygonWithHoles(anchorPolygonWithHoles));
    }

    private Polygon createPolygonWithHoles(final LatLong anchor) {
        Paint paintFill = Utils.createPaint(
                AndroidGraphicFactory.INSTANCE.createColor(Color.GREEN), 2,
                Style.FILL);
        Paint paintStroke = Utils.createPaint(
                AndroidGraphicFactory.INSTANCE.createColor(Color.RED), 10,
                Style.STROKE);
        Polygon polygonWithHoles = new Polygon(paintFill, paintStroke, AndroidGraphicFactory.INSTANCE) {
            @Override
            public boolean onLongPress(LatLong tapLatLong, Point layerXY, Point tapXY) {
                if (contains(tapXY, mapView)) {
                    Toast.makeText(OverlayMapViewer.this, "Polygon long press\n" + tapLatLong, Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            }

            @Override
            public boolean onTap(LatLong tapLatLong, Point layerXY, Point tapXY) {
                if (contains(tapXY, mapView)) {
                    Toast.makeText(OverlayMapViewer.this, "Polygon tap\n" + tapLatLong, Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            }
        };
        polygonWithHoles.addPoint(anchor);
        polygonWithHoles.addPoint(addFraction(anchor, 10, 10));
        polygonWithHoles.addPoint(addFraction(anchor, 50, 10));
        polygonWithHoles.addPoint(addFraction(anchor, 50, 80));
        polygonWithHoles.addPoint(addFraction(anchor, 10, 80));

        polygonWithHoles.addHole(Arrays.asList(
                addFraction(anchor, 20, 20),
                addFraction(anchor, 25, 30),
                addFraction(anchor, 15, 30)));
        polygonWithHoles.addHole(Arrays.asList(
                addFraction(anchor, 40, 40),
                addFraction(anchor, 45, 40),
                addFraction(anchor, 45, 60),
                addFraction(anchor, 40, 60)));
        polygonWithHoles.addHole(Arrays.asList(
                addFraction(anchor, 20, 40),
                addFraction(anchor, 35, 70),
                addFraction(anchor, 25, 60),
                addFraction(anchor, 15, 60)));

        return polygonWithHoles;
    }

    private static LatLong addFraction(final LatLong latLon, final int latAdd, final int lonAdd) {
        return new LatLong(latLon.getLatitude() + ((double) latAdd / 5000d),
                latLon.getLongitude() + ((double) lonAdd / 5000d));
    }

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
        super.createLayers();

        // we just add a few more overlays
        addOverlayLayers(mapView.getLayerManager().getLayers());

        mapView.setMapViewCenterY(0.75f);
        mapView.getModel().frameBufferModel.setOverdrawFactor(Math.max(mapView.getModel().frameBufferModel.getOverdrawFactor(), mapView.getMapViewCenterY() * 2));
        rotationAngle = mapView.getMapRotation().degrees;
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
