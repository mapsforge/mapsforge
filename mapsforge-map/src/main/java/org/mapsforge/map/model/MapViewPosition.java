/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2015-2016 devemux86
 * Copyright 2015 Andreas Schildbach
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

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class MapViewPosition extends Observable implements Persistable {

    /**
     * This thread performs all user-driven animations for movement or zooming. Each animation request will be queued in a fifo queue and
     * processed one after each other. If the system has high cpu load the jobs will not fully animate but instead just perform a few or
     * even perform just the final step.
     */
    private class Animator extends Thread {

        private LinkedBlockingQueue<AbstractAnimationJob> jobs = new LinkedBlockingQueue<>();

        private AtomicBoolean working = new AtomicBoolean(false);

        public Animator() {
            // set thread priority above normal
            setPriority((Thread.NORM_PRIORITY + Thread.MAX_PRIORITY) / 2);
        }

        @Override
        public void run() {
            super.run();
            try {
                while (true) {
                    AbstractAnimationJob job = jobs.take();
                    if (job == null)
                        break;
                    working.set(true);
                    job.doWork();
                    working.set(false);
                }
            } catch (InterruptedException e) {
                working.set(false);
                //e.printStackTrace();
                // process is interrupted because it should end. Do nothing here
            }
        }

        public void put(AbstractAnimationJob job) {
            try {
                jobs.put(job);
            } catch (InterruptedException e) {
                // should not happen since we have no limitation on the size of the queue
                e.printStackTrace();
            }
        }

        public boolean hasWork() {
            return working.get() || jobs.size() > 0;
        }
    }

    /////////////////////////////////////////////////////////////////////////

    private abstract class AbstractAnimationJob {

        private long timeStart;
        private long timeEnd;

        AbstractAnimationJob() {
            timeStart = System.currentTimeMillis();
            timeEnd = timeStart + animationDuration;
        }

        private void doWork() {
            boolean cont = preparation();
            if (!cont)
                return;
            while (true) {
                long currentTime = System.currentTimeMillis();
                if (currentTime >= this.timeEnd) {
                    doEndWork();
                    return;
                } else {
                    float timeElapsedRatio = (currentTime - this.timeStart) / (1f * animationDuration);
                    doIntermediateWork(timeElapsedRatio);
                }
                try {
                    Thread.sleep(animationSleeptime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * Returns true if the job should continue, false if there is nothing to do
         *
         * @return
         */
        protected boolean preparation() {
            return true;
        }

        protected abstract void doEndWork();

        protected abstract void doIntermediateWork(float percent);
    }

    /////////////////////////////////////////////////////////////////////////

    private class MoveAnimationJob extends AbstractAnimationJob {

        private double destLatitude;
        private double destLongitude;

        private double startLatitude;
        private double startLongitude;
        private double diffLatitude;
        private double diffLongitude;

        protected MoveAnimationJob(double destLatitude, double destLongitude) {
            this.destLatitude = destLatitude;
            this.destLongitude = destLongitude;
        }

        @Override
        protected boolean preparation() {
            super.preparation();
            this.startLatitude = latitude;
            this.startLongitude = longitude;
            this.diffLatitude = destLatitude - startLatitude;
            this.diffLongitude = destLongitude - startLongitude;

            if (diffLongitude == 0 && diffLatitude == 0)
                return false;
            // TODO if it does not cost much cpu: check diff in pixels and if there is 0 or 1 pixels difference just perform doEndWork()
            return true;
        }

        @Override
        protected void doEndWork() {
            setCenter(startLatitude + diffLatitude, startLongitude + diffLongitude);
        }

        @Override
        protected void doIntermediateWork(float percent) {
            setCenter(startLatitude + diffLatitude * percent, startLongitude + diffLongitude * percent);
        }
    }

    /////////////////////////////////////////////////////////////////////////

    private class ZoomAnimationJob extends AbstractAnimationJob {

        private double destScaleFactor;

        private double startScaleFactor;
        private double diffScaleFactor;

        protected ZoomAnimationJob(double destScaleFactor) {
            this.destScaleFactor = destScaleFactor;
        }

        @Override
        protected boolean preparation() {
            super.preparation();
            startScaleFactor = scaleFactor;
            diffScaleFactor = destScaleFactor - startScaleFactor;
            if (diffScaleFactor == 0)
                return false;
            return true;
        }

        private double calculateScaleFactor(float percent) {
            return this.startScaleFactor + diffScaleFactor * percent;
        }

        @Override
        protected void doEndWork() {
            setScaleFactorPivot(startScaleFactor + diffScaleFactor, null);
        }

        @Override
        protected void doIntermediateWork(float percent) {
            setScaleFactor(calculateScaleFactor(percent));
        }
    }

    /////////////////////////////////////////////////////////////////////////

    private class MoveAndZoomAnimationJob extends AbstractAnimationJob {

        private double destLatitude;
        private double destLongitude;

        private double startLatitude;
        private double startLongitude;
        private double diffLatitude;
        private double diffLongitude;

        private double destScaleFactor;

        private double startScaleFactor;
        private double diffScaleFactor;

        protected MoveAndZoomAnimationJob(double destLatitude, double destLongitude, double destScaleFactor) {
            this.destLatitude = destLatitude;
            this.destLongitude = destLongitude;

            this.destScaleFactor = destScaleFactor;
        }

        @Override
        protected boolean preparation() {
            super.preparation();
            this.startLatitude = latitude;
            this.startLongitude = longitude;
            this.diffLatitude = destLatitude - startLatitude;
            this.diffLongitude = destLongitude - startLongitude;

            startScaleFactor = scaleFactor;
            diffScaleFactor = destScaleFactor - startScaleFactor;

            // TODO if it does not cost much cpu: check diff in pixels and if there is 0 or 1 pixels difference and no zoom just perform doEndWork()
            return true;
        }

        private double calculateScaleFactor(float percent) {
            return this.startScaleFactor + diffScaleFactor * percent;
        }

        @Override
        protected void doEndWork() {
            setScaleFactorCenter(startScaleFactor + diffScaleFactor, null, startLatitude + diffLatitude, startLongitude + diffLongitude);
        }

        @Override
        protected void doIntermediateWork(float percent) {
            setScaleFactorCenter(calculateScaleFactor(percent), pivot, startLatitude + diffLatitude * percent, startLongitude + diffLongitude * percent);
        }
    }

    /////////////////////////////////////////////////////////////////////////

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

    // debugging tip: for investigating what happens during the zoom animation
    // just make the times longer for duration and frame length
    private static int animationDuration = 250;
    private static int animationSleeptime = 15;

    private final DisplayModel displayModel;
    private double latitude;
    private double longitude;
    private BoundingBox mapLimit;
    private LatLong pivot;
    private double scaleFactor;
    private final Animator animator;
    private byte zoomLevel;
    private byte zoomLevelMax;
    private byte zoomLevelMin;

    public MapViewPosition(DisplayModel displayModel) {
        super();
        this.displayModel = displayModel;
        this.zoomLevelMax = Byte.MAX_VALUE;
        this.animator = new Animator();
        this.animator.start();
    }

    /**
     * Sets the duration for the animation in milliseconds
     *
     * @param animationDuration
     */
    public static void setAnimationDuration(int animationDuration) {
        MapViewPosition.animationDuration = animationDuration;
    }

    /**
     * Gets the duration for the animation in milliseconds
     *
     * @return
     */
    public static int getAnimationDuration() {
        return animationDuration;
    }

    /**
     * Sets the sleep time in milliseconds between the animation steps
     *
     * @param animationSleeptime
     */
    public static void setAnimationSleeptime(int animationSleeptime) {
        MapViewPosition.animationSleeptime = animationSleeptime;
    }

    /**
     * Gets the sleep time in milliseconds between the animation steps
     *
     * @return
     */
    public static int getAnimationSleeptime() {
        return animationSleeptime;
    }

    /**
     * Animate the map towards the given position. The position is assured to be within the maplimit if set.
     */
    public void animateTo(final LatLong latLong) {
        synchronized (this) {
            LatLong newPosition = latLong;
            if (this.mapLimit != null) {
                newPosition = new LatLong(Math.max(Math.min(latitude, this.mapLimit.maxLatitude), this.mapLimit.minLatitude),
                        Math.max(Math.min(longitude, this.mapLimit.maxLongitude), this.mapLimit.minLongitude));
            }
            MoveAnimationJob job = new MoveAnimationJob(newPosition.latitude, newPosition.longitude);
            animator.put(job);
        }
    }

    /**
     * Animate the map to the given zoomlevel. The zoomlevel is assured to be inbetween zoomLevelMin and zoomLevelMax.
     *
     * @param zoomLevel
     */
    private void animateToInternal(int zoomLevel) {
        synchronized (this) {
            byte newZoomLevel = (byte) Math.max(Math.min(zoomLevel, this.zoomLevelMax), this.zoomLevelMin);
            ZoomAnimationJob job = new ZoomAnimationJob(Math.pow(2, newZoomLevel));
            this.zoomLevel = newZoomLevel;
            animator.put(job);
        }
    }

    /**
     * Animate the map to the given zoomlevel. The zoomlevel is assured to be inbetween zoomLevelMin and zoomLevelMax.
     *
     * @param zoomLevel
     */
    public void animateTo(byte zoomLevel) {
        synchronized (this) {
            byte newZoomLevel = (byte) Math.max(Math.min(zoomLevel, this.zoomLevelMax), this.zoomLevelMin);
            ZoomAnimationJob job = new ZoomAnimationJob(Math.pow(2, newZoomLevel));
            this.zoomLevel = newZoomLevel;
            animator.put(job);
        }
    }

    /**
     * Animates the map to the given zoomlevel AND position.  The position is assured to be within the maplimit if set. The zoomlevel is assured to be inbetween zoomLevelMin and zoomLevelMax.
     *
     */
    public void animateTo(LatLong latLong, byte zoomLevel) {
        animateTo(latLong.latitude, latLong.longitude, zoomLevel);
    }

    public void animateTo(double latitude, double longitude, byte zoomLevel) {
        synchronized (this) {
            double newLatitude;
            double newLongitude;
            if (this.mapLimit != null) {
                newLatitude = Math.max(Math.min(latitude, this.mapLimit.maxLatitude), this.mapLimit.minLatitude);
                newLongitude = Math.max(Math.min(longitude, this.mapLimit.maxLongitude), this.mapLimit.minLongitude);
            } else {
                newLatitude = latitude;
                newLongitude = longitude;
            }
            byte newZoomLevel = (byte) Math.max(Math.min(zoomLevel, this.zoomLevelMax), this.zoomLevelMin);
            MoveAndZoomAnimationJob job = new MoveAndZoomAnimationJob(newLatitude, newLongitude, Math.pow(2, newZoomLevel));
            this.zoomLevel = newZoomLevel;
            animator.put(job);
        }
    }

    /**
     * Returns true if the animation is in progress or animation is in the queue for being processed.
     *
     * @return
     */
    public boolean animationInProgress() {
        return animator.hasWork(); //MercatorProjection.zoomLevelToScaleFactor(this.zoomLevel);
    }

    public void destroy() {
        this.animator.interrupt();
    }

    /**
     * Returns the current center position of the map. The position may change rapidly if an animation is in progress. If you are
     * using animation consider keeping the positions and zoomfactors in your code and not using this getters.
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
     * Returns a newly instantiated object representing the current position and zoomlevel. Note that the postion or zoomlevel may
     * change rapidly if an animation is in progress.
     *
     * @return the current center position and zoom level of the map.
     */
    public synchronized MapPosition getMapPosition() {
        return new MapPosition(getCenter(), this.zoomLevel);
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

    public synchronized double getScaleFactor() {
        return this.scaleFactor;
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
        this.scaleFactor = Math.pow(2, this.zoomLevel);
    }

    /**
     * Moves the center position of the map by the given amount of pixels.
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
     *
     * @param moveHorizontal the amount of pixels to move this MapViewPosition horizontally.
     * @param moveVertical   the amount of pixels to move this MapViewPosition vertically.
     * @param zoomLevelDiff  the difference in desired zoom level.
     */
    public void moveCenterAndZoom(double moveHorizontal, double moveVertical, byte zoomLevelDiff) {
        moveCenterAndZoom(moveHorizontal, moveVertical, zoomLevelDiff, true);
    }

    /**
     * Moves the center position of the map by the given amount of pixels and sets the zoomlevel. If animated is true the zoom will be animated (the movement won't)
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
            if (animated) {
                animateTo(newLatitude, newLongitude, (byte) (zoomLevel + zoomLevelDiff));
            } else {
                setCenterInternal(newLatitude, newLongitude);
                setZoomLevelInternal(this.zoomLevel + zoomLevelDiff);
                notifyObservers();
            }
        }
    }

    /**
     * Moves the center position of the map by the given amount of pixels.
     *
     * @param moveHorizontal the amount of pixels to move this MapViewPosition horizontally.
     * @param moveVertical   the amount of pixels to move this MapViewPosition vertically.
     */
    public void moveCenter(double moveHorizontal, double moveVertical) {
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
        setCenter(latLong.latitude, latLong.longitude);
    }

    /**
     * Sets the new center position of the map.
     */
    public void setCenter(double latitude, double longitude) {
        synchronized (this) {
            setCenterInternal(latitude, longitude);
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
     * Note: The default zoom level changes are animated.
     */
    public void setMapPosition(MapPosition mapPosition) {
        setMapPosition(mapPosition, true);
    }

    /**
     * Sets the new center position of the map. Parameter animated is not used.
     */
    public void setMapPosition(MapPosition mapPosition, boolean animated) {
        synchronized (this) {
            setCenterInternal(mapPosition.latLong.latitude, mapPosition.latLong.longitude);
            setZoomLevelInternal(mapPosition.zoomLevel);
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
    public void setPivot(LatLong pivot) {
        synchronized (this) {
            this.pivot = pivot;
        }
    }

    private void setScaleFactorPivot(double scaleFactor, LatLong pivot) {
        synchronized (this) {
            this.scaleFactor = scaleFactor;
            this.pivot = pivot;
        }
        notifyObservers();
    }

    private void setScaleFactorCenter(double scaleFactor, LatLong pivot, double latitude, double longitude) {
        synchronized (this) {
            this.scaleFactor = scaleFactor;
            this.pivot = pivot;
            setCenterInternal(latitude, longitude);
        }
        notifyObservers();

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
    public void setZoomLevel(byte zoomLevel) {
        setZoomLevel(zoomLevel, true);
    }

    /**
     * Sets the new zoom level of the map. Animation is used if the corresponding parameter is set to true
     *
     * @param zoomLevel desired zoom level
     * @param animated  true if the transition should be animated, false otherwise
     * @throws IllegalArgumentException if the zoom level is negative.
     */
    public void setZoomLevel(byte zoomLevel, boolean animated) {
        if (zoomLevel < 0) {
            throw new IllegalArgumentException("zoomLevel must not be negative: " + zoomLevel);
        }
        if (animated && this.zoomLevel != zoomLevel) {
            // perform animation only if there is something to do
            animateToInternal(zoomLevel);
        } else {

            synchronized (this) {
                setZoomLevelInternal(zoomLevel);
            }
            notifyObservers();
        }
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
     * Changes the current zoom level by the given value if possible. Animation is used if the parameter animation is set to true, otherwise the zoomlevel is set directly
     */
    public void zoom(byte zoomLevelDiff, boolean animated) {
        if (zoomLevelDiff == 0) {
            return;
        }
        if (animated) {
            // perform animation only if there is something to do
            animateToInternal(zoomLevel + zoomLevelDiff);
        } else {
            synchronized (this) {
                setZoomLevelInternal(this.zoomLevel + zoomLevelDiff);
            }
            notifyObservers();
        }
    }

    /**
     * Increases the current zoom level by one if possible.
     * <p/>
     * Note: The default zoom level changes are animated.
     */
    public void zoomIn() {
        zoomIn(true);
    }

    /**
     * Increases the current zoom level by one if possible.
     */
    public void zoomIn(boolean animated) {
        zoom((byte) 1, animated);
    }

    /**
     * Decreases the current zoom level by one if possible.
     * <p/>
     * Note: The default zoom level changes are animated.
     */
    public void zoomOut() {
        zoomOut(true);
    }

    /**
     * Decreases the current zoom level by one if possible.
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

    private void setZoomLevelInternal(int zoomLevel) {
        byte newZoomLevel = (byte) Math.max(Math.min(zoomLevel, this.zoomLevelMax), this.zoomLevelMin);
        this.setScaleFactor(Math.pow(2, newZoomLevel));
        this.setPivot(null);
        this.zoomLevel = newZoomLevel;
    }
}
