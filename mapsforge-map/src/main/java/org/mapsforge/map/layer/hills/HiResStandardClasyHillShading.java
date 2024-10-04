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

/**
 * <p>
 * High resolution / high quality implementation of {@link StandardClasyHillShading}.
 * </p>
 * <p>
 * It doubles the number of pixels on each axis and uses bicubic interpolation to achieve the best visual results.
 * </p>
 * <p>
 * Uses two separate computing threads by default.
 * </p>
 *
 * @see StandardClasyHillShading
 */
public class HiResStandardClasyHillShading extends StandardClasyHillShading {

    public static final int ComputingThreadsCountDefault = 2;

    public HiResStandardClasyHillShading(final ClasyParams clasyParams) {
        super(clasyParams);
    }

    /**
     * Uses two separate computing threads by default.
     */
    public HiResStandardClasyHillShading() {
        super(new ClasyParams.Builder()
                .setComputingThreadsCount(ComputingThreadsCountDefault)
                .setHighQuality(true)
                .build());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getOutputAxisLen(final HgtCache.HgtFileInfo source) {
        return 2 * getInputAxisLen(source);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int processOneUnitElement(final double nw, final double sw, final double se, final double ne, final double nwnw, final double wnw, final double wsw, final double swsw, final double ssw, final double sse, final double sese, final double ese, final double ene, final double nene, final double nne, final double nnw, final double mpe, int outputIx, final ComputingParams computingParams) {
        final double center = getBicubicPoint(0.5, 0.5, nw, sw, se, ne, nwnw, wnw, wsw, swsw, ssw, sse, sese, ese, ene, nene, nne, nnw);
        final double nwswHalf = getBicubicPointZeroX(0.5, nw, sw, se, ne, nwnw, wnw, wsw, swsw, ssw, sse, sese, ese, ene, nene, nne, nnw);
        final double swseHalf = getBicubicPointZeroY(0.5, nw, sw, se, ne, nwnw, wnw, wsw, swsw, ssw, sse, sese, ese, ene, nene, nne, nnw);
        final double neseHalf = getBicubicPoint(1.0, 0.5, nw, sw, se, ne, nwnw, wnw, wsw, swsw, ssw, sse, sese, ese, ene, nene, nne, nnw);
        final double nwneHalf = getBicubicPoint(0.5, 1.0, nw, sw, se, ne, nwnw, wnw, wsw, swsw, ssw, sse, sese, ese, ene, nene, nne, nnw);

        final double mpeHalf = 0.5 * mpe;

        // NW corner
        computingParams.mOutput[outputIx] = unitElementToShadePixel(nw, nwswHalf, center, nwneHalf, mpeHalf);
        // SW corner
        computingParams.mOutput[outputIx + computingParams.mOutputWidth] = unitElementToShadePixel(nwswHalf, sw, swseHalf, center, mpeHalf);

        outputIx++;

        // SE corner
        computingParams.mOutput[outputIx + computingParams.mOutputWidth] = unitElementToShadePixel(center, swseHalf, se, neseHalf, mpeHalf);
        // NE corner
        computingParams.mOutput[outputIx] = unitElementToShadePixel(nwneHalf, center, neseHalf, ne, mpeHalf);

        outputIx++;

        return outputIx;
    }

    public static double getBicubicPoint(double x, double y, double nw, double sw, double se, double ne, double nwnw, double wnw, double wsw, double swsw, double ssw, double sse, double sese, double ese, double ene, double nene, double nne, double nnw) {
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
        final double a22 = 9 * (f00 - f10 - f01 + f11) + 6 * fx00 + 3 * fx10 - 6 * fx01 - 3 * fx11 + 6 * (fy00 - fy10) + 3 * (fy01 - fy11) + 4 * fxy00 + 2 * (fxy10 + fxy01) + fxy11;
        final double a32 = 6 * (-f00 + f10 + f01 - f11) + 3 * (-fx00 - fx10 + fx01 + fx11) + 4 * (-fy00 + fy10) + 2 * (-fy01 + fy11 - fxy00 - fxy10) - fxy01 - fxy11;
        final double a03 = 2 * (f00 - f01) + fy00 + fy01;
        final double a13 = 2 * (fx00 - fx01) + fxy00 + fxy01;
        final double a23 = 6 * (-f00 + f10 + f01 - f11) - 4 * fx00 - 2 * fx10 + 4 * fx01 + 2 * fx11 + 3 * (-fy00 + fy10 - fy01 + fy11) - 2 * fxy00 - fxy10 - 2 * fxy01 - fxy11;
        final double a33 = 4 * (f00 - f10 - f01 + f11) + 2 * (fx00 + fx10 - fx01 - fx11 + fy00 - fy10 + fy01 - fy11) + fxy00 + fxy10 + fxy01 + fxy11;

        final double x2 = x * x;
        final double x3 = x2 * x;
        final double y2 = y * y;
        final double y3 = y2 * y;

        final double c0 = a00 + y * a01 + y2 * a02 + y3 * a03;
        final double c1 = a10 + y * a11 + y2 * a12 + y3 * a13;
        final double c2 = a20 + y * a21 + y2 * a22 + y3 * a23;
        final double c3 = a30 + y * a31 + y2 * a32 + y3 * a33;

        final double point = c0 + x * c1 + x2 * c2 + x3 * c3;

        return point;
    }

    protected double getBicubicPointZeroX(double y, double nw, double sw, double se, double ne, double nwnw, double wnw, double wsw, double swsw, double ssw, double sse, double sese, double ese, double ene, double nene, double nne, double nnw) {
        final double f00 = sw;
        final double f01 = nw;

        final double fy00 = 0.5 * (nw - ssw);
        final double fy01 = 0.5 * (nnw - sw);

        final double a00 = f00;
        final double a01 = fy00;
        final double a02 = 3 * (-f00 + f01) - 2 * fy00 - fy01;
        final double a03 = 2 * (f00 - f01) + fy00 + fy01;

        final double y2 = y * y;
        final double y3 = y2 * y;

        final double point = a00 + y * a01 + y2 * a02 + y3 * a03;

        return point;
    }

    protected double getBicubicPointZeroY(double x, double nw, double sw, double se, double ne, double nwnw, double wnw, double wsw, double swsw, double ssw, double sse, double sese, double ese, double ene, double nene, double nne, double nnw) {
        final double f00 = sw;
        final double f10 = se;

        final double fx00 = 0.5 * (se - wsw);
        final double fx10 = 0.5 * (ese - sw);

        final double a00 = f00;
        final double a10 = fx00;
        final double a20 = 3 * (-f00 + f10) - 2 * fx00 - fx10;
        final double a30 = 2 * (f00 - f10) + fx00 + fx10;

        final double x2 = x * x;
        final double x3 = x2 * x;

        final double c0 = a00;
        final double c1 = a10;
        final double c2 = a20;
        final double c3 = a30;

        final double point = c0 + x * c1 + x2 * c2 + x3 * c3;

        return point;
    }
}
