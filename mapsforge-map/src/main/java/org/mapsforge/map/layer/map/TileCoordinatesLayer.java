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

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.LayerUtil;
import org.mapsforge.map.layer.TilePosition;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Typeface;

public final class TileCoordinatesLayer extends Layer {
	public static final TileCoordinatesLayer INSTANCE = new TileCoordinatesLayer();
	private static final Paint PAINT_FILL = createPaint(Color.BLACK, 0);
	private static final Paint PAINT_STROKE = createPaint(Color.WHITE, 3);

	private static Paint createPaint(int color, float strokeWidth) {
		Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setColor(color);
		paint.setStrokeWidth(strokeWidth);
		paint.setStyle(Style.STROKE);
		paint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
		paint.setTextSize(20);
		return paint;
	}

	private static void drawText(String text, float x, float y, Canvas canvas) {
		canvas.drawText(text, x, y, PAINT_STROKE);
		canvas.drawText(text, x, y, PAINT_FILL);
	}

	// TODO remove this variable
	private int i;

	private TileCoordinatesLayer() {
		// do nothing
	}

	@Override
	public void draw(BoundingBox boundingBox, MapPosition mapPosition, Canvas canvas) {
		++this.i;
		List<TilePosition> tilePositions = LayerUtil.getTilePositions(boundingBox, mapPosition, canvas);
		for (TilePosition tilePosition : tilePositions) {
			drawTileCoordinates(tilePosition, canvas);
		}
	}

	private void drawTileCoordinates(TilePosition tilePosition, Canvas canvas) {
		float x = (float) tilePosition.point.x;
		float y = (float) tilePosition.point.y;

		drawText("X: " + tilePosition.tile.tileX, x + 10, y + 30, canvas);
		drawText("Y: " + tilePosition.tile.tileY, x + 10, y + 60, canvas);
		drawText("Z: " + tilePosition.tile.zoomLevel, x + 10, y + 90, canvas);
		drawText("i: " + this.i, x + 10, y + 120, canvas);
	}
}
