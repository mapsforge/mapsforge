/*
 * Copyright 2014 Ludwig M Brinckmann
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
import org.mapsforge.core.graphics.Paint;
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

	private final Paint lineFront, lineBack;
	private final Map<Byte, Double> spacingConfig;

	/**
	 * Ctor.
	 * @param displayModel the display model of the map view.
	 * @param lineFront the top line paint
	 * @param lineBack the back line paint.
	 * @param spacingConfig a map containing the spacing for every zoom level.
	 */
	public Grid(DisplayModel displayModel, Map<Byte, Double> spacingConfig,
	            Paint lineBack, Paint lineFront) {
		super();

		this.displayModel = displayModel;
		this.lineFront = lineFront;
		this.lineBack = lineBack;
		this.spacingConfig = spacingConfig;
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

			for (double latitude = minLatitude; latitude <= maxLatitude; latitude += spacing) {
				int bottom = (int) (MercatorProjection.latitudeToPixelY(latitude, mapSize) - topLeftPoint.y);
				int top = (int) (MercatorProjection.latitudeToPixelY(latitude + spacing, mapSize) - topLeftPoint.y);

				for (double longitude = minLongitude; longitude <= maxLongitude; longitude += spacing) {
					int left = (int) (MercatorProjection.longitudeToPixelX(longitude, mapSize) - topLeftPoint.x);
					int right = (int) (MercatorProjection.longitudeToPixelX(longitude + spacing, mapSize) - topLeftPoint.x);
					// draw both back paints first, then front paints on top.
					canvas.drawLine(left, bottom, left, top, this.lineBack);
					canvas.drawLine(left, bottom, right, bottom, this.lineBack);
					canvas.drawLine(left, bottom, left, top, this.lineFront);
					canvas.drawLine(left, bottom, right, bottom, this.lineFront);
				}
			}
		}
	}
}
