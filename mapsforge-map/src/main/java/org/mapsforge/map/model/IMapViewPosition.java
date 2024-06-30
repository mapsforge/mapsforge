/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2015-2020 devemux86
 * Copyright 2015 Andreas Schildbach
 * Copyright 2016-2018 mikes222
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
import org.mapsforge.core.model.Rotation;
import org.mapsforge.map.model.common.Observer;
import org.mapsforge.map.model.common.PreferencesFacade;

/**
 * An interface to being able to use different implementations of MapViewPosition.
 */
public interface IMapViewPosition {

    /**
     * Adds an observer to the class.
     * <p>
     * Needed by mapsforge core classes.
     */
    void addObserver(Observer observer);

    /**
     * Animates the map towards the given position (move). The given position will be at the center
     * of the screen after the animation. The position is assured to be within the map limit.
     */
    void animateTo(final LatLong latLong);

    /**
     * @return true if animation is in progress or animation is in the queue for being processed.
     */
    boolean animationInProgress();

    /**
     * Destroys the class.
     */
    void destroy();

    /**
     * Returns the current center position of the map. The position may change rapidly if an
     * animation is in progress. If you are using animation consider keeping the positions and
     * zoom factors in your code and not using this getter.
     *
     * @return the current center position of the map.
     */
    LatLong getCenter();

    /**
     * @return the current limit of the map (might be null).
     */
    BoundingBox getMapLimit();

    /**
     * Returns a newly instantiated object representing the current position and zoomlevel.
     * Note that the position or zoomlevel may change rapidly if an animation is in progress.
     *
     * @return the current center position and zoom level of the map.
     */
    MapPosition getMapPosition();

    float getMapViewCenterX();

    float getMapViewCenterY();

    LatLong getPivot();

    Rotation getRotation();

    /**
     * Gets the current scale factor. The scale factor is normally 2^zoomLevel but it may differ if
     * in between a zoom-animation or in between a pinch-to-zoom process.
     * <p>
     * Needed by mapsforge core classes.
     */
    double getScaleFactor();

    /**
     * @return the current zoom of the map.
     */
    double getZoom();

    /**
     * @return the current zoom level of the map.
     */
    byte getZoomLevel();

    byte getZoomLevelMax();

    byte getZoomLevelMin();

    /**
     * Initializes the class.
     * <p>
     * Needed by mapsforge core classes.
     */
    void init(PreferencesFacade preferencesFacade);

    /**
     * Animates the center position of the map by the given amount of pixels.
     * <p>
     * Used by TouchGestureHandler.
     *
     * @param moveHorizontal the amount of pixels to move this MapViewPosition horizontally.
     * @param moveVertical   the amount of pixels to move this MapViewPosition vertically.
     */
    void moveCenter(double moveHorizontal, double moveVertical);

    /**
     * Moves the center position of the map by the given amount of pixels.
     * <p>
     * Used by TouchGestureHandler.
     *
     * @param moveHorizontal the amount of pixels to move this MapViewPosition horizontally.
     * @param moveVertical   the amount of pixels to move this MapViewPosition vertically.
     * @param animated       whether the move should be animated.
     */
    void moveCenter(double moveHorizontal, double moveVertical, boolean animated);

    /**
     * Animates the center position of the map by the given amount of pixels.
     * <p>
     * This method is used by the TouchGestureHandler to perform a zoom on double-tap action.
     *
     * @param moveHorizontal the amount of pixels to move this MapViewPosition horizontally.
     * @param moveVertical   the amount of pixels to move this MapViewPosition vertically.
     * @param zoomLevelDiff  the difference in desired zoom level.
     */
    void moveCenterAndZoom(double moveHorizontal, double moveVertical, byte zoomLevelDiff);

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
    void moveCenterAndZoom(double moveHorizontal, double moveVertical, byte zoomLevelDiff, boolean animated);

    /**
     * Removes an observer from the class.
     * <p>
     * Needed by mapsforge core classes.
     */
    void removeObserver(Observer observer);

    /**
     * Saves the current state.
     * <p>
     * Needed by mapsforge core classes.
     */
    void save(PreferencesFacade preferencesFacade);

    /**
     * Sets the new center position of the map.
     * <p>
     * This is NOT animated.
     */
    void setCenter(LatLong latLong);

    /**
     * Sets the new limit of the map (might be null).
     */
    void setMapLimit(BoundingBox mapLimit);

    /**
     * Sets the new center position and zoom level of the map.
     * <p/>
     * The default zoom level changes are animated.
     */
    void setMapPosition(MapPosition mapPosition);

    /**
     * Sets the new center position and zoom level of the map.
     */
    void setMapPosition(MapPosition mapPosition, boolean animated);

    void setMapViewCenterX(float mapViewCenterX);

    void setMapViewCenterY(float mapViewCenterY);

    void setPivot(LatLong pivot);

    /**
     * Sets the new rotation of the map.
     */
    void setRotation(Rotation rotation);

    void setScaleFactorAdjustment(double scaleFactorCumulative);

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
    void setZoom(double zoom);

    /**
     * Sets the new zoom of the map.
     *
     * @param zoom     desired zoom
     * @param animated true if the transition should be animated, false otherwise
     * @throws IllegalArgumentException if the zoom is negative.
     */
    void setZoom(double zoom, boolean animated);

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
    void setZoomLevel(byte zoomLevel);

    /**
     * Sets the new zoom level of the map.
     *
     * @param zoomLevel desired zoom level
     * @param animated  true if the transition should be animated, false otherwise
     * @throws IllegalArgumentException if the zoom level is negative.
     */
    void setZoomLevel(byte zoomLevel, boolean animated);

    void setZoomLevelMax(byte zoomLevelMax);

    void setZoomLevelMin(byte zoomLevelMin);

    /**
     * Changes the current zoom level by the given value if possible.
     * <p/>
     * Note: The default zoom level changes are animated.
     */
    void zoom(byte zoomLevelDiff);

    /**
     * Changes the current zoom level by the given value if possible.
     */
    void zoom(byte zoomLevelDiff, boolean animated);

    /**
     * Increases the current zoom level by one if possible. Zooming is always around the center.
     * <p/>
     * The default zoom level changes are animated.
     */
    void zoomIn();

    /**
     * Increases the current zoom level by one if possible. Zooming is always around the center.
     */
    void zoomIn(boolean animated);

    /**
     * Decreases the current zoom level by one if possible. Zooming is always around the center.
     * <p/>
     * The default zoom level changes are animated.
     */
    void zoomOut();

    /**
     * Decreases the current zoom level by one if possible. Zooming is always around the center.
     */
    void zoomOut(boolean animated);
}
