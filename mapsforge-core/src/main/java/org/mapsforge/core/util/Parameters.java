/*
 * Copyright 2017-2022 devemux86
 * Copyright 2019 Matthew Egeler
 * Copyright 2020 Lukas Bai
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

import org.mapsforge.core.model.Tag;

import java.util.HashSet;
import java.util.Set;

public final class Parameters {

    public enum ParentTilesRendering {QUALITY, SPEED, OFF}

    public enum SymbolScaling {ALL, POI}

    /**
     * If true will use anti-aliasing in rendering.
     */
    public static boolean ANTI_ALIASING = true;

    /**
     * Enable the elastic zoom.
     */
    public static boolean ELASTIC_ZOOM = false;

    /**
     * Enable the fractional zoom.
     */
    public static boolean FRACTIONAL_ZOOM = false;

    /**
     * Process layer scroll events.
     */
    public static boolean LAYER_SCROLL_EVENT = false;

    /**
     * Use AwtLuminanceShadingComposite or AlphaComposite.
     */
    public static boolean LUMINANCE_COMPOSITE = true;

    /**
     * Maximum buffer size for map files.
     */
    public static int MAXIMUM_BUFFER_SIZE = 10000000;

    /**
     * The default number of threads is one greater than the number of processors, as one thread is
     * likely to be blocked on I/O reading map data. Technically this value can change to a better
     * implementation, maybe one that also takes the available memory into account would be good.
     */
    public static int NUMBER_OF_THREADS = Runtime.getRuntime().availableProcessors() + 1;

    /**
     * Parent tiles rendering mode.
     */
    public static ParentTilesRendering PARENT_TILES_RENDERING = ParentTilesRendering.SPEED;

    /**
     * Polygon exceptions.
     */
    public static final Set<Tag> POLYGON_EXCEPTIONS = new HashSet<>();

    /**
     * Zoom level to render polygons.
     * e.g. 10 for better performance.
     */
    public static int POLYGON_ZOOM_MIN = -1;

    /**
     * Faster map rotation with matrix.
     */
    public static boolean ROTATION_MATRIX = true;

    /**
     * If square frame buffer is enabled, the frame buffer allocated for drawing will be
     * large enough for drawing in either orientation, so no change is needed when the device
     * orientation changes. To avoid overly large frame buffers, the aspect ratio for this policy
     * determines when this will be used.
     */
    public static boolean SQUARE_FRAME_BUFFER = true;

    /**
     * Symbol scaling mode.
     */
    public static SymbolScaling SYMBOL_SCALING = SymbolScaling.ALL;

    /**
     * Validate coordinates.
     */
    public static boolean VALIDATE_COORDINATES = true;

    static {
        POLYGON_EXCEPTIONS.add(new Tag("natural", "sea"));
        POLYGON_EXCEPTIONS.add(new Tag("natural", "nosea"));

        POLYGON_EXCEPTIONS.add(new Tag("freizeitkarte", "meer"));
        POLYGON_EXCEPTIONS.add(new Tag("freizeitkarte", "land"));
    }

    private Parameters() {
        throw new IllegalStateException();
    }
}
