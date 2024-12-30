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

import static org.mapsforge.map.layer.hills.HgtFileInfo.HGT_ELEMENT_SIZE;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

public interface DemFile {
    Logger LOGGER = Logger.getLogger(DemFile.class.getName());

    /**
     * Default buffer size is adjusted to the line size in a 3" HGT file (works well for 1" HGT files too),
     * to optimize read performance but also reduce wasteful read-ahead (buffer fill) during multi-threaded processing
     */
    int BufferSizeDefault = 1201 * HGT_ELEMENT_SIZE;

    /**
     * Buffer size for emulating "raw" streams, when reading one {@code Short} at a time, skipping between
     */
    int BufferSizeRaw = HGT_ELEMENT_SIZE;

    String getName();

    /**
     * @return Size in bytes.
     */
    long getSize();

    /**
     * @param bufferSize Buffered input stream buffer size, can be useful to optimize I/O read performance
     * @return Buffered stream.
     */
    InputStream openInputStream(int bufferSize) throws IOException;

    /**
     * @return Buffered stream.
     */
    InputStream asStream() throws IOException;

    /**
     * @return Raw stream (i.e. unbuffered if possible).
     */
    InputStream asRawStream() throws IOException;
}
