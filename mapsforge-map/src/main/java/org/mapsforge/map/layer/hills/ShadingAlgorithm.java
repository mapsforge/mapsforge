/*
 * Copyright 2017 usrusr
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
package org.mapsforge.map.layer.hills;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.GraphicFactory;

import java.io.BufferedInputStream;
import java.io.IOException;

public interface ShadingAlgorithm {

    Bitmap convertTile(RawHillTileSource source, GraphicFactory graphicFactory);

    /**
     * Abstracts the file handling and access so that ShadingAlgorithm implementations
     * could run on any height model source (e.g. on an android content provider for
     * data sharing between apps) as long as they understand the format of the stream
     */
    interface RawHillTileSource {
        long getSize();

        BufferedInputStream openInputStream() throws IOException;

        /**
         * Just in case someone wants to sacrifice speed for fidelity
         */
        RawHillTileSource getNeighborNorth();

        RawHillTileSource getNeighborSouth();

        RawHillTileSource getNeighborEast();

        RawHillTileSource getNeighborWest();
    }
}
