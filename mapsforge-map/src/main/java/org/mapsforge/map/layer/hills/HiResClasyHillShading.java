/*
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

import java.io.IOException;
import java.io.InputStream;

/**
 * <p>
 * High resolution / high quality implementation of {@link StandardClasyHillShading}.
 * </p>
 * <p>
 * It quadruples the number of pixels of the output bitmap (doubles the width and height) and uses bicubic interpolation to achieve the best visual results.
 * </p>
 * <p>
 * Quadrupling does increase the memory used, but it's nothing extreme: The output bitmap is about the size of a 17-megapixel photograph,
 * given that the input data was 1-arcsecond one (1", the highest resolution you can get at this time). For 3-arcsecond data (3"), this drops to about 6 MB.
 * </p>
 * <p>
 * In other words, a standard 1" DEM file containing 1° square data will be processed to an output bitmap of about 7200x7200 px and 52 MB in size.
 * </p>
 * <p>
 * To greatly improve efficiency at wider zoom levels, you should consider using the adaptive quality version instead: {@link AdaptiveClasyHillShading}.
 * It provides the best results with excellent performance throughout the zoom level range.
 * </p>
 *
 * @see AdaptiveClasyHillShading
 * @see StandardClasyHillShading
 */
public class HiResClasyHillShading extends StandardClasyHillShading {

    /**
     * Construct this using the parameters provided.
     *
     * @param clasyParams Parameters to use while constructing this.
     * @see AClasyHillShading#AClasyHillShading(ClasyParams)
     * @see ClasyParams
     */
    public HiResClasyHillShading(final ClasyParams clasyParams) {
        super(clasyParams);
    }

    /**
     * Uses default values for all parameters.
     *
     * @see AClasyHillShading#AClasyHillShading()
     */
    public HiResClasyHillShading() {
        super();
    }

    @Override
    protected byte[] convert(InputStream inputStream, int dummyAxisLen, int dummyRowLen, int padding, int zoomLevel, double pxPerLat, double pxPerLon, HgtFileInfo hgtFileInfo) throws IOException {
        return doTheWork_(hgtFileInfo, true, padding, zoomLevel, pxPerLat, pxPerLon);
    }

    @Override
    public int getOutputAxisLen(final HgtFileInfo hgtFileInfo, int zoomLevel, double pxPerLat, double pxPerLon) {
        return 2 * getInputAxisLen(hgtFileInfo);
    }

    @Override
    protected int processRow_4x4(short[] input, int firstLineIx, int secondLineOffset, int thirdLineOffset, int fourthLineOffset, double dsf, int outputIx, ComputingParams computingParams) {
        final double dsfDouble = 2 * dsf;

        final int secondLineIx = firstLineIx + secondLineOffset;
        final int thirdLineIx = firstLineIx + thirdLineOffset;
        final int fourthLineIx = firstLineIx + fourthLineOffset;

        final int nwnw = input[firstLineIx - 1];
        final int wnw = input[secondLineIx - 1];
        final int wsw = input[thirdLineIx - 1];
        final int swsw = input[fourthLineIx - 1];

        int nw = input[secondLineIx];
        int sw = input[thirdLineIx];
        int se = input[thirdLineIx + 1];
        int ne = input[secondLineIx + 1];

        final int ssw = input[fourthLineIx];
        final int sse = input[fourthLineIx + 1];
        int sese = input[fourthLineIx + 2];
        int ese = input[thirdLineIx + 2];
        int ene = input[secondLineIx + 2];
        int nene = input[firstLineIx + 2];
        final int nne = input[firstLineIx + 1];
        final int nnw = input[firstLineIx];

        int f00 = sw;
        int f10 = se;
        int f01 = nw;
        int f11 = ne;

        double fx00 = 0.5 * (se - wsw);
        double fx10 = 0.5 * (ese - sw);
        double fx01 = 0.5 * (ne - wnw);
        double fx11 = 0.5 * (ene - nw);
        double fy00 = 0.5 * (nw - ssw);
        double fy10 = 0.5 * (ne - sse);
        double fy01 = 0.5 * (nnw - sw);
        double fy11 = 0.5 * (nne - se);

        double fxy00 = 0.25 * (ne - sse - wnw + swsw);
        double fxy10 = 0.25 * (ene - sese - nw + ssw);
        double fxy01 = 0.25 * (nne - se - nwnw + wsw);
        double fxy11 = 0.25 * (nene - ese - nnw + sw);


        // Offset by two to save on incrementing indices later
        firstLineIx += 2;

        final int limit = firstLineIx + computingParams.mInputAxisLen - 2;

        for (; ; ) {
            final double center, nwswHalf, swseHalf, neseHalf, nwneHalf;
            {
                // Bicubic interpolation
                final double var1 = f00 - f10;
                final double var2 = fy00 - fy10;
                final double var3 = fxy00 + fxy10;
                final double var4 = fx00 - fx01;
                final double var5 = f00 - f01;
                final double var6 = fy00 + fy01;
                final double var7 = fx10 - fx11;
                final double var8 = fxy10 + fxy01 + fxy11;
                final double var9 = fy01 - fy11;
                final double var10 = var5 - f10 + f11;
                final double var11 = var4 + var7;
                final double var12 = var6 - fy10 - fy11;
                final double var13 = var1 + var1 + fx00 + fx10;
                final double var14 = var2 + var2;
                final double var15 = var14 + var3;
                final double var16 = var5 + var5 + var6;
                final double var17 = var4 + var4;
                final double var18 = var17 + fxy00 + fxy01;
                final double var19 = var10 + var10 + var10;
                final double var20 = -var19 - var19;
                final double var21 = fxy00 + fxy00 + var8;
                final double var22 = var15 + var9;
                final double var23 = var7 + var18;
                final double var24 = var10 + var10 + var11 + var12;
                final double var25 = var19 + var14 + var17 + var7 + var9;

                final double a00 = f00;
                final double a10 = fx00;
                final double a20 = -var13 - var1 - fx00;
                final double a30 = var13;
                final double a01 = fy00;
                final double a11 = fxy00;
                final double a21 = -var15 - var2 - fxy00;
                final double a31 = var15;
                final double a02 = -var16 - var5 - fy00;
                final double a12 = -var18 - var4 - fxy00;
                final double a22 = var25 + var25 + var25 + var21 + var21 - fxy11;
                final double a32 = var20 - var11 - var11 - var11 - var22 - var22 - var8 + fxy10;
                final double a03 = var16;
                final double a13 = var18;
                final double a23 = var20 - var23 - var23 - var12 - var12 - var12 - var8 + fxy01;
                final double a33 = var24 + var24 + fxy00 + var8;

                // Value at the point halfway between SW and SE
                swseHalf = a00 + 0.5 * a10 + 0.25 * a20 + 0.125 * a30;

                {
                    final double e0 = a00 + a01 + a02 + a03;
                    final double e1 = a10 + a11 + a12 + a13;
                    final double e2 = a20 + a21 + a22 + a23;
                    final double e3 = a30 + a31 + a32 + a33;

                    // Value at the point halfway between NW and NE
                    nwneHalf = e0 + 0.5 * e1 + 0.25 * e2 + 0.125 * e3;
                }

                {
                    final double c0 = a00 + 0.5 * a01 + 0.25 * a02 + 0.125 * a03;
                    final double c1 = a10 + 0.5 * a11 + 0.25 * a12 + 0.125 * a13;
                    final double c2 = a20 + 0.5 * a21 + 0.25 * a22 + 0.125 * a23;
                    final double c3 = a30 + 0.5 * a31 + 0.25 * a32 + 0.125 * a33;

                    // Value at the center of the NW-SW-SE-NE square
                    center = c0 + 0.5 * c1 + 0.25 * c2 + 0.125 * c3;

                    // Value at the point halfway between NE and SE
                    neseHalf = c0 + c1 + c2 + c3;

                    // Value at the point halfway between NW and SW
                    nwswHalf = c0;
                }
            }

            // NW corner
            computingParams.mOutput[outputIx] = unitElementToShadePixel(nw, nwswHalf, center, nwneHalf, dsfDouble);
            // SW corner
            computingParams.mOutput[outputIx + computingParams.mOutputWidth] = unitElementToShadePixel(nwswHalf, sw, swseHalf, center, dsfDouble);
            // SE corner
            computingParams.mOutput[1 + outputIx + computingParams.mOutputWidth] = unitElementToShadePixel(center, swseHalf, se, neseHalf, dsfDouble);
            // NE corner
            computingParams.mOutput[1 + outputIx] = unitElementToShadePixel(nwneHalf, center, neseHalf, ne, dsfDouble);

            outputIx += 2;

            if (++firstLineIx < limit) {
                // Slide to the next column, reuse any old value we can
                nw = ne;
                ne = ene;
                sw = se;
                se = ese;

                f00 = f10;
                f01 = f11;
                fx00 = fx10;
                fx01 = fx11;
                fy00 = fy10;
                fy01 = fy11;
                fxy00 = fxy10;
                fxy01 = fxy11;

                f10 = se;
                f11 = ne;
                fy10 = 0.5 * (ne - sese);
                fy11 = 0.5 * (nene - se);

                nene = input[firstLineIx];
                ene = input[firstLineIx + secondLineOffset];
                ese = input[firstLineIx + thirdLineOffset];
                sese = input[firstLineIx + fourthLineOffset];

                fx10 = 0.5 * (ese - sw);
                fx11 = 0.5 * (ene - nw);
                fxy10 = 0.25 * (ene - sese - nw + sse);
                fxy11 = 0.25 * (nene - ese - nne + sw);
            } else {
                break;
            }
        }

        return outputIx;
    }

    @Override
    protected int processUnitElement_4x4(final double nw, final double sw, final double se, final double ne, final double nwnw, final double wnw, final double wsw, final double swsw, final double ssw, final double sse, final double sese, final double ese, final double ene, final double nene, final double nne, final double nnw, final double dsf, int outputIx, final ComputingParams computingParams) {
        final double center, nwswHalf, swseHalf, neseHalf, nwneHalf;
        {
            // Bicubic interpolation
            final double f00 = sw;
            final double f10 = se;
            final double f01 = nw;
            final double f11 = ne;

            final double fx00 = 0.5 * (se - wsw);
            final double fx10 = 0.5 * (ese - sw);
            final double fx01 = 0.5 * (ne - wnw);
            final double fx11 = 0.5 * (ene - nw);
            final double fy00 = 0.5 * (nw - ssw);
            final double fy10 = 0.5 * (ne - sse);
            final double fy01 = 0.5 * (nnw - sw);
            final double fy11 = 0.5 * (nne - se);

            final double fxy00 = 0.25 * (ne - sse - wnw + swsw);
            final double fxy10 = 0.25 * (ene - sese - nw + ssw);
            final double fxy01 = 0.25 * (nne - se - nwnw + wsw);
            final double fxy11 = 0.25 * (nene - ese - nnw + sw);

            final double var1 = f00 - f10;
            final double var2 = fy00 - fy10;
            final double var3 = fxy00 + fxy10;
            final double var4 = fx00 - fx01;
            final double var5 = f00 - f01;
            final double var6 = fy00 + fy01;
            final double var7 = fx10 - fx11;
            final double var8 = fxy10 + fxy01 + fxy11;
            final double var9 = fy01 - fy11;
            final double var10 = var5 - f10 + f11;
            final double var11 = var4 + var7;
            final double var12 = var6 - fy10 - fy11;
            final double var13 = var1 + var1 + fx00 + fx10;
            final double var14 = var2 + var2;
            final double var15 = var14 + var3;
            final double var16 = var5 + var5 + var6;
            final double var17 = var4 + var4;
            final double var18 = var17 + fxy00 + fxy01;
            final double var19 = var10 + var10 + var10;
            final double var20 = -var19 - var19;
            final double var21 = fxy00 + fxy00 + var8;
            final double var22 = var15 + var9;
            final double var23 = var7 + var18;
            final double var24 = var10 + var10 + var11 + var12;
            final double var25 = var19 + var14 + var17 + var7 + var9;

            final double a00 = f00;
            final double a10 = fx00;
            final double a20 = -var13 - var1 - fx00;
            final double a30 = var13;
            final double a01 = fy00;
            final double a11 = fxy00;
            final double a21 = -var15 - var2 - fxy00;
            final double a31 = var15;
            final double a02 = -var16 - var5 - fy00;
            final double a12 = -var18 - var4 - fxy00;
            final double a22 = var25 + var25 + var25 + var21 + var21 - fxy11;
            final double a32 = var20 - var11 - var11 - var11 - var22 - var22 - var8 + fxy10;
            final double a03 = var16;
            final double a13 = var18;
            final double a23 = var20 - var23 - var23 - var12 - var12 - var12 - var8 + fxy01;
            final double a33 = var24 + var24 + fxy00 + var8;

            // Value at the point halfway between SW and SE
            swseHalf = a00 + 0.5 * a10 + 0.25 * a20 + 0.125 * a30;

            {
                final double e0 = a00 + a01 + a02 + a03;
                final double e1 = a10 + a11 + a12 + a13;
                final double e2 = a20 + a21 + a22 + a23;
                final double e3 = a30 + a31 + a32 + a33;

                // Value at the point halfway between NW and NE
                nwneHalf = e0 + 0.5 * e1 + 0.25 * e2 + 0.125 * e3;
            }

            {
                final double c0 = a00 + 0.5 * a01 + 0.25 * a02 + 0.125 * a03;
                final double c1 = a10 + 0.5 * a11 + 0.25 * a12 + 0.125 * a13;
                final double c2 = a20 + 0.5 * a21 + 0.25 * a22 + 0.125 * a23;
                final double c3 = a30 + 0.5 * a31 + 0.25 * a32 + 0.125 * a33;

                // Value at the center of the NW-SW-SE-NE square
                center = c0 + 0.5 * c1 + 0.25 * c2 + 0.125 * c3;

                // Value at the point halfway between NE and SE
                neseHalf = c0 + c1 + c2 + c3;

                // Value at the point halfway between NW and SW
                nwswHalf = c0;
            }
        }

        final double dsfDouble = 2 * dsf;

        // NW corner
        computingParams.mOutput[outputIx] = unitElementToShadePixel(nw, nwswHalf, center, nwneHalf, dsfDouble);
        // SW corner
        computingParams.mOutput[outputIx + computingParams.mOutputWidth] = unitElementToShadePixel(nwswHalf, sw, swseHalf, center, dsfDouble);
        // SE corner
        computingParams.mOutput[1 + outputIx + computingParams.mOutputWidth] = unitElementToShadePixel(center, swseHalf, se, neseHalf, dsfDouble);
        // NE corner
        computingParams.mOutput[1 + outputIx] = unitElementToShadePixel(nwneHalf, center, neseHalf, ne, dsfDouble);

        return outputIx + 2;
    }

    /**
     * Note: This "raw" method is provided as a reference.
     *
     * <p>
     * Get bicubic interpolated value corresponding to coordinates (x,y) from a 4x4 unit element.
     * The coordinates (x,y)=(0..1,0..1) select a point from the nw-sw-se-ne square, where (0,0) corresponds to the south-west vertex,
     * and (1,1) corresponds to the north-east vertex.
     * </p>
     * <p>
     * 4x4 unit element layout:
     * <pre>{@code
     * nwnw nnw nne nene
     * wnw   nw ne   ene
     * wsw   sw se   ese
     * swsw ssw sse sese}
     * </pre>
     * </p>
     *
     * @param x    X-coordinate of the interpolation point. [0..1]
     * @param y    Y-coordinate of the interpolation point. [0..1]
     * @param nw   North-west value.
     * @param sw   South-west value.
     * @param se   South-east value.
     * @param ne   North-east value.
     * @param nwnw North-west-north-west value.
     * @param wnw  West-north-west value.
     * @param wsw  West-south-west value.
     * @param swsw South-west-south-west value.
     * @param ssw  South-south-west value.
     * @param sse  South-south-east value.
     * @param sese South-east-south-east value.
     * @param ese  East-south-east value.
     * @param ene  East-north-east value.
     * @param nene North-east-north-east value.
     * @param nne  North-north-east value.
     * @param nnw  North-north-west value.
     * @return Bicubic interpolated value from a 4x4 unit element corresponding to coordinates (x,y).
     */
    public double getBicubicPoint(double x, double y, double nw, double sw, double se, double ne, double nwnw, double wnw, double wsw, double swsw, double ssw, double sse, double sese, double ese, double ene, double nene, double nne, double nnw) {
        final double f00 = sw;
        final double f10 = se;
        final double f01 = nw;
        final double f11 = ne;

        final double fx00 = 0.5 * (se - wsw);
        final double fx10 = 0.5 * (ese - sw);
        final double fx01 = 0.5 * (ne - wnw);
        final double fx11 = 0.5 * (ene - nw);
        final double fy00 = 0.5 * (nw - ssw);
        final double fy10 = 0.5 * (ne - sse);
        final double fy01 = 0.5 * (nnw - sw);
        final double fy11 = 0.5 * (nne - se);

        final double fxy00 = 0.25 * (ne - sse - wnw + swsw);
        final double fxy10 = 0.25 * (ene - sese - nw + ssw);
        final double fxy01 = 0.25 * (nne - se - nwnw + wsw);
        final double fxy11 = 0.25 * (nene - ese - nnw + sw);

        final double a00 = f00;
        final double a10 = fx00;
        final double a20 = 3 * (-f00 + f10) - 2 * fx00 - fx10;
        final double a30 = 2 * (f00 - f10) + fx00 + fx10;
        final double a01 = fy00;
        final double a11 = fxy00;
        final double a21 = 3 * (-fy00 + fy10) - 2 * fxy00 - fxy10;
        final double a31 = 2 * (fy00 - fy10) + fxy00 + fxy10;
        final double a02 = 3 * (-f00 + f01) - 2 * fy00 - fy01;
        final double a12 = 3 * (-fx00 + fx01) - 2 * fxy00 - fxy01;
        final double a22 = 9 * (f00 - f10 - f01 + f11) + 6 * (fx00 - fx01 + fy00 - fy10) + 3 * (fx10 - fx11 + fy01 - fy11) + 4 * fxy00 + 2 * (fxy10 + fxy01) + fxy11;
        final double a32 = 6 * (-f00 + f10 + f01 - f11) + 3 * (-fx00 - fx10 + fx01 + fx11) + 4 * (-fy00 + fy10) + 2 * (-fy01 + fy11 - fxy00 - fxy10) - fxy01 - fxy11;
        final double a03 = 2 * (f00 - f01) + fy00 + fy01;
        final double a13 = 2 * (fx00 - fx01) + fxy00 + fxy01;
        final double a23 = 6 * (-f00 + f10 + f01 - f11) + 4 * (-fx00 + fx01) + 2 * (-fx10 + fx11 - fxy00 - fxy01) + 3 * (-fy00 + fy10 - fy01 + fy11) - fxy10 - fxy11;
        final double a33 = 4 * (f00 - f10 - f01 + f11) + 2 * (fx00 + fx10 - fx01 - fx11 + fy00 - fy10 + fy01 - fy11) + fxy00 + fxy10 + fxy01 + fxy11;

        final double c0 = a00 + y * (a01 + y * (a02 + y * a03));
        final double c1 = a10 + y * (a11 + y * (a12 + y * a13));
        final double c2 = a20 + y * (a21 + y * (a22 + y * a23));
        final double c3 = a30 + y * (a31 + y * (a32 + y * a33));

        return c0 + x * (c1 + x * (c2 + x * c3));
    }
}
