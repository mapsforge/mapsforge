package org.mapsforge.map.view;

/**
 * This listener can be used to get informed about _manual_ changes of position or zoom. It will not inform about automatic (software-driven) changes.
 * The intentional purpose for this class is to switch off automatic positioning or automatic zooming as soon as the user takes manual positioning or zooming.
 * <p>
 * Created by Mike on 11/11/2017.
 */

public interface IManualInputListener {

    /**
     * a manual movement has been started. The user drags the map over the screen. This method is called before any movement takes place. There is no
     * guarantee that this method is called just once per user intervention.
     */
    void manualMoveStarted();

    /**
     * a manual zoom has been started. The user uses pinch-to-zoom, uses its mousewheel or the ZoomControls. This method is called before any zoom takes place.
     * There is no guarantee that this method is called just once per user intervention.
     */
    void manualZoomStarted();
}
