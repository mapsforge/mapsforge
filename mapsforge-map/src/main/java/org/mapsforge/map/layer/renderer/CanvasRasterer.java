/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014-2015 Ludwig M Brinckmann
 * Copyright 2016-2020 devemux86
 * Copyright 2017 usrusr
 * Copyright 2020 Adrian Batzill
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
import org.mapsforge.core.graphics.Curve;
import org.mapsforge.core.graphics.Filter;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.GraphicUtils;
import org.mapsforge.core.graphics.Matrix;
import org.mapsforge.core.graphics.Path;
import org.mapsforge.core.mapelements.MapElementContainer;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Rectangle;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.Parameters;
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
                    drawShapePaintContainer(renderContext, wayList.get(index));
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

    private void drawHillshading(HillshadingContainer container) {
        canvas.shadeBitmap(container.bitmap, container.hillsRect, container.tileRect, container.magnitude);
    }

    private void drawPath(RenderContext renderContext, ShapePaintContainer shapePaintContainer, Point[][] coordinates, float dy) {
        this.path.clear();

        for (Point[] innerList : coordinates) {
            Point[] points;
            if (dy != 0f) {
                points = RendererUtils.parallelPath(innerList, dy);
            } else {
                points = innerList;
            }
            if (points.length >= 2) {
                // iterate over lines based on curveStyle
                if (shapePaintContainer.curveStyle == Curve.CUBIC) {
                    // prepare variables
                    float[] p1 = new float[]{(float) points[0].x, (float) points[0].y};
                    float[] p2 = new float[]{0.0f, 0.0f};
                    float[] p3 = new float[]{0.0f, 0.0f};

                    // add first point
                    this.path.moveTo(p1[0], p1[1]);
                    for (int i = 1; i < points.length; ++i) {
                        // get ending coordinates
                        p3[0] = (float) points[i].x;
                        p3[1] = (float) points[i].y;
                        p2[0] = (p1[0] + p3[0]) / 2.0f;
                        p2[1] = (p1[1] + p3[1]) / 2.0f;

                        // add spline over middle point and end on 'end' point
                        this.path.quadTo(p1[0], p1[1], p2[0], p2[1]);

                        // store end point as start point for next section
                        p1[0] = p3[0];
                        p1[1] = p3[1];
                    }

                    // add last segment
                    this.path.quadTo(p2[0], p2[1], p3[0], p3[1]);
                } else {
                    // construct line
                    this.path.moveTo((float) points[0].x, (float) points[0].y);
                    for (int i = 1; i < points.length; ++i) {
                        this.path.lineTo((int) points[i].x, (int) points[i].y);
                    }
                }
            }
        }

        if (Parameters.NUMBER_OF_THREADS > 1) {
            // Make sure setting the shader shift and actual drawing is synchronized,
            // since the paint object is shared between multiple threads.
            synchronized (shapePaintContainer.paint) {
                shapePaintContainer.paint.setBitmapShaderShift(renderContext.rendererJob.tile.getOrigin());
                this.canvas.drawPath(this.path, shapePaintContainer.paint);
            }
        } else {
            this.canvas.drawPath(this.path, shapePaintContainer.paint);
        }
    }

    private void drawShapePaintContainer(RenderContext renderContext, ShapePaintContainer shapePaintContainer) {
        ShapeContainer shapeContainer = shapePaintContainer.shapeContainer;
        ShapeType shapeType = shapeContainer.getShapeType();
        switch (shapeType) {
            case CIRCLE:
                drawCircleContainer(shapePaintContainer);
                break;
            case HILLSHADING:
                HillshadingContainer hillshadingContainer = (HillshadingContainer) shapeContainer;
                drawHillshading(hillshadingContainer);
                break;
            case POLYLINE:
                PolylineContainer polylineContainer = (PolylineContainer) shapeContainer;
                drawPath(renderContext, shapePaintContainer, polylineContainer.getCoordinatesRelativeToOrigin(), shapePaintContainer.dy);
                break;
        }
    }
}
