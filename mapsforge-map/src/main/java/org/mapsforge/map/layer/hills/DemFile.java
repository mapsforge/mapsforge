/*
 * Copyright 2022 usrusr
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
import java.util.logging.Logger;

public interface DemFile {
    Logger LOGGER = Logger.getLogger(DemFile.class.getName());

    /**
     * Buffer size is relatively small to reduce wasteful read-ahead (buffer fill) during multi-threaded processing
     */
    int BufferSize = 512;

    /**
     * Buffer size for "raw" streams, when reading one Short at a time, skipping between
     */
    int BufferSizeRaw = Short.SIZE / Byte.SIZE;

    String getName();

    /**
     * @return Size in bytes.
     */
    long getSize();

    /**
     * @return Buffered stream.
     */
    InputStream openInputStream() throws IOException;

    /**
     * @return Buffered stream.
     */
    InputStream asStream() throws IOException;

    /**
     * @return Raw stream (i.e. unbuffered if possible).
     */
    InputStream asRawStream() throws IOException;
}
