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

import org.mapsforge.core.util.IOUtils;
import org.mapsforge.core.util.MercatorProjection;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbsShadingAlgorithmDefaults implements ShadingAlgorithm {

    private static final Logger LOGGER = Logger.getLogger(AbsShadingAlgorithmDefaults.class.getName());

    protected abstract byte[] convert(InputStream map, int axisLength, int rowLen, int padding, HgtCache.HgtFileInfo source) throws IOException;

    public short readNext(InputStream din, short fallback) throws IOException {
        final int read1 = din.read();
        final int read2 = din.read();

        if (read1 != -1 && read2 != -1) {
            short read = (short) ((read1 << 8) | read2);

            if (read == Short.MIN_VALUE)
                return fallback;

            return read;
        } else {
            return fallback;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getInputAxisLen(HgtCache.HgtFileInfo source) {
        long size = source.getSize();
        long elements = size / 2;
        int rowLen = (int) Math.ceil(Math.sqrt(elements));
        if (rowLen * rowLen * 2L != size) {
            return 0;
        }
        return rowLen - 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getOutputAxisLen(HgtCache.HgtFileInfo source) {
        return getInputAxisLen(source);
    }

    @Override
    public RawShadingResult transformToByteBuffer(HgtCache.HgtFileInfo source, int padding) {
        final int axisLength = getOutputAxisLen(source);
        final int rowLen = axisLength + 1;

        InputStream map = null;
        try {
            map = source.getFile().asStream();

            final byte[] bytes;
            if (map != null) {
                bytes = convert(map, axisLength, rowLen, padding, source);
            }
            else
            {
                // If stream could not be opened, simply return zeros
                final int bitmapWidth = axisLength + 2 * padding;
                bytes = new byte[bitmapWidth * bitmapWidth];
            }
            return new RawShadingResult(bytes, axisLength, axisLength, padding);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.toString(), e);
            return null;
        } finally {
            IOUtils.closeQuietly(map);
        }
    }

    public double getLatUnitDistance(final double latitude, final long fileAxisLen)
    {
        // ~85 north + ~85 south = ~170 north to south
        return MercatorProjection.calculateGroundResolution(latitude, 170L * fileAxisLen);
    }
}
