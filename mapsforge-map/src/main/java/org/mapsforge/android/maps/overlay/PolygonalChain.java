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
package org.mapsforge.android.maps.overlay;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.mapsforge.core.model.GeoPoint;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.util.MercatorProjection;

import android.graphics.Path;

/**
 * A {@code PolygonalChain} is a connected series of line segments specified by a list of {@link GeoPoint GeoPoints}.
 */
public class PolygonalChain {
	private final List<GeoPoint> geoPoints;

	/**
	 * @param geoPoints
	 *            the initial GeoPoints of this polygonal chain (may be null).
	 */
	public PolygonalChain(Collection<GeoPoint> geoPoints) {
		if (geoPoints == null) {
			this.geoPoints = Collections.synchronizedList(new ArrayList<GeoPoint>());
		} else {
			this.geoPoints = Collections.synchronizedList(new ArrayList<GeoPoint>(geoPoints));
		}
	}

	/**
	 * @return a synchronized (thread-safe) list of all GeoPoints of this polygonal chain. Manual synchronization on
	 *         this list is necessary when iterating over it.
	 */
	public List<GeoPoint> getGeoPoints() {
		synchronized (this.geoPoints) {
			return this.geoPoints;
		}
	}

	/**
	 * @return true if the first and the last GeoPoint of this polygonal chain are equal, false otherwise.
	 */
	public boolean isClosed() {
		synchronized (this.geoPoints) {
			int numberOfGeoPoints = this.geoPoints.size();
			if (numberOfGeoPoints < 2) {
				return false;
			}

			GeoPoint geoPointFirst = this.geoPoints.get(0);
			GeoPoint geoPointLast = this.geoPoints.get(numberOfGeoPoints - 1);
			return geoPointFirst.equals(geoPointLast);
		}
	}

	/**
	 * @param zoomLevel
	 *            the zoom level at which this {@code PolygonalChain} should draw itself.
	 * @param canvasPosition
	 *            the top-left pixel position of the canvas on the world map at the given zoom level.
	 * @param closeAutomatically
	 *            whether the generated path should always be closed.
	 * @return a {@code Path} representing this {@code PolygonalChain} (may be null).
	 */
	protected Path draw(byte zoomLevel, Point canvasPosition, boolean closeAutomatically) {
		synchronized (this.geoPoints) {
			int numberOfGeoPoints = this.geoPoints.size();
			if (numberOfGeoPoints < 2) {
				return null;
			}

			Path path = new Path();
			for (int i = 0; i < numberOfGeoPoints; ++i) {
				GeoPoint geoPoint = this.geoPoints.get(i);
				double latitude = geoPoint.latitude;
				double longitude = geoPoint.longitude;
				float pixelX = (float) (MercatorProjection.longitudeToPixelX(longitude, zoomLevel) - canvasPosition.x);
				float pixelY = (float) (MercatorProjection.latitudeToPixelY(latitude, zoomLevel) - canvasPosition.y);

				if (i == 0) {
					path.moveTo(pixelX, pixelY);
				} else {
					path.lineTo(pixelX, pixelY);
				}
			}

			if (closeAutomatically && !isClosed()) {
				path.close();
			}
			return path;
		}
	}
}
