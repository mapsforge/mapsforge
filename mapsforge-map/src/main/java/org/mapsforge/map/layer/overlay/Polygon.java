/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2015-2018 devemux86
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
import org.mapsforge.core.graphics.FillRule;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Path;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.util.LatLongUtils;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.layer.Layer;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A {@code Polygon} draws a closed connected series of line segments specified by a list of {@link LatLong LatLongs}.
 * If the first and the last {@code LatLong} are not equal, the {@code Polygon} will be closed automatically.
 * <p/>
 * A {@code Polygon} holds two {@link Paint} objects to allow for different outline and filling. These paints define
 * drawing parameters such as color, stroke width, pattern and transparency.
 * <p/>
 * A {@code Polygon} can optionally contain one or more "holes". See method {@link #addHole(List)}} for more information and restrictions
 */
public class Polygon extends Layer {

    private BoundingBox boundingBox;
    private final GraphicFactory graphicFactory;
    private final boolean keepAligned;
    private final List<LatLong> latLongs = new CopyOnWriteArrayList<>();
    private List<List<LatLong>> holes; //optional, lazy-initialized for better performance
    private Paint paintFill;
    private Paint paintStroke;

    /**
     * @param paintFill      the initial {@code Paint} used to fill this polygon (may be null).
     * @param paintStroke    the initial {@code Paint} used to stroke this polygon (may be null).
     * @param graphicFactory the GraphicFactory
     */
    public Polygon(Paint paintFill, Paint paintStroke, GraphicFactory graphicFactory) {
        this(paintFill, paintStroke, graphicFactory, false);
    }

    /**
     * @param paintFill      the initial {@code Paint} used to fill this polygon (may be null).
     * @param paintStroke    the initial {@code Paint} used to stroke this polygon (may be null).
     * @param graphicFactory the GraphicFactory
     * @param keepAligned    if set to true it will keep the bitmap aligned with the map,
     *                       to avoid a moving effect of a bitmap shader.
     */
    public Polygon(Paint paintFill, Paint paintStroke, GraphicFactory graphicFactory, boolean keepAligned) {
        super();
        this.keepAligned = keepAligned;
        this.paintFill = paintFill;
        this.paintStroke = paintStroke;
        this.graphicFactory = graphicFactory;
    }

    public synchronized void addPoint(LatLong point) {
        this.latLongs.add(point);
        updatePoints();
    }

    public synchronized void addPoints(List<LatLong> points) {
        this.latLongs.addAll(points);
        updatePoints();
    }

    /**
     * Adds a hole to this polygon.
     * <p>
     * A polygon can contain multiple holes. Each hole must be completely inside the polygon.
     * Holes may not overlap. All hole's winding must be the opposite of the winding of the outer polygon
     * (e.g. if outer polygon's points are in counter-clockwise order, then the points of each hole must be in
     * clockwise order; and vice versa).
     *
     * @param holePoints path of the hole. If first and last point are not equal, path will be closed automatically
     */
    public synchronized void addHole(List<LatLong> holePoints) {
        if (this.holes == null) {
            this.holes = new CopyOnWriteArrayList<>();
        }
        this.holes.add(new CopyOnWriteArrayList<>(holePoints));
    }

    public synchronized void clear() {
        this.latLongs.clear();
        if (this.holes != null) {
            this.holes.clear();
        }
        updatePoints();
    }

    public synchronized boolean contains(LatLong tapLatLong) {
        final boolean contains = LatLongUtils.contains(latLongs, tapLatLong);
        if (holes == null || !contains) {
            return contains;
        }

        for (List<LatLong> hole : this.holes) {
            if (LatLongUtils.contains(hole, tapLatLong)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public synchronized void draw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point topLeftPoint) {
        if (this.latLongs.size() < 2 || (this.paintStroke == null && this.paintFill == null)) {
            return;
        }

        if (this.boundingBox != null && !this.boundingBox.intersects(boundingBox)) {
            return;
        }

        Path path = this.graphicFactory.createPath();
        long mapSize = MercatorProjection.getMapSize(zoomLevel, displayModel.getTileSize());

        addToPath(path, this.latLongs, mapSize, topLeftPoint);

        if (this.holes != null && !this.holes.isEmpty()) {
            path.setFillRule(FillRule.EVEN_ODD);
            for (List<LatLong> hole : holes) {
                addToPath(path, hole, mapSize, topLeftPoint);
            }
        }

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

    private static void addToPath(Path path, List<LatLong> points, long mapSize, Point topLeftPoint) {
        final Iterator<LatLong> iterator = points.iterator();
        LatLong latLong = iterator.next();
        float x = (float) (MercatorProjection.longitudeToPixelX(latLong.longitude, mapSize) - topLeftPoint.x);
        float y = (float) (MercatorProjection.latitudeToPixelY(latLong.latitude, mapSize) - topLeftPoint.y);
        path.moveTo(x, y);

        while (iterator.hasNext()) {
            latLong = iterator.next();
            x = (float) (MercatorProjection.longitudeToPixelX(latLong.longitude, mapSize) - topLeftPoint.x);
            y = (float) (MercatorProjection.latitudeToPixelY(latLong.latitude, mapSize) - topLeftPoint.y);
            path.lineTo(x, y);
        }
        path.close();
    }


    /**
     * @return a thread-safe list of LatLongs in this polygon.
     */
    public List<LatLong> getLatLongs() {
        return this.latLongs;
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

    public synchronized void setPoints(List<LatLong> points) {
        this.latLongs.clear();
        this.latLongs.addAll(points);
        updatePoints();
    }

    private void updatePoints() {
        this.boundingBox = this.latLongs.isEmpty() ? null : new BoundingBox(this.latLongs);
    }
}
