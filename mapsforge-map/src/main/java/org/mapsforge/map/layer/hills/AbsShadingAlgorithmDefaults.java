/*
 * Copyright 2020-2022 usrusr
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
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbsShadingAlgorithmDefaults implements ShadingAlgorithm {

    private static final Logger LOGGER = Logger.getLogger(AbsShadingAlgorithmDefaults.class.getName());

    protected abstract byte[] convert(ByteBuffer map, int axisLength, int rowLen, int padding, HgtCache.HgtFileInfo source) throws IOException;

    @Override
    public RawShadingResult transformToByteBuffer(HgtCache.HgtFileInfo source, int padding) {
        int axisLength = getAxisLenght(source);
        int rowLen = axisLength + 1;
        try {
            ByteBuffer map = source.getFile().asByteBuffer();

            byte[] bytes = convert(map, axisLength, rowLen, padding, source);
            return new RawShadingResult(bytes, axisLength, axisLength, padding);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            return null;
        }
    }
}
