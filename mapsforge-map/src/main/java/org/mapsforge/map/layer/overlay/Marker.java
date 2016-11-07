/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
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
package org.mapsforge.map.layer.overlay;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Rectangle;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.layer.Layer;

/**
 * A {@code Marker} draws a {@link Bitmap} at a given geographical position.
 */
public class Marker extends Layer {
    protected Bitmap bitmap;
    protected int horizontalOffset;
    protected LatLong latLong;
    protected int verticalOffset;

    /**
     * @param latLong  the initial geographical coordinates of this marker (may be null).
     * @param bitmap the initial {@code Bitmap} of this marker (may be null).
     * @param horizontalOffset the horizontal marker offset.
     * @param verticalOffset the vertical marker offset.
     */
    public Marker(final LatLong latLong, final Bitmap bitmap, final int horizontalOffset, final int verticalOffset) {
        super();

        this.latLong = latLong;
        this.bitmap = bitmap;
        this.horizontalOffset = horizontalOffset;
        this.verticalOffset = verticalOffset;
    }

    public synchronized boolean contains(final Point center, final Point point) {
        final Rectangle r = new Rectangle(center.x - (float) this.bitmap.getWidth() / 2 + this.horizontalOffset,
                center.y - (float) this.bitmap.getHeight() / 2 + this.verticalOffset,
                center.x + (float) this.bitmap.getWidth() / 2 + this.horizontalOffset,
                center.y + (float) this.bitmap.getHeight() / 2 + this.verticalOffset);
        return r.contains(point);
    }

    @Override
    public synchronized void draw(final BoundingBox boundingBox, final byte zoomLevel, final Canvas canvas,
            final Point topLeftPoint) {
        if (this.latLong == null || this.bitmap == null || this.bitmap.isDestroyed()) {
            return;
        }

        final long mapSize = MercatorProjection.getMapSize(zoomLevel, this.displayModel.getTileSize());
        final double pixelX = MercatorProjection.longitudeToPixelX(this.latLong.longitude, mapSize);
        final double pixelY = MercatorProjection.latitudeToPixelY(this.latLong.latitude, mapSize);

        final int halfBitmapWidth = this.bitmap.getWidth() / 2;
        final int halfBitmapHeight = this.bitmap.getHeight() / 2;

        final int left = (int) (pixelX - topLeftPoint.x - halfBitmapWidth + this.horizontalOffset);
        final int top = (int) (pixelY - topLeftPoint.y - halfBitmapHeight + this.verticalOffset);
        final int right = left + this.bitmap.getWidth();
        final int bottom = top + this.bitmap.getHeight();

        final Rectangle bitmapRectangle = new Rectangle(left, top, right, bottom);
        final Rectangle canvasRectangle = new Rectangle(0, 0, canvas.getWidth(), canvas.getHeight());
        if (!canvasRectangle.intersects(bitmapRectangle)) {
            return;
        }

        canvas.drawBitmap(this.bitmap, left, top);
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

    /**
     * @param bitmap
     *            the new {@code Bitmap} of this marker (may be null).
     */
    public synchronized void setBitmap(final Bitmap bitmap) {
        if (this.bitmap != null && this.bitmap.equals(bitmap)) {
            return;
        }
        if (this.bitmap != null) {
            this.bitmap.decrementRefCount();
        }
        this.bitmap = bitmap;
    }

    /**
     * @param horizontalOffset
     *            the new horizontal offset of this marker.
     */
    public synchronized void setHorizontalOffset(final int horizontalOffset) {
        this.horizontalOffset = horizontalOffset;
    }

    /**
     * @param latLong
     *            the new geographical coordinates of this marker (may be null).
     */
    public synchronized void setLatLong(final LatLong latLong) {
        this.latLong = latLong;
    }

    /**
     * @param verticalOffset
     *            the new vertical offset of this marker.
     */
    public synchronized void setVerticalOffset(final int verticalOffset) {
        this.verticalOffset = verticalOffset;
    }

}
