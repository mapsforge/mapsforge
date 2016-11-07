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
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Rectangle;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.Layers;
import org.mapsforge.map.layer.overlay.Marker;

import java.util.ArrayList;
import java.util.List;

/**
 * A group marker contains {@link ChildMarker}s on same location.
 */
public class GroupMarker extends Marker {

    /**
     * The list with all child markers on this location.
     */
    private final List<ChildMarker> children = new ArrayList<>();
    /**
     * TRUE if group is expanded, FALSE if not.
     */
    private boolean expanded;
    /**
     * A list with all layers.
     */
    private final Layers layers;
    /**
     * The paint object for the text.
     */
    private final Paint paintText;

    /**
     * @param latLong          the location of the marker.
     * @param bitmap           the bitmap for the group marker.
     * @param horizontalOffset the horizontal offset for the group marker.
     * @param verticalOffset   the vertical offset for the group marker.
     * @param layers           the layers object with all layers.
     * @param paintText        the paint object for the text, if NULL no text inside bitmap will shown.
     */
    public GroupMarker(LatLong latLong, Bitmap bitmap, int horizontalOffset, int verticalOffset, Layers layers, Paint paintText) {
        super(latLong, bitmap, horizontalOffset, verticalOffset);

        this.layers = layers;
        this.paintText = paintText;
    }

    @Override
    public void draw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point topLeftPoint) {
        if (this.getLatLong() == null || this.getBitmap() == null || this.getBitmap().isDestroyed()) {
            return;
        }

        long mapSize = MercatorProjection.getMapSize(zoomLevel, this.displayModel.getTileSize());
        double pixelX = MercatorProjection.longitudeToPixelX(this.getLatLong().longitude, mapSize);
        double pixelY = MercatorProjection.latitudeToPixelY(this.getLatLong().latitude, mapSize);

        int halfBitmapWidth = this.getBitmap().getWidth() / 2;
        int halfBitmapHeight = this.getBitmap().getHeight() / 2;

        int left = (int) (pixelX - topLeftPoint.x - halfBitmapWidth + this.getHorizontalOffset());
        int top = (int) (pixelY - topLeftPoint.y - halfBitmapHeight + this.getVerticalOffset());
        int right = left + this.getBitmap().getWidth();
        int bottom = top + this.getBitmap().getHeight();

        Rectangle bitmapRectangle = new Rectangle(left, top, right, bottom);
        Rectangle canvasRectangle = new Rectangle(0, 0, canvas.getWidth(), canvas.getHeight());
        if (!canvasRectangle.intersects(bitmapRectangle)) {
            return;
        }

        canvas.drawBitmap(this.getBitmap(), left, top);

        if (this.paintText != null) {
            String text = String.valueOf(this.children.size());
            canvas.drawText(text, left + halfBitmapWidth - 5, top + halfBitmapHeight + 5, this.paintText);
        }
    }

    /**
     * @return The list with child markers.
     */
    public List<ChildMarker> getChildren() {
        return this.children;
    }

    /**
     * Click on group marker shows all children on a spiral.
     */
    @Override
    public boolean onTap(LatLong tapLatLong, Point layerXY, Point tapXY) {
        this.expanded = !this.expanded;

        double centerX = layerXY.x + this.getHorizontalOffset();
        double centerY = layerXY.y + this.getVerticalOffset();

        double radiusX = this.getBitmap().getWidth() / 2;
        double radiusY = this.getBitmap().getHeight() / 2;

        double distX = Math.abs(centerX - tapXY.x);
        double distY = Math.abs(centerY - tapXY.y);

        if (distX < radiusX && distY < radiusY) {
            if (this.expanded) {
                // remove all child markers
                for (Layer elt : this.layers) {
                    if (elt instanceof ChildMarker) {
                        this.layers.remove(elt);
                    }
                }

                // begin with (n). than the child marker will be over the line.
                int i = this.children.size();
                for (ChildMarker marker : this.children) {
                    marker.init(i, getBitmap(), getHorizontalOffset(), getVerticalOffset());
                    // add child to layer
                    this.layers.add(marker);
                    i--;
                }
            } else {
                // remove all child layers
                for (ChildMarker childMarker : this.children) {
                    this.layers.remove(childMarker);
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public void setVisible(boolean visible, boolean redraw) {
        for (ChildMarker childMarker : this.children) {
            childMarker.setVisible(visible, false);
        }
        super.setVisible(visible, redraw);
    }
}
