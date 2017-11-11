package org.mapsforge.map.layer.overlay;

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Path;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.layer.Layer;

/**
 * A special polygon with just 4 cornerrs. Always cloased.
 * Created by Mike on 11/3/2017.
 */

public class Rectangle extends Layer {

    private final boolean keepAligned;
    private Paint paintFill;
    private Paint paintStroke;
    private Path path;
    private double latitudeBottom;
    private double latitudeTop;
    private double longitudeLeft;
    private double longitudeRight;

    /**
     * @param paintFill      the initial {@code Paint} used to fill this polygon (may be null).
     * @param paintStroke    the initial {@code Paint} used to stroke this polygon (may be null).
     * @param graphicFactory the GraphicFactory
     */
    public Rectangle(Paint paintFill, Paint paintStroke, GraphicFactory graphicFactory) {
        this(paintFill, paintStroke, graphicFactory, false);
    }

    /**
     * @param paintFill      the initial {@code Paint} used to fill this polygon (may be null).
     * @param paintStroke    the initial {@code Paint} used to stroke this polygon (may be null).
     * @param graphicFactory the GraphicFactory
     * @param keepAligned    if set to true it will keep the bitmap aligned with the map,
     *                       to avoid a moving effect of a bitmap shader.
     */
    public Rectangle(Paint paintFill, Paint paintStroke, GraphicFactory graphicFactory, boolean keepAligned) {
        super();
        this.keepAligned = keepAligned;
        this.paintFill = paintFill;
        this.paintStroke = paintStroke;
        //this.graphicFactory = graphicFactory;
        path = graphicFactory.createPath();
    }

    public synchronized boolean contains(LatLong tapLatLong) {
        return tapLatLong.getLatitude() > latitudeBottom && tapLatLong.getLatitude() < latitudeTop
                && tapLatLong.getLongitude() > longitudeLeft && tapLatLong.getLongitude() < longitudeRight;
    }

    public void setBoundary(double latitudeBottom, double longitudeLeft, double latitudeTop, double longitudeRight) {
        this.latitudeBottom = latitudeBottom;
        this.longitudeLeft = longitudeLeft;
        this.latitudeTop = latitudeTop;
        this.longitudeRight = longitudeRight;
    }

    public double getLatitudeBottom() {
        return latitudeBottom;
    }

    public double getLatitudeTop() {
        return latitudeTop;
    }

    public double getLongitudeLeft() {
        return longitudeLeft;
    }

    public double getLongitudeRight() {
        return longitudeRight;
    }

    @Override
    public synchronized void draw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point topLeftPoint) {
        if (this.paintStroke == null && this.paintFill == null) {
            return;
        }

        path.clear();
        long mapSize = MercatorProjection.getMapSize(zoomLevel, displayModel.getTileSize());
        float xl = (float) (MercatorProjection.longitudeToPixelX(longitudeLeft, mapSize) - topLeftPoint.x);
        float yb = (float) (MercatorProjection.latitudeToPixelY(latitudeBottom, mapSize) - topLeftPoint.y);
        float xr = (float) (MercatorProjection.longitudeToPixelX(longitudeRight, mapSize) - topLeftPoint.x);
        float yt = (float) (MercatorProjection.latitudeToPixelY(latitudeTop, mapSize) - topLeftPoint.y);

        path.moveTo(xl, yb);
        path.lineTo(xr, yb);
        path.lineTo(xr, yt);
        path.lineTo(xl, yt);
        path.lineTo(xl, yb);

        if (this.paintStroke != null) {
            if (this.keepAligned) {
                this.paintStroke.setBitmapShaderShift(topLeftPoint);
            }
            canvas.drawPath(path, this.paintStroke);
        }
        if (this.paintFill != null) {
            if (this.keepAligned) {
                this.paintFill.setBitmapShaderShift(topLeftPoint);
            }

            canvas.drawPath(path, this.paintFill);
        }
    }

    /**
     * @return the {@code Paint} used to fill this polygon (may be null).
     */
    public synchronized Paint getPaintFill() {
        return this.paintFill;
    }

    /**
     * @return the {@code Paint} used to stroke this polygon (may be null).
     */
    public synchronized Paint getPaintStroke() {
        return this.paintStroke;
    }

    /**
     * @return true if it keeps the bitmap aligned with the map, to avoid a
     * moving effect of a bitmap shader, false otherwise.
     */
    public boolean isKeepAligned() {
        return keepAligned;
    }

    /**
     * @param paintFill the new {@code Paint} used to fill this polygon (may be null).
     */
    public synchronized void setPaintFill(Paint paintFill) {
        this.paintFill = paintFill;
    }

    /**
     * @param paintStroke the new {@code Paint} used to stroke this polygon (may be null).
     */
    public synchronized void setPaintStroke(Paint paintStroke) {
        this.paintStroke = paintStroke;
    }

}
