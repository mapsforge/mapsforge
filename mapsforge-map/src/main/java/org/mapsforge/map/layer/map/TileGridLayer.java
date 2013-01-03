/*
 * Copyright 2010, 2011, 2012 mapsforge.org
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
package org.mapsforge.map.layer.map;

import java.util.ArrayList;
import java.util.List;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.LayerUtil;
import org.mapsforge.map.layer.TilePosition;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;

public final class TileGridLayer extends Layer {
	public static final TileGridLayer INSTANCE = new TileGridLayer();
	private static final Paint PAINT_FILL = createPaint(Color.BLACK, 2);
	private static final Paint PAINT_STROKE = createPaint(Color.WHITE, 4);

	private static Paint createPaint(int color, float strokeWidth) {
		Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setColor(color);
		paint.setStrokeWidth(strokeWidth);
		paint.setStyle(Style.STROKE);
		return paint;
	}

	private static void drawPaths(Canvas canvas, List<Path> paths, Paint paint) {
		for (Path path : paths) {
			canvas.drawPath(path, paint);
		}
	}

	private static Path createPath(TilePosition tilePosition) {
		float x = (float) tilePosition.point.x;
		float y = (float) tilePosition.point.y;

		Path path = new Path();
		path.moveTo(x, y);
		path.lineTo(x + Tile.TILE_SIZE, y);
		path.lineTo(x + Tile.TILE_SIZE, y + Tile.TILE_SIZE);
		path.lineTo(x, y + Tile.TILE_SIZE);
		path.lineTo(x, y);

		return path;
	}

	private TileGridLayer() {
		// do nothing
	}

	@Override
	public void draw(BoundingBox boundingBox, MapPosition mapPosition, Canvas canvas) {
		List<TilePosition> tilePositions = LayerUtil.getTilePositions(boundingBox, mapPosition, canvas);
		List<Path> paths = new ArrayList<Path>();
		for (TilePosition tilePosition : tilePositions) {
			paths.add(createPath(tilePosition));
		}

		drawPaths(canvas, paths, PAINT_STROKE);
		drawPaths(canvas, paths, PAINT_FILL);
	}
}
