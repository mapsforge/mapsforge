/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright © 2014 devemux86
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

import java.util.List;

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.FontFamily;
import org.mapsforge.core.graphics.FontStyle;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.LayerUtil;
import org.mapsforge.map.layer.TilePosition;
import org.mapsforge.map.model.DisplayModel;

public class TileCoordinatesLayer extends Layer {
	private static Paint createPaint(GraphicFactory graphicFactory, DisplayModel displayModel) {
		Paint paint = graphicFactory.createPaint();
		paint.setColor(Color.BLACK);
		paint.setTypeface(FontFamily.DEFAULT, FontStyle.BOLD);
		paint.setTextSize(20 * displayModel.getScaleFactor());
		return paint;
	}

	private static Paint createPaintStroke(GraphicFactory graphicFactory, DisplayModel displayModel) {
		Paint paint = graphicFactory.createPaint();
		paint.setColor(Color.WHITE);
		paint.setTypeface(FontFamily.DEFAULT, FontStyle.BOLD);
		paint.setTextSize(20 * displayModel.getScaleFactor());
		paint.setStrokeWidth(2 * displayModel.getScaleFactor());
		paint.setStyle(Style.STROKE);
		return paint;
	}

	private final DisplayModel displayModel;
	private final Paint paint, paintStroke;

	public TileCoordinatesLayer(GraphicFactory graphicFactory, DisplayModel displayModel) {
		super();

		this.displayModel = displayModel;
		this.paint = createPaint(graphicFactory, displayModel);
		this.paintStroke = createPaintStroke(graphicFactory, displayModel);
	}

	@Override
	public void draw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point topLeftPoint) {
		List<TilePosition> tilePositions = LayerUtil.getTilePositions(boundingBox, zoomLevel, topLeftPoint,
				this.displayModel.getTileSize());
		for (int i = tilePositions.size() - 1; i >= 0; --i) {
			drawTileCoordinates(tilePositions.get(i), canvas);
		}
	}

	private void drawTileCoordinates(TilePosition tilePosition, Canvas canvas) {
		int x = (int) (tilePosition.point.x + 15 * displayModel.getScaleFactor());
		int y = (int) (tilePosition.point.y + 30 * displayModel.getScaleFactor());
		Tile tile = tilePosition.tile;

		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("X: ");
		stringBuilder.append(tile.tileX);
		String text = stringBuilder.toString();
		canvas.drawText(text, x, y, this.paintStroke);
		canvas.drawText(text, x, y, this.paint);

		stringBuilder.setLength(0);
		stringBuilder.append("Y: ");
		stringBuilder.append(tile.tileY);
		text = stringBuilder.toString();
		canvas.drawText(text, x, (int) (y + 30 * displayModel.getScaleFactor()), this.paintStroke);
		canvas.drawText(text, x, (int) (y + 30 * displayModel.getScaleFactor()), this.paint);

		stringBuilder.setLength(0);
		stringBuilder.append("Z: ");
		stringBuilder.append(tile.zoomLevel);
		text = stringBuilder.toString();
		canvas.drawText(text, x, (int) (y + 60 * displayModel.getScaleFactor()), this.paintStroke);
		canvas.drawText(text, x, (int) (y + 60 * displayModel.getScaleFactor()), this.paint);
	}
}
