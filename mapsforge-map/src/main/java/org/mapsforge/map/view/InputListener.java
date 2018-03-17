/*
 * Copyright 2018 mikes222
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
package org.mapsforge.map.view;

/**
 * This listener can be used to get informed about manual changes of position or zoom.
 * It will not inform about automatic (software-driven) changes.
 * The intentional purpose for this class is to switch off automatic positioning or
 * automatic zooming as soon as the user takes manual positioning or zooming.
 */
public interface InputListener {

    /**
     * A manual movement has been started. The user drags the map over the screen.
     * This method is called before any movement takes place.
     * There is no guarantee that this method is called just once per user intervention.
     */
    void onMoveEvent();

    /**
     * A manual zoom has been started. The user uses pinch-to-zoom, uses its mouse wheel
     * or the ZoomControls. This method is called before any zoom takes place.
     * There is no guarantee that this method is called just once per user intervention.
     */
    void onZoomEvent();
}
