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

import org.mapsforge.android.maps.inputhandling.ZoomAnimator;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.GeoPoint;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.core.util.MercatorProjection;

import android.graphics.Point;

/**
 * A MapViewPosition stores the latitude and longitude coordinates of a MapView together with its zoom level.
 */
public class MapViewPosition {
	private double latitude;
	private double longitude;
	private final MapView mapView;
	private byte zoomLevel;

	MapViewPosition(MapView mapView) {
		this.mapView = mapView;
		this.latitude = 0;
		this.longitude = 0;
		this.zoomLevel = 0;
	}

	/**
	 * @return the currently visible boundaries of the map.
	 */
	public synchronized BoundingBox getBoundingBox() {
		double pixelX = MercatorProjection.longitudeToPixelX(this.longitude, this.zoomLevel);
		double pixelY = MercatorProjection.latitudeToPixelY(this.latitude, this.zoomLevel);
		int halfCanvasWidth = this.mapView.getWidth() / 2;
		int halfCanvasHeight = this.mapView.getHeight() / 2;

		long mapSize = MercatorProjection.getMapSize(this.zoomLevel);
		double pixelXMin = Math.max(0, pixelX - halfCanvasWidth);
		double pixelYMin = Math.max(0, pixelY - halfCanvasHeight);
		double pixelXMax = Math.min(mapSize, pixelX + halfCanvasWidth);
		double pixelYMax = Math.min(mapSize, pixelY + halfCanvasHeight);

		double minLatitude = MercatorProjection.pixelYToLatitude(pixelYMax, this.zoomLevel);
		double minLongitude = MercatorProjection.pixelXToLongitude(pixelXMin, this.zoomLevel);
		double maxLatitude = MercatorProjection.pixelYToLatitude(pixelYMin, this.zoomLevel);
		double maxLongitude = MercatorProjection.pixelXToLongitude(pixelXMax, this.zoomLevel);

		return new BoundingBox(minLatitude, minLongitude, maxLatitude, maxLongitude);
	}

	/**
	 * @return the current center position of the map.
	 */
	public synchronized GeoPoint getCenter() {
		return new GeoPoint(this.latitude, this.longitude);
	}

	/**
	 * @return the current center position and zoom level of the map.
	 */
	public synchronized MapPosition getMapPosition() {
		return new MapPosition(getCenter(), this.zoomLevel);
	}

	/**
	 * @return the current zoom level of the map.
	 */
	public synchronized byte getZoomLevel() {
		return this.zoomLevel;
	}

	/**
	 * Moves the center position of the map by the given amount of pixels without an animation.
	 * 
	 * @param moveHorizontal
	 *            the amount of pixels to move this MapViewPosition horizontally.
	 * @param moveVertical
	 *            the amount of pixels to move this MapViewPosition vertically.
	 */
	public void moveCenter(float moveHorizontal, float moveVertical) {
		synchronized (this) {
			double pixelX = MercatorProjection.longitudeToPixelX(this.longitude, this.zoomLevel) - moveHorizontal;
			double pixelY = MercatorProjection.latitudeToPixelY(this.latitude, this.zoomLevel) - moveVertical;

			long mapSize = MercatorProjection.getMapSize(this.zoomLevel);
			pixelX = Math.min(Math.max(0, pixelX), mapSize);
			pixelY = Math.min(Math.max(0, pixelY), mapSize);

			double newLatitude = MercatorProjection.pixelYToLatitude(pixelY, this.zoomLevel);
			double newLongitude = MercatorProjection.pixelXToLongitude(pixelX, this.zoomLevel);
			setCenterInternal(new GeoPoint(newLatitude, newLongitude));
		}
		this.mapView.redraw();
	}

	/**
	 * Sets the new center position of the map without an animation.
	 * 
	 * @param geoPoint
	 *            the new center position of the map.
	 */
	public void setCenter(GeoPoint geoPoint) {
		setCenterInternal(geoPoint);
		this.mapView.redraw();
	}

	/**
	 * Sets the new center position and zoom level of the map without an animation.
	 * 
	 * @param mapPosition
	 *            the new center position and zoom level of the map.
	 */
	public void setMapPosition(MapPosition mapPosition) {
		synchronized (this) {
			setCenterInternal(mapPosition.geoPoint);
			setZoomLevelInternal(mapPosition.zoomLevel);
		}
		this.mapView.redraw();
	}

	/**
	 * Sets the new zoom level of the map without an animation.
	 * 
	 * @param zoomLevel
	 *            the new zoom level of the map.
	 * @throws IllegalArgumentException
	 *             if the given zoom level is negative.
	 */
	public void setZoomLevel(byte zoomLevel) {
		setZoomLevelInternal(zoomLevel);
		this.mapView.redraw();
	}

	/**
	 * Starts an animation to increase or decrease the current zoom level.
	 * 
	 * @param zoomLevelDiff
	 *            the difference to the current zoom level.
	 * @param scaleFactorStart
	 *            the scale factor at the begin of the animation.
	 */
	public void zoom(byte zoomLevelDiff, float scaleFactorStart) {
		float scaleFactorEnd = setZoomLevelDiff(zoomLevelDiff);
		int pivotX = this.mapView.getWidth() / 2;
		int pivotY = this.mapView.getHeight() / 2;

		ZoomAnimator zoomAnimator = this.mapView.getZoomAnimator();
		zoomAnimator.startAnimation(scaleFactorStart, scaleFactorEnd, pivotX, pivotY);
	}

	/**
	 * Starts an animation to increase the current zoom level by one.
	 */
	public void zoomIn() {
		zoom((byte) 1, 1);
	}

	/**
	 * Starts an animation to decrease the current zoom level by one.
	 */
	public void zoomOut() {
		zoom((byte) -1, 1);
	}

	private byte limitZoomLevel(byte newZoomLevel) {
		byte zoomLevelMin = this.mapView.getMapZoomControls().getZoomLevelMin();
		byte zoomLevelMax = this.mapView.getZoomLevelMax();
		return (byte) Math.max(Math.min(newZoomLevel, zoomLevelMax), zoomLevelMin);
	}

	private void setCenterInternal(GeoPoint geoPoint) {
		MapPosition mapPositionBefore = getMapPosition();
		synchronized (this) {
			this.latitude = geoPoint.latitude;
			this.longitude = geoPoint.longitude;
		}

		Projection projection = this.mapView.getProjection();
		Point pointBefore = projection.toPoint(mapPositionBefore.geoPoint, null, mapPositionBefore.zoomLevel);
		Point pointAfter = projection.toPoint(getCenter(), null, mapPositionBefore.zoomLevel);

		FrameBuffer frameBuffer = this.mapView.getFrameBuffer();
		frameBuffer.matrixPostTranslate(pointBefore.x - pointAfter.x, pointBefore.y - pointAfter.y);
	}

	private synchronized float setZoomLevelDiff(byte zoomLevelDiff) {
		return setZoomLevelNew((byte) (this.zoomLevel + zoomLevelDiff));
	}

	private void setZoomLevelInternal(byte zoomLevelNew) {
		float scaleFactor = setZoomLevelNew(zoomLevelNew);
		int pivotX = this.mapView.getWidth() / 2;
		int pivotY = this.mapView.getHeight() / 2;

		FrameBuffer frameBuffer = this.mapView.getFrameBuffer();
		frameBuffer.matrixPostScale(scaleFactor, scaleFactor, pivotX, pivotY);
	}

	private synchronized float setZoomLevelNew(byte zoomLevelUnlimited) {
		byte zoomLevelOld = this.zoomLevel;
		byte zoomLevelNew = limitZoomLevel(zoomLevelUnlimited);
		if (zoomLevelNew == zoomLevelOld) {
			return 1;
		}

		this.zoomLevel = zoomLevelNew;
		this.mapView.getMapZoomControls().onZoomLevelChange(zoomLevelNew);
		return (float) Math.pow(2, zoomLevelNew - zoomLevelOld);
	}
}
