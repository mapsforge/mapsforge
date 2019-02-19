/*
 * Copyright 2017-2019 devemux86
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
package org.mapsforge.core.util;

public final class Parameters {

    /**
     * If true the <code>MapViewPosition2</code> will be used instead of default <code>MapViewPosition</code>.
     */
    public static boolean MAP_VIEW_POSITION2 = false;

    /**
     * Maximum buffer size for map files.
     */
    public static int MAXIMUM_BUFFER_SIZE = 8000000;

    /**
     * The default number of threads is one greater than the number of processors, as one thread is
     * likely to be blocked on I/O reading map data. Technically this value can change to a better
     * implementation, maybe one that also takes the available memory into account would be good.
     * For stability reasons (see #591), we set default number of threads to 1.
     */
    public static int NUMBER_OF_THREADS = 1;//Runtime.getRuntime().availableProcessors() + 1;

    /**
     * If true draw also parent tiles during map rendering.
     */
    public static boolean PARENT_TILES_RENDERING = true;

    /**
     * If true, use fast, less memory-consuming rendering for parent tiles.
     * Note that fast parent tiles rendering will give poorer image quality
     * for parent tiles as compared to default parent tile rendering.
     */
    public static boolean FAST_PARENT_TILES_RENDERING = false;

    /**
     * If square frame buffer is enabled, the frame buffer allocated for drawing will be
     * large enough for drawing in either orientation, so no change is needed when the device
     * orientation changes. To avoid overly large frame buffers, the aspect ratio for this policy
     * determines when this will be used.
     */
    public static boolean SQUARE_FRAME_BUFFER = true;

    private Parameters() {
        throw new IllegalStateException();
    }
}
