/*
 * Copyright 2017-2022 usrusr
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

    int getAxisLenght(HgtCache.HgtFileInfo source);

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

        /**
         * fill padding like clamp
         */
        void fillPadding(HillshadingBitmap.Border side) {
            int innersteps;
            int skip;
            int outersteps;
            int start;
            int sourceOffset;
            int sourceOuterStep;
            int sourceInnerStep;
            int lineLen = padding * 2 + width;
            if (side.vertical) {
                innersteps = padding;
                skip = width + padding;
                outersteps = height;
                if (side == HillshadingBitmap.Border.WEST) {
                    start = padding * lineLen; // first col, after padding ignored lines
                    sourceOffset = start + padding;
                } else {
                    start = padding * lineLen + padding + width; // first padding col after padding ignored lines + nearly one line
                    sourceOffset = start - 1;
                }
                sourceInnerStep = 0;
                sourceOuterStep = lineLen;
            } else { // horizontal
                innersteps = width;
                skip = 2 * padding;
                outersteps = padding;
                if (side == HillshadingBitmap.Border.NORTH) {
                    start = padding;
                    sourceOffset = start + padding * lineLen;
                } else {
                    start = (height + padding) * lineLen + padding;
                    sourceOffset = start - lineLen;
                }
                sourceInnerStep = 1;
                sourceOuterStep = -width; // "carriage return"
            }

            int dest = start;
            int src = sourceOffset;
            for (int o = 0; o < outersteps; o++) {

                for (int i = 0; i < innersteps; i++) {
                    bytes[dest] = bytes[src];
                    dest++;
                    src += sourceInnerStep;
                }

                dest += skip;
                src += sourceOuterStep;
            }
        }

        public void fillPadding() {
            if (padding < 1) return;
            fillPadding(HillshadingBitmap.Border.EAST);
            fillPadding(HillshadingBitmap.Border.WEST);
            fillPadding(HillshadingBitmap.Border.NORTH);
            fillPadding(HillshadingBitmap.Border.SOUTH);

            // fill diagonal padding (this won't be blended with neighbors but the artifacts of that are truely minimal)
            int lineLen = padding * 2 + width;
            int widthOncePadded = width + padding;
            int heightOncePadded = height + padding;
            byte nw = bytes[lineLen * padding + padding];
            byte ne = bytes[lineLen * padding + widthOncePadded - 1];
            byte se = bytes[lineLen * (heightOncePadded - 1) + padding];
            byte sw = bytes[lineLen * (heightOncePadded - 1) + (widthOncePadded - 1)];

            int seOffset = lineLen * heightOncePadded;
            int swOffset = seOffset + widthOncePadded;
            for (int y = 0; y < padding; y++) {
                int yoff = lineLen * y;
                for (int x = 0; x < padding; x++) {
                    bytes[x + yoff] = nw;
                    bytes[x + yoff + widthOncePadded] = ne;
                    bytes[x + yoff + seOffset] = se;
                    bytes[x + yoff + swOffset] = sw;
                }
            }
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
