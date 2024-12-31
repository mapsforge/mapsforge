/*
 * Copyright 2020-2022 usrusr
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

import org.mapsforge.core.util.MercatorProjection;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AShadingAlgorithm implements ShadingAlgorithm {

    protected final Logger LOGGER = Logger.getLogger(this.getClass().getName());

    protected static final int ZoomLevelMinDefault = 0;
    protected static final int ZoomLevelMaxDefault = Integer.MAX_VALUE;

    protected abstract byte[] convert(InputStream map, int axisLength, int rowLen, int padding, int zoomLevel, double pxPerLat, double pxPerLon, HgtFileInfo source) throws IOException;

    public short readNext(final InputStream is) throws IOException {
        final int read1 = is.read();
        final int read2 = is.read();

        if (read1 < 0 || read2 < 0) {
            return Short.MIN_VALUE;
        } else {
            return (short) ((read1 << 8) | read2);
        }
    }

    public short readNext(InputStream din, short fallback) throws IOException {
        final int read1 = din.read();
        final int read2 = din.read();

        if (read1 != -1 && read2 != -1) {
            short read = (short) ((read1 << 8) | read2);

            if (read == Short.MIN_VALUE) {
                return fallback;
            }

            return read;
        } else {
            return fallback;
        }
    }

    @Override
    public int getInputAxisLen(HgtFileInfo hgtFileInfo) {
        return hgtFileInfo.getAxisLen();
    }

    @Override
    public int getOutputAxisLen(HgtFileInfo hgtFileInfo, int zoomLevel, double pxPerLat, double pxPerLon) {
        return getInputAxisLen(hgtFileInfo);
    }

    @Override
    public int getOutputWidth(HgtFileInfo hgtFileInfo, int padding, int zoomLevel, double pxPerLat, double pxPerLon) {
        return 2 * padding + getOutputAxisLen(hgtFileInfo, zoomLevel, pxPerLat, pxPerLon);
    }

    @Override
    public long getOutputSizeBytes(HgtFileInfo hgtFileInfo, int padding, int zoomLevel, double pxPerLat, double pxPerLon) {
        final long outputWidth = getOutputWidth(hgtFileInfo, padding, zoomLevel, pxPerLat, pxPerLon);

        return outputWidth * outputWidth;
    }

    @Override
    public long getCacheTag(HgtFileInfo hgtFileInfo, int padding, int zoomLevel, double pxPerLat, double pxPerLon) {
        long output = hgtFileInfo.hashCode();
        output = 31 * output + padding;
        output = 31 * output + getCacheTagBin(hgtFileInfo, zoomLevel, pxPerLat, pxPerLon);

        return output;
    }

    @Override
    public long getCacheTagBin(HgtFileInfo hgtFileInfo, int zoomLevel, double pxPerLat, double pxPerLon) {
        return 0;
    }

    @Override
    public int getZoomMin(HgtFileInfo hgtFileInfo) {
        return ZoomLevelMinDefault;
    }

    @Override
    public int getZoomMax(HgtFileInfo hgtFileInfo) {
        return ZoomLevelMaxDefault;
    }

    @Override
    public RawShadingResult transformToByteBuffer(HgtFileInfo source, int padding, int zoomLevel, double pxPerLat, double pxPerLon) {
        RawShadingResult output = null;

        final int axisLength = getOutputAxisLen(source, zoomLevel, pxPerLat, pxPerLon);
        final int rowLen = axisLength + 1;

        try {
            final byte[] bytes = convert(null, axisLength, rowLen, padding, zoomLevel, pxPerLat, pxPerLon, source);

            if (bytes != null) {
                output = new RawShadingResult(bytes, axisLength, axisLength, padding);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.toString(), e);
        }

        return output;
    }

    public double getLatUnitDistance(final double latitude, final long fileAxisLen) {
        return MercatorProjection.calculateGroundResolution(latitude, 360 * fileAxisLen);
    }
}
