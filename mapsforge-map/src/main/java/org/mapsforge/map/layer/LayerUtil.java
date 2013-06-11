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
package org.mapsforge.map.layer;

import java.util.ArrayList;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.MercatorProjection;

public final class LayerUtil {
	public static ArrayList<TilePosition> getTilePositions(BoundingBox boundingBox, byte zoomLevel, Point topLeftPoint) {
		long tileLeft = MercatorProjection.longitudeToTileX(boundingBox.minLongitude, zoomLevel);
		long tileTop = MercatorProjection.latitudeToTileY(boundingBox.maxLatitude, zoomLevel);
		long tileRight = MercatorProjection.longitudeToTileX(boundingBox.maxLongitude, zoomLevel);
		long tileBottom = MercatorProjection.latitudeToTileY(boundingBox.minLatitude, zoomLevel);

		int initialCapacity = (int) ((tileRight - tileLeft + 1) * (tileBottom - tileTop + 1));
		ArrayList<TilePosition> tilePositions = new ArrayList<TilePosition>(initialCapacity);

		for (long tileY = tileTop; tileY <= tileBottom; ++tileY) {
			for (long tileX = tileLeft; tileX <= tileRight; ++tileX) {
				double pixelX = MercatorProjection.tileToPixel(tileX) - topLeftPoint.x;
				double pixelY = MercatorProjection.tileToPixel(tileY) - topLeftPoint.y;

				tilePositions.add(new TilePosition(new Tile(tileX, tileY, zoomLevel), new Point(pixelX, pixelY)));
			}
		}

		return tilePositions;
	}

	private LayerUtil() {
		throw new IllegalStateException();
	}
}
