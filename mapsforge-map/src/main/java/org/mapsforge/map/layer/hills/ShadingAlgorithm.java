/*
 * Copyright 2017-2022 usrusr
 * Copyright 2024 Sublimis
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

import org.mapsforge.core.graphics.HillshadingBitmap;

public interface ShadingAlgorithm {

    //HillshadingBitmap convertTile(RawHillTileSource source, GraphicFactory graphicFactory);

    /**
     * @return Length of a side of a (square) input array minus one (to account for HGT overlap).
     */
    int getInputAxisLen(HgtCache.HgtFileInfo source);

    /**
     * @return Length of a side of a (square) output array.
     */
    int getOutputAxisLen(HgtCache.HgtFileInfo source);

    RawShadingResult transformToByteBuffer(HgtCache.HgtFileInfo hgtFileInfo, int padding);

    class RawShadingResult {
        public final byte[] bytes;
        public final int width;
        public final int height;
        public final int padding;

        public RawShadingResult(byte[] bytes, int width, int height, int padding) {
            this.bytes = bytes;
            this.width = width;
            this.height = height;
            this.padding = padding;
        }
    }

    /**
     * Abstracts the file handling and access so that ShadingAlgorithm implementations
     * could run on any height model source (e.g. on an android content provider for
     * data sharing between apps) as long as they understand the format of the stream
     */
    interface RawHillTileSource {
        long getSize();

        DemFile getFile();

        /* for overlap */
        HillshadingBitmap getFinishedConverted();

        /**
         * A ShadingAlgorithm might want to determine the projected dimensions of the tile
         */
        double northLat();

        double southLat();

        double westLng();

        double eastLng();
    }
}
