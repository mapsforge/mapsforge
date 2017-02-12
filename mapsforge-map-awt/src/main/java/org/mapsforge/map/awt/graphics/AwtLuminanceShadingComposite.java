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
package org.mapsforge.map.awt.graphics;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.CompositeContext;
import java.awt.RenderingHints;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.Arrays;

/**
 * An alternative to {@link AlphaComposite} that blends in HSV
 * so that it does not flatten saturation like AlphaComposite would.
 */
public class AwtLuminanceShadingComposite implements Composite {
    private final float magnitude;

    public AwtLuminanceShadingComposite(float magnitude) {
        this.magnitude = Math.max(0f, Math.min(magnitude, 1f));
    }

    @Override
    public CompositeContext createContext(final ColorModel srcColorModel, final ColorModel dstColorModel, final RenderingHints hints) {
        return new CompositeContext() {
            int[] srcBytes = new int[4];
            float[] inFloats = new float[4];
            float[] hsvFloats = new float[4];
            float[] outFloats = new float[4];

            @Override
            public void dispose() {
            }

            @Override
            public void compose(Raster src, Raster dstIn, WritableRaster dstOut) {
                int width = src.getWidth();
                int height = src.getHeight();
                float magnitudeRest = 1f - magnitude;

                for (int x = 0; x < width; ++x) {
                    for (int y = 0; y < height; ++y) {
                        src.getPixel(x, y, srcBytes);
                        int shadeVal = srcBytes[0];

                        float add = shadeVal * magnitude;

                        dstIn.getPixel(x, y, inFloats);

                        toHsv(inFloats, hsvFloats);

                        float inV = hsvFloats[2];
                        float vScaled = inV * magnitudeRest;

                        float vSum = vScaled + add;

                        hsvFloats[2] = vSum;

                        fromHsv(hsvFloats, outFloats);

                        dstOut.setPixel(x, y, outFloats);
                    }
                }
            }
        };
    }

    static void toHsv(float[] inFloats, float[] outFloats) {
        outFloats[3] = inFloats[3];

        float r = inFloats[0];
        float g = inFloats[1];
        float b = inFloats[2];

        float sixth = 42.5f; // 255f / 6f;
        float h, s, v, min, max;
        if (r == g && g == b) {
            h = 0;
            max = r;
            min = r;
        } else if (r >= g && r >= b) {
            max = r;
            min = Math.min(b, g);
            h = sixth * (0 + (g - b) / (max - min));
        } else if (g >= b) {
            max = g;
            min = Math.min(r, b);
            h = sixth * (2 + (b - r) / (max - min));
        } else {
            max = b;
            min = Math.min(r, g);
            h = sixth * (4 + (r - g) / (max - min));
        }
        if (h < 0) {
            h = h + 255f;
        }
        if (max == 0f) {
            s = 0;
        } else {
            s = 255f * (max - min) / max;
        }
        v = max;

        outFloats[0] = h;
        outFloats[1] = s;
        outFloats[2] = v;
    }

    static void fromHsv(float[] inFloats, float[] outFloats) {
        float h = inFloats[0];
        float s = inFloats[1];
        float v = inFloats[2];

        float f = h / 42.5f;
        int block = (int) f;
        f = (f - block) / 6f;

        float sfract = s / 255f;

        float p = v * (1 - sfract);
        float q = v * (1 - sfract * f);
        float t = v * (1 - sfract * (1 - f));

        float r, g, b;

        if (block == 0 || block == 6) {
            r = v;
            g = t;
            b = p;
        } else if (block == 1) {
            r = q;
            g = v;
            b = p;
        } else if (block == 2) {
            r = p;
            g = v;
            b = t;
        } else if (block == 3) {
            r = p;
            g = q;
            b = v;
        } else if (block == 4) {
            r = t;
            g = p;
            b = v;
        } else if (block == 5) {
            r = v;
            g = p;
            b = q;
        } else {
            throw new IllegalStateException("nonsensical hsv values in " + Arrays.toString(inFloats));
        }

        outFloats[0] = r;
        outFloats[1] = g;
        outFloats[2] = b;
        outFloats[3] = inFloats[3];
    }
}
