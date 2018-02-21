/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2014-2018 devemux86
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
package org.mapsforge.map.android.layers;

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.overlay.Circle;
import org.mapsforge.map.layer.overlay.Marker;

/**
 * A {@link Layer} implementation to display the current location.
 * Needs to be added before requesting location updates, otherwise no DisplayModel is set.
 */
public class MyLocationOverlay extends Layer {

    private final Circle circle;
    private final Marker marker;

    /**
     * Constructs a new {@code MyLocationOverlay} without an accuracy circle.
     *
     * @param marker a marker to display at the current location
     */
    public MyLocationOverlay(Marker marker) {
        this(marker, null);
    }

    /**
     * Constructs a new {@code MyLocationOverlay} with an accuracy circle.
     *
     * @param marker a marker to display at the current location
     * @param circle a circle to show the location accuracy (can be null)
     */
    public MyLocationOverlay(Marker marker, Circle circle) {
        super();
        this.marker = marker;
        this.circle = circle;
    }

    @Override
    public synchronized void draw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point topLeftPoint) {
        if (this.circle != null) {
            this.circle.draw(boundingBox, zoomLevel, canvas, topLeftPoint);
        }
        this.marker.draw(boundingBox, zoomLevel, canvas, topLeftPoint);
    }

    @Override
    protected void onAdd() {
        if (this.circle != null) {
            this.circle.setDisplayModel(this.displayModel);
        }
        this.marker.setDisplayModel(this.displayModel);
    }

    @Override
    public void onDestroy() {
        this.marker.onDestroy();
    }

    public void setPosition(double latitude, double longitude, float accuracy) {
        synchronized (this) {
            LatLong latLong = new LatLong(latitude, longitude);
            this.marker.setLatLong(latLong);
            if (this.circle != null) {
                this.circle.setLatLong(latLong);
                this.circle.setRadius(accuracy);
            }
            requestRedraw();
        }
    }
}
