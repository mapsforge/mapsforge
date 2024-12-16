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

import org.mapsforge.core.model.*;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.core.util.Parameters;
import org.mapsforge.map.model.common.Observable;
import org.mapsforge.map.model.common.Persistable;
import org.mapsforge.map.model.common.PreferencesFacade;
import org.mapsforge.map.util.PausableThread;

public class MapViewPosition extends Observable implements Persistable {

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

        private void doWorkZoom() {
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
            if (Parameters.FRACTIONAL_ZOOM) {
                mapSize = MercatorProjection.getMapSizeWithScaleFactor(scaleFactor, displayModel.getTileSize());
            } else {
                mapSize = MercatorProjection.getMapSize(zoomLevel, displayModel.getTileSize());
            }
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

    private static final double LOG_2 = Math.log(2);

    private static final String LATITUDE = "latitude";
    private static final String LATITUDE_MAX = "latitudeMax";
    private static final String LATITUDE_MIN = "latitudeMin";
    private static final String LONGITUDE = "longitude";
    private static final String LONGITUDE_MAX = "longitudeMax";
    private static final String LONGITUDE_MIN = "longitudeMin";
    private static final String ROTATION_ANGLE = "rotationAngle";
    private static final String ROTATION_PX = "rotationPx";
    private static final String ROTATION_PY = "rotationPy";
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
    private float mapViewCenterX = 0.5f;
    private float mapViewCenterY = 0.5f;
    private LatLong pivot;
    private Rotation rotation;
    private double scaleFactor;
    private byte zoomLevel, zoomLevelMax, zoomLevelMin;

    public MapViewPosition(DisplayModel displayModel) {
        super();
        this.displayModel = displayModel;
        this.zoomLevelMax = Byte.MAX_VALUE;
        this.animator = new Animator();
        this.animator.start();
        this.rotation = Rotation.NULL_ROTATION;
        this.scaleFactor = 1;
    }

    /**
     * Animates the map towards the given position (move). The given position will be at the center
     * of the screen after the animation. The position is assured to be within the map limit.
     */
    public void animateTo(final LatLong latLong) {
        animator.startAnimationMove(latLong);
    }

    /**
     * @return true if animation is in progress or animation is in the queue for being processed.
     */
    public boolean animationInProgress() {
        if (Parameters.FRACTIONAL_ZOOM) {
            return this.zoomLevel + 1 < getZoom();
        } else {
            return this.scaleFactor != MercatorProjection.zoomLevelToScaleFactor(this.zoomLevel);
        }
    }

    /**
     * Destroys the class.
     */
    public void destroy() {
        this.animator.finish();
    }

    /**
     * Returns the current center position of the map. The position may change rapidly if an
     * animation is in progress. If you are using animation consider keeping the positions and
     * zoom factors in your code and not using this getter.
     *
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
     * Returns a newly instantiated object representing the current position and zoomlevel.
     * Note that the position or zoomlevel may change rapidly if an animation is in progress.
     *
     * @return the current center position and zoom level of the map.
     */
    public synchronized MapPosition getMapPosition() {
        if (Parameters.FRACTIONAL_ZOOM) {
            return new MapPosition(getCenter(), getZoom(), this.rotation);
        } else {
            return new MapPosition(getCenter(), this.zoomLevel, this.rotation);
        }
    }

    public synchronized float getMapViewCenterX() {
        return this.mapViewCenterX;
    }

    public synchronized float getMapViewCenterY() {
        return this.mapViewCenterY;
    }

    /**
     * The pivot point is the point the map zooms around.
     *
     * @return the lat/long coordinates of the map pivot point if set or null otherwise.
     */
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

    public synchronized Rotation getRotation() {
        return this.rotation;
    }

    /**
     * Gets the current scale factor. The scale factor is normally 2^zoomLevel but it may differ if
     * in between a zoom-animation or in between a pinch-to-zoom process.
     * <p>
     * Needed by mapsforge core classes.
     */
    public synchronized double getScaleFactor() {
        return this.scaleFactor;
    }

    /**
     * @return the current zoom of the map.
     */
    public synchronized double getZoom() {
        return Math.log(this.scaleFactor) / LOG_2;
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

    /**
     * Initializes the class.
     * <p>
     * Needed by mapsforge core classes.
     */
    @Override
    public synchronized void init(PreferencesFacade preferencesFacade) {
        this.latitude = preferencesFacade.getDouble(LATITUDE, 0);
        this.longitude = preferencesFacade.getDouble(LONGITUDE, 0);
        float rotationAngle = preferencesFacade.getFloat(ROTATION_ANGLE, 0);
        float rotationPx = preferencesFacade.getFloat(ROTATION_PX, 0);
        float rotationPy = preferencesFacade.getFloat(ROTATION_PY, 0);
        this.rotation = new Rotation(rotationAngle, rotationPx, rotationPy);

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
     * <p>
     * Used by TouchGestureHandler.
     *
     * @param moveHorizontal the amount of pixels to move this MapViewPosition horizontally.
     * @param moveVertical   the amount of pixels to move this MapViewPosition vertically.
     */
    public void moveCenter(double moveHorizontal, double moveVertical) {
        this.moveCenterAndZoom(moveHorizontal, moveVertical, (byte) 0, true);
    }

    /**
     * Moves the center position of the map by the given amount of pixels.
     * <p>
     * Used by TouchGestureHandler.
     *
     * @param moveHorizontal the amount of pixels to move this MapViewPosition horizontally.
     * @param moveVertical   the amount of pixels to move this MapViewPosition vertically.
     * @param animated       whether the move should be animated.
     */
    public void moveCenter(double moveHorizontal, double moveVertical, boolean animated) {
        this.moveCenterAndZoom(moveHorizontal, moveVertical, (byte) 0, animated);
    }

    /**
     * Animates the center position of the map by the given amount of pixels.
     * <p>
     * This method is used by the TouchGestureHandler to perform a zoom on double-tap action.
     *
     * @param moveHorizontal the amount of pixels to move this MapViewPosition horizontally.
     * @param moveVertical   the amount of pixels to move this MapViewPosition vertically.
     * @param zoomLevelDiff  the difference in desired zoom level.
     */
    public void moveCenterAndZoom(double moveHorizontal, double moveVertical, byte zoomLevelDiff) {
        moveCenterAndZoom(moveHorizontal, moveVertical, zoomLevelDiff, true);
    }

    /**
     * Moves the center position of the map by the given amount of pixels.
     * <p>
     * This method is used by the TouchGestureHandler to perform a zoom on double-tap action.
     *
     * @param moveHorizontal the amount of pixels to move this MapViewPosition horizontally.
     * @param moveVertical   the amount of pixels to move this MapViewPosition vertically.
     * @param zoomLevelDiff  the difference in desired zoom level.
     * @param animated       whether the move should be animated.
     */
    public void moveCenterAndZoom(double moveHorizontal, double moveVertical, byte zoomLevelDiff, boolean animated) {
        synchronized (this) {
            long mapSize;
            if (Parameters.FRACTIONAL_ZOOM) {
                mapSize = MercatorProjection.getMapSizeWithScaleFactor(this.scaleFactor, this.displayModel.getTileSize());
            } else {
                mapSize = MercatorProjection.getMapSize(this.zoomLevel, this.displayModel.getTileSize());
            }
            double pixelX = MercatorProjection.longitudeToPixelX(this.longitude, mapSize) - moveHorizontal;
            double pixelY = MercatorProjection.latitudeToPixelY(this.latitude, mapSize) - moveVertical;

            pixelX = Math.min(Math.max(0, pixelX), mapSize);
            pixelY = Math.min(Math.max(0, pixelY), mapSize);

            double newLatitude = MercatorProjection.pixelYToLatitude(pixelY, mapSize);
            double newLongitude = MercatorProjection.pixelXToLongitude(pixelX, mapSize);
            setCenterInternal(newLatitude, newLongitude);
            if (zoomLevelDiff != 0) {
                setZoomLevelInternal(this.zoomLevel + zoomLevelDiff, animated);
            }
        }
        notifyObservers();
    }

    /**
     * Saves the current state.
     * <p>
     * Needed by mapsforge core classes.
     */
    @Override
    public synchronized void save(PreferencesFacade preferencesFacade) {
        preferencesFacade.putDouble(LATITUDE, this.latitude);
        preferencesFacade.putDouble(LONGITUDE, this.longitude);
        preferencesFacade.putFloat(ROTATION_ANGLE, this.rotation.degrees);
        preferencesFacade.putFloat(ROTATION_PX, this.rotation.px);
        preferencesFacade.putFloat(ROTATION_PY, this.rotation.py);

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
     * <p>
     * This is NOT animated.
     */
    public void setCenter(LatLong latLong) {
        synchronized (this) {
            setCenterInternal(latLong.latitude, latLong.longitude);
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
     * <p/>
     * The default zoom level changes are animated.
     */
    public void setMapPosition(MapPosition mapPosition) {
        setMapPosition(mapPosition, true);
    }

    /**
     * Sets the new center position and zoom level of the map.
     */
    public void setMapPosition(MapPosition mapPosition, boolean animated) {
        synchronized (this) {
            setCenterInternal(mapPosition.latLong.latitude, mapPosition.latLong.longitude);
            if (Parameters.FRACTIONAL_ZOOM) {
                setZoomInternal(mapPosition.zoom, animated);
            } else {
                setZoomLevelInternal(mapPosition.zoomLevel, animated);
            }
            setRotationInternal(mapPosition.rotation);
        }
        notifyObservers();
    }

    public void setMapViewCenterX(float mapViewCenterX) {
        if (mapViewCenterX < 0 || mapViewCenterX > 1) {
            throw new IllegalArgumentException("mapViewCenterX must be between 0 and 1: " + mapViewCenterX);
        }
        synchronized (this) {
            this.mapViewCenterX = mapViewCenterX;
        }
    }

    public void setMapViewCenterY(float mapViewCenterY) {
        if (mapViewCenterY < 0 || mapViewCenterY > 1) {
            throw new IllegalArgumentException("mapViewCenterY must be between 0 and 1: " + mapViewCenterY);
        }
        synchronized (this) {
            this.mapViewCenterY = mapViewCenterY;
        }
    }

    /**
     * The pivot point is the point the map is scaled around when zooming. In normal mode the pivot point is whatever
     * the view center is (this is indicated by setting the pivot to null), but when hand-zooming the pivot point can be
     * any point on the map. It is stored as lat/long and retrieved as an x/y coordinate depending on the current zoom
     * level.
     *
     * @param pivot lat/long of pivot point, null for map center
     */
    public void setPivot(LatLong pivot) {
        synchronized (this) {
            this.pivot = pivot;
        }
    }

    /**
     * Sets the new rotation of the map.
     */
    public void setRotation(Rotation rotation) {
        synchronized (this) {
            setRotationInternal(rotation);
        }
        notifyObservers();
    }

    /**
     * Sets the new scale factor to be applied.
     */
    public void setScaleFactor(double scaleFactor) {
        synchronized (this) {
            this.scaleFactor = Math.max(1, scaleFactor);
        }
        notifyObservers();
    }

    public void setScaleFactorAdjustment(double adjustment) {
        synchronized (this) {
            this.setScaleFactor(Math.pow(2, zoomLevel) * adjustment);
        }
        notifyObservers();
    }

    /**
     * Sets the new zoom of the map.
     * <p>
     * Animation is NOT used.
     * <p>
     * Implemented for external use, NOT used internally.
     *
     * @param zoom new zoom.
     * @throws IllegalArgumentException if the zoom is negative.
     */
    public void setZoom(double zoom) {
        setZoom(zoom, true);
    }

    /**
     * Sets the new zoom of the map.
     *
     * @param zoom     desired zoom
     * @param animated true if the transition should be animated, false otherwise
     * @throws IllegalArgumentException if the zoom is negative.
     */
    public void setZoom(double zoom, boolean animated) {
        if (zoom < 0) {
            throw new IllegalArgumentException("zoom must not be negative: " + zoom);
        }
        synchronized (this) {
            setZoomInternal(zoom, animated);
        }
        notifyObservers();
    }

    /**
     * Sets the new zoom level of the map.
     * <p>
     * Animation is NOT used.
     * <p>
     * Implemented for external use, NOT used internally.
     *
     * @param zoomLevel new zoom level.
     * @throws IllegalArgumentException if the zoom level is negative.
     */
    public void setZoomLevel(byte zoomLevel) {
        setZoomLevel(zoomLevel, true);
    }

    /**
     * Sets the new zoom level of the map.
     *
     * @param zoomLevel desired zoom level
     * @param animated  true if the transition should be animated, false otherwise
     * @throws IllegalArgumentException if the zoom level is negative.
     */
    public void setZoomLevel(byte zoomLevel, boolean animated) {
        if (zoomLevel < 0) {
            throw new IllegalArgumentException("zoomLevel must not be negative: " + zoomLevel);
        }
        synchronized (this) {
            setZoomLevelInternal(zoomLevel, animated);
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
     * <p/>
     * Note: The default zoom level changes are animated.
     */
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
     * Increases the current zoom level by one if possible. Zooming is always around the center.
     * <p/>
     * The default zoom level changes are animated.
     */
    public void zoomIn() {
        zoomIn(true);
    }

    /**
     * Increases the current zoom level by one if possible. Zooming is always around the center.
     */
    public void zoomIn(boolean animated) {
        zoom((byte) 1, animated);
    }

    /**
     * Decreases the current zoom level by one if possible. Zooming is always around the center.
     * <p/>
     * The default zoom level changes are animated.
     */
    public void zoomOut() {
        zoomOut(true);
    }

    /**
     * Decreases the current zoom level by one if possible. Zooming is always around the center.
     */
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

    private void setRotationInternal(Rotation rotation) {
        this.rotation = rotation;
    }

    private void setZoomInternal(double zoom, boolean animated) {
        zoom = Math.max(Math.min(zoom, this.zoomLevelMax), this.zoomLevelMin);
        this.zoomLevel = (byte) Math.floor(zoom);
        if (animated) {
            this.animator.startAnimationZoom(getScaleFactor(), Math.pow(2, zoom));
        } else {
            this.setScaleFactor(Math.pow(2, zoom));
            this.setPivot(null);
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
