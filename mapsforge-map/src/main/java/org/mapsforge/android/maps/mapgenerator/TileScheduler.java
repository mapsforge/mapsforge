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
package org.mapsforge.android.maps.mapgenerator;

import org.mapsforge.android.maps.MapView;
import org.mapsforge.core.model.GeoPoint;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.MercatorProjection;

final class TileScheduler {
	private static final int ZOOM_LEVEL_PENALTY = 5;

	/**
	 * Calculates the priority for the given tile based on the current position and zoom level of the supplied MapView.
	 * The smaller the distance from the tile center to the MapView center, the higher its priority. If the zoom level
	 * of a tile differs from the zoom level of the MapView, its priority decreases.
	 * 
	 * @param tile
	 *            the tile whose priority should be calculated.
	 * @param mapView
	 *            the MapView whose current position and zoom level define the priority of the tile.
	 * @return the current priority of the tile. A smaller number means a higher priority.
	 */
	static double getPriority(Tile tile, MapView mapView) {
		byte tileZoomLevel = tile.zoomLevel;

		// calculate the center coordinates of the tile
		long tileCenterPixelX = MercatorProjection.tileXToPixelX(tile.tileX) + (Tile.TILE_SIZE >> 1);
		long tileCenterPixelY = MercatorProjection.tileYToPixelY(tile.tileY) + (Tile.TILE_SIZE >> 1);
		double tileCenterLongitude = MercatorProjection.pixelXToLongitude(tileCenterPixelX, tileZoomLevel);
		double tileCenterLatitude = MercatorProjection.pixelYToLatitude(tileCenterPixelY, tileZoomLevel);

		// calculate the Euclidian distance from the MapView center to the tile center
		MapPosition mapPosition = mapView.getMapViewPosition().getMapPosition();
		GeoPoint geoPoint = mapPosition.geoPoint;
		double longitudeDiff = geoPoint.longitude - tileCenterLongitude;
		double latitudeDiff = geoPoint.latitude - tileCenterLatitude;
		double euclidianDistance = Math.sqrt(longitudeDiff * longitudeDiff + latitudeDiff * latitudeDiff);

		if (mapPosition.zoomLevel == tileZoomLevel) {
			return euclidianDistance;
		}

		int zoomLevelDiff = Math.abs(mapPosition.zoomLevel - tileZoomLevel);
		double scaleFactor = Math.pow(2, zoomLevelDiff);

		double scaledEuclidianDistance;
		if (mapPosition.zoomLevel < tileZoomLevel) {
			scaledEuclidianDistance = euclidianDistance * scaleFactor;
		} else {
			scaledEuclidianDistance = euclidianDistance / scaleFactor;
		}

		double zoomLevelPenalty = zoomLevelDiff * ZOOM_LEVEL_PENALTY;
		return scaledEuclidianDistance * zoomLevelPenalty;
	}

	private TileScheduler() {
		throw new IllegalStateException();
	}
}
