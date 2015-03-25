/*
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2015 devemux86
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
package org.mapsforge.map.layer.overlay;

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.model.DisplayModel;

import java.util.Map;

/**
 * The Grid layer draws a geographical grid.
 */
public class Grid extends Layer {
	private static Paint createLineFront(GraphicFactory graphicFactory, DisplayModel displayModel) {
		Paint paint = graphicFactory.createPaint();
		paint.setColor(Color.BLUE);
		paint.setStrokeWidth(2 * displayModel.getScaleFactor());
		paint.setStyle(Style.STROKE);
		return paint;
	}

	private static Paint createLineBack(GraphicFactory graphicFactory, DisplayModel displayModel) {
		Paint paint = graphicFactory.createPaint();
		paint.setColor(Color.WHITE);
		paint.setStrokeWidth(4 * displayModel.getScaleFactor());
		paint.setStyle(Style.STROKE);
		return paint;
	}

	private final Paint lineBack, lineFront;
	private final Map<Byte, Double> spacingConfig;

	/**
	 * Ctor.
	 * @param graphicFactory the graphic factory. 
	 * @param displayModel the display model of the map view.
	 * @param spacingConfig a map containing the spacing for every zoom level.
	 */
	public Grid(GraphicFactory graphicFactory, DisplayModel displayModel,
			Map<Byte, Double> spacingConfig) {
		super();

		this.displayModel = displayModel;
		this.spacingConfig = spacingConfig;

		this.lineBack = createLineBack(graphicFactory, displayModel);
		this.lineFront = createLineFront(graphicFactory, displayModel);
	}

	/**
	 * Ctor.
	 * @param displayModel the display model of the map view.
	 * @param spacingConfig a map containing the spacing for every zoom level.
	 * @param lineBack the back line paint.
	 * @param lineFront the top line paint.
	 */
	public Grid(DisplayModel displayModel, Map<Byte, Double> spacingConfig,
			Paint lineBack, Paint lineFront) {
		super();

		this.displayModel = displayModel;
		this.spacingConfig = spacingConfig;
		this.lineBack = lineBack;
		this.lineFront = lineFront;
	}

	@Override
	public void draw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point topLeftPoint) {
		if (spacingConfig.containsKey(zoomLevel)) {
			double spacing = spacingConfig.get(zoomLevel);

			double minLongitude = spacing * (Math.floor(boundingBox.minLongitude / spacing));
			double maxLongitude = spacing * (Math.ceil(boundingBox.maxLongitude / spacing));
			double minLatitude = spacing * (Math.floor(boundingBox.minLatitude / spacing));
			double maxLatitude = spacing * (Math.ceil(boundingBox.maxLatitude / spacing));

			long mapSize = MercatorProjection.getMapSize(zoomLevel, this.displayModel.getTileSize());

			int bottom = (int) (MercatorProjection.latitudeToPixelY(minLatitude, mapSize) - topLeftPoint.y);
			int top = (int) (MercatorProjection.latitudeToPixelY(maxLatitude, mapSize) - topLeftPoint.y);
			int left = (int) (MercatorProjection.longitudeToPixelX(minLongitude, mapSize) - topLeftPoint.x);
			int right = (int) (MercatorProjection.longitudeToPixelX(maxLongitude, mapSize) - topLeftPoint.x);

			for (double latitude = minLatitude; latitude <= maxLatitude; latitude += spacing) {
				int pixelY = (int) (MercatorProjection.latitudeToPixelY(latitude, mapSize) - topLeftPoint.y);
				canvas.drawLine(left, pixelY, right, pixelY, this.lineBack);
			}

			for (double longitude = minLongitude; longitude <= maxLongitude; longitude += spacing) {
				int pixelX = (int) (MercatorProjection.longitudeToPixelX(longitude, mapSize) - topLeftPoint.x);
				canvas.drawLine(pixelX, bottom, pixelX, top, this.lineBack);
			}

			for (double latitude = minLatitude; latitude <= maxLatitude; latitude += spacing) {
				int pixelY = (int) (MercatorProjection.latitudeToPixelY(latitude, mapSize) - topLeftPoint.y);
				canvas.drawLine(left, pixelY, right, pixelY, this.lineFront);
			}

			for (double longitude = minLongitude; longitude <= maxLongitude; longitude += spacing) {
				int pixelX = (int) (MercatorProjection.longitudeToPixelX(longitude, mapSize) - topLeftPoint.x);
				canvas.drawLine(pixelX, bottom, pixelX, top, this.lineFront);
			}
		}
	}
}
