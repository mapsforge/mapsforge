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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class DemFileZipEntryFS implements DemFile {

    protected final File zipFile;
    protected final String zipEntryName;
    protected final long zipEntrySize;

    public DemFileZipEntryFS(File zipFile, String zipEntryName, long zipEntrySize) {
        this.zipFile = zipFile;
        this.zipEntryName = zipEntryName;
        this.zipEntrySize = zipEntrySize;
    }

    @Override
    public String getName() {
        return zipEntryName;
    }

    @Override
    public long getSize() {
        return zipEntrySize;
    }

    @Override
    public InputStream openInputStream(int bufferSize) throws IOException {
        InputStream output = null;

        final InputStream rawStream = rawStream();

        if (rawStream != null) {
            output = new BufferedInputStream(rawStream, bufferSize);
        }

        return output;
    }

    @Override
    public InputStream asStream() throws IOException {
        return openInputStream(BufferSizeDefault);
    }

    @Override
    public InputStream asRawStream() throws IOException {
        InputStream output = null;

        final InputStream rawStream = rawStream();

        if (rawStream != null) {
            output = new BufferedInputStream(rawStream, BufferSizeRaw);
        }

        return output;
    }

    protected InputStream rawStream() throws IOException {
        final ZipFile zipFile = new ZipFile(this.zipFile);
        final ZipEntry zipEntry = zipFile.getEntry(zipEntryName);

        return new DemZipInputStream(zipFile, zipEntry);
    }
}
