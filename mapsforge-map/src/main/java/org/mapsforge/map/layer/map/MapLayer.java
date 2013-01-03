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

import java.util.List;
import java.util.Random;

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

public class MapLayer extends Layer {
	private static void drawTile(TilePosition tilePosition, Canvas canvas) {
		float x = (float) tilePosition.point.x;
		float y = (float) tilePosition.point.y;

		Path path = new Path();
		path.moveTo(x, y);
		path.lineTo(x + Tile.TILE_SIZE, y);
		path.lineTo(x + Tile.TILE_SIZE, y + Tile.TILE_SIZE);
		path.lineTo(x, y + Tile.TILE_SIZE);
		path.lineTo(x, y);

		Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		Random random = new Random(tilePosition.tile.hashCode());
		int color = Color.argb(random.nextInt(), random.nextInt(), random.nextInt(), random.nextInt());
		paint.setColor(color);
		paint.setStyle(Style.FILL);
		canvas.drawPath(path, paint);
	}

	@Override
	public void draw(BoundingBox boundingBox, MapPosition mapPosition, Canvas canvas) {
		List<TilePosition> tilePositions = LayerUtil.getTilePositions(boundingBox, mapPosition, canvas);
		for (TilePosition tilePosition : tilePositions) {
			drawTile(tilePosition, canvas);
		}
	}
}
