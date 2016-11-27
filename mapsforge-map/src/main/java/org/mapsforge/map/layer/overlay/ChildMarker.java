package org.mapsforge.map.layer.overlay;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Rectangle;
import org.mapsforge.core.util.MercatorProjection;

/**
 * A {@code Marker} draws a {@link Bitmap} at a given position to its parent. If polyPaintStroke ({@link Paint}) is set a line between this marker and its
 * parent will be drawn.
 * 
 * @author pickelm
 *
 */
public class ChildMarker extends Marker {

    /** The polyline paint stroke. To draw the line. If not, no line will be drawn. */
    private final Paint polyPaintStroke;
    /** The Id of the point in the group. */
    private int id;
    /** The horizontal offset of the group marker. */
    private int groupHOffset;
    /** The vertical offset of the group marker. */
    private int groupVOffset;
    /** The half group marker bitmap width. */
    private int groupBitmapHalfWidth;
    /** The half group marker bitmap height. */
    private int groupBitmapHalfHeight;
    /** The left (x) pixel delta between child and group. */
    protected int deltaLeft;
    /** The top (y) pixel delta between child and group. */
    protected int deltaTop;

    /**
     * The Constructor.
     * 
     * @param latLong the gps position of the marker.
     * @param bitmap the bitmap for the group marker.
     * @param horizontalOffset the horizontal offset for the group marker.
     * @param verticalOffset the vertical offset for the group marker.
     * @param polyPaintStroke the polyPaintStroke to set. If NULL, no polyline will be drawn.
     */
    public ChildMarker(final LatLong latLong, final Bitmap bitmap, final int horizontalOffset, final int verticalOffset,
            final Paint polyPaintStroke) {
        super(latLong, bitmap, horizontalOffset, verticalOffset);

        this.polyPaintStroke = polyPaintStroke;
    }

    /**
     * Setter.
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
        final double pixelX = MercatorProjection.longitudeToPixelX(this.latLong.longitude, mapSize);
        final double pixelY = MercatorProjection.latitudeToPixelY(this.latLong.latitude, mapSize);

        final int halfBitmapWidth = this.bitmap.getWidth() / 2;
        final int halfBitmapHeight = this.bitmap.getHeight() / 2;

        int leftGroup = (int) (pixelX - topLeftPoint.x - groupBitmapHalfWidth + groupHOffset);
        int topGroup = (int) (pixelY - topLeftPoint.y - groupBitmapHalfHeight + groupVOffset);

        // code for circle
        // final int left = (int) (this.radius * Math.cos(Math.toRadians(this.radians * this.id))
        // + this.groupMarker.getLeft()) + this.horizontalOffset;
        // final int top = (int) (this.radius * Math.sin(Math.toRadians(this.radians * this.id))
        // + this.groupMarker.getTop()) + this.verticalOffset;

        // calculate position on the spiral
        final double radius = 20d * Math.pow(this.id, 0.6d);
        final double theta = 1.5d * (Math.pow(this.id, 0.7d) - 1);
        final int left = (int) Math.round(radius * Math.cos(theta)) + leftGroup
                + this.horizontalOffset;
        final int top = (int) Math.round(radius * Math.sin(theta)) + topGroup + this.verticalOffset;

        final int right = left + this.bitmap.getWidth();
        final int bottom = top + this.bitmap.getHeight();

        final Rectangle bitmapRectangle = new Rectangle(left, top, right, bottom);
        final Rectangle canvasRectangle = new Rectangle(0, 0, canvas.getWidth(), canvas.getHeight());
        if (!canvasRectangle.intersects(bitmapRectangle)) {
            return;
        }

        // draw line between group center and this child marker before the bitmap
        if (this.polyPaintStroke != null) {
            canvas.drawLine(left + halfBitmapWidth, top + this.verticalOffset + halfBitmapHeight,
                    leftGroup + groupBitmapHalfWidth, topGroup
                            + groupVOffset + groupBitmapHalfHeight,
                    this.polyPaintStroke);
        }

        canvas.drawBitmap(this.bitmap, left, top);
        
        this.deltaLeft = left - leftGroup;
        this.deltaTop = top - topGroup;
    }
    
    

    /**
     * Initialise. Set group marker parameter. To know index and calculate position on spiral.
     * @param index the index of this child marker.
     * @param bitmap the bitmap of the group marker
     * @param verticalOffset the vertical offset of the group marker
     * @param horizontalOffset the horizontal offset of the group marker
     */
    public void init(int index, Bitmap bitmap, int verticalOffset, int horizontalOffset) {
        id = index;
        
        groupBitmapHalfHeight = bitmap.getHeight() /2;
        groupBitmapHalfWidth = bitmap.getWidth() /2;
        groupVOffset = verticalOffset;
        groupHOffset = horizontalOffset;
    }
    
    /**
     * Getter.
     * @return the (x) delta to parent group marker.
     */
    public int getDeltaLeftToGroup(){
        return deltaLeft;
    }
    
    /**
     * Getter.
     * @return the (y) delta to parent group marker. 
     */
    public int getDeltaTopToGroup(){
        return deltaTop;
    }

}
