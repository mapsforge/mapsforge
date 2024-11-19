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
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class DemZipInputStream extends InputStream {

    protected final ZipFile zipFile;
    protected final InputStream zipInputStream;

    public DemZipInputStream(ZipFile zipFile, ZipEntry zipEntry) throws IOException {
        this.zipFile = zipFile;
        this.zipInputStream = zipFile.getInputStream(zipEntry);
    }

    public void close() throws IOException {
        zipInputStream.close();
        zipFile.close();
    }

    public int read() throws IOException {
        return zipInputStream.read();
    }

    public int read(byte[] bytes) throws IOException {
        return zipInputStream.read(bytes);
    }

    public int read(byte[] bytes, int i, int i1) throws IOException {
        return zipInputStream.read(bytes, i, i1);
    }

    public long skip(long l) throws IOException {
        return zipInputStream.skip(l);
    }

    public int available() throws IOException {
        return zipInputStream.available();
    }

    public void mark(int i) {
        zipInputStream.mark(i);
    }

    public void reset() throws IOException {
        zipInputStream.reset();
    }

    public boolean markSupported() {
        return zipInputStream.markSupported();
    }

    // TODO (2024-11): Methods below must wait for Java 9/12
//    public byte[] readAllBytes() throws IOException {
//        return zipInputStream.readAllBytes();
//    }
//
//    public byte[] readNBytes(int i) throws IOException {
//        return zipInputStream.readNBytes(i);
//    }
//
//    public int readNBytes(byte[] bytes, int i, int i1) throws IOException {
//        return zipInputStream.readNBytes(bytes, i, i1);
//    }
//
//    public void skipNBytes(long l) throws IOException {
//        zipInputStream.skipNBytes(l);
//    }
//
//    public long transferTo(OutputStream outputStream) throws IOException {
//        return zipInputStream.transferTo(outputStream);
//    }
}
