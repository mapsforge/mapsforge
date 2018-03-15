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
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.model.common.Observable;
import org.mapsforge.map.model.common.Persistable;
import org.mapsforge.map.model.common.PreferencesFacade;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This implementation uses a thread to animate the movement of the map. It has currently a problem while zooming out therefore this implementation
 * is considered as experimental and should not be used in production. This implementation does not use a pivot but instead changes the center
 * of the screen while zooming. Therefore we need a special implementation of {@link org.mapsforge.map.view.FrameBuffer} and {@link org.mapsforge.map.controller.FrameBufferController}
 * <p>
 * In order to use that class try the following:
 * <pre>
 *     {@code
 *             mapView = new MapView(getActivity()) {
 *             @Override
 *             protected Model createModel() {
 *             //super.createModel();
 *             return new Model() {
 *
 *             @Override
 *             protected IMapViewPosition createMapViewPosition(DisplayModel displayModel) {
 *             //return super.createMapViewPosition();
 *             return new AnimationQueueMapViewPosition(displayModel);
 *             }
 *             };
 *             }
 *             };
 *     }
 * </pre>
 */
public class AnimationQueueMapViewPosition extends Observable implements Persistable, IMapViewPosition {

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

        public void stopAnimation() {
            //jobs.clear();
            // flag to let the job stop
            stop.set(true);
            while (working.get()) {
                // while until the job is still running. If no job was running this loop is skipped
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            // remove the flag just in the case no job was active
            stop.set(false);
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
                if (stop.get()) {
                    doEndWork();
                    stop.set(false);
                    return;
                }
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
                    // we should stop. Do nothing here
                    //e.printStackTrace();
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
        protected void doIntermediateWork(float percent) {
            setCenter(startLatitude + diffLatitude * percent, startLongitude + diffLongitude * percent);
        }

        @Override
        protected void doEndWork() {
            setCenter(startLatitude + diffLatitude, startLongitude + diffLongitude);
        }

    }

    /////////////////////////////////////////////////////////////////////////

    private class ZoomAnimationJob extends AbstractAnimationJob {

        private double destScaleFactor;
        private byte destZoomLevel;

        private double startScaleFactor;
        private double diffScaleFactor;

        protected ZoomAnimationJob(byte destZoomLevel) {
            this.destScaleFactor = Math.pow(2, destZoomLevel);
            this.destZoomLevel = destZoomLevel;

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
        protected void doIntermediateWork(float percent) {
            setScaleFactor(calculateScaleFactor(percent));
        }

        @Override
        protected void doEndWork() {
            setScaleFactor(destScaleFactor);
            AnimationQueueMapViewPosition.this.zoomLevel = destZoomLevel;
        }

    }

    /////////////////////////////////////////////////////////////////////////

    private class MoveAndZoomAnimationJob extends AbstractAnimationJob {

        private double destLatitude;
        private double destLongitude;
        private double destScaleFactor;
        private byte destZoomLevel;


        private double startLatitude;
        private double startLongitude;
        private double diffLatitude;
        private double diffLongitude;


        private double startScaleFactor;
        private double diffScaleFactor;

        protected MoveAndZoomAnimationJob(double destLatitude, double destLongitude, byte destZoomLevel) {
            this.destScaleFactor = Math.pow(2, destZoomLevel);
            this.destZoomLevel = destZoomLevel;
            if (mapLimit == null) {
                destLatitude = Math.max(Math.min(destLatitude, 90), -90.0);
                destLongitude = Math.max(Math.min(destLongitude, 180), -180.0);
            } else {
                destLatitude = Math.max(Math.min(destLatitude, mapLimit.maxLatitude), mapLimit.minLatitude);
                destLongitude = Math.max(Math.min(destLongitude, mapLimit.maxLongitude), mapLimit.minLongitude);
            }
            this.destLatitude = destLatitude;
            this.destLongitude = destLongitude;
        }

        @Override
        protected boolean preparation() {
            super.preparation();

            startScaleFactor = scaleFactor;
            diffScaleFactor = destScaleFactor - startScaleFactor;

//            if (diffScaleFactor == 0)
//                return false;
            startLatitude = latitude;
            startLongitude = longitude;
            diffLatitude = destLatitude - startLatitude;
            diffLongitude = destLongitude - startLongitude;
            return true;
        }

        private double calculateScaleFactor(float percent) {
            return this.startScaleFactor + diffScaleFactor * percent;
        }

        @Override
        protected void doIntermediateWork(float percent) {
            setScaleFactorCenter(calculateScaleFactor(percent), startLatitude + diffLatitude * percent, startLongitude + diffLongitude * percent);
        }

        @Override
        protected void doEndWork() {
            AnimationQueueMapViewPosition.this.zoomLevel = destZoomLevel;
            setScaleFactorCenter(calculateScaleFactor(1), destLatitude, destLongitude);
            //setPivot(null);
            //System.out.println("moveZoomAnimation last step: " + calculateScaleFactor(1) + ", " + destLatitude + "/" + destLongitude);
        }

        @Override
        public String toString() {
            return "MoveAndZoomAnimationJob{" +
                    "destScaleFactor=" + destScaleFactor +
                    ", destZoomLevel=" + destZoomLevel +
                    ", startLatitude=" + startLatitude +
                    ", startLongitude=" + startLongitude +
                    ", diffLatitude=" + diffLatitude +
                    ", diffLongitude=" + diffLongitude +
                    ", destLatitude=" + destLatitude +
                    ", destLongitude=" + destLongitude +
                    ", startScaleFactor=" + startScaleFactor +
                    ", diffScaleFactor=" + diffScaleFactor +
                    "} " + super.toString();
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
    private static int animationDuration = 300;
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
    private static final AtomicBoolean stop = new AtomicBoolean();


    public AnimationQueueMapViewPosition(DisplayModel displayModel) {
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
        AnimationQueueMapViewPosition.animationDuration = animationDuration;
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
        AnimationQueueMapViewPosition.animationSleeptime = animationSleeptime;
    }

    /**
     * Gets the sleep time in milliseconds between the animation steps
     *
     * @return
     */
    public static int getAnimationSleeptime() {
        return animationSleeptime;
    }


    //@Override
    public void animateTo(byte zoomLevel) {
        byte newZoomLevel = (byte) Math.max(Math.min(zoomLevel, this.zoomLevelMax), this.zoomLevelMin);
        ZoomAnimationJob zoomAnimationJob = new ZoomAnimationJob(newZoomLevel);
        animator.put(zoomAnimationJob);
    }

    @Override
    public void animateTo(final LatLong latLong) {
        synchronized (this) {
            LatLong newPosition = latLong;
            if (this.mapLimit != null) {
                newPosition = new LatLong(Math.max(Math.min(newPosition.latitude, this.mapLimit.maxLatitude), this.mapLimit.minLatitude),
                        Math.max(Math.min(newPosition.longitude, this.mapLimit.maxLongitude), this.mapLimit.minLongitude));
            }
            MoveAnimationJob job = new MoveAnimationJob(newPosition.latitude, newPosition.longitude);
            animator.put(job);
        }
    }

    //@Override
    public void animateTo(LatLong latLong, byte zoomLevel) {
        synchronized (this) {
            if (latLong == null) {
                animateTo(zoomLevel);
            } else {
                LatLong newPosition = latLong;
                if (this.mapLimit != null) {
                    newPosition = new LatLong(Math.max(Math.min(newPosition.latitude, this.mapLimit.maxLatitude), this.mapLimit.minLatitude),
                            Math.max(Math.min(newPosition.longitude, this.mapLimit.maxLongitude), this.mapLimit.minLongitude));
                }
                byte newZoomLevel = (byte) Math.max(Math.min(zoomLevel, this.zoomLevelMax), this.zoomLevelMin);
                MoveAndZoomAnimationJob job = new MoveAndZoomAnimationJob(newPosition.getLatitude(), newPosition.getLongitude(), newZoomLevel);
                animator.put(job);
            }
        }
    }

    //@Override
    public void animateToPivot(LatLong pivot, byte zoomLevelDiff) {
        synchronized (this) {
            byte newZoomLevel = (byte) Math.max(Math.min(zoomLevel + zoomLevelDiff, this.zoomLevelMax), this.zoomLevelMin);
            if (pivot == null) {
                ZoomAnimationJob job = new ZoomAnimationJob(newZoomLevel);
                animator.put(job);
            } else {
                double factor = Math.pow(2, zoomLevelDiff);

                long mapSize = MercatorProjection.getMapSize(zoomLevel, displayModel.getTileSize());
                double oldCenterLatitudePixel = MercatorProjection.latitudeToPixelY(this.latitude, mapSize);
                double oldCenterLongitudePixel = MercatorProjection.longitudeToPixelX(this.longitude, mapSize);
                double pivotLatitudePixel = MercatorProjection.latitudeToPixelY(pivot.latitude, mapSize);
                double pivotLongitudePixel = MercatorProjection.longitudeToPixelX(pivot.longitude, mapSize);
                double newCenterLatitude = MercatorProjection.pixelYToLatitude(pivotLatitudePixel - (pivotLatitudePixel - oldCenterLatitudePixel) / factor, mapSize);
                double newCenterLongitude = MercatorProjection.pixelXToLongitude(pivotLongitudePixel - (pivotLongitudePixel - oldCenterLongitudePixel) / factor, mapSize);
                if (mapLimit != null) {
                    newCenterLatitude = Math.max(Math.min(newCenterLatitude, this.mapLimit.maxLatitude), this.mapLimit.minLatitude);
                    newCenterLongitude = Math.max(Math.min(newCenterLongitude, this.mapLimit.maxLongitude), this.mapLimit.minLongitude);
                }
                MoveAndZoomAnimationJob job = new MoveAndZoomAnimationJob(newCenterLatitude, newCenterLongitude, newZoomLevel);
                animator.put(job);
            }
        }
    }

    @Override
    public boolean animationInProgress() {
        return animator.hasWork(); //MercatorProjection.zoomLevelToScaleFactor(this.zoomLevel);
    }

    //@Override
    public void stopAnimation() {
        animator.stopAnimation();
        pivot = null;
    }

    @Override
    public LatLong getPivot() {
        return pivot;
    }

    @Override
    public void destroy() {
        this.animator.interrupt();
    }

    @Override
    public synchronized LatLong getCenter() {
        return new LatLong(this.latitude, this.longitude);
    }

    /**
     * @return the current limit of the map (might be null).
     */
    public synchronized BoundingBox getMapLimit() {
        return this.mapLimit;
    }

    @Override
    public synchronized MapPosition getMapPosition() {
        return new MapPosition(getCenter(), this.zoomLevel);
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

    @Override
    public void setScaleFactorAdjustment(double scaleFactorCumulative) {
        // not implemented because I want to change the api in order to avoid this call
    }

    /**
     * Moves the center position of the map by the given amount of pixels.
     *
     * @param moveHorizontal the amount of pixels to move this AnimationQueueMapViewPosition horizontally.
     * @param moveVertical   the amount of pixels to move this AnimationQueueMapViewPosition vertically.
     * @param animated       whether the move should be animated.
     */
    @Override
    public void moveCenter(double moveHorizontal, double moveVertical, boolean animated) {
        this.moveCenterAndZoom(moveHorizontal, moveVertical, (byte) 0, animated);
    }

    /**
     * Moves the center position of the map by the given amount of pixels and sets the zoomlevel. If animated is true the zoom will be animated (the movement won't)
     *
     * @param moveHorizontal the amount of pixels to move this AnimationQueueMapViewPosition horizontally.
     * @param moveVertical   the amount of pixels to move this AnimationQueueMapViewPosition vertically.
     * @param zoomLevelDiff  the difference in desired zoom level.
     * @param animated       whether the move should be animated.
     */
    private void moveCenterAndZoom(double moveHorizontal, double moveVertical, byte zoomLevelDiff, boolean animated) {
        synchronized (this) {
            long mapSize = MercatorProjection.getMapSize(this.zoomLevel, this.displayModel.getTileSize());
            double pixelX = MercatorProjection.longitudeToPixelX(this.longitude, mapSize)
                    - moveHorizontal;
            double pixelY = MercatorProjection.latitudeToPixelY(this.latitude, mapSize) - moveVertical;

            pixelX = Math.min(Math.max(0, pixelX), mapSize);
            pixelY = Math.min(Math.max(0, pixelY), mapSize);

            double newLatitude = MercatorProjection.pixelYToLatitude(pixelY, mapSize);
            double newLongitude = MercatorProjection.pixelXToLongitude(pixelX, mapSize);
            byte newZoomLevel = (byte) Math.max(Math.min(zoomLevel, this.zoomLevelMax), this.zoomLevelMin);
            if (animated) {
                animateTo(new LatLong(newLatitude, newLongitude), newZoomLevel);
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
     * @param moveHorizontal the amount of pixels to move this AnimationQueueMapViewPosition horizontally.
     * @param moveVertical   the amount of pixels to move this AnimationQueueMapViewPosition vertically.
     */
    @Override
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
     * <p>
     * //     * @param pivot lat/long of pivot point, null for map center
     */
//    public void setPivot(LatLong pivot) {
//        synchronized (this) {
//            this.pivot = pivot;
//        }
//    }
    private void setScaleFactorCenter(double scaleFactor, double latitude, double longitude) {
        synchronized (this) {
            this.scaleFactor = scaleFactor;
            setCenterInternal(latitude, longitude);
        }
        notifyObservers();

    }

    /**
     * Sets the new scale factor to be applied.
     */
    private void setScaleFactor(double scaleFactor) {
        synchronized (this) {
            this.scaleFactor = scaleFactor;
        }
        notifyObservers();
    }

    //@Override
    public void setScaleFactorAdjustment(LatLong pivot, double adjustment) {
        synchronized (this) {
            this.pivot = pivot;
            double destScaleFactor = (Math.pow(2, zoomLevel) * adjustment);
            this.scaleFactor = destScaleFactor;
        }
        notifyObservers();
    }

    @Override
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
     * <p/>
     * Note: The default zoom level changes are animated.
     */
    //@Override
    public void zoom(LatLong pivot, byte zoomLevelDiff) {
        zoom(pivot, zoomLevelDiff, true);
    }

    /**
     * Changes the current zoom level by the given value if possible. Animation is used if the parameter animation is set to true, otherwise the zoomlevel is set directly
     */
    private void zoom(LatLong pivot, byte zoomLevelDiff, boolean animated) {
        if (zoomLevelDiff == 0 && scaleFactor == Math.pow(2, zoomLevel)) {
            // scale factor is equal to the current zoomlevel and we do not want to change the zoomlevel, abort process
            return;
        }
        if (animated) {
            animateTo(pivot, zoomLevelDiff);
        } else {
            synchronized (this) {
                setZoomLevelInternal(this.zoomLevel + zoomLevelDiff);
            }
            notifyObservers();
        }
    }

    @Override
    public void zoomIn() {
        zoom(null, (byte) 1, true);
    }

    @Override
    public void zoomOut() {
        zoom(null, (byte) -1, true);
    }

    @Override
    public void zoom(byte zoomLevelDiff) {
        zoom(null, zoomLevelDiff, true);
    }

    private void setCenterInternal(double latitude, double longitude) {
        if (this.mapLimit == null) {
            this.latitude = Math.max(Math.min(latitude, 90), -90.0);
            this.longitude = Math.max(Math.min(longitude, 180), -180.0);
        } else {
            this.latitude = Math.max(Math.min(latitude, this.mapLimit.maxLatitude), this.mapLimit.minLatitude);
            this.longitude = Math.max(Math.min(longitude, this.mapLimit.maxLongitude), this.mapLimit.minLongitude);
        }
    }

    private void setZoomLevelInternal(int zoomLevel) {
        byte newZoomLevel = (byte) Math.max(Math.min(zoomLevel, this.zoomLevelMax), this.zoomLevelMin);
        this.setScaleFactor(Math.pow(2, newZoomLevel));
        this.zoomLevel = newZoomLevel;
    }

    @Override
    public void setPivot(LatLong pivot) {

    }

    @Override
    public void moveCenterAndZoom(double moveHorizontal, double moveVertical, byte zoomLevelDiff) {

    }
}
