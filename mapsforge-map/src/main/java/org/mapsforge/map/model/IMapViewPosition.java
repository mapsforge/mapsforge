package org.mapsforge.map.model;

import org.mapsforge.core.model.ExtendedMapPosition;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.map.model.common.Observer;
import org.mapsforge.map.model.common.PreferencesFacade;

/**
 * An interface to being able to use different implementations of the viewPosition.
 */

public interface IMapViewPosition {

    /**
     * initializes the class.
     * Needed by mapsforge core classes
     *
     * @param preferencesFacade
     */
    void init(PreferencesFacade preferencesFacade);

    /**
     * Destroys the class
     */
    void destroy();


    /**
     * Saves the current state.
     * Needed by mapsforge core classes
     *
     * @param preferencesFacade
     */
    void save(PreferencesFacade preferencesFacade);

    /**
     * Adds an observer to the class.
     * Needed by mapsforge core classes
     *
     * @param observer
     */
    void addObserver(Observer observer);

    /**
     * Removes an observer from the class.
     * Needed by mapsforge core classes
     *
     * @param observer
     */
    void removeObserver(Observer observer);

    /**
     * Animate the map towards the given position (move). The given position will be at the center of the
     * screen after the animation. The position is assured to be within the maplimit.
     */
    void animateTo(final LatLong latLong);

    /**
     * Animate the map to the given zoomlevel (zoom). The zoomlevel is assured to be inbetween zoomLevelMin and zoomLevelMax. The
     * center is moved to the given position while zooming.
     * <p>
     * NOT used internally
     *
     * @param latLong     the new center of the map or null
     * @param zoomLevel the new zoomlevel
     */
    void animateTo(LatLong latLong, byte zoomLevel);

    /**
     * This method moves and zooms the map to the desired position and zoomlevel-difference. Note that the pivot is NOT the center of the screen but
     * instead points to the lat/lon coordinates which should be fixed while moving/zooming everything around this coordinate. The new center of the
     * screen is set based on the pivot (which should not move on screen) and the given new zoomLevel.
     * <p>
     * touchGestureHandler calls this method as an alternative to
     * mapViewPosition.setPivot(pivot); and
     * mapViewPosition.moveCenterAndZoom(moveHorizontal, moveVertical, zoomLevelDiff);
     * <p>
     * NOTE: Internal use only
     *
     * @param pivot the pivot point which should be fixed during the animation or null if the map should be zoomed around the center
     * @param zoomLevelDiff the difference of the zoomlevel, positive values for zooming in, negative values for zooming-out
     */
    void animateToPivot(LatLong pivot, byte zoomLevelDiff);

    /**
     * animates the new zoom level of the map. If a pivot is set the zoom takes place around pivot and pivot is deleted afterwards.
     * <p/>
     * // TODO better to ignore/delete pivot or set the pivot with this method
     *
     * @throws IllegalArgumentException if the zoom level is negative.
     */
    void animateTo(byte zoomLevel);

    /**
     * Gets the current scale factor. The scale factor is normally 2^zoomLevel but it may differ if inbetween a zoom-animation or
     * inbetween a pinch-to-zoom process.
     * Needed by mapsforge core classes
     *
     * @return
     */
    double getScaleFactor();

    /**
     * Returns a newly instantiated object representing the current position and zoomlevel. Note that the postion or zoomlevel may
     * change rapidly if an animation is in progress.
     *
     * @return the current center position and zoom level of the map.
     */
    ExtendedMapPosition getMapPosition();

    /**
     * Increases the current zoom level by one if possible. Zooming is always around the center.
     * <p/>
     * Note: The default zoom level changes are animated.
     */
    void zoomIn();

    /**
     * Decreases the current zoom level by one if possible. Zooming is always around the center.
     * <p/>
     * Note: The default zoom level changes are animated.
     */
    void zoomOut();

    /**
     * @return the current zoom level of the map.
     */
    byte getZoomLevel();

    /**
     * Returns true if the animation is in progress or animation is in the queue for being processed.
     *
     * @return
     */
    boolean animationInProgress();

    /**
     * Stops running or queued animations and wait until the animation has been stopped
     */
    void stopAnimation();

    /**
     * Returns the current center position of the map. The position may change rapidly if an animation is in progress. If you are
     * using animation consider keeping the positions and zoomfactors in your code and not using this getters.
     *
     * @return the current center position of the map.
     */
    LatLong getCenter();

    /**
     * Sets the new center position of the map. This is NOT animated
     */
    void setCenter(LatLong latLong);

    /**
     * Sets the new center position and zoom level of the map.
     * <p/>
     * Note: The default zoom level changes are animated.
     */
    void setMapPosition(MapPosition mapPosition);

    /**
     * Sets the new zoom level of the map. Animation is NOT used
     * <p>
     * implemented for external use, NOT used internally.
     *
     * @param zoomLevel new zoom level
     * @throws IllegalArgumentException if the zoom level is negative.
     */
    void setZoomLevel(byte zoomLevel);

    /**
     * Changes the current zoom level by the given value if possible.
     * <p/>
     * Note: The default zoom level changes are animated.
     *
     * @param pivot the pivot to zoom around or null to zoom around the center of the screen
     *
     */
    void zoom(LatLong pivot, byte zoomLevelDiff);

    byte getZoomLevelMin();

    byte getZoomLevelMax();

    void setZoomLevelMin(byte zoomLevelMin);

    void setZoomLevelMax(byte zoomLevelMax);

    /**
     * Moves the center position of the map by the given amount of pixels. Does NOT animate the move
     *
     * @param moveHorizontal the amount of pixels to move this AnimationQueueMapViewPosition horizontally.
     * @param moveVertical   the amount of pixels to move this AnimationQueueMapViewPosition vertically.
     */
    void moveCenter(double moveHorizontal, double moveVertical);

    /**
     * Moves the center position of the map by the given amount of pixels.
     *
     * Used by TouchGestureHandler
     *
     * @param moveHorizontal the amount of pixels to move this AnimationQueueMapViewPosition horizontally.
     * @param moveVertical   the amount of pixels to move this AnimationQueueMapViewPosition vertically.
     * @param animated       whether the move should be animated.
     */
    void moveCenter(double moveHorizontal, double moveVertical, boolean animated);

    void setPivot(LatLong pivot);

    /**
     * Animates the center position of the map by the given amount of pixels.
     * This method is used by the TouchGestureHandler to perform a zoom on double-tap action.
     *
     * @param moveHorizontal the amount of pixels to move this MapViewPosition horizontally.
     * @param moveVertical   the amount of pixels to move this MapViewPosition vertically.
     * @param zoomLevelDiff  the difference in desired zoom level.
     */
    void moveCenterAndZoom(double moveHorizontal, double moveVertical, byte zoomLevelDiff);

    /**
     * Sets the scale factor adjustment. This is the multiplication factor between the last zoomLevel and the next scale factor.
     * This method is used by pinch-to-zoom functionality.
     *
     * @param adjustment the adjustment based on the current zoom level.
     * @param pivot      the pivot
     */
    void setScaleFactorAdjustment(LatLong pivot, double adjustment);
}
