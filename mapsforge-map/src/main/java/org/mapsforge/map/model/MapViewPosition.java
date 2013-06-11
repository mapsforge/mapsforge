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
package org.mapsforge.map.model;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.model.common.Observable;
import org.mapsforge.map.model.common.Persistable;
import org.mapsforge.map.model.common.PreferencesFacade;

public class MapViewPosition extends Observable implements Persistable {
	private static final String LATITUDE = "latitude";
	private static final String LATITUDE_MAX = "latitudeMax";
	private static final String LATITUDE_MIN = "latitudeMin";
	private static final String LONGITUDE = "longitude";
	private static final String LONGITUDE_MAX = "longitudeMax";
	private static final String LONGITUDE_MIN = "longitudeMin";
	private static final String ZOOM_LEVEL = "zoomLevel";
	private static final String ZOOM_LEVEL_MAX = "zoomLevelMax";
	private static final String ZOOM_LEVEL_MIN = "zoomLevelMin";

	private static boolean isNan(double... values) {
		for (double value : values) {
			if (Double.isNaN(value)) {
				return true;
			}
		}

		return false;
	}

	private double latitude;
	private double longitude;
	private BoundingBox mapLimit;
	private byte zoomLevel;
	private byte zoomLevelMax;
	private byte zoomLevelMin;

	public MapViewPosition() {
		super();

		this.zoomLevelMax = Byte.MAX_VALUE;
	}

	/**
	 * @return the current center position of the map.
	 */
	public synchronized LatLong getCenter() {
		return new LatLong(this.latitude, this.longitude);
	}

	/**
	 * @return the current limit of the map (might be null).
	 */
	public synchronized BoundingBox getMapLimit() {
		return this.mapLimit;
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

	public synchronized byte getZoomLevelMax() {
		return this.zoomLevelMax;
	}

	public synchronized byte getZoomLevelMin() {
		return this.zoomLevelMin;
	}

	@Override
	public synchronized void init(PreferencesFacade preferencesFacade) {
		this.latitude = preferencesFacade.getDouble(LATITUDE, 0);
		this.longitude = preferencesFacade.getDouble(LONGITUDE, 0);

		double maxLatitude = preferencesFacade.getDouble(LATITUDE_MAX, Double.NaN);
		double minLatitude = preferencesFacade.getDouble(LATITUDE_MIN, Double.NaN);
		double maxLongitude = preferencesFacade.getDouble(LONGITUDE_MAX, Double.NaN);
		double minLongitude = preferencesFacade.getDouble(LONGITUDE_MIN, Double.NaN);

		if (isNan(maxLatitude, minLatitude, maxLongitude, minLongitude)) {
			this.mapLimit = null;
		} else {
			this.mapLimit = new BoundingBox(minLatitude, minLongitude, maxLatitude, maxLongitude);
		}

		this.zoomLevel = preferencesFacade.getByte(ZOOM_LEVEL, (byte) 0);
		this.zoomLevelMax = preferencesFacade.getByte(ZOOM_LEVEL_MAX, Byte.MAX_VALUE);
		this.zoomLevelMin = preferencesFacade.getByte(ZOOM_LEVEL_MIN, (byte) 0);
	}

	/**
	 * Moves the center position of the map by the given amount of pixels.
	 * 
	 * @param moveHorizontal
	 *            the amount of pixels to move this MapViewPosition horizontally.
	 * @param moveVertical
	 *            the amount of pixels to move this MapViewPosition vertically.
	 */
	public void moveCenter(double moveHorizontal, double moveVertical) {
		synchronized (this) {
			double pixelX = MercatorProjection.longitudeToPixelX(this.longitude, this.zoomLevel) - moveHorizontal;
			double pixelY = MercatorProjection.latitudeToPixelY(this.latitude, this.zoomLevel) - moveVertical;

			long mapSize = MercatorProjection.getMapSize(this.zoomLevel);
			pixelX = Math.min(Math.max(0, pixelX), mapSize);
			pixelY = Math.min(Math.max(0, pixelY), mapSize);

			double newLatitude = MercatorProjection.pixelYToLatitude(pixelY, this.zoomLevel);
			double newLongitude = MercatorProjection.pixelXToLongitude(pixelX, this.zoomLevel);
			setCenterInternal(new LatLong(newLatitude, newLongitude));
		}
		notifyObservers();
	}

	@Override
	public synchronized void save(PreferencesFacade preferencesFacade) {
		preferencesFacade.putDouble(LATITUDE, this.latitude);
		preferencesFacade.putDouble(LONGITUDE, this.longitude);

		if (this.mapLimit == null) {
			preferencesFacade.putDouble(LATITUDE_MAX, Double.NaN);
			preferencesFacade.putDouble(LATITUDE_MIN, Double.NaN);
			preferencesFacade.putDouble(LONGITUDE_MAX, Double.NaN);
			preferencesFacade.putDouble(LONGITUDE_MIN, Double.NaN);
		} else {
			preferencesFacade.putDouble(LATITUDE_MAX, this.mapLimit.maxLatitude);
			preferencesFacade.putDouble(LATITUDE_MIN, this.mapLimit.minLatitude);
			preferencesFacade.putDouble(LONGITUDE_MAX, this.mapLimit.maxLongitude);
			preferencesFacade.putDouble(LONGITUDE_MIN, this.mapLimit.minLongitude);
		}

		preferencesFacade.putByte(ZOOM_LEVEL, this.zoomLevel);
		preferencesFacade.putByte(ZOOM_LEVEL_MAX, this.zoomLevelMax);
		preferencesFacade.putByte(ZOOM_LEVEL_MIN, this.zoomLevelMin);
	}

	/**
	 * Sets the new center position of the map.
	 */
	public void setCenter(LatLong latLong) {
		synchronized (this) {
			setCenterInternal(latLong);
		}
		notifyObservers();
	}

	/**
	 * Sets the new limit of the map (might be null).
	 */
	public void setMapLimit(BoundingBox mapLimit) {
		synchronized (this) {
			this.mapLimit = mapLimit;
		}
		notifyObservers();
	}

	/**
	 * Sets the new center position and zoom level of the map.
	 */
	public void setMapPosition(MapPosition mapPosition) {
		synchronized (this) {
			setCenterInternal(mapPosition.latLong);
			setZoomLevelInternal(mapPosition.zoomLevel);
		}
		notifyObservers();
	}

	/**
	 * Sets the new zoom level of the map.
	 * 
	 * @throws IllegalArgumentException
	 *             if the zoom level is negative.
	 */
	public void setZoomLevel(byte zoomLevel) {
		if (zoomLevel < 0) {
			throw new IllegalArgumentException("zoomLevel must not be negative: " + zoomLevel);
		}
		synchronized (this) {
			setZoomLevelInternal(zoomLevel);
		}
		notifyObservers();
	}

	public void setZoomLevelMax(byte zoomLevelMax) {
		if (zoomLevelMax < 0) {
			throw new IllegalArgumentException("zoomLevelMax must not be negative: " + zoomLevelMax);
		}
		synchronized (this) {
			if (zoomLevelMax < this.zoomLevelMin) {
				throw new IllegalArgumentException("zoomLevelMax must be >= zoomLevelMin: " + zoomLevelMax);
			}
			this.zoomLevelMax = zoomLevelMax;
		}
		notifyObservers();
	}

	public void setZoomLevelMin(byte zoomLevelMin) {
		if (zoomLevelMin < 0) {
			throw new IllegalArgumentException("zoomLevelMin must not be negative: " + zoomLevelMin);
		}
		synchronized (this) {
			if (zoomLevelMin > this.zoomLevelMax) {
				throw new IllegalArgumentException("zoomLevelMin must be <= zoomLevelMax: " + zoomLevelMin);
			}
			this.zoomLevelMin = zoomLevelMin;
		}
		notifyObservers();
	}

	/**
	 * Changes the current zoom level by the given value if possible.
	 */
	public void zoom(byte zoomLevelDiff) {
		synchronized (this) {
			setZoomLevelInternal(this.zoomLevel + zoomLevelDiff);
		}
		notifyObservers();
	}

	/**
	 * Increases the current zoom level by one if possible.
	 */
	public void zoomIn() {
		zoom((byte) 1);
	}

	/**
	 * Decreases the current zoom level by one if possible.
	 */
	public void zoomOut() {
		zoom((byte) -1);
	}

	private void setCenterInternal(LatLong latLong) {
		if (this.mapLimit == null) {
			this.latitude = latLong.latitude;
			this.longitude = latLong.longitude;
		} else {
			this.latitude = Math.max(Math.min(latLong.latitude, this.mapLimit.maxLatitude), this.mapLimit.minLatitude);
			this.longitude = Math.max(Math.min(latLong.longitude, this.mapLimit.maxLongitude),
					this.mapLimit.minLongitude);
		}
	}

	private void setZoomLevelInternal(int zoomLevel) {
		this.zoomLevel = (byte) Math.max(Math.min(zoomLevel, this.zoomLevelMax), this.zoomLevelMin);
	}
}
