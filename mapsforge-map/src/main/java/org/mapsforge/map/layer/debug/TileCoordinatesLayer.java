/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2014-2016 devemux86
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
import org.mapsforge.core.graphics.FontFamily;
import org.mapsforge.core.graphics.FontStyle;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.TilePosition;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.util.LayerUtil;

import java.util.List;

public class TileCoordinatesLayer extends Layer {
    private static Paint createPaintFront(GraphicFactory graphicFactory, DisplayModel displayModel) {
        Paint paint = graphicFactory.createPaint();
        paint.setColor(Color.RED);
        paint.setTypeface(FontFamily.DEFAULT, FontStyle.BOLD);
        paint.setTextSize(12 * displayModel.getScaleFactor());
        return paint;
    }

    private static Paint createPaintBack(GraphicFactory graphicFactory, DisplayModel displayModel) {
        Paint paint = graphicFactory.createPaint();
        paint.setColor(Color.WHITE);
        paint.setTypeface(FontFamily.DEFAULT, FontStyle.BOLD);
        paint.setTextSize(12 * displayModel.getScaleFactor());
        paint.setStrokeWidth(2 * displayModel.getScaleFactor());
        paint.setStyle(Style.STROKE);
        return paint;
    }

    private final DisplayModel displayModel;
    private final Paint paintBack, paintFront;
    private boolean drawSimple;
    private final StringBuilder stringBuilder = new StringBuilder();

    public TileCoordinatesLayer(GraphicFactory graphicFactory, DisplayModel displayModel) {
        super();

        this.displayModel = displayModel;

        this.paintBack = createPaintBack(graphicFactory, displayModel);
        this.paintFront = createPaintFront(graphicFactory, displayModel);
    }

    public TileCoordinatesLayer(DisplayModel displayModel, Paint paintBack, Paint paintFront) {
        super();

        this.displayModel = displayModel;
        this.paintBack = paintBack;
        this.paintFront = paintFront;
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
        Tile tile = tilePosition.tile;
        if (drawSimple) {
            stringBuilder.setLength(0);
            stringBuilder.append(tile.zoomLevel).append(" / ").append(tile.tileX).append(" / ").append(tile.tileY);
            String text = stringBuilder.toString();
            int x = (int) (tilePosition.point.x + (tile.tileSize - this.paintBack.getTextWidth(text)) / 2);
            int y = (int) (tilePosition.point.y + (tile.tileSize + this.paintBack.getTextHeight(text)) / 2);
            canvas.drawText(text, x, y, this.paintBack);
            canvas.drawText(text, x, y, this.paintFront);
        } else {
            int x = (int) (tilePosition.point.x + 8 * displayModel.getScaleFactor());
            int y = (int) (tilePosition.point.y + 24 * displayModel.getScaleFactor());

            stringBuilder.setLength(0);
            stringBuilder.append("X: ");
            stringBuilder.append(tile.tileX);
            String text = stringBuilder.toString();
            canvas.drawText(text, x, y, this.paintBack);
            canvas.drawText(text, x, y, this.paintFront);

            stringBuilder.setLength(0);
            stringBuilder.append("Y: ");
            stringBuilder.append(tile.tileY);
            text = stringBuilder.toString();
            canvas.drawText(text, x, (int) (y + 24 * displayModel.getScaleFactor()), this.paintBack);
            canvas.drawText(text, x, (int) (y + 24 * displayModel.getScaleFactor()), this.paintFront);

            stringBuilder.setLength(0);
            stringBuilder.append("Z: ");
            stringBuilder.append(tile.zoomLevel);
            text = stringBuilder.toString();
            canvas.drawText(text, x, (int) (y + 48 * displayModel.getScaleFactor()), this.paintBack);
            canvas.drawText(text, x, (int) (y + 48 * displayModel.getScaleFactor()), this.paintFront);
        }
    }

    public boolean isDrawSimple() {
        return drawSimple;
    }

    public void setDrawSimple(boolean drawSimple) {
        this.drawSimple = drawSimple;
    }
}
