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
package org.mapsforge.map.android.layer;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.overlay.Circle;
import org.mapsforge.map.layer.overlay.Marker;
import org.mapsforge.map.model.MapViewPosition;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;

/**
 * A thread-safe {@link Layer} implementation to display the current location. NOTE: This code really does not reflect
 * Android best practice and used in production leads to bad user experience (e.g. long time to first fix, excessive
 * battery use, non-compliance with the Android lifecycle...). Best use the new location services provided by Google
 * Play Services. Also note that MyLocationOverlay needs to be added to a view before requesting location updates
 * (otherwise no DisplayModel is set).
 */
public class MyLocationOverlay extends Layer implements LocationListener {
	private static final GraphicFactory GRAPHIC_FACTORY = AndroidGraphicFactory.INSTANCE;
	private float minDistance = 0.0f;
	private long minTime = 0;

	/**
	 * @param location
	 *            the location whose geographical coordinates should be converted.
	 * @return a new LatLong with the geographical coordinates taken from the given location.
	 */
	public static LatLong locationToLatLong(Location location) {
		return new LatLong(location.getLatitude(), location.getLongitude(), true);
	}

	private static Paint getDefaultCircleFill() {
		return getPaint(GRAPHIC_FACTORY.createColor(48, 0, 0, 255), 0, Style.FILL);
	}

	private static Paint getDefaultCircleStroke() {
		return getPaint(GRAPHIC_FACTORY.createColor(160, 0, 0, 255), 2, Style.STROKE);
	}

	private static Paint getPaint(int color, int strokeWidth, Style style) {
		Paint paint = GRAPHIC_FACTORY.createPaint();
		paint.setColor(color);
		paint.setStrokeWidth(strokeWidth);
		paint.setStyle(style);
		return paint;
	}

	private boolean centerAtNextFix;
	private final Circle circle;
	private Location lastLocation;
	private final LocationManager locationManager;
	private final MapViewPosition mapViewPosition;
	private final Marker marker;
	private boolean myLocationEnabled;
	private boolean snapToLocationEnabled;

	/**
	 * Constructs a new {@code MyLocationOverlay} with the default circle paints.
	 * 
	 * @param context
	 *            a reference to the application context.
	 * @param mapViewPosition
	 *            the {@code MapViewPosition} whose location will be updated.
	 * @param bitmap
	 *            a bitmap to display at the current location (might be null).
	 */
	public MyLocationOverlay(Context context, MapViewPosition mapViewPosition, Bitmap bitmap) {
		this(context, mapViewPosition, bitmap, getDefaultCircleFill(), getDefaultCircleStroke());
	}

	/**
	 * Constructs a new {@code MyLocationOverlay} with the given circle paints.
	 * 
	 * @param context
	 *            a reference to the application context.
	 * @param mapViewPosition
	 *            the {@code MapViewPosition} whose location will be updated.
	 * @param bitmap
	 *            a bitmap to display at the current location (might be null).
	 * @param circleFill
	 *            the {@code Paint} used to fill the circle that represents the accuracy of the current location (might be null).
	 * @param circleStroke
	 *            the {@code Paint} used to stroke the circle that represents the accuracy of the current location (might be null).
	 */
	public MyLocationOverlay(Context context, MapViewPosition mapViewPosition, Bitmap bitmap, Paint circleFill,
			Paint circleStroke) {
		super();

		this.mapViewPosition = mapViewPosition;
		this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		this.marker = new Marker(null, bitmap, 0, 0);
		this.circle = new Circle(null, 0, circleFill, circleStroke);
	}

	/**
	 * Stops the receiving of location updates. Has no effect if location updates are already disabled.
	 */
	public synchronized void disableMyLocation() {
		if (this.myLocationEnabled) {
			this.myLocationEnabled = false;
			this.locationManager.removeUpdates(this);
			// TODO trigger redraw?
		}
	}

	@Override
	public synchronized void draw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point topLeftPoint) {
		if (!this.myLocationEnabled) {
			return;
		}

		this.circle.draw(boundingBox, zoomLevel, canvas, topLeftPoint);
		this.marker.draw(boundingBox, zoomLevel, canvas, topLeftPoint);
	}

	/**
	 * Enables the receiving of location updates from the most accurate {@link LocationProvider} available.
	 * 
	 * @param centerAtFirstFix
	 *            whether the map should be centered to the first received location fix.
	 * @return true if at least one location provider was found, false otherwise.
	 */
	public synchronized boolean enableMyLocation(boolean centerAtFirstFix) {
		if (!enableBestAvailableProvider()) {
			return false;
		}

		this.centerAtNextFix = centerAtFirstFix;
		this.circle.setDisplayModel(this.displayModel);
		this.marker.setDisplayModel(this.displayModel);
		return true;
	}

	/**
	 * @return the most-recently received location fix (might be null).
	 */
	public synchronized Location getLastLocation() {
		return this.lastLocation;
	}

	/**
	 * @return true if the map will be centered at the next received location fix, false otherwise.
	 */
	public synchronized boolean isCenterAtNextFix() {
		return this.centerAtNextFix;
	}

	/**
	 * @return true if the receiving of location updates is currently enabled, false otherwise.
	 */
	public synchronized boolean isMyLocationEnabled() {
		return this.myLocationEnabled;
	}

	/**
	 * @return true if the snap-to-location mode is enabled, false otherwise.
	 */
	public synchronized boolean isSnapToLocationEnabled() {
		return this.snapToLocationEnabled;
	}

	@Override
	public void onDestroy() {
		this.marker.onDestroy();
	}

	@Override
	public void onLocationChanged(Location location) {

		synchronized (this) {
			this.lastLocation = location;

			LatLong latLong = locationToLatLong(location);
			this.marker.setLatLong(latLong);
			this.circle.setLatLong(latLong);
			if (location.getAccuracy() != 0) {
				this.circle.setRadius(location.getAccuracy());
			} else {
				// on the emulator we do not get an accuracy
				this.circle.setRadius(40);
			}

			if (this.centerAtNextFix || this.snapToLocationEnabled) {
				this.centerAtNextFix = false;
				this.mapViewPosition.setCenter(latLong);
			}

			requestRedraw();
		}
	}

	@Override
	public void onProviderDisabled(String provider) {
		enableBestAvailableProvider();
	}

	@Override
	public void onProviderEnabled(String provider) {
		enableBestAvailableProvider();
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// do nothing
	}

	/**
	 * Minimum distance between location updates, in meters.
	 * You should call this before calling {@link MyLocationOverlay#enableMyLocation(boolean)}.
	 */
	public void setMinDistance(float minDistance) {
		this.minDistance = minDistance;
	}

	/**
	 * Minimum time interval between location updates, in milliseconds.
	 * You should call this before calling {@link MyLocationOverlay#enableMyLocation(boolean)}.
	 */
	public void setMinTime(long minTime) {
		this.minTime = minTime;
	}

	/**
	 * @param snapToLocationEnabled
	 *            whether the map should be centered at each received location fix.
	 */
	public synchronized void setSnapToLocationEnabled(boolean snapToLocationEnabled) {
		this.snapToLocationEnabled = snapToLocationEnabled;
	}

	private synchronized boolean enableBestAvailableProvider() {
		disableMyLocation();

		boolean result = false;
		for (String provider : this.locationManager.getProviders(true)) {
			if (LocationManager.GPS_PROVIDER.equals(provider)
					|| LocationManager.NETWORK_PROVIDER.equals(provider)) {
				result = true;
				this.locationManager.requestLocationUpdates(provider, minTime, minDistance, this);
			}
		}
		this.myLocationEnabled = result;
		return result;
	}
}
