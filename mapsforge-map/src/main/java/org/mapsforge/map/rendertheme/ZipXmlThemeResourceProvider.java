/*
 * Copyright 2021 eddiemuc
 * Copyright 2021 devemux86
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
package org.mapsforge.map.rendertheme;

import org.mapsforge.core.util.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Resource provider reading resource files out of a zip input stream.
 * <p>
 * Resources are cached.
 */
public class ZipXmlThemeResourceProvider implements XmlThemeResourceProvider {

    private final Map<String, byte[]> files = new HashMap<>();
    private final List<XmlTheme> xmlThemes = new ArrayList<>();

    public static class XmlTheme {
        public final String name;
        public final String path;

        public XmlTheme(String zipName) {
            int idx = zipName == null ? -1 : zipName.lastIndexOf("/");
            if (idx < 0) {
                this.name = zipName == null ? "" : zipName;
                this.path = "";
            } else {
                this.name = zipName.substring(idx + 1);
                this.path = zipName.substring(0, idx);
            }
        }

        public String toString() {
            return path.isEmpty() ? name : path + "/" + name;
        }
    }

    /**
     * @param zipInputStream zip stream to read resources from
     * @throws IOException if a problem occurs reading the stream
     */
    public ZipXmlThemeResourceProvider(ZipInputStream zipInputStream) throws IOException {
        this(zipInputStream, Integer.MAX_VALUE);
    }

    /**
     * @param zipInputStream         zip stream to read resources from
     * @param maxResourceSizeToCache only resources in the zip stream with a maximum size of this parameter (in bytes) are cached and provided
     * @throws IOException if a problem occurs reading the stream
     */
    public ZipXmlThemeResourceProvider(ZipInputStream zipInputStream, int maxResourceSizeToCache) throws IOException {
        if (zipInputStream == null) {
            return;
        }

        try {
            ZipEntry zipEntry = zipInputStream.getNextEntry();
            while (zipEntry != null) {
                if (!zipEntry.isDirectory() && zipEntry.getSize() <= maxResourceSizeToCache) {
                    byte[] entry = readComplete(zipInputStream, (int) zipEntry.getSize());
                    String fileName = zipEntry.getName();
                    if (fileName.startsWith("/")) {
                        fileName = fileName.substring(1);
                    }
                    files.put(fileName, entry);
                    if (fileName.endsWith(".xml")) {
                        xmlThemes.add(new XmlTheme(fileName));
                    }
                }
                zipEntry = zipInputStream.getNextEntry();
            }
        } finally {
            IOUtils.closeQuietly(zipInputStream);
        }
    }

    @Override
    public InputStream createInputStream(String relativePath, String source) {
        String sourceKey = source;
        if (sourceKey.startsWith(XmlUtils.PREFIX_FILE)) {
            sourceKey = sourceKey.substring(XmlUtils.PREFIX_FILE.length());
        }
        if (sourceKey.startsWith("/")) {
            sourceKey = sourceKey.substring(1);
        }
        if (relativePath != null) {
            if (relativePath.startsWith("/")) {
                relativePath = relativePath.substring(1);
            }
            if (relativePath.endsWith("/")) {
                relativePath = relativePath.substring(0, relativePath.length() - 1);
            }
            sourceKey = relativePath.isEmpty() ? sourceKey : relativePath + "/" + sourceKey;
        }
        if (files.containsKey(sourceKey)) {
            return new ByteArrayInputStream(files.get(sourceKey));
        }
        return null;
    }

    public int getSize() {
        return files.size();
    }

    public List<XmlTheme> getXmlThemes() {
        return xmlThemes;
    }

    private static byte[] readComplete(InflaterInputStream stream, int size) throws IOException {
        final byte[] result = new byte[size];
        int chunkSize = Math.min(size, 8 * 1024);
        int readBytes;
        int pos = 0;
        while ((readBytes = stream.read(result, pos, chunkSize)) > 0) {
            pos += readBytes;
            if (chunkSize > result.length - pos) {
                chunkSize = result.length - pos;
            }
        }
        return result;
    }
}
