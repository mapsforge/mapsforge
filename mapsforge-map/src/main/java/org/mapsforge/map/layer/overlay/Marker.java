/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2016-2017 devemux86
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
package org.mapsforge.map.layer.overlay;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.model.*;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.view.MapView;

/**
 * A {@code Marker} draws a {@link Bitmap} at a given geographical position.
 */
public class Marker extends Layer {
    private boolean billboard = true;
    private Bitmap bitmap;
    private int horizontalOffset;
    private LatLong latLong;
    private int verticalOffset;

    /**
     * @param latLong          the initial geographical coordinates of this marker (may be null).
     * @param bitmap           the initial {@code Bitmap} of this marker (may be null).
     * @param horizontalOffset the horizontal marker offset.
     * @param verticalOffset   the vertical marker offset.
     */
    public Marker(LatLong latLong, Bitmap bitmap, int horizontalOffset, int verticalOffset) {
        super();

        this.latLong = latLong;
        this.bitmap = bitmap;
        this.horizontalOffset = horizontalOffset;
        this.verticalOffset = verticalOffset;
    }

    public synchronized boolean contains(Point center, Point point, MapView mapView) {
        double scaleFactor = Math.pow(2, mapView.getModel().mapViewPosition.getZoom())
                / Math.pow(2, mapView.getModel().mapViewPosition.getZoomLevel());
        // Touch min 20x20 px at baseline mdpi (160dpi)
        double width = Math.max(20 * this.displayModel.getScaleFactor(), this.bitmap.getWidth() * scaleFactor);
        double height = Math.max(20 * this.displayModel.getScaleFactor(), this.bitmap.getHeight() * scaleFactor);
        Rectangle r = new Rectangle(
                center.x - width / 2 + this.horizontalOffset * scaleFactor,
                center.y - height / 2 + this.verticalOffset * scaleFactor,
                center.x + width / 2 + this.horizontalOffset * scaleFactor,
                center.y + height / 2 + this.verticalOffset * scaleFactor);
        return r.contains(point);
    }

    @Override
    public synchronized void draw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point topLeftPoint, Rotation rotation) {
        if (this.latLong == null || this.bitmap == null || this.bitmap.isDestroyed()) {
            return;
        }

        long mapSize = MercatorProjection.getMapSize(zoomLevel, this.displayModel.getTileSize());
        double pixelX = MercatorProjection.longitudeToPixelX(this.latLong.longitude, mapSize);
        double pixelY = MercatorProjection.latitudeToPixelY(this.latLong.latitude, mapSize);

        int halfBitmapWidth = this.bitmap.getWidth() / 2;
        int halfBitmapHeight = this.bitmap.getHeight() / 2;

        int left = (int) (pixelX - topLeftPoint.x - halfBitmapWidth + this.horizontalOffset);
        int top = (int) (pixelY - topLeftPoint.y - halfBitmapHeight + this.verticalOffset);
        int right = left + this.bitmap.getWidth();
        int bottom = top + this.bitmap.getHeight();

        Rectangle bitmapRectangle = new Rectangle(left, top, right, bottom);
        Rectangle canvasRectangle = new Rectangle(0, 0, canvas.getWidth(), canvas.getHeight());
        if (!canvasRectangle.intersects(bitmapRectangle)) {
            return;
        }

        if (billboard && !Rotation.noRotation(rotation)) {
            canvas.rotate(new Rotation(-rotation.degrees, (float) (pixelX - topLeftPoint.x), (float) (pixelY - topLeftPoint.y)));
        }
        canvas.drawBitmap(this.bitmap, left, top);
        if (billboard && !Rotation.noRotation(rotation)) {
            canvas.rotate(new Rotation(rotation.degrees, (float) (pixelX - topLeftPoint.x), (float) (pixelY - topLeftPoint.y)));
        }
    }

    public synchronized boolean isBillboard() {
        return this.billboard;
    }

    /**
     * @return the {@code Bitmap} of this marker (may be null).
     */
    public synchronized Bitmap getBitmap() {
        return this.bitmap;
    }

    /**
     * @return the horizontal offset of this marker.
     */
    public synchronized int getHorizontalOffset() {
        return this.horizontalOffset;
    }

    /**
     * @return the geographical coordinates of this marker (may be null).
     */
    public synchronized LatLong getLatLong() {
        return this.latLong;
    }

    /**
     * @return Gets the LatLong Position of the Object
     */
    @Override
    public synchronized LatLong getPosition() {
        return this.latLong;
    }

    /**
     * @return the vertical offset of this marker.
     */
    public synchronized int getVerticalOffset() {
        return this.verticalOffset;
    }

    @Override
    public synchronized void onDestroy() {
        if (this.bitmap != null) {
            this.bitmap.decrementRefCount();
        }
    }

    public synchronized void setBillboard(boolean billboard) {
        this.billboard = billboard;
    }

    /**
     * @param bitmap the new {@code Bitmap} of this marker (may be null).
     */
    public synchronized void setBitmap(Bitmap bitmap) {
        if (this.bitmap != null && this.bitmap.equals(bitmap)) {
            return;
        }
        if (this.bitmap != null) {
            this.bitmap.decrementRefCount();
        }
        this.bitmap = bitmap;
    }

    /**
     * @param horizontalOffset the new horizontal offset of this marker.
     */
    public synchronized void setHorizontalOffset(int horizontalOffset) {
        this.horizontalOffset = horizontalOffset;
    }

    /**
     * @param latLong the new geographical coordinates of this marker (may be null).
     */
    public synchronized void setLatLong(LatLong latLong) {
        this.latLong = latLong;
    }

    /**
     * @param verticalOffset the new vertical offset of this marker.
     */
    public synchronized void setVerticalOffset(int verticalOffset) {
        this.verticalOffset = verticalOffset;
    }

}
