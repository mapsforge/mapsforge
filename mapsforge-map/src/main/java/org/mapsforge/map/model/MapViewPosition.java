/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2015-2020 devemux86
 * Copyright 2015 Andreas Schildbach
 * Copyright 2016 mikes222
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
import org.mapsforge.core.model.Point;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.model.common.Observable;
import org.mapsforge.map.model.common.Persistable;
import org.mapsforge.map.model.common.PreferencesFacade;
import org.mapsforge.map.util.PausableThread;

public class MapViewPosition extends Observable implements IMapViewPosition, Persistable {

    private class Animator extends PausableThread {

        // debugging tip: for investigating what happens during the zoom animation
        // just make the times longer for duration and frame length
        private static final int DEFAULT_DURATION = 250;
        private static final int FRAME_LENGTH_IN_MS = 15;

        private static final int DEFAULT_MOVE_STEPS = 25;

        // move parameters
        private long mapSize;
        private int moveSteps;
        private double targetPixelX, targetPixelY;

        // zoom parameters
        private double scaleDifference;
        private double startScaleFactor;
        private long timeEnd, timeStart;
        private boolean zoomAnimation;

        private double calculateScaleFactor(float percent) {
            return this.startScaleFactor + this.scaleDifference * percent;
        }

        @Override
        protected void doWork() throws InterruptedException {
            doWorkMove();
            doWorkZoom();
            sleep(FRAME_LENGTH_IN_MS);
        }

        private void doWorkMove() {
            if (moveSteps == 0)
                return;
            double currentPixelX = MercatorProjection.longitudeToPixelX(longitude, mapSize);
            double currentPixelY = MercatorProjection.latitudeToPixelY(latitude, mapSize);
            double stepSizeX = Math.abs(targetPixelX - currentPixelX) / moveSteps;
            double stepSizeY = Math.abs(targetPixelY - currentPixelY) / moveSteps;
            double signX = Math.signum(currentPixelX - targetPixelX);
            double signY = Math.signum(currentPixelY - targetPixelY);
            --moveSteps;

            moveCenter(stepSizeX * signX, stepSizeY * signY);
        }

        private void doWorkZoom() throws InterruptedException {
            if (!this.zoomAnimation)
                return;
            if (System.currentTimeMillis() >= this.timeEnd) {
                this.zoomAnimation = false;
                MapViewPosition.this.setScaleFactor(calculateScaleFactor(1));
                MapViewPosition.this.setPivot(null);
            } else {
                float timeElapsedRatio = (System.currentTimeMillis() - this.timeStart) / (1f * DEFAULT_DURATION);
                MapViewPosition.this.setScaleFactor(calculateScaleFactor(timeElapsedRatio));
            }
        }

        @Override
        protected ThreadPriority getThreadPriority() {
            return ThreadPriority.ABOVE_NORMAL;
        }

        @Override
        protected boolean hasWork() {
            return this.moveSteps > 0 || this.zoomAnimation;
        }

        void startAnimationMove(LatLong latLong) {
            // TODO is this properly synchronized?
            mapSize = MercatorProjection.getMapSize(zoomLevel, displayModel.getTileSize());
            targetPixelX = MercatorProjection.longitudeToPixelX(latLong.longitude, mapSize);
            targetPixelY = MercatorProjection.latitudeToPixelY(latLong.latitude, mapSize);
            moveSteps = DEFAULT_MOVE_STEPS;
            synchronized (this) {
                notify();
            }
        }

        void startAnimationZoom(double startScaleFactor, double targetScaleFactor) {
            // TODO is this properly synchronized?
            this.startScaleFactor = startScaleFactor;
            this.scaleDifference = targetScaleFactor - this.startScaleFactor;
            this.zoomAnimation = true;
            this.timeStart = System.currentTimeMillis();
            this.timeEnd = this.timeStart + DEFAULT_DURATION;
            synchronized (this) {
                notify();
            }
        }
    }

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

    private final Animator animator;
    private final DisplayModel displayModel;
    private double latitude, longitude;
    private BoundingBox mapLimit;
    private LatLong pivot;
    private double scaleFactor;
    private byte zoomLevel, zoomLevelMax, zoomLevelMin;

    public MapViewPosition(DisplayModel displayModel) {
        super();
        this.displayModel = displayModel;
        this.zoomLevelMax = Byte.MAX_VALUE;
        this.animator = new Animator();
        this.animator.start();
    }

    /**
     * Animate the map towards the given position.
     */
    @Override
    public void animateTo(final LatLong latLong) {
        animator.startAnimationMove(latLong);
    }

    @Override
    public boolean animationInProgress() {
        return this.scaleFactor != MercatorProjection.zoomLevelToScaleFactor(this.zoomLevel);
    }

    @Override
    public void destroy() {
        this.animator.finish();
    }

    /**
     * @return the current center position of the map.
     */
    @Override
    public synchronized LatLong getCenter() {
        return new LatLong(this.latitude, this.longitude);
    }

    /**
     * @return the current limit of the map (might be null).
     */
    @Override
    public synchronized BoundingBox getMapLimit() {
        return this.mapLimit;
    }

    /**
     * @return the current center position and zoom level of the map.
     */
    @Override
    public synchronized MapPosition getMapPosition() {
        return new MapPosition(getCenter(), this.zoomLevel);
    }

    /**
     * The pivot point is the point the map zooms around.
     *
     * @return the lat/long coordinates of the map pivot point if set or null otherwise.
     */
    @Override
    public synchronized LatLong getPivot() {
        return this.pivot;
    }

    /**
     * The pivot point is the point the map zooms around. If the map zooms around its center null is returned, otherwise
     * the zoom-specific x/y pixel coordinates for the MercatorProjection (note: not the x/y coordinates for the map
     * view or the frame buffer, the MapViewPosition knows nothing about them).
     *
     * @param zoomLevel the zoomlevel to compute the x/y coordinates for
     * @return the x/y coordinates of the map pivot point if set or null otherwise.
     */

    public synchronized Point getPivotXY(byte zoomLevel) {
        if (this.pivot != null) {
            return MercatorProjection.getPixel(this.pivot, MercatorProjection.getMapSize(zoomLevel, displayModel.getTileSize()));
        }
        return null;
    }

    @Override
    public synchronized double getScaleFactor() {
        return this.scaleFactor;
    }

    /**
     * @return the current zoom level of the map.
     */
    @Override
    public synchronized byte getZoomLevel() {
        return this.zoomLevel;
    }

    @Override
    public synchronized byte getZoomLevelMax() {
        return this.zoomLevelMax;
    }

    @Override
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
        this.scaleFactor = Math.pow(2, this.zoomLevel);
    }

    /**
     * Animates the center position of the map by the given amount of pixels.
     *
     * @param moveHorizontal the amount of pixels to move this MapViewPosition horizontally.
     * @param moveVertical   the amount of pixels to move this MapViewPosition vertically.
     */
    @Override
    public void moveCenter(double moveHorizontal, double moveVertical) {
        this.moveCenterAndZoom(moveHorizontal, moveVertical, (byte) 0, true);
    }

    /**
     * Moves the center position of the map by the given amount of pixels.
     *
     * @param moveHorizontal the amount of pixels to move this MapViewPosition horizontally.
     * @param moveVertical   the amount of pixels to move this MapViewPosition vertically.
     * @param animated       whether the move should be animated.
     */
    @Override
    public void moveCenter(double moveHorizontal, double moveVertical, boolean animated) {
        this.moveCenterAndZoom(moveHorizontal, moveVertical, (byte) 0, animated);
    }

    /**
     * Animates the center position of the map by the given amount of pixels.
     *
     * @param moveHorizontal the amount of pixels to move this MapViewPosition horizontally.
     * @param moveVertical   the amount of pixels to move this MapViewPosition vertically.
     * @param zoomLevelDiff  the difference in desired zoom level.
     */
    @Override
    public void moveCenterAndZoom(double moveHorizontal, double moveVertical, byte zoomLevelDiff) {
        moveCenterAndZoom(moveHorizontal, moveVertical, zoomLevelDiff, true);
    }

    /**
     * Moves the center position of the map by the given amount of pixels.
     *
     * @param moveHorizontal the amount of pixels to move this MapViewPosition horizontally.
     * @param moveVertical   the amount of pixels to move this MapViewPosition vertically.
     * @param zoomLevelDiff  the difference in desired zoom level.
     * @param animated       whether the move should be animated.
     */
    public void moveCenterAndZoom(double moveHorizontal, double moveVertical, byte zoomLevelDiff, boolean animated) {
        synchronized (this) {
            long mapSize = MercatorProjection.getMapSize(this.zoomLevel, this.displayModel.getTileSize());
            double pixelX = MercatorProjection.longitudeToPixelX(this.longitude, mapSize)
                    - moveHorizontal;
            double pixelY = MercatorProjection.latitudeToPixelY(this.latitude, mapSize) - moveVertical;

            pixelX = Math.min(Math.max(0, pixelX), mapSize);
            pixelY = Math.min(Math.max(0, pixelY), mapSize);

            double newLatitude = MercatorProjection.pixelYToLatitude(pixelY, mapSize);
            double newLongitude = MercatorProjection.pixelXToLongitude(pixelX, mapSize);
            setCenterInternal(newLatitude, newLongitude);
            setZoomLevelInternal(this.zoomLevel + zoomLevelDiff, animated);
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
    @Override
    public void setCenter(LatLong latLong) {
        synchronized (this) {
            setCenterInternal(latLong.latitude, latLong.longitude);
        }
        notifyObservers();
    }

    /**
     * Sets the new limit of the map (might be null).
     */
    @Override
    public void setMapLimit(BoundingBox mapLimit) {
        synchronized (this) {
            this.mapLimit = mapLimit;
        }
        notifyObservers();
    }

    /**
     * Sets the new center position and zoom level of the map.
     * <p/>
     * Note: The default zoom level changes are animated.
     */
    @Override
    public void setMapPosition(MapPosition mapPosition) {
        setMapPosition(mapPosition, true);
    }

    /**
     * Sets the new center position and zoom level of the map.
     */
    @Override
    public void setMapPosition(MapPosition mapPosition, boolean animated) {
        synchronized (this) {
            setCenterInternal(mapPosition.latLong.latitude, mapPosition.latLong.longitude);
            setZoomLevelInternal(mapPosition.zoomLevel, animated);
        }
        notifyObservers();
    }

    /**
     * The pivot point is the point the map is scaled around when zooming. In normal mode the pivot point is whatever
     * the view center is (this is indicated by setting the pivot to null), but when hand-zooming the pivot point can be
     * any point on the map. It is stored as lat/long and retrieved as an x/y coordinate depending on the current zoom
     * level.
     *
     * @param pivot lat/long of pivot point, null for map center
     */
    @Override
    public void setPivot(LatLong pivot) {
        synchronized (this) {
            this.pivot = pivot;
        }
    }

    /**
     * Sets the new scale factor to be applied.
     */
    public void setScaleFactor(double scaleFactor) {
        synchronized (this) {
            this.scaleFactor = scaleFactor;
        }
        notifyObservers();
    }

    @Override
    public void setScaleFactorAdjustment(double adjustment) {
        synchronized (this) {
            this.setScaleFactor(Math.pow(2, zoomLevel) * adjustment);
        }
        notifyObservers();
    }

    /**
     * Sets the new zoom level of the map.
     * <p/>
     * Note: The default zoom level changes are animated.
     *
     * @throws IllegalArgumentException if the zoom level is negative.
     */
    @Override
    public void setZoomLevel(byte zoomLevel) {
        setZoomLevel(zoomLevel, true);
    }

    /**
     * Sets the new zoom level of the map
     *
     * @param zoomLevel desired zoom level
     * @param animated  true if the transition should be animated, false otherwise
     * @throws IllegalArgumentException if the zoom level is negative.
     */
    @Override
    public void setZoomLevel(byte zoomLevel, boolean animated) {
        if (zoomLevel < 0) {
            throw new IllegalArgumentException("zoomLevel must not be negative: " + zoomLevel);
        }
        synchronized (this) {
            setZoomLevelInternal(zoomLevel, animated);
        }
        notifyObservers();
    }

    @Override
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

    @Override
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
     * <p/>
     * Note: The default zoom level changes are animated.
     */
    @Override
    public void zoom(byte zoomLevelDiff) {
        zoom(zoomLevelDiff, true);
    }

    /**
     * Changes the current zoom level by the given value if possible.
     */
    public void zoom(byte zoomLevelDiff, boolean animated) {
        synchronized (this) {
            setZoomLevelInternal(this.zoomLevel + zoomLevelDiff, animated);
        }
        notifyObservers();
    }

    /**
     * Increases the current zoom level by one if possible.
     * <p/>
     * Note: The default zoom level changes are animated.
     */
    @Override
    public void zoomIn() {
        zoomIn(true);
    }

    /**
     * Increases the current zoom level by one if possible.
     */
    @Override
    public void zoomIn(boolean animated) {
        zoom((byte) 1, animated);
    }

    /**
     * Decreases the current zoom level by one if possible.
     * <p/>
     * Note: The default zoom level changes are animated.
     */
    @Override
    public void zoomOut() {
        zoomOut(true);
    }

    /**
     * Decreases the current zoom level by one if possible.
     */
    @Override
    public void zoomOut(boolean animated) {
        zoom((byte) -1, animated);
    }

    private void setCenterInternal(double latitude, double longitude) {
        if (this.mapLimit == null) {
            this.latitude = latitude;
            this.longitude = longitude;
        } else {
            this.latitude = Math.max(Math.min(latitude, this.mapLimit.maxLatitude), this.mapLimit.minLatitude);
            this.longitude = Math.max(Math.min(longitude, this.mapLimit.maxLongitude), this.mapLimit.minLongitude);
        }
    }

    private void setZoomLevelInternal(int zoomLevel, boolean animated) {
        this.zoomLevel = (byte) Math.max(Math.min(zoomLevel, this.zoomLevelMax), this.zoomLevelMin);
        if (animated) {
            this.animator.startAnimationZoom(getScaleFactor(), Math.pow(2, this.zoomLevel));
        } else {
            this.setScaleFactor(Math.pow(2, this.zoomLevel));
            this.setPivot(null);
        }
    }
}
