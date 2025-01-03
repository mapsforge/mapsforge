/*
 * Copyright 2017 usrusr
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

/**
 * <p>
 * Simulates diffuse lighting to some degree, while leaving horizontal surfaces unshaded (desired property).
 * Note: For better results and greater flexibility consider using the newer algorithms, e.g. {@link AdaptiveClasyHillShading}, {@link StandardClasyHillShading} or {@link HiResClasyHillShading}.
 * </p>
 * <em>(2024) The original description from 2017, which is no longer entirely accurate:</em>
 * Simulates diffuse lighting (without self-shadowing) except for scaling the light values below horizontal and above horizontal
 * differently so that both make full use of the available dynamic range while maintaining horizontal neutral identical to {@link SimpleShadingAlgorithm}
 * and to the standard neutral value that is filled in when there is no hill shading but the always-option is set to true in the theme.
 * <p>
 * <p>More accurate than {@link SimpleShadingAlgorithm}, but maybe not as useful for visualizing both softly rolling hills and dramatic mountain ranges at the same time.</p>
 *
 * @see AdaptiveClasyHillShading
 * @see HiResClasyHillShading
 * @see StandardClasyHillShading
 * @see HalfResClasyHillShading
 * @see QuarterResClasyHillShading
 */
public class DiffuseLightShadingAlgorithm extends AShadingAlgorithm {

    protected final float heightAngle;
    protected final double ast2;
    protected final double neutral;
    /**
     * light height (relative to 1:1:x)
     */
    protected double a;

    public DiffuseLightShadingAlgorithm() {
        this(50f);
    }

    /**
     * height angle of light source over ground (in degrees 0..90)
     */
    public DiffuseLightShadingAlgorithm(float heightAngle) {
        this.heightAngle = heightAngle;
        this.a = heightAngleToRelativeHeight(heightAngle);
        ast2 = Math.sqrt(2 + this.a * this.a);
        neutral = calculateRaw(0, 0);
    }

    protected static double heightAngleToRelativeHeight(float heightAngle) {
        double radians = heightAngle / 180d * Math.PI;

        return Math.tan(radians) * Math.sqrt(2d);
    }

    public double getLightHeight() {
        return a;
    }

    @Override
    public RawShadingResult transformToByteBuffer(HgtFileInfo source, int padding, int zoomLevel, double pxPerLat, double pxPerLon) {
        final int axisLength = getOutputAxisLen(source, zoomLevel, pxPerLat, pxPerLon);
        final int rowLen = axisLength + 1;

        InputStream map = null;
        try {
            map = source
                    .getFile()
                    .asStream();

            final byte[] bytes;
            if (map != null) {
                bytes = convert(map, axisLength, rowLen, padding, zoomLevel, pxPerLat, pxPerLon, source);
            } else {
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

    protected byte[] convert(InputStream din, int axisLength, int rowLen, int padding, int zoomLevel, double pxPerLat, double pxPerLon, HgtFileInfo fileInfo) throws IOException {
        final byte[] bytes = new byte[(axisLength + 2 * padding) * (axisLength + 2 * padding)];
        final short[] ringbuffer = new short[rowLen];

        int outidx = (axisLength + 2 * padding) * padding + padding;
        int rbcur = 0;
        {
            short last = 0;
            for (int col = 0; col < rowLen; col++) {
                last = readNext(din, last);
                ringbuffer[rbcur++] = last;
            }
        }

        final double southPerPixel = MercatorProjection.calculateGroundResolution(fileInfo.southLat(), axisLength * 170L);
        final double northPerPixel = MercatorProjection.calculateGroundResolution(fileInfo.northLat(), axisLength * 170L);

        final double southPerPixelByLine = southPerPixel / (2 * axisLength);
        final double northPerPixelByLine = northPerPixel / (2 * axisLength);

        for (int line = 1; line <= axisLength; line++) {
            if (rbcur >= rowLen) {
                rbcur = 0;
            }

            short nw = ringbuffer[rbcur];
            short sw = readNext(din, nw);
            ringbuffer[rbcur++] = sw;

            final double halfmetersPerPixel = (southPerPixelByLine * line + northPerPixelByLine * (axisLength - line));

            for (int col = 1; col <= axisLength; col++) {
                final short ne = ringbuffer[rbcur];
                final short se = readNext(din, ne);
                ringbuffer[rbcur++] = se;

                final int noso = (se - ne) + (sw - nw);
                final int eawe = (ne - nw) + (se - sw);

                bytes[outidx++] = calculate(noso / halfmetersPerPixel, eawe / halfmetersPerPixel);

                nw = ne;
                sw = se;
            }

            outidx += 2 * padding;
        }

        return bytes;
    }

    protected byte calculate(double n, double e) {
        final double raw = calculateRaw(n, e);

        return (byte) Math.min(255, Math.round(255 * Math.abs(raw - neutral)));
    }

    /**
     * return 0..1
     */
    protected double calculateRaw(double n, double e) {
        // calculate the distance of the normal vector to a plane orthogonal to the light source and passing through zero,
        // the fraction of distance to vector lenght is proportional to the amount of light that would be hitting a disc
        // orthogonal to the normal vector
        double normPlaneDist = (e + n + a) / (ast2 * Math.sqrt(n * n + e * e + 1));

        double lightness = Math.max(0, normPlaneDist);
        return lightness;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DiffuseLightShadingAlgorithm that = (DiffuseLightShadingAlgorithm) o;

        return Double.compare(that.a, a) == 0;
    }

    @Override
    public int hashCode() {
        long temp = Double.doubleToLongBits(a);
        return (int) (temp ^ (temp >>> 32));
    }

    @Override
    public String toString() {
        return "DiffuseLightShadingAlgorithm{" +
                "heightAngle=" + heightAngle +
                '}';
    }
}
