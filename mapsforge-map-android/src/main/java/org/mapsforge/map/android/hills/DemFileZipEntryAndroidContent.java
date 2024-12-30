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
package org.mapsforge.map.android.hills;

import android.content.ContentResolver;

import org.mapsforge.core.util.IOUtils;
import org.mapsforge.map.android.hills.DemFolderAndroidContent.Entry;
import org.mapsforge.map.layer.hills.DemFile;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.zip.ZipInputStream;

/**
 * <em>WARNING: The performance of this can be very poor. Use {@link org.mapsforge.map.layer.hills.DemFileZipEntryFS} instead if you can.</em>
 */
public class DemFileZipEntryAndroidContent implements DemFile {

    protected final Entry contentResolverEntry;
    protected final ContentResolver contentResolver;
    protected final String zipEntryName;
    protected final long zipEntrySize;

    /**
     * <em>WARNING: The performance of this can be very poor. Use {@link org.mapsforge.map.layer.hills.DemFileZipEntryFS} instead if you can.</em>
     */
    public DemFileZipEntryAndroidContent(String zipEntry, long zipEntrySize, Entry contentResolverEntry, ContentResolver contentResolver) {
        this.contentResolverEntry = contentResolverEntry;
        this.contentResolver = contentResolver;
        this.zipEntryName = zipEntry;
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
        return rawStream(bufferSize);
    }

    @Override
    public InputStream asStream() throws IOException {
        return openInputStream(BufferSizeDefault);
    }

    @Override
    public InputStream asRawStream() throws IOException {
        return rawStream(BufferSizeRaw);
    }

    public InputStream rawStream(int bufferSize) throws IOException {
        ZipInputStream output = null;

        {
            ZipInputStream zipInputStream = null;
            try {
                zipInputStream = new ZipInputStream(new BufferedInputStream(contentResolver.openInputStream(contentResolverEntry.uri), bufferSize));
            } catch (FileNotFoundException e) {
                LOGGER.log(Level.WARNING, e.toString());
            }

            output = DemFolderZipAndroidContent.positionZipInputStreamToEntry(zipInputStream, getName());

            if (output == null) {
                IOUtils.closeQuietly(zipInputStream);
            }
        }

        return output;
    }
}
