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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class DemFileFS implements DemFile {

    protected final File file;

    public DemFileFS(File file) {
        this.file = file;
    }

    @Override
    public String getName() {
        return file.getName();
    }

    @Override
    public long getSize() {
        return file.length();
    }

    @Override
    public InputStream openInputStream(int bufferSize) throws IOException {
        return new BufferedInputStream(rawStream(), bufferSize);
    }

    @Override
    public InputStream asStream() throws IOException {
        return openInputStream(BufferSizeDefault);
    }

    @Override
    public InputStream asRawStream() throws IOException {
        return new BufferedInputStream(rawStream(), BufferSizeRaw);
    }

    protected InputStream rawStream() throws IOException {
        return new FileInputStream(file);
    }
}
