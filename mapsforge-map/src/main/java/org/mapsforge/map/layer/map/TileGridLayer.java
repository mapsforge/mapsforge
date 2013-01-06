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

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.layer.Layer;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;

public final class TileGridLayer extends Layer {
	public static final TileGridLayer INSTANCE = new TileGridLayer();
	private static final Paint PAINT = createPaint();

	private static Paint createPaint() {
		Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setColor(Color.BLACK);
		paint.setStrokeWidth(3);
		paint.setStyle(Style.STROKE);
		return paint;
	}

	private TileGridLayer() {
		// do nothing
	}

	@Override
	public void draw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point canvasPosition) {
		long tileLeft = MercatorProjection.longitudeToTileX(boundingBox.minLongitude, zoomLevel);
		long tileTop = MercatorProjection.latitudeToTileY(boundingBox.maxLatitude, zoomLevel);
		long tileRight = MercatorProjection.longitudeToTileX(boundingBox.maxLongitude, zoomLevel);
		long tileBottom = MercatorProjection.latitudeToTileY(boundingBox.minLatitude, zoomLevel);

		float pixelX1 = (float) (MercatorProjection.tileXToPixelX(tileLeft) - canvasPosition.x);
		float pixelY1 = (float) (MercatorProjection.tileYToPixelY(tileTop) - canvasPosition.y);
		float pixelX2 = (float) (MercatorProjection.tileXToPixelX(tileRight) - canvasPosition.x + Tile.TILE_SIZE);
		float pixelY2 = (float) (MercatorProjection.tileYToPixelY(tileBottom) - canvasPosition.y + Tile.TILE_SIZE);

		for (float lineX = pixelX1; lineX <= pixelX2; lineX += Tile.TILE_SIZE) {
			canvas.drawLine(lineX, pixelY1, lineX, pixelY2, PAINT);
		}

		for (float lineY = pixelY1; lineY <= pixelY2; lineY += Tile.TILE_SIZE) {
			canvas.drawLine(pixelX1, lineY, pixelX2, lineY, PAINT);
		}
	}
}
