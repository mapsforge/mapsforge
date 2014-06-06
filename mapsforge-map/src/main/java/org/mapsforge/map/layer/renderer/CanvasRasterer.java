/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright © 2014 Ludwig M Brinckmann
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

import java.util.List;
import java.util.Set;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.GraphicUtils;
import org.mapsforge.core.graphics.Matrix;
import org.mapsforge.core.graphics.Path;
import org.mapsforge.core.mapelements.MapElementContainer;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Tile;

class CanvasRasterer {
	private final Canvas canvas;
	private final Path path;
	private final Matrix symbolMatrix;

	CanvasRasterer(GraphicFactory graphicFactory) {
		this.canvas = graphicFactory.createCanvas();
		this.symbolMatrix = graphicFactory.createMatrix();
		this.path = graphicFactory.createPath();
	}

	void destroy() {
		this.canvas.destroy();
	}

	void drawWays(List<List<List<ShapePaintContainer>>> drawWays, Tile tile) {
		int levelsPerLayer = drawWays.get(0).size();

		for (int layer = 0, layers = drawWays.size(); layer < layers; ++layer) {
			List<List<ShapePaintContainer>> shapePaintContainers = drawWays.get(layer);

			for (int level = 0; level < levelsPerLayer; ++level) {
				List<ShapePaintContainer> wayList = shapePaintContainers.get(level);

				for (int index = wayList.size() - 1; index >= 0; --index) {
					drawShapePaintContainer(wayList.get(index), tile);
				}
			}
		}
	}

	void drawMapElements(Set<MapElementContainer> elements, Tile tile) {
		for (MapElementContainer element : elements) {
			element.draw(canvas, tile.getOrigin(), this.symbolMatrix);
		}
	}

	void fill(int color) {
		if (GraphicUtils.getAlpha(color) > 0) {
			this.canvas.fillColor(color);
		}
	}

	void setCanvasBitmap(Bitmap bitmap) {
		this.canvas.setBitmap(bitmap);
	}

	private void drawCircleContainer(ShapePaintContainer shapePaintContainer) {
		CircleContainer circleContainer = (CircleContainer) shapePaintContainer.shapeContainer;
		Point point = circleContainer.point;
		this.canvas.drawCircle((int) point.x, (int) point.y, (int) circleContainer.radius, shapePaintContainer.paint);
	}

	private void drawPath(ShapePaintContainer shapePaintContainer, List<List<Point>> coordinates, float dy) {
		this.path.clear();

		for (List<Point> innerList : coordinates) {
			List<Point> points;
			if (dy != 0f) {
				points = RendererUtils.parallelPath(innerList, dy);
			} else {
				points = innerList;
			}
			if (points.size() >= 2) {
				Point point = points.get(0);
				this.path.moveTo((float) point.x, (float) point.y);
				for (int i = 1; i < points.size(); ++i) {
					point = points.get(i);
					this.path.lineTo((int) point.x, (int) point.y);
				}
			}
		}

		this.canvas.drawPath(this.path, shapePaintContainer.paint);
	}

	private void drawShapePaintContainer(ShapePaintContainer shapePaintContainer, Tile tile) {
		ShapeType shapeType = shapePaintContainer.shapeContainer.getShapeType();
		switch (shapeType) {
			case CIRCLE:
				drawCircleContainer(shapePaintContainer);
				return;

			case POLYLINE:
				PolylineContainer polylineContainer = (PolylineContainer) shapePaintContainer.shapeContainer;
				drawPath(shapePaintContainer, polylineContainer.getCoordinatesRelativeToTile(), shapePaintContainer.dy);
				return;
		}
	}
}
