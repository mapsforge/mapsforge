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
package org.mapsforge.map.android.hills;

import android.content.ContentResolver;

import org.mapsforge.map.layer.hills.DemFile;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public class DemFileAndroidContent implements DemFile {
    protected final DemFolderAndroidContent.Entry entry;
    protected final ContentResolver contentResolver;

    public DemFileAndroidContent(DemFolderAndroidContent.Entry entry, ContentResolver contentResolver) {
        this.entry = entry;
        this.contentResolver = contentResolver;
    }

    @Override
    public String getName() {
        return entry.name;
    }

    @Override
    public long getSize() {
        return entry.size;
    }

    @Override
    public InputStream openInputStream(int bufferSize) throws IOException {
        InputStream output = rawStream();

        if (output != null) {
            output = new BufferedInputStream(output, bufferSize);
        }

        return output;
    }

    @Override
    public InputStream asStream() throws IOException {
        return openInputStream(BufferSizeDefault);
    }

    @Override
    public InputStream asRawStream() throws IOException {
        InputStream output = rawStream();

        if (output != null) {
            output = new BufferedInputStream(output, BufferSizeRaw);
        }

        return output;
    }

    protected InputStream rawStream() throws IOException {
        return contentResolver.openInputStream(entry.uri);
    }
}
