/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2015-2017 devemux86
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

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Path;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.util.LatLongUtils;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.util.MapViewProjection;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A {@code Polyline} draws a connected series of line segments specified by a list of {@link LatLong LatLongs}.
 * <p/>
 * A {@code Polyline} holds a {@link Paint} object which defines drawing parameters such as color, stroke width, pattern
 * and transparency.
 */
public class Polyline extends Layer {

    //private final GraphicFactory graphicFactory;
    private final boolean keepAligned;
    private final List<LatLong> latLongs = new CopyOnWriteArrayList<LatLong>();
    private Paint paintStroke;
    private Path path;

    /**
     * @param paintStroke    the initial {@code Paint} used to stroke this polyline (may be null).
     * @param graphicFactory the GraphicFactory
     */
    public Polyline(Paint paintStroke, GraphicFactory graphicFactory) {
        this(paintStroke, graphicFactory, false);
    }

    /**
     * @param paintStroke    the initial {@code Paint} used to stroke this polyline (may be null).
     * @param graphicFactory the GraphicFactory
     * @param keepAligned    if set to true it will keep the bitmap aligned with the map,
     *                       to avoid a moving effect of a bitmap shader.
     */
    public Polyline(Paint paintStroke, GraphicFactory graphicFactory, boolean keepAligned) {
        super();

        this.keepAligned = keepAligned;
        this.paintStroke = paintStroke;
        this.path = graphicFactory.createPath();
    }

    public synchronized boolean contains(Point tapXY, MapViewProjection mapViewProjection) {
        // Touch min 20 px at baseline mdpi (160dpi)
        double distance = Math.max(20 / 2 * this.displayModel.getScaleFactor(),
                this.paintStroke.getStrokeWidth() / 2);
        Point point2 = null;
        for (int i = 0; i < this.latLongs.size() - 1; i++) {
            Point point1 = i == 0 ? mapViewProjection.toPixels(this.latLongs.get(i)) : point2;
            point2 = mapViewProjection.toPixels(this.latLongs.get(i + 1));
            if (LatLongUtils.distanceSegmentPoint(point1.x, point1.y, point2.x, point2.y, tapXY.x, tapXY.y) <= distance) {
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized void draw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point topLeftPoint) {
        if (this.latLongs.isEmpty() || this.paintStroke == null) {
            return;
        }

        Iterator<LatLong> iterator = this.latLongs.iterator();
        if (!iterator.hasNext()) {
            return;
        }

        LatLong latLong = iterator.next();
        long mapSize = MercatorProjection.getMapSize(zoomLevel, displayModel.getTileSize());
        float x = (float) (MercatorProjection.longitudeToPixelX(latLong.longitude, mapSize) - topLeftPoint.x);
        float y = (float) (MercatorProjection.latitudeToPixelY(latLong.latitude, mapSize) - topLeftPoint.y);

        path.clear();
        path.moveTo(x, y);

        while (iterator.hasNext()) {
            latLong = iterator.next();
            x = (float) (MercatorProjection.longitudeToPixelX(latLong.longitude, mapSize) - topLeftPoint.x);
            y = (float) (MercatorProjection.latitudeToPixelY(latLong.latitude, mapSize) - topLeftPoint.y);

            path.lineTo(x, y);
        }

        if (this.keepAligned) {
            this.paintStroke.setBitmapShaderShift(topLeftPoint);
        }
        canvas.drawPath(path, this.paintStroke);
    }

    /**
     * @return a thread-safe list of LatLongs in this polyline.
     */
    public List<LatLong> getLatLongs() {
        return this.latLongs;
    }

    /**
     * @return the {@code Paint} used to stroke this polyline (may be null).
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
     * @param paintStroke the new {@code Paint} used to stroke this polyline (may be null).
     */
    public synchronized void setPaintStroke(Paint paintStroke) {
        this.paintStroke = paintStroke;
    }

}
