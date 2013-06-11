/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
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
package org.mapsforge.map.layer.debug;

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.layer.Layer;

public class TileGridLayer extends Layer {
	private static Paint createPaint(GraphicFactory graphicFactory) {
		Paint paint = graphicFactory.createPaint();
		paint.setColor(Color.BLACK);
		paint.setStrokeWidth(2);
		paint.setStyle(Style.STROKE);
		return paint;
	}

	private final Paint paint;

	public TileGridLayer(GraphicFactory graphicFactory) {
		super();

		this.paint = createPaint(graphicFactory);
	}

	@Override
	public void draw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point topLeftPoint) {
		long tileLeft = MercatorProjection.longitudeToTileX(boundingBox.minLongitude, zoomLevel);
		long tileTop = MercatorProjection.latitudeToTileY(boundingBox.maxLatitude, zoomLevel);
		long tileRight = MercatorProjection.longitudeToTileX(boundingBox.maxLongitude, zoomLevel);
		long tileBottom = MercatorProjection.latitudeToTileY(boundingBox.minLatitude, zoomLevel);

		int pixelX1 = (int) (MercatorProjection.tileToPixel(tileLeft) - topLeftPoint.x);
		int pixelY1 = (int) (MercatorProjection.tileToPixel(tileTop) - topLeftPoint.y);
		int pixelX2 = (int) (MercatorProjection.tileToPixel(tileRight) - topLeftPoint.x + Tile.TILE_SIZE);
		int pixelY2 = (int) (MercatorProjection.tileToPixel(tileBottom) - topLeftPoint.y + Tile.TILE_SIZE);

		for (int lineX = pixelX1; lineX <= pixelX2 + 1; lineX += Tile.TILE_SIZE) {
			canvas.drawLine(lineX, pixelY1, lineX, pixelY2, this.paint);
		}

		for (int lineY = pixelY1; lineY <= pixelY2 + 1; lineY += Tile.TILE_SIZE) {
			canvas.drawLine(pixelX1, lineY, pixelX2, lineY, this.paint);
		}
	}
}
