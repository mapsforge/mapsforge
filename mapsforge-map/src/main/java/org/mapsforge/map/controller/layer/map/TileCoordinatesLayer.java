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

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.controller.layer.Layer;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;

public final class TileCoordinatesLayer extends Layer {
	public static final TileCoordinatesLayer INSTANCE = new TileCoordinatesLayer();
	private static final Paint PAINT = createPaint();

	private static Paint createPaint() {
		Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setColor(Color.BLACK);
		paint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
		paint.setTextSize(20);
		return paint;
	}

	private static void drawTileCoordinates(TilePosition tilePosition, Canvas canvas) {
		float x = (float) tilePosition.point.x + 15;
		float y = (float) tilePosition.point.y + 30;
		Tile tile = tilePosition.tile;

		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("X: ");
		stringBuilder.append(tile.tileX);
		canvas.drawText(stringBuilder.toString(), x, y, PAINT);

		stringBuilder.setLength(0);
		stringBuilder.append("Y: ");
		stringBuilder.append(tile.tileY);
		canvas.drawText(stringBuilder.toString(), x, y + 30, PAINT);

		stringBuilder.setLength(0);
		stringBuilder.append("Z: ");
		stringBuilder.append(tile.zoomLevel);
		canvas.drawText(stringBuilder.toString(), x, y + 60, PAINT);
	}

	private TileCoordinatesLayer() {
		// do nothing
	}

	@Override
	public void draw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point canvasPosition) {
		ArrayList<TilePosition> tilePositions = LayerUtil.getTilePositions(boundingBox, zoomLevel, canvasPosition);
		for (int i = tilePositions.size() - 1; i >= 0; --i) {
			drawTileCoordinates(tilePositions.get(i), canvas);
		}
	}
}
