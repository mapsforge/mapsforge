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

public interface ShadingAlgorithm {

    /**
     * @return Length of a side of a (square) input array minus one (to account for HGT overlap).
     */
    int getInputAxisLen(HgtFileInfo hgtFileInfo);

    /**
     * @param zoomLevel Zoom level (to determine shading quality requirements)
     * @param pxPerLat  Tile pixels per degree of latitude (to determine shading quality requirements)
     * @param pxPerLon  Tile pixels per degree of longitude (to determine shading quality requirements)
     * @return Length of a side of a (square) output array, not including padding.
     */
    int getOutputAxisLen(HgtFileInfo hgtFileInfo, int zoomLevel, double pxPerLat, double pxPerLon);

    /**
     * @param padding   Padding of the output, useful to minimize border interpolation artifacts (no need to be larger than 1)
     * @param zoomLevel Zoom level (to determine shading quality requirements)
     * @param pxPerLat  Tile pixels per degree of latitude (to determine shading quality requirements)
     * @param pxPerLon  Tile pixels per degree of longitude (to determine shading quality requirements)
     * @return Width of a rectangular output array, including padding.
     */
    int getOutputWidth(HgtFileInfo hgtFileInfo, int padding, int zoomLevel, double pxPerLat, double pxPerLon);

    /**
     * @param padding   Padding of the output, useful to minimize border interpolation artifacts (no need to be larger than 1)
     * @param zoomLevel Zoom level (to determine shading quality requirements)
     * @param pxPerLat  Tile pixels per degree of latitude (to determine shading quality requirements)
     * @param pxPerLon  Tile pixels per degree of longitude (to determine shading quality requirements)
     * @return Height of a rectangular output array, including padding.
     */
    int getOutputHeight(HgtFileInfo hgtFileInfo, int padding, int zoomLevel, double pxPerLat, double pxPerLon);

    /**
     * @param padding   Padding of the output, useful to minimize border interpolation artifacts (no need to be larger than 1)
     * @param zoomLevel Zoom level (to determine shading quality requirements)
     * @param pxPerLat  Tile pixels per degree of latitude (to determine shading quality requirements)
     * @param pxPerLon  Tile pixels per degree of longitude (to determine shading quality requirements)
     * @return Estimated size of the output array, in bytes, padding included.
     */
    long getOutputSizeBytes(HgtFileInfo hgtFileInfo, int padding, int zoomLevel, double pxPerLat, double pxPerLon);

    /**
     * This is used when deciding whether a cached hill shading tile should be refreshed.
     *
     * @param hgtFileInfo HGT file info
     * @param padding     Padding in the output bitmap
     * @param zoomLevel   Zoom level (to determine shading quality requirements)
     * @param pxPerLat    Tile pixels per degree of latitude (to determine shading quality requirements)
     * @param pxPerLon    Tile pixels per degree of longitude (to determine shading quality requirements)
     * @return Cache tag
     */
    long getCacheTag(HgtFileInfo hgtFileInfo, int padding, int zoomLevel, double pxPerLat, double pxPerLon);

    /**
     * Convert the display parameters to a number whose semantics depends on shading algorithm implementation.
     * This could be used in {@link #getCacheTag(HgtFileInfo, int, int, double, double)}.
     *
     * @param hgtFileInfo HGT file info
     * @param zoomLevel   Zoom level intended for the hill shading tile
     * @param pxPerLat    Tile pixels per degree of latitude (to determine shading quality requirements)
     * @param pxPerLon    Tile pixels per degree of longitude (to determine shading quality requirements)
     * @return Converted number
     */
    long getCacheTagBin(HgtFileInfo hgtFileInfo, int zoomLevel, double pxPerLat, double pxPerLon);

    /**
     * @return Minimum supported zoom level (default should be 0).
     */
    int getZoomMin(HgtFileInfo hgtFileInfo);

    /**
     * @return Maximum supported zoom level (default should be {@link Integer#MAX_VALUE}).
     */
    int getZoomMax(HgtFileInfo hgtFileInfo);

    /**
     * @param padding   Padding of the output, useful to minimize border interpolation artifacts (no need to be larger than 1)
     * @param zoomLevel Zoom level (to determine shading quality requirements)
     * @param pxPerLat  Tile pixels per degree of latitude (to determine shading quality requirements)
     * @param pxPerLon  Tile pixels per degree of longitude (to determine shading quality requirements)
     */
    RawShadingResult transformToByteBuffer(HgtFileInfo hgtFileInfo, int padding, int zoomLevel, double pxPerLat, double pxPerLon);

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

        int getAxisLen();

        DemFile getFile();

        /**
         * A ShadingAlgorithm might want to determine the projected dimensions of the tile
         */
        double northLat();

        double southLat();

        double westLng();

        double eastLng();
    }
}
