/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2013-2014 Ludwig M Brinckmann
 * Copyright 2014-2020 devemux86
 * Copyright 2014 Jordan Black
 * Copyright 2015 Andreas Schildbach
 * Copyright 2018 mikes222
 * Copyright 2019 mg4gh
 * Copyright 2021 eddiemuc
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
package org.mapsforge.map.android.input;

import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.Scroller;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Rotation;
import org.mapsforge.core.util.Parameters;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.Layers;
import org.mapsforge.map.model.MapViewPosition;

/**
 * Central handling of touch gestures.
 * <ul>
 * <li>Scroll (pan)</li>
 * <li>Fling</li>
 * <li>Scale</li>
 * <li>Quick scale (double tap + swipe)</li>
 * <li>Double tap (zoom)</li>
 * <li>Tap (overlay)</li>
 * <li>Long press (overlay)</li>
 * <li>Rotation</li>
 * </ul>
 */
public class TouchGestureHandler extends GestureDetector.SimpleOnGestureListener implements ScaleGestureDetector.OnScaleGestureListener, Runnable {

    public static double DELTA_ANGLE = 15;
    public static double DELTA_SCALE = 0.2;
    public static long DELTA_TIME = 25;

    private static final double LOG_2 = Math.log(2);

    private boolean doubleTapEnabled = true;
    private final Scroller flinger;
    private int flingLastX, flingLastY;
    private float focusX, focusY;
    private final Handler handler = new Handler(Looper.myLooper());
    private boolean isInDoubleTap, isInRotation, isInScale;
    private final MapView mapView;
    private LatLong pivot;
    private boolean rotationEnabled = false;
    private boolean scaleEnabled = true;
    private float scaleFactorCumulative;
    private byte zoomLevelStart;

    // Rotation
    private float currentAngle, startAngle, startScaleFactorCumulative;
    private long lastTime;

    public TouchGestureHandler(MapView mapView) {
        this.mapView = mapView;
        this.flinger = new Scroller(mapView.getContext());
    }

    private static double angleDifference(double angle1, double angle2) {
        return 180 - Math.abs(Math.abs(angle1 - angle2) - 180);
    }

    public void destroy() {
        this.handler.removeCallbacksAndMessages(null);
    }

    /**
     * Get state of double tap gestures:<br/>
     * - Double tap (zoom)
     */
    public boolean isDoubleTapEnabled() {
        return doubleTapEnabled;
    }

    public boolean isRotationEnabled() {
        return rotationEnabled;
    }

    /**
     * Get state of scale gestures:<br/>
     * - Scale<br/>
     * - Quick scale (double tap + swipe)<br/>
     * - Double tap (zoom)
     */
    public boolean isScaleEnabled() {
        return scaleEnabled;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        if (!this.scaleEnabled) {
            return false;
        }

        int action = e.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                this.isInDoubleTap = true;
                break;
            case MotionEvent.ACTION_UP:
                // Quick scale in between (cancel double tap)
                if (this.isInDoubleTap) {
                    MapViewPosition mapViewPosition = this.mapView.getModel().mapViewPosition;
                    if (this.doubleTapEnabled && mapViewPosition.getZoomLevel() < mapViewPosition.getZoomLevelMax()) {
                        if (Parameters.FRACTIONAL_ZOOM) {
                            this.mapView.onZoomEvent();
                            mapViewPosition.setZoom(mapViewPosition.getZoom() + 1);
                        } else {
                            Point center = this.mapView.getModel().mapViewDimension.getDimension().getCenter();
                            byte zoomLevelDiff = 1;
                            double moveHorizontal = (center.x - e.getX() + this.mapView.getOffsetX()) / Math.pow(2, zoomLevelDiff);
                            double moveVertical = (center.y - e.getY() + this.mapView.getOffsetY()) / Math.pow(2, zoomLevelDiff);
                            LatLong pivot = this.mapView.getMapViewProjection().fromPixels(e.getX(), e.getY());
                            if (pivot != null) {
                                this.mapView.onMoveEvent();
                                this.mapView.onZoomEvent();
                                mapViewPosition.setPivot(pivot);
                                if ((moveHorizontal != 0 || moveVertical != 0) && !Rotation.noRotation(this.mapView.getMapRotation())) {
                                    Rotation mapRotation = new Rotation(this.mapView.getMapRotation().degrees, 0, 0);
                                    Point rotated = mapRotation.rotate(moveHorizontal, moveVertical);
                                    moveHorizontal = rotated.x;
                                    moveVertical = rotated.y;
                                }
                                mapViewPosition.moveCenterAndZoom(moveHorizontal, moveVertical, zoomLevelDiff);
                            }
                        }
                    }
                    this.isInDoubleTap = false;
                    return true;
                }
                break;
        }

        return false;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        this.isInRotation = false;
        this.isInScale = false;
        this.flinger.forceFinished(true);
        return true;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (!this.isInScale && !this.isInRotation && e1.getPointerCount() == 1 && e2.getPointerCount() == 1) {
            this.flinger.fling(0, 0, (int) -velocityX, (int) -velocityY, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE);
            this.flingLastX = this.flingLastY = 0;
            this.handler.removeCallbacksAndMessages(null);
            this.handler.post(this);
            return true;
        }
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        // Normal or quick scale or rotation (no long press)
        if (!this.isInScale && !this.isInDoubleTap && !this.isInRotation) {
            Point tapXY = new Point(e.getX(), e.getY());
            LatLong tapLatLong;
            Point offset = tapXY.offset(-this.mapView.getOffsetX(), -this.mapView.getOffsetY());
            if (!Rotation.noRotation(this.mapView.getMapRotation())) {
                Point rotated = this.mapView.getMapRotation().rotate(offset);
                tapLatLong = this.mapView.getMapViewProjection().fromPixels(rotated.x, rotated.y);
            } else {
                tapLatLong = this.mapView.getMapViewProjection().fromPixels(offset.x, offset.y);
            }
            if (tapLatLong != null) {
                for (int i = this.mapView.getLayerManager().getLayers().size() - 1; i >= 0; --i) {
                    Layer layer = safeGet(this.mapView.getLayerManager().getLayers(), i);
                    if (layer == null) {
                        // Layers were modified in the meantime, stop processing outdated data
                        break;
                    }
                    Point layerXY = this.mapView.getMapViewProjection().toPixels(layer.getPosition());
                    if (layerXY != null) {
                        if (!Rotation.noRotation(this.mapView.getMapRotation())) {
                            layerXY = this.mapView.getMapRotation().rotate(layerXY, true);
                        }
                        layerXY = layerXY.offset(this.mapView.getOffsetX(), this.mapView.getOffsetY());
                    }
                    if (layer.onLongPress(tapLatLong, layerXY, tapXY)) {
                        break;
                    }
                }
            }
        }
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        if (this.isInRotation) {
            return true;
        }
        if (detector.getScaleFactor() <= 0) {
            return true;
        }
        this.startScaleFactorCumulative *= detector.getScaleFactor();
        if (this.rotationEnabled && !this.isInScale && Math.abs(this.startScaleFactorCumulative - 1) <= DELTA_SCALE) {
            return true;
        }
        this.isInScale = true;
        this.scaleFactorCumulative *= detector.getScaleFactor();
        if (!Parameters.FRACTIONAL_ZOOM) {
            this.mapView.getModel().mapViewPosition.setPivot(pivot);
        }
        this.mapView.getModel().mapViewPosition.setScaleFactorAdjustment(scaleFactorCumulative);

        if (Parameters.FRACTIONAL_ZOOM) {
            double zoomLevelOffset = Math.log(this.scaleFactorCumulative) / LOG_2;
            if (!Double.isNaN(zoomLevelOffset) && zoomLevelOffset != 0) {
                this.mapView.getModel().mapViewPosition.setZoom(Math.max(0, this.zoomLevelStart + zoomLevelOffset), false);
            }
        }
        return true;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        if (!this.scaleEnabled) {
            return false;
        }
        if (this.isInRotation) {
            return false;
        }

        this.scaleFactorCumulative = (float) (this.mapView.getModel().mapViewPosition.getScaleFactor()
                / Math.pow(2, this.mapView.getModel().mapViewPosition.getZoomLevel()));
        this.startScaleFactorCumulative = this.scaleFactorCumulative;
        this.zoomLevelStart = this.mapView.getModel().mapViewPosition.getZoomLevel();

        if (Parameters.FRACTIONAL_ZOOM) {
            this.mapView.onZoomEvent();
        } else {
            // Quick scale
            if (this.isInDoubleTap) {
                this.mapView.onZoomEvent();
                if (this.mapView.getOffsetX() != 0 || this.mapView.getOffsetY() != 0) {
                    this.mapView.onMoveEvent();
                    this.focusX = this.mapView.getWidth() * 0.5f + this.mapView.getOffsetX();
                    this.focusY = this.mapView.getHeight() * 0.5f + this.mapView.getOffsetY();
                    this.pivot = this.mapView.getMapViewProjection().fromPixels(focusX, focusY);
                } else {
                    this.pivot = null;
                }
            } else {
                this.mapView.onMoveEvent();
                this.mapView.onZoomEvent();
                this.focusX = detector.getFocusX();
                this.focusY = detector.getFocusY();
                this.pivot = this.mapView.getMapViewProjection().fromPixels(focusX, focusY);
            }
        }
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        if (!Parameters.FRACTIONAL_ZOOM) {
            double zoomLevelOffset = Math.log(this.scaleFactorCumulative) / LOG_2;
            if (!Double.isNaN(zoomLevelOffset) && zoomLevelOffset != 0) {
                byte zoomLevelDiff;
                if (Parameters.ELASTIC_ZOOM) {
                    zoomLevelDiff = (byte) Math.round(zoomLevelOffset);
                } else {
                    zoomLevelDiff = (byte) Math.round(zoomLevelOffset < 0 ? Math.floor(zoomLevelOffset) : Math.ceil(zoomLevelOffset));
                }

                MapViewPosition mapViewPosition = this.mapView.getModel().mapViewPosition;
                if (zoomLevelDiff != 0 && pivot != null) {
                    // Zoom with focus
                    double moveHorizontal = 0, moveVertical = 0;
                    Point center = this.mapView.getModel().mapViewDimension.getDimension().getCenter();
                    if (zoomLevelDiff > 0) {
                        // Zoom in
                        for (int i = 1; i <= zoomLevelDiff; i++) {
                            if (mapViewPosition.getZoomLevel() + i > mapViewPosition.getZoomLevelMax()) {
                                break;
                            }
                            moveHorizontal += (center.x - focusX + this.mapView.getOffsetX()) / Math.pow(2, i);
                            moveVertical += (center.y - focusY + this.mapView.getOffsetY()) / Math.pow(2, i);
                        }
                    } else {
                        // Zoom out
                        for (int i = -1; i >= zoomLevelDiff; i--) {
                            if (mapViewPosition.getZoomLevel() + i < mapViewPosition.getZoomLevelMin()) {
                                break;
                            }
                            moveHorizontal -= (center.x - focusX + this.mapView.getOffsetX()) / Math.pow(2, i + 1);
                            moveVertical -= (center.y - focusY + this.mapView.getOffsetY()) / Math.pow(2, i + 1);
                        }
                    }
                    mapViewPosition.setPivot(pivot);
                    if ((moveHorizontal != 0 || moveVertical != 0) && !Rotation.noRotation(this.mapView.getMapRotation())) {
                        Rotation mapRotation = new Rotation(this.mapView.getMapRotation().degrees, 0, 0);
                        Point rotated = mapRotation.rotate(moveHorizontal, moveVertical);
                        moveHorizontal = rotated.x;
                        moveVertical = rotated.y;
                    }
                    mapViewPosition.moveCenterAndZoom(moveHorizontal, moveVertical, zoomLevelDiff);
                } else {
                    // Zoom without focus
                    mapViewPosition.zoom(zoomLevelDiff);
                }
            }
        }

        this.isInDoubleTap = false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        if (!this.isInScale && !this.isInRotation && e1.getPointerCount() == 1 && e2.getPointerCount() == 1) {
            if (Parameters.LAYER_SCROLL_EVENT) {
                for (int i = this.mapView.getLayerManager().getLayers().size() - 1; i >= 0; --i) {
                    Layer layer = safeGet(this.mapView.getLayerManager().getLayers(), i);
                    if (layer == null) {
                        // Layers were modified in the meantime, stop processing outdated data
                        break;
                    }
                    if (layer.onScroll(e1.getX(), e1.getY(), e2.getX(), e2.getY())) {
                        return true;
                    }
                }
            }

            if ((distanceX != 0 || distanceY != 0) && !Rotation.noRotation(this.mapView.getMapRotation())) {
                Rotation mapRotation = new Rotation(this.mapView.getMapRotation().degrees, 0, 0);
                Point rotated = mapRotation.rotate(distanceX, distanceY);
                distanceX = (float) rotated.x;
                distanceY = (float) rotated.y;
            }

            this.mapView.onMoveEvent();
            this.mapView.getModel().mapViewPosition.moveCenter(-distanceX, -distanceY, false);
            return true;
        }
        return false;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        Point tapXY = new Point(e.getX(), e.getY());
        LatLong tapLatLong;
        Point offset = tapXY.offset(-this.mapView.getOffsetX(), -this.mapView.getOffsetY());
        if (!Rotation.noRotation(this.mapView.getMapRotation())) {
            Point rotated = this.mapView.getMapRotation().rotate(offset);
            tapLatLong = this.mapView.getMapViewProjection().fromPixels(rotated.x, rotated.y);
        } else {
            tapLatLong = this.mapView.getMapViewProjection().fromPixels(offset.x, offset.y);
        }
        if (tapLatLong != null) {
            for (int i = this.mapView.getLayerManager().getLayers().size() - 1; i >= 0; --i) {
                Layer layer = safeGet(this.mapView.getLayerManager().getLayers(), i);
                if (layer == null) {
                    // Layers were modified in the meantime, stop processing outdated data
                    break;
                }
                Point layerXY = this.mapView.getMapViewProjection().toPixels(layer.getPosition());
                if (layerXY != null) {
                    if (!Rotation.noRotation(this.mapView.getMapRotation())) {
                        layerXY = this.mapView.getMapRotation().rotate(layerXY, true);
                    }
                    layerXY = layerXY.offset(this.mapView.getOffsetX(), this.mapView.getOffsetY());
                }
                if (layer.onTap(tapLatLong, layerXY, tapXY)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (this.rotationEnabled && event.getPointerCount() == 2 && !this.isInScale) {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_POINTER_DOWN) {
                this.currentAngle = rotation(event);
                this.startAngle = this.currentAngle;
            }
            final float delta = rotation(event) - this.currentAngle;
            this.currentAngle += delta;
            if (!this.isInRotation && angleDifference(this.startAngle, this.currentAngle) < DELTA_ANGLE) {
                return false;
            }
            if (System.currentTimeMillis() - DELTA_TIME > this.lastTime) {
                this.isInRotation = true;
                this.lastTime = System.currentTimeMillis();
                this.mapView.rotate(new Rotation(this.mapView.getMapRotation().degrees + delta, this.mapView.getWidth() * 0.5f, this.mapView.getHeight() * 0.5f));
                this.mapView.getLayerManager().redrawLayers();
            }
        }
        return true;
    }

    private static float rotation(MotionEvent event) {
        double dx = event.getX(0) - event.getX(1);
        double dy = event.getY(0) - event.getY(1);
        return (float) Math.toDegrees(Math.atan2(dy, dx));
    }

    @Override
    public void run() {
        boolean flingerRunning = !this.flinger.isFinished() && this.flinger.computeScrollOffset();
        double moveHorizontal = this.flingLastX - this.flinger.getCurrX();
        double moveVertical = this.flingLastY - this.flinger.getCurrY();
        if ((moveHorizontal != 0 || moveVertical != 0) && !Rotation.noRotation(this.mapView.getMapRotation())) {
            Rotation mapRotation = new Rotation(this.mapView.getMapRotation().degrees, 0, 0);
            Point rotated = mapRotation.rotate(moveHorizontal, moveVertical);
            moveHorizontal = rotated.x;
            moveVertical = rotated.y;
        }
        this.mapView.getModel().mapViewPosition.moveCenter(moveHorizontal, moveVertical);
        this.flingLastX = this.flinger.getCurrX();
        this.flingLastY = this.flinger.getCurrY();
        if (flingerRunning) {
            this.handler.post(this);
        }
    }

    /**
     * Returns element of index from given layers or null if index is out of bounds.
     * This method is intended to be used in situations where existence of element
     * on position 'index' is unsure due to parallel usage.
     *
     * @param layers layers to get element from
     * @param index  index of element to get
     * @return element of layers or null if there is no such element
     */
    private static Layer safeGet(final Layers layers, final int index) {
        synchronized (layers) {
            return index >= 0 && index < layers.size() ? layers.get(index) : null;
        }
    }

    /**
     * Set state of double tap gestures:<br/>
     * - Double tap (zoom)
     */
    public void setDoubleTapEnabled(boolean doubleTapEnabled) {
        this.doubleTapEnabled = doubleTapEnabled;
    }

    public void setRotationEnabled(boolean rotationEnabled) {
        this.rotationEnabled = rotationEnabled;
    }

    /**
     * Set state of scale gestures:<br/>
     * - Scale<br/>
     * - Quick scale (double tap + swipe)<br/>
     * - Double tap (zoom)
     */
    public void setScaleEnabled(boolean scaleEnabled) {
        this.scaleEnabled = scaleEnabled;
    }
}
