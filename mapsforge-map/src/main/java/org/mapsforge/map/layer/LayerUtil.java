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
package org.mapsforge.map.layer;

import java.util.ArrayList;
import java.util.List;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.GeoPoint;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.MercatorProjection;

import android.graphics.Canvas;

public final class LayerUtil {
	public static Point getCanvasPosition(MapPosition mapPosition, int width, int height) {
		GeoPoint centerPoint = mapPosition.geoPoint;
		byte zoomLevel = mapPosition.zoomLevel;

		double pixelX = MercatorProjection.longitudeToPixelX(centerPoint.longitude, zoomLevel) - width / 2;
		double pixelY = MercatorProjection.latitudeToPixelY(centerPoint.latitude, zoomLevel) - height / 2;
		return new Point(pixelX, pixelY);
	}

	public static List<TilePosition> getTilePositions(BoundingBox boundingBox, MapPosition mapPosition, Canvas canvas) {
		byte zoomLevel = mapPosition.zoomLevel;

		long tileLeft = MercatorProjection.longitudeToTileX(boundingBox.minLongitude, zoomLevel);
		long tileTop = MercatorProjection.latitudeToTileY(boundingBox.maxLatitude, zoomLevel);
		long tileRight = MercatorProjection.longitudeToTileX(boundingBox.maxLongitude, zoomLevel);
		long tileBottom = MercatorProjection.latitudeToTileY(boundingBox.minLatitude, zoomLevel);

		Point canvasPosition = getCanvasPosition(mapPosition, canvas.getWidth(), canvas.getHeight());
		List<TilePosition> tilePositions = new ArrayList<TilePosition>();

		for (long tileX = tileLeft; tileX <= tileRight; ++tileX) {
			for (long tileY = tileTop; tileY <= tileBottom; ++tileY) {
				double longitude = MercatorProjection.tileXToLongitude(tileX, zoomLevel);
				double latitude = MercatorProjection.tileYToLatitude(tileY, zoomLevel);

				double pixelX = MercatorProjection.longitudeToPixelX(longitude, zoomLevel) - canvasPosition.x;
				double pixelY = MercatorProjection.latitudeToPixelY(latitude, zoomLevel) - canvasPosition.y;

				tilePositions.add(new TilePosition(new Tile(tileX, tileY, zoomLevel), new Point(pixelX, pixelY)));
			}
		}

		return tilePositions;
	}
}
