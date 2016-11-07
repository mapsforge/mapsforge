package org.mapsforge.map.layer.overlay;

import java.util.ArrayList;
import java.util.List;

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

/**
 * A {@code Marker} draws a {@link Bitmap} at a given geographical position. Marker to show that multiple pois are on same location. On click this markers
 * behind are shown.
 * 
 * @author pickelm
 *
 */
public class GroupMarker extends Marker {

    /** TRUE if group is expanded, FALSE if not. */
    private boolean expanded;

    /** The list with all child marker of this location. */
    private final List<ChildMarker> childs = new ArrayList<>();

    /** X coordinates. */
    private int left;
    /** Y coordinates. */
    private int top;

    /** A list with all layers. */
    private final Layers layers;
    /** The paint object for the text. */
    private final Paint paintText;

    /**
     * The Constructor.
     * 
     * @param latLong
     *            the gps position of the marker.
     * @param bitmap
     *            the bitmap for the group marker.
     * @param horizontalOffset
     *            the horizontal offset for the group marker.
     * @param verticalOffset
     *            the vertical offset for the group marker.
     * @param layers
     *            the layers object with all layers.
     * @param paintText
     *            the paint object for the text. If NULL no text inside bitmap will shown.
     */
    public GroupMarker(final LatLong latLong, final Bitmap bitmap, final int horizontalOffset, final int verticalOffset,
            final Layers layers, final Paint paintText) {
        super(latLong, bitmap, horizontalOffset, verticalOffset);

        this.layers = layers;
        this.paintText = paintText;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mapsforge.map.layer.Layer#draw(org.mapsforge.core.model.BoundingBox, byte, org.mapsforge.core.graphics.Canvas, org.mapsforge.core.model.Point)
     */
    @Override
    public void draw(final BoundingBox boundingBox, final byte zoomLevel, final Canvas canvas,
            final Point topLeftPoint) {
        if (this.latLong == null || this.bitmap == null || this.bitmap.isDestroyed()) {
            return;
        }

        final long mapSize = MercatorProjection.getMapSize(zoomLevel, this.displayModel.getTileSize());
        final double pixelX = MercatorProjection.longitudeToPixelX(this.latLong.longitude, mapSize);
        final double pixelY = MercatorProjection.latitudeToPixelY(this.latLong.latitude, mapSize);

        final int halfBitmapWidth = this.bitmap.getWidth() / 2;
        final int halfBitmapHeight = this.bitmap.getHeight() / 2;

        this.left = (int) (pixelX - topLeftPoint.x - halfBitmapWidth + this.horizontalOffset);
        this.top = (int) (pixelY - topLeftPoint.y - halfBitmapHeight + this.verticalOffset);
        final int right = this.left + this.bitmap.getWidth();
        final int bottom = this.top + this.bitmap.getHeight();

        final Rectangle bitmapRectangle = new Rectangle(this.left, this.top, right, bottom);
        final Rectangle canvasRectangle = new Rectangle(0, 0, canvas.getWidth(), canvas.getHeight());
        if (!canvasRectangle.intersects(bitmapRectangle)) {
            return;
        }

        canvas.drawBitmap(this.bitmap, this.left, this.top);

        if (this.paintText != null) {
            final String text = String.valueOf(this.childs.size());
            canvas.drawText(text, this.left + halfBitmapWidth - 5, this.top + halfBitmapHeight + 5, this.paintText);
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mapsforge.map.layer.Layer#onTap(org.mapsforge.core.model.LatLong, org.mapsforge.core.model.Point, org.mapsforge.core.model.Point)
     */
    @Override
    public boolean onTap(final LatLong tapLatLong, final Point layerXY, final Point tapXY) {
        this.expanded = !this.expanded;

        final double centerX = layerXY.x + this.getHorizontalOffset();
        final double centerY = layerXY.y + this.getVerticalOffset();

        final double radiusX = this.getBitmap().getWidth() / 2;
        final double radiusY = this.getBitmap().getHeight() / 2;

        final double distX = Math.abs(centerX - tapXY.x);
        final double distY = Math.abs(centerY - tapXY.y);

        if (distX < radiusX && distY < radiusY) {
            if (this.expanded) {

                // remove all child marker
                for (final Layer elt : this.layers) {
                    if (elt instanceof ChildMarker) {
                        this.layers.remove(elt);
                    }
                }

                // begin with (n). than the child marker will be over the line.
                int i = this.childs.size();
                for (final ChildMarker marker : this.childs) {
                    marker.setPosition(i);
                    // add child to layer
                    this.layers.add(marker);
                    i--;
                }
            } else {
                // remove all child layers
                for (final ChildMarker childMarker : this.childs) {
                    this.layers.remove(childMarker);
                }
            }
            return true;
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mapsforge.map.layer.Layer#setVisible(boolean, boolean)
     */
    @Override
    public void setVisible(final boolean visible, final boolean redraw) {
        for (final ChildMarker childMarker : this.childs) {
            childMarker.setVisible(visible, false);
        }
        super.setVisible(visible, redraw);

    }

    /**
     * Getter.
     * 
     * @return the left
     */
    public int getLeft() {
        return this.left;
    }

    /**
     * Getter.
     * 
     * @return the top
     */
    public int getTop() {
        return this.top;
    }

    /**
     * Getter.
     * 
     * @return The list with children.
     */
    public List<ChildMarker> getChildren() {
        return this.childs;
    }

}
