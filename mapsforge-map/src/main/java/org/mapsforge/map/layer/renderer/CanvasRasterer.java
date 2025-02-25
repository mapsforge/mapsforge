/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014-2015 Ludwig M Brinckmann
 * Copyright 2016-2020 devemux86
 * Copyright 2017 usrusr
 * Copyright 2020 Adrian Batzill
 * Copyright 2024-2025 Sublimis
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

import org.mapsforge.core.graphics.*;
import org.mapsforge.core.mapelements.MapElementContainer;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Rectangle;
import org.mapsforge.core.model.Rotation;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.Parameters;
import org.mapsforge.map.rendertheme.RenderContext;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class CanvasRasterer {
    private final RenderContext renderContext;
    private final Canvas canvas;
    private final Path path;
    private final Matrix symbolMatrix;

    /**
     * This will count paths vs. lines usage for performance diagnostics
     */
    private final boolean DEBUG_COUNTS = false;
    private final AtomicInteger linesCount = DEBUG_COUNTS ? new AtomicInteger() : null;
    private final AtomicInteger pathsCount = DEBUG_COUNTS ? new AtomicInteger() : null;


    public CanvasRasterer(RenderContext renderContext, GraphicFactory graphicFactory) {
        this.renderContext = renderContext;
        this.canvas = graphicFactory.createCanvas();
        this.path = graphicFactory.createPath();
        this.symbolMatrix = graphicFactory.createMatrix();
    }

    public void destroy() {
        this.canvas.destroy();

        if (DEBUG_COUNTS) {
            System.out.println("LINES: " + linesCount.get() + "  PATHS: " + pathsCount.get());
        }
    }

    /**
     * Input is assumed to already be sorted by drawing priority.
     */
    void drawMapElements(List<MapElementContainer> elements, Tile tile) {
        for (MapElementContainer element : elements) {
            element.draw(canvas, tile.getOrigin(), this.symbolMatrix, Rotation.NULL_ROTATION);
        }
    }

    void fill(int color) {
        if (GraphicUtils.getAlpha(color) > 0) {
            this.canvas.fillColor(color);
        }
    }

    /**
     * Fills the area outside the specified rectangle with color. Use this method when
     * overpainting with a transparent color as it sets the PorterDuff mode.
     * This method is used to blank out areas that fall outside the map area.
     *
     * @param color      the fill color for the outside area
     * @param insideArea the inside area on which not to draw
     */
    void fillOutsideAreas(Color color, Rectangle insideArea) {
        this.canvas.setClipDifference((float) insideArea.left, (float) insideArea.top, (float) insideArea.getWidth(), (float) insideArea.getHeight());
        this.canvas.fillColor(color);
        this.canvas.resetClip();
    }

    /**
     * Fills the area outside the specified rectangle with color.
     * This method is used to blank out areas that fall outside the map area.
     *
     * @param color      the fill color for the outside area
     * @param insideArea the inside area on which not to draw
     */
    void fillOutsideAreas(int color, Rectangle insideArea) {
        this.canvas.setClipDifference((float) insideArea.left, (float) insideArea.top, (float) insideArea.getWidth(), (float) insideArea.getHeight());
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
        final Bitmap bitmap = container.bitmap;

        // Synchronized to prevent concurrent modification by other threads e.g. while merging neighbors
        synchronized (bitmap.getMutex()) {
            canvas.shadeBitmap(bitmap, container.hillsRect, container.tileRect, container.magnitude, container.color);
        }
    }

    private void drawPath(ShapePaintContainer shapePaintContainer, Point[][] coordinates, float dy) {
        if (shapePaintContainer.curveStyle == Curve.CUBIC) {
            // When cubic, paths must be used.
            makeCubicPath(coordinates, dy, this.path);
            drawPath(shapePaintContainer);
        } else if (shapePaintContainer.paint.isComplexStyle()) {
            // When complex (e.g. filled) style, paths must be used.
            makeLinesPath(coordinates, dy, this.path);
            drawPath(shapePaintContainer);
        } else {
            // When neither cubic nor complex style, use lines (esp. on Android):
            //   * To prevent libhwui.so "null pointer dereference" SIGSEGV crashes.
            //   * For performance.
            drawLines(shapePaintContainer, coordinates, dy);
        }
    }

    private void drawPath(ShapePaintContainer shapePaintContainer) {
        if (!this.path.isEmpty()) {
            if (Parameters.NUMBER_OF_THREADS > 1) {
                // Make sure setting the shader shift and actual drawing is synchronized,
                // since the paint object is shared between multiple threads.
                synchronized (shapePaintContainer.paint) {
                    final RenderContext renderContext = this.renderContext;
                    shapePaintContainer.paint.setBitmapShaderShift(renderContext.rendererJob.tile.getOrigin());
                    this.canvas.drawPath(this.path, shapePaintContainer.paint);
                }
            } else {
                this.canvas.drawPath(this.path, shapePaintContainer.paint);
            }

            if (DEBUG_COUNTS) {
                pathsCount.incrementAndGet();
            }
        }
    }

    private void drawLines(ShapePaintContainer shapePaintContainer, Point[][] coordinates, float dy) {
        if (Parameters.NUMBER_OF_THREADS > 1) {
            // Make sure setting the shader shift and actual drawing is synchronized,
            // since the paint object is shared between multiple threads.
            synchronized (shapePaintContainer.paint) {
                final RenderContext renderContext = this.renderContext;
                shapePaintContainer.paint.setBitmapShaderShift(renderContext.rendererJob.tile.getOrigin());
                this.canvas.drawLines(coordinates, dy, shapePaintContainer.paint);
            }
        } else {
            this.canvas.drawLines(coordinates, dy, shapePaintContainer.paint);
        }

        if (DEBUG_COUNTS) {
            linesCount.incrementAndGet();
        }
    }

    public void drawShapePaintContainer(ShapePaintContainer shapePaintContainer) {
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
                drawPath(shapePaintContainer, polylineContainer.getCoordinatesRelativeToOrigin(), shapePaintContainer.dy);
                break;
        }
    }

    private static void makeCubicPath(Point[][] coordinates, float dy, Path path) {
        path.clear();

        for (Point[] innerList : coordinates) {
            final Point[] points = dy == 0f ? innerList : RendererUtils.parallelPath(innerList, dy);
            if (points.length >= 2) {
                float[] p1 = new float[]{(float) points[0].x, (float) points[0].y};
                float[] p2 = new float[]{0.0f, 0.0f};
                float[] p3 = new float[]{0.0f, 0.0f};

                // add first point
                path.moveTo(p1[0], p1[1]);

                for (int i = 1; i < points.length; ++i) {
                    // get ending coordinates
                    p3[0] = (float) points[i].x;
                    p3[1] = (float) points[i].y;
                    p2[0] = 0.5f * (p1[0] + p3[0]);
                    p2[1] = 0.5f * (p1[1] + p3[1]);

                    // add spline over middle point and end on 'end' point
                    path.quadTo(p1[0], p1[1], p2[0], p2[1]);

                    // store end point as start point for next section
                    p1[0] = p3[0];
                    p1[1] = p3[1];
                }

                // add last segment
                path.quadTo(p2[0], p2[1], p3[0], p3[1]);
            }
        }
    }

    private static void makeLinesPath(Point[][] coordinates, float dy, Path path) {
        path.clear();

        for (Point[] innerList : coordinates) {
            final Point[] points = dy == 0f ? innerList : RendererUtils.parallelPath(innerList, dy);
            if (points.length >= 2) {
                path.moveTo((float) points[0].x, (float) points[0].y);

                for (int i = 1; i < points.length; ++i) {
                    path.lineTo((float) points[i].x, (float) points[i].y);
                }
            }
        }
    }
}
