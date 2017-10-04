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

/**
 * An alternative to {@link AlphaComposite} that blends the strongest color component and scales the others accordingly,
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
                        float inV = Math.max(inFloats[0], Math.max(inFloats[1], inFloats[2]));

                        float vScaled = inV * magnitudeRest;

                        float vSum = vScaled + add;

                        if (inV == 0) {
                            outFloats[0] = vSum;
                            outFloats[1] = vSum;
                            outFloats[2] = vSum;
                        } else {
                            float factor = vSum / inV;
                            outFloats[0] = factor * inFloats[0];
                            outFloats[1] = factor * inFloats[1];
                            outFloats[2] = factor * inFloats[2];
                        }
                        dstOut.setPixel(x, y, outFloats);
                    }
                }
            }
        };
    }
}
