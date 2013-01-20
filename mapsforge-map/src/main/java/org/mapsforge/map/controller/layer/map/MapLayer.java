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
package org.mapsforge.map.controller.layer.map;

import java.util.ArrayList;
import java.util.Random;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.controller.layer.Layer;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class MapLayer extends Layer {
	private static void drawTile(TilePosition tilePosition, Canvas canvas) {
		float x = (float) tilePosition.point.x;
		float y = (float) tilePosition.point.y;

		Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		Random random = new Random(tilePosition.tile.hashCode());
		int color = Color.rgb(random.nextInt(), random.nextInt(), random.nextInt());
		paint.setColor(color);

		canvas.drawRect(x, y, x + Tile.TILE_SIZE, y + Tile.TILE_SIZE, paint);
	}

	@Override
	public void draw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point canvasPosition) {
		ArrayList<TilePosition> tilePositions = LayerUtil.getTilePositions(boundingBox, zoomLevel, canvasPosition);
		for (int i = tilePositions.size() - 1; i >= 0; --i) {
			drawTile(tilePositions.get(i), canvas);
		}
	}
}
