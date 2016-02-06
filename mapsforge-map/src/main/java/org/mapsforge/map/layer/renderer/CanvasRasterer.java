/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014-2015 Ludwig M Brinckmann
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
package org.mapsforge.map.layer.renderer;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.Filter;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.GraphicUtils;
import org.mapsforge.core.graphics.Matrix;
import org.mapsforge.core.graphics.Path;
import org.mapsforge.core.mapelements.MapElementContainer;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Rectangle;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.rendertheme.RenderContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class CanvasRasterer {
    private final Canvas canvas;
    private final Path path;
    private final Matrix symbolMatrix;

    public CanvasRasterer(GraphicFactory graphicFactory) {
        this.canvas = graphicFactory.createCanvas();
        this.path = graphicFactory.createPath();
        this.symbolMatrix = graphicFactory.createMatrix();
    }

    public void destroy() {
        this.canvas.destroy();
    }

    void drawWays(RenderContext renderContext) {
        int levelsPerLayer = renderContext.ways.get(0).size();

        for (int layer = 0, layers = renderContext.ways.size(); layer < layers; ++layer) {
            List<List<ShapePaintContainer>> shapePaintContainers = renderContext.ways.get(layer);

            for (int level = 0; level < levelsPerLayer; ++level) {
                List<ShapePaintContainer> wayList = shapePaintContainers.get(level);

                for (int index = wayList.size() - 1; index >= 0; --index) {
                    drawShapePaintContainer(wayList.get(index));
                }
            }
        }
    }

    void drawMapElements(Set<MapElementContainer> elements, Tile tile) {
        // we have a set of all map elements (needed so we do not draw elements twice),
        // but we need to draw in priority order as we now allow overlaps. So we
        // convert into list, then sort, then draw.
        List<MapElementContainer> elementsAsList = new ArrayList<>(elements);
        // draw elements in order of priority: lower priority first, so more important
        // elements will be drawn on top (in case of display=true) items.
        Collections.sort(elementsAsList);

        for (MapElementContainer element : elementsAsList) {
            // The color filtering takes place in TileLayer
            element.draw(canvas, tile.getOrigin(), this.symbolMatrix, Filter.NONE);
        }
    }

    void fill(int color) {
        if (GraphicUtils.getAlpha(color) > 0) {
            this.canvas.fillColor(color);
        }
    }

    /**
     * Fills the area outside the specificed rectangle with color. Use this method when
     * overpainting with a transparent color as it sets the PorterDuff mode.
     * This method is used to blank out areas that fall outside the map area.
     *
     * @param color      the fill color for the outside area
     * @param insideArea the inside area on which not to draw
     */
    void fillOutsideAreas(Color color, Rectangle insideArea) {
        this.canvas.setClipDifference((int) insideArea.left, (int) insideArea.top, (int) insideArea.getWidth(), (int) insideArea.getHeight());
        this.canvas.fillColor(color);
        this.canvas.resetClip();
    }

    /**
     * Fills the area outside the specificed rectangle with color.
     * This method is used to blank out areas that fall outside the map area.
     *
     * @param color      the fill color for the outside area
     * @param insideArea the inside area on which not to draw
     */
    void fillOutsideAreas(int color, Rectangle insideArea) {
        this.canvas.setClipDifference((int) insideArea.left, (int) insideArea.top, (int) insideArea.getWidth(), (int) insideArea.getHeight());
        this.canvas.fillColor(color);
        this.canvas.resetClip();
    }

    void setCanvasBitmap(Bitmap bitmap) {
        this.canvas.setBitmap(bitmap);
    }

    private void drawCircleContainer(ShapePaintContainer shapePaintContainer) {
        CircleContainer circleContainer = (CircleContainer) shapePaintContainer.shapeContainer;
        Point point = circleContainer.point;
        this.canvas.drawCircle((int) point.x, (int) point.y, (int) circleContainer.radius, shapePaintContainer.paint);
    }

    private void drawPath(ShapePaintContainer shapePaintContainer, Point[][] coordinates, float dy) {
        this.path.clear();

        for (Point[] innerList : coordinates) {
            Point[] points;
            if (dy != 0f) {
                points = RendererUtils.parallelPath(innerList, dy);
            } else {
                points = innerList;
            }
            if (points.length >= 2) {
                Point point = points[0];
                this.path.moveTo((float) point.x, (float) point.y);
                for (int i = 1; i < points.length; ++i) {
                    point = points[i];
                    this.path.lineTo((int) point.x, (int) point.y);
                }
            }
        }

        this.canvas.drawPath(this.path, shapePaintContainer.paint);
    }

    private void drawShapePaintContainer(ShapePaintContainer shapePaintContainer) {
        ShapeType shapeType = shapePaintContainer.shapeContainer.getShapeType();
        switch (shapeType) {
            case CIRCLE:
                drawCircleContainer(shapePaintContainer);
                break;
            case POLYLINE:
                PolylineContainer polylineContainer = (PolylineContainer) shapePaintContainer.shapeContainer;
                drawPath(shapePaintContainer, polylineContainer.getCoordinatesRelativeToOrigin(), shapePaintContainer.dy);
                break;
        }
    }
}
