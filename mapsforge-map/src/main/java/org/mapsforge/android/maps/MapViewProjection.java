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
package org.mapsforge.android.maps;

import org.mapsforge.core.model.GeoPoint;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.core.util.MercatorProjection;

import android.graphics.Point;

class MapViewProjection implements Projection {
	private static final String INVALID_MAP_VIEW_DIMENSIONS = "invalid MapView dimensions";

	private final MapView mapView;

	MapViewProjection(MapView mapView) {
		this.mapView = mapView;
	}

	@Override
	public GeoPoint fromPixels(int x, int y) {
		if (this.mapView.getWidth() <= 0 || this.mapView.getHeight() <= 0) {
			return null;
		}

		MapPosition mapPosition = this.mapView.getMapViewPosition().getMapPosition();

		// calculate the pixel coordinates of the top left corner
		GeoPoint geoPoint = mapPosition.geoPoint;
		double pixelX = MercatorProjection.longitudeToPixelX(geoPoint.longitude, mapPosition.zoomLevel);
		double pixelY = MercatorProjection.latitudeToPixelY(geoPoint.latitude, mapPosition.zoomLevel);
		pixelX -= this.mapView.getWidth() >> 1;
		pixelY -= this.mapView.getHeight() >> 1;

		// convert the pixel coordinates to a GeoPoint and return it
		return new GeoPoint(MercatorProjection.pixelYToLatitude(pixelY + y, mapPosition.zoomLevel),
				MercatorProjection.pixelXToLongitude(pixelX + x, mapPosition.zoomLevel));
	}

	@Override
	public double getLatitudeSpan() {
		if (this.mapView.getWidth() > 0 && this.mapView.getHeight() > 0) {
			GeoPoint top = fromPixels(0, 0);
			GeoPoint bottom = fromPixels(0, this.mapView.getHeight());
			return Math.abs(top.latitude - bottom.latitude);
		}
		throw new IllegalStateException(INVALID_MAP_VIEW_DIMENSIONS);
	}

	@Override
	public double getLongitudeSpan() {
		if (this.mapView.getWidth() > 0 && this.mapView.getHeight() > 0) {
			GeoPoint left = fromPixels(0, 0);
			GeoPoint right = fromPixels(this.mapView.getWidth(), 0);
			return Math.abs(left.longitude - right.longitude);
		}
		throw new IllegalStateException(INVALID_MAP_VIEW_DIMENSIONS);
	}

	@Override
	public float metersToPixels(float meters, byte zoom) {
		double latitude = this.mapView.getMapViewPosition().getCenter().latitude;
		double groundResolution = MercatorProjection.calculateGroundResolution(latitude, zoom);
		return (float) (meters * (1 / groundResolution));
	}

	@Override
	public Point toPixels(GeoPoint in, Point out) {
		if (this.mapView.getWidth() <= 0 || this.mapView.getHeight() <= 0) {
			return null;
		}

		MapPosition mapPosition = this.mapView.getMapViewPosition().getMapPosition();

		// calculate the pixel coordinates of the top left corner
		GeoPoint geoPoint = mapPosition.geoPoint;
		double pixelX = MercatorProjection.longitudeToPixelX(geoPoint.longitude, mapPosition.zoomLevel);
		double pixelY = MercatorProjection.latitudeToPixelY(geoPoint.latitude, mapPosition.zoomLevel);
		pixelX -= this.mapView.getWidth() >> 1;
		pixelY -= this.mapView.getHeight() >> 1;

		if (out == null) {
			// create a new point and return it
			return new Point(
					(int) (MercatorProjection.longitudeToPixelX(in.longitude, mapPosition.zoomLevel) - pixelX),
					(int) (MercatorProjection.latitudeToPixelY(in.latitude, mapPosition.zoomLevel) - pixelY));
		}

		// reuse the existing point
		out.x = (int) (MercatorProjection.longitudeToPixelX(in.longitude, mapPosition.zoomLevel) - pixelX);
		out.y = (int) (MercatorProjection.latitudeToPixelY(in.latitude, mapPosition.zoomLevel) - pixelY);
		return out;
	}

	@Override
	public Point toPoint(GeoPoint in, Point out, byte zoom) {
		if (out == null) {
			// create a new point and return it
			return new Point((int) MercatorProjection.longitudeToPixelX(in.longitude, zoom),
					(int) MercatorProjection.latitudeToPixelY(in.latitude, zoom));
		}

		// reuse the existing point
		out.x = (int) MercatorProjection.longitudeToPixelX(in.longitude, zoom);
		out.y = (int) MercatorProjection.latitudeToPixelY(in.latitude, zoom);
		return out;
	}
}
