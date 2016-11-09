package org.mapsforge.map.layer.overlay;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Rectangle;
import org.mapsforge.core.util.LatLongUtils;
import org.mapsforge.core.util.MercatorProjection;

/**
 * A {@code Marker} draws a {@link Bitmap} at a given position to its parent. If polyPaintStroke ({@link Paint}) is set a line between this marker and its
 * parent will be drawn.
 * 
 * @author pickelm
 *
 */
public class ChildMarker extends Marker {

    /** The parent group marker. */
    private final GroupMarker groupMarker;
    /** The polyline paint stroke. To draw the line. If not, no line will be drawn. */
    private final Paint polyPaintStroke;
    /** The Id of the point in the group. */
    private int id;

    /**
     * The Constructor.
     * 
     * @param bitmap the bitmap for the group marker.
     * @param horizontalOffset the horizontal offset for the group marker.
     * @param verticalOffset the vertical offset for the group marker.
     * @param polyPaintStroke the polyPaintStroke to set. If NULL, no polyline will be drawn.
     */
    public ChildMarker(final Bitmap bitmap, final int horizontalOffset, final int verticalOffset,
            final GroupMarker parentMarker, final Paint polyPaintStroke) {
        super(new LatLong(LatLongUtils.LATITUDE_MIN, LatLongUtils.LONGITUDE_MIN), bitmap, horizontalOffset, verticalOffset);

        this.groupMarker = parentMarker;
        this.polyPaintStroke = polyPaintStroke;
    }

    /**
     * Setter.
     * 
     * @param index the index of this child marker for the parent.
     */
    public void setPosition(final int index) {
        this.id = index;
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
        MercatorProjection.longitudeToPixelX(this.latLong.longitude, mapSize);
        MercatorProjection.latitudeToPixelY(this.latLong.latitude, mapSize);

        // code for circle
        // final int left = (int) (this.radius * Math.cos(Math.toRadians(this.radians * this.id))
        // + this.groupMarker.getLeft()) + this.horizontalOffset;
        // final int top = (int) (this.radius * Math.sin(Math.toRadians(this.radians * this.id))
        // + this.groupMarker.getTop()) + this.verticalOffset;

        // calculate position on the spiral
        final double radius = 20d * Math.pow(this.id, 0.6d);
        final double theta = 1.5d * (Math.pow(this.id, 0.7d) - 1);
        final int left = (int) Math.round(radius * Math.cos(theta)) + this.groupMarker.getLeft()
                + this.horizontalOffset;
        final int top = (int) Math.round(radius * Math.sin(theta)) + this.groupMarker.getTop() + this.verticalOffset;

        final int right = left + this.bitmap.getWidth();
        final int bottom = top + this.bitmap.getHeight();

        final Rectangle bitmapRectangle = new Rectangle(left, top, right, bottom);
        final Rectangle canvasRectangle = new Rectangle(0, 0, canvas.getWidth(), canvas.getHeight());
        if (!canvasRectangle.intersects(bitmapRectangle)) {
            return;
        }

        // draw line between group center and this child marker before the bitmap
        if (this.polyPaintStroke != null) {
            canvas.drawLine(left + this.bitmap.getWidth() / 2, top + this.verticalOffset + this.bitmap.getHeight() / 2,
                    this.groupMarker.getLeft() + this.groupMarker.getBitmap().getWidth() / 2, this.groupMarker.getTop()
                            + this.groupMarker.getVerticalOffset() + this.groupMarker.getBitmap().getHeight() / 2,
                    this.polyPaintStroke);
        }

        canvas.drawBitmap(this.bitmap, left, top);
    }

}
