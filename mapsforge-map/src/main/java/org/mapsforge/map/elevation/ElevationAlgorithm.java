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

import org.mapsforge.map.layer.hills.AThreadedHillShading;
import org.mapsforge.map.layer.hills.HgtFileInfo;

public class ElevationAlgorithm extends AThreadedHillShading {

    // We're using byte array to store short-s.
    public static final int OUTPUT_ELEMENT_SIZE = (Short.SIZE / Byte.SIZE);

    @Override
    protected int processUnitElement_2x2(double nw, double sw, double se, double ne, double dsf, int outputIx, ComputingParams computingParams) {
        final short nwShort = (short) nw;

        computingParams.mOutput[outputIx] = (byte) ((nwShort >> 8) & 0xff);
        computingParams.mOutput[outputIx + 1] = (byte) nwShort;

        return outputIx + OUTPUT_ELEMENT_SIZE;
    }

    @Override
    protected int getOutputElementSizeBytes() {
        return OUTPUT_ELEMENT_SIZE;
    }

    @Override
    protected int getResolutionFactor(int inputAxisLen, int outputAxisLen) {
        return 1;
    }
}
