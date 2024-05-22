/*
 * Copyright 2016 mapicke
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
package org.mapsforge.samples.android.group;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.model.*;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.layer.overlay.Marker;

/**
 * A child marker of a {@link GroupMarker}.
 */
public class ChildMarker extends Marker {

    /**
     * The pixel delta between child and group.
     */
    protected int deltaLeft, deltaTop;
    /**
     * The group marker half bitmap size.
     */
    private int groupBitmapHalfWidth, groupBitmapHalfHeight;
    /**
     * The offset of the group marker.
     */
    private int groupHOffset, groupVOffset;
    /**
     * The index of the point in the group.
     */
    private int index;
    /**
     * The polyline paint stroke to draw the line. If NULL no line will be drawn.
     */
    private final Paint polyPaintStroke;

    /**
     * @param latLong          the location of the marker.
     * @param bitmap           the bitmap for the group marker.
     * @param horizontalOffset the horizontal offset for the group marker.
     * @param verticalOffset   the vertical offset for the group marker.
     * @param polyPaintStroke  the polyPaintStroke to set, if NULL no line between this marker and its parent will be drawn.
     */
    public ChildMarker(LatLong latLong, Bitmap bitmap, int horizontalOffset, int verticalOffset, Paint polyPaintStroke) {
        super(latLong, bitmap, horizontalOffset, verticalOffset);

        this.polyPaintStroke = polyPaintStroke;
    }

    @Override
    public void draw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point topLeftPoint, Rotation rotation) {
        if (this.getLatLong() == null || this.getBitmap() == null || this.getBitmap().isDestroyed()) {
            return;
        }

        long mapSize = MercatorProjection.getMapSize(zoomLevel, this.displayModel.getTileSize());
        double pixelX = MercatorProjection.longitudeToPixelX(this.getLatLong().longitude, mapSize);
        double pixelY = MercatorProjection.latitudeToPixelY(this.getLatLong().latitude, mapSize);

        int halfBitmapWidth = this.getBitmap().getWidth() / 2;
        int halfBitmapHeight = this.getBitmap().getHeight() / 2;

        int leftGroup = (int) (pixelX - topLeftPoint.x - groupBitmapHalfWidth + groupHOffset);
        int topGroup = (int) (pixelY - topLeftPoint.y - groupBitmapHalfHeight + groupVOffset);

        // code for circle
        // int left = (int) (this.radius * Math.cos(Math.toRadians(this.radians * this.id))
        // + this.groupMarker.getLeft()) + this.horizontalOffset;
        // int top = (int) (this.radius * Math.sin(Math.toRadians(this.radians * this.id))
        // + this.groupMarker.getTop()) + this.verticalOffset;

        // calculate position on the spiral
        double radius = 20d * Math.pow(this.index, 0.6d);
        double theta = 1.5d * (Math.pow(this.index, 0.7d) - 1);
        int left = (int) Math.round(radius * Math.cos(theta)) + leftGroup + this.getHorizontalOffset();
        int top = (int) Math.round(radius * Math.sin(theta)) + topGroup + this.getVerticalOffset();

        int right = left + this.getBitmap().getWidth();
        int bottom = top + this.getBitmap().getHeight();

        Rectangle bitmapRectangle = new Rectangle(left, top, right, bottom);
        Rectangle canvasRectangle = new Rectangle(0, 0, canvas.getWidth(), canvas.getHeight());
        if (!canvasRectangle.intersects(bitmapRectangle)) {
            return;
        }

        // draw line between group center and this child marker before the bitmap
        if (this.polyPaintStroke != null) {
            canvas.drawLine(left + halfBitmapWidth, top + this.getVerticalOffset() + halfBitmapHeight,
                    leftGroup + groupBitmapHalfWidth, topGroup + groupVOffset + groupBitmapHalfHeight,
                    this.polyPaintStroke);
        }

        canvas.drawBitmap(this.getBitmap(), left, top);

        this.deltaLeft = left - leftGroup;
        this.deltaTop = top - topGroup;
    }

    /**
     * Set group marker parameter. To know index and calculate position on spiral.
     *
     * @param index            the index of this child marker.
     * @param bitmap           the bitmap of the group marker.
     * @param horizontalOffset the horizontal offset of the group marker.
     * @param verticalOffset   the vertical offset of the group marker.
     */
    public void init(int index, Bitmap bitmap, int horizontalOffset, int verticalOffset) {
        this.index = index;

        this.groupBitmapHalfHeight = bitmap.getHeight() / 2;
        this.groupBitmapHalfWidth = bitmap.getWidth() / 2;
        this.groupHOffset = horizontalOffset;
        this.groupVOffset = verticalOffset;
    }

    /**
     * @return the (x) delta to parent group marker.
     */
    public int getDeltaLeftToGroup() {
        return deltaLeft;
    }

    /**
     * @return the (y) delta to parent group marker.
     */
    public int getDeltaTopToGroup() {
        return deltaTop;
    }

    /**
     * @param index the index of this child marker for the parent.
     */
    public void setIndex(int index) {
        this.index = index;
    }
}
