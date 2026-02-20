/*
 * Copyright 2025 Sublimis
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
package org.mapsforge.map.elevation;

import org.mapsforge.core.util.Constants;
import org.mapsforge.map.layer.hills.DemFolder;
import org.mapsforge.map.layer.hills.HiResClasyHillShading;
import org.mapsforge.map.rendertheme.renderinstruction.Hillshading;

import java.util.List;

/**
 * Get elevations of geographic points using offline digital elevation model data (DEM/SRTM).
 */
public class ElevationAPI {

    public static final short INVALID_VALUE = Short.MIN_VALUE;

    // To prevent cache starvation
    protected final int CacheMinCount = 1;

    // Each HGT file contains 1° x 1° DEM data, so there can be at most 180 x 360 HGT files.
    // The actual number of files is smaller because there is currently no bathymetric data.
    protected final int CacheMaxCount = 360 * 180;

    // One 1" HGT file converted to a same-sized bitmap is about 13 MB, for high-quality this is 52 MB.
    // For ultra-low-quality while rendering wide zoom in adaptive mode, bitmap size per 1" HGT file can be as low as a few hundred bytes.
    protected final long CacheMaxBytes = Constants.MAX_MEMORY_MB * 1000 * 1000 / 10;

    protected final ElevationCache elevationCache;
    protected final DemFolder demFolder;

    public ElevationAPI(DemFolder demFolder) {
        this.demFolder = demFolder;
        this.elevationCache = new ElevationCache(demFolder, CacheMinCount, CacheMaxCount, CacheMaxBytes);
    }

    /**
     * Get elevations for a set of location points using {@link Mode#BICUBIC} interpolation mode.
     *
     * @param latLon Input array of latitude-longitude pairs. Eg. one point is {@code latLon[0] = lat, latLon[1] = lon}.
     * @param output Output array, with the length exactly half the length of the input array.
     * @return Number of valid elevations found. If there is no missing data, this will be equal to the length of the output array (i.e. the number of points in the input array).
     * In any case, each output array element will contain the elevation in meters (referenced to the WGS84/EGM96 geoid) for the corresponding input array point, or {@link ElevationAPI#INVALID_VALUE} if data is missing.
     */
    public int getElevation(final double[] latLon, final double[] output) {
        return getElevation(latLon, output, Mode.BICUBIC);
    }

    /**
     * Get elevations for a set of location points.
     *
     * @param latLon Input array of latitude-longitude pairs. Eg. one point is {@code latLon[0] = lat, latLon[1] = lon}.
     * @param output Output array, with the length exactly half the length of the input array.
     * @param mode   Interpolation mode, either {@link Mode#BICUBIC} (default) or {@link Mode#BILINEAR}.
     * @return Number of valid elevations found. If there is no missing data, this will be equal to the length of the output array (i.e. the number of points in the input array).
     * In any case, each output array element will contain the elevation in meters (referenced to the WGS84/EGM96 geoid) for the corresponding input array point, or {@link ElevationAPI#INVALID_VALUE} if data is missing.
     */
    public int getElevation(final double[] latLon, final double[] output, Mode mode) {
        int retVal = 0;

        if (latLon != null) {
            final ElevationBitmap[] elevationBitmapRef = new ElevationBitmap[]{null};

            for (int i = 0; i < latLon.length / 2; i++) {
                final double lat = latLon[2 * i];
                final double lon = latLon[2 * i + 1];

                final double elevation = getElevation(lat, lon, elevationBitmapRef, mode);

                output[i] = elevation;

                if (isValid(elevation)) {
                    ++retVal;
                }
            }
        }

        return retVal;
    }

    /**
     * Get elevations for a set of location points using {@link Mode#BICUBIC} interpolation mode.
     *
     * @param latLon Input list of latitude-longitude pairs. Eg. one point is {@code latLon[0] = lat, latLon[1] = lon}.
     * @param output Output array, with the length exactly the length of the input list.
     * @return Number of valid elevations found. If there is no missing data, this will be equal to the length of the output array (i.e. the number of points in the input list).
     * In any case, each output array element will contain the elevation in meters (referenced to the WGS84/EGM96 geoid) for the corresponding input array point, or {@link ElevationAPI#INVALID_VALUE} if data is missing.
     */
    public int getElevation(final List<double[]> latLon, final double[] output) {
        return getElevation(latLon, output, Mode.BICUBIC);
    }

    /**
     * Get elevations for a set of location points.
     *
     * @param latLon Input list of latitude-longitude pairs. Eg. one point is {@code latLon[0] = lat, latLon[1] = lon}.
     * @param output Output array, with the length exactly the length of the input list.
     * @param mode   Interpolation mode, either {@link Mode#BICUBIC} (default) or {@link Mode#BILINEAR}.
     * @return Number of valid elevations found. If there is no missing data, this will be equal to the length of the output array (i.e. the number of points in the input list).
     * In any case, each output array element will contain the elevation in meters (referenced to the WGS84/EGM96 geoid) for the corresponding input array point, or {@link ElevationAPI#INVALID_VALUE} if data is missing.
     */
    public int getElevation(final List<double[]> latLon, final double[] output, Mode mode) {
        int retVal = 0;

        if (latLon != null) {
            final ElevationBitmap[] elevationBitmapRef = new ElevationBitmap[]{null};

            for (int i = 0; i < latLon.size(); i++) {
                final double[] latLonPoint = latLon.get(i);

                final double elevation = getElevation(latLonPoint[0], latLonPoint[1], elevationBitmapRef, mode);

                output[i] = elevation;

                if (isValid(elevation)) {
                    ++retVal;
                }
            }
        }

        return retVal;
    }

    /**
     * Get the elevation of a single location point using {@link Mode#BICUBIC} interpolation mode.
     *
     * @param lat Latitude of the location point.
     * @param lon Longitude of the location point.
     * @return Elevation in meters (referenced to the WGS84/EGM96 geoid), or {@link ElevationAPI#INVALID_VALUE} if data is missing.
     */
    public double getElevation(final double lat, final double lon) {
        return getElevation(lat, lon, Mode.BICUBIC);
    }

    /**
     * Get the elevation of a single location point.
     *
     * @param lat  Latitude of the location point.
     * @param lon  Longitude of the location point.
     * @param mode Interpolation mode, either {@link Mode#BICUBIC} (default) or {@link Mode#BILINEAR}.
     * @return Elevation in meters (referenced to the WGS84/EGM96 geoid), or {@link ElevationAPI#INVALID_VALUE} if data is missing.
     */
    public double getElevation(final double lat, final double lon, final Mode mode) {
        return getElevation(lat, lon, null, mode);
    }

    protected double getElevation(final double lat, final double lon, ElevationBitmap[] elevationBitmapRef, Mode mode) {
        double elevation = INVALID_VALUE;

        if (elevationBitmapRef == null) {
            elevationBitmapRef = new ElevationBitmap[]{null};
        }

        // Lat, lon can be negative
        final int hgtLat = (int) Math.floor(lat);
        final int hgtLon = (int) Math.floor(lon);

        ElevationBitmap elevationBitmap = null;

        if (elevationBitmapRef[0] != null) {
            elevationBitmap = elevationBitmapRef[0];
        } else {
            try {
                elevationBitmap = (ElevationBitmap) this.elevationCache.getHillshadingBitmap(hgtLat, hgtLon, 0, 0, 0, 0);
            } catch (Exception ignored) {
            }

            if (elevationBitmap != null) {
                elevationBitmapRef[0] = elevationBitmap;
            }
        }

        if (elevationBitmap != null) {
            final int width = elevationBitmap.width;
            final int height = elevationBitmap.height;
            final byte[] buffer = elevationBitmap.buffer;

            if (buffer != null) {
                // Always positive values, taken from the lower-left corner towards upper-right corner
                final double latFract = lat - hgtLat;
                final double lonFract = lon - hgtLon;

                final double nwXExact = (lonFract * width / Hillshading.ShadingLonStep);
                final double nwYExact = (Hillshading.ShadingLatStep - latFract) * height / Hillshading.ShadingLatStep;

                final int nwX = (int) nwXExact;
                final int nwY = (int) nwYExact;

                final short nw = getNw(nwX, nwY, width, height, hgtLat, hgtLon, elevationBitmapRef);
                final short sw = getSw(nwX, nwY, width, height, hgtLat, hgtLon, elevationBitmapRef);
                final short se = getSe(nwX, nwY, width, height, hgtLat, hgtLon, elevationBitmapRef);
                final short ne = getNe(nwX, nwY, width, height, hgtLat, hgtLon, elevationBitmapRef);

                if (isValid(nw, sw, se, ne)) {
                    final double x = nwXExact - nwX;
                    final double y = 1 - (nwYExact - nwY);

                    if (mode == Mode.BICUBIC) {
                        final short nwnw = getNwnw(nwX, nwY, width, height, hgtLat, hgtLon, elevationBitmapRef);
                        final short nnw = getNnw(nwX, nwY, width, height, hgtLat, hgtLon, elevationBitmapRef);
                        final short nne = getNne(nwX, nwY, width, height, hgtLat, hgtLon, elevationBitmapRef);
                        final short nene = getNene(nwX, nwY, width, height, hgtLat, hgtLon, elevationBitmapRef);
                        final short wnw = getWnw(nwX, nwY, width, height, hgtLat, hgtLon, elevationBitmapRef);
                        final short ene = getEne(nwX, nwY, width, height, hgtLat, hgtLon, elevationBitmapRef);
                        final short wsw = getWsw(nwX, nwY, width, height, hgtLat, hgtLon, elevationBitmapRef);
                        final short ese = getEse(nwX, nwY, width, height, hgtLat, hgtLon, elevationBitmapRef);
                        final short swsw = getSwsw(nwX, nwY, width, height, hgtLat, hgtLon, elevationBitmapRef);
                        final short ssw = getSsw(nwX, nwY, width, height, hgtLat, hgtLon, elevationBitmapRef);
                        final short sse = getSse(nwX, nwY, width, height, hgtLat, hgtLon, elevationBitmapRef);
                        final short sese = getSese(nwX, nwY, width, height, hgtLat, hgtLon, elevationBitmapRef);

                        if (isValid(nwnw, wnw, wsw, swsw) && isValid(ssw, sse, sese, ese) && isValid(ene, nene, nne, nnw)) {
                            // Bicubic interpolation
                            elevation = HiResClasyHillShading.getBicubicPoint(x, y, nw, sw, se, ne, nwnw, wnw, wsw, swsw, ssw, sse, sese, ese, ene, nene, nne, nnw);
                        }
                    }

                    if (false == isValid(elevation)) {
                        // Fallback to bilinear interpolation
                        elevation = sw * (1 - x) * (1 - y) + nw * (1 - x) * y + se * x * (1 - y) + ne * x * y;
                    }
                }
            }
        }

        return elevation;
    }

    protected short getNwnw(final int x, final int y, int width, int height, int hgtLat, int hgtLon, ElevationBitmap[] elevationBitmapRef) {
        return getPoint(x - 1, y - 1, width, height, hgtLat, hgtLon, elevationBitmapRef);
    }

    protected short getNnw(final int x, final int y, int width, int height, int hgtLat, int hgtLon, ElevationBitmap[] elevationBitmapRef) {
        return getPoint(x, y - 1, width, height, hgtLat, hgtLon, elevationBitmapRef);
    }

    protected short getNne(final int x, final int y, int width, int height, int hgtLat, int hgtLon, ElevationBitmap[] elevationBitmapRef) {
        return getPoint(x + 1, y - 1, width, height, hgtLat, hgtLon, elevationBitmapRef);
    }

    protected short getNene(final int x, final int y, int width, int height, int hgtLat, int hgtLon, ElevationBitmap[] elevationBitmapRef) {
        return getPoint(x + 2, y - 1, width, height, hgtLat, hgtLon, elevationBitmapRef);
    }

    protected short getWnw(final int x, final int y, int width, int height, int hgtLat, int hgtLon, ElevationBitmap[] elevationBitmapRef) {
        return getPoint(x - 1, y, width, height, hgtLat, hgtLon, elevationBitmapRef);
    }

    protected short getNw(final int x, final int y, int width, int height, int hgtLat, int hgtLon, ElevationBitmap[] elevationBitmapRef) {
        return getPoint(x, y, width, height, hgtLat, hgtLon, elevationBitmapRef);
    }

    protected short getNe(final int x, final int y, int width, int height, int hgtLat, int hgtLon, ElevationBitmap[] elevationBitmapRef) {
        return getPoint(x + 1, y, width, height, hgtLat, hgtLon, elevationBitmapRef);
    }

    protected short getEne(final int x, final int y, int width, int height, int hgtLat, int hgtLon, ElevationBitmap[] elevationBitmapRef) {
        return getPoint(x + 2, y, width, height, hgtLat, hgtLon, elevationBitmapRef);
    }

    protected short getWsw(final int x, final int y, int width, int height, int hgtLat, int hgtLon, ElevationBitmap[] elevationBitmapRef) {
        return getPoint(x - 1, y + 1, width, height, hgtLat, hgtLon, elevationBitmapRef);
    }

    protected short getSw(final int x, final int y, int width, int height, int hgtLat, int hgtLon, ElevationBitmap[] elevationBitmapRef) {
        return getPoint(x, y + 1, width, height, hgtLat, hgtLon, elevationBitmapRef);
    }

    protected short getSe(final int x, final int y, int width, int height, int hgtLat, int hgtLon, ElevationBitmap[] elevationBitmapRef) {
        return getPoint(x + 1, y + 1, width, height, hgtLat, hgtLon, elevationBitmapRef);
    }

    protected short getEse(final int x, final int y, int width, int height, int hgtLat, int hgtLon, ElevationBitmap[] elevationBitmapRef) {
        return getPoint(x + 2, y + 1, width, height, hgtLat, hgtLon, elevationBitmapRef);
    }

    protected short getSwsw(final int x, final int y, int width, int height, int hgtLat, int hgtLon, ElevationBitmap[] elevationBitmapRef) {
        return getPoint(x - 1, y + 2, width, height, hgtLat, hgtLon, elevationBitmapRef);
    }

    protected short getSsw(final int x, final int y, int width, int height, int hgtLat, int hgtLon, ElevationBitmap[] elevationBitmapRef) {
        return getPoint(x, y + 2, width, height, hgtLat, hgtLon, elevationBitmapRef);
    }

    protected short getSse(final int x, final int y, int width, int height, int hgtLat, int hgtLon, ElevationBitmap[] elevationBitmapRef) {
        return getPoint(x + 1, y + 2, width, height, hgtLat, hgtLon, elevationBitmapRef);
    }

    protected short getSese(final int x, final int y, int width, int height, int hgtLat, int hgtLon, ElevationBitmap[] elevationBitmapRef) {
        return getPoint(x + 2, y + 2, width, height, hgtLat, hgtLon, elevationBitmapRef);
    }

    protected short getPoint(int x, int y, int width, int height, int hgtLat, int hgtLon, final ElevationBitmap[] elevationBitmapRef) {
        short retVal = INVALID_VALUE;

        int xSpill = 0, ySpill = 0;

        if (x < 0) {
            hgtLon -= Hillshading.ShadingLonStep;
            xSpill = x;
        } else if (x >= width) {
            hgtLon += Hillshading.ShadingLonStep;
            xSpill = 1 + x - width;
        }

        if (y < 0) {
            hgtLat -= Hillshading.ShadingLatStep;
            ySpill = y;
        } else if (y >= height) {
            hgtLat += Hillshading.ShadingLatStep;
            ySpill = 1 + y - height;
        }

        ElevationBitmap elevationBitmap = null;

        if (xSpill != 0 || ySpill != 0) {
            try {
                elevationBitmap = (ElevationBitmap) this.elevationCache.getHillshadingBitmap(hgtLat, hgtLon, 0, 0, 0, 0);
            } catch (Exception ignored) {
            }

            if (elevationBitmap != null) {
                elevationBitmapRef[0] = elevationBitmap;
            }
        } else {
            elevationBitmap = elevationBitmapRef[0];
        }

        if (elevationBitmap != null) {
            width = elevationBitmap.width;
            height = elevationBitmap.height;
            final byte[] buffer = elevationBitmap.buffer;

            if (buffer != null) {
                if (xSpill < 0) {
                    x = xSpill + width;
                } else if (xSpill > 0) {
                    x = xSpill - 1;
                }

                if (ySpill < 0) {
                    y = ySpill + height;
                } else if (ySpill > 0) {
                    y = ySpill - 1;
                }

                retVal = getPoint(x, y, buffer, width);
            }
        }

        return retVal;
    }

    protected short getPoint(final int x, final int y, final byte[] buffer, final int width) {
        short retVal = INVALID_VALUE;

        final int index = (x + width * y) * ElevationAlgorithm.OUTPUT_ELEMENT_SIZE;

        if (index < buffer.length - 1) {
            retVal = getShortFromBytes(buffer, index);
        }

        return retVal;
    }

    protected short getShortFromBytes(final byte[] buffer, final int index) {
        final int read1 = buffer[index];
        final int read2 = buffer[index + 1];

        return (short) ((read1 << 8) | (read2 & 0xff));
    }

    public static boolean isValid(double a) {
        return a != INVALID_VALUE;
    }

    public static boolean isValid(short a) {
        return a != INVALID_VALUE;
    }

    public static boolean isValid(short a, short b, short c, short d) {
        return a != INVALID_VALUE && b != INVALID_VALUE && c != INVALID_VALUE && d != INVALID_VALUE;
    }

    public enum Mode {
        /**
         * Bicubic interpolation mode. More accurate, uses more processing. The default.
         */
        BICUBIC,
        /**
         * Bilinear interpolation mode. Less accurate, somewhat faster. Consider this only if your task is to process millions of elevation points as quickly as possible.
         */
        BILINEAR
    }
}
