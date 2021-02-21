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
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Resource provider reading resource files out of a zip input stream.
 * <p>
 * Resources are cached.
 */
public class ZipXmlThemeResourceProvider implements XmlThemeResourceProvider {

    private final Map<String, byte[]> files = new HashMap<>();
    private final List<String> xmlThemes = new ArrayList<>();

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
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                if (zipEntry.isDirectory() || zipEntry.getSize() > maxResourceSizeToCache) {
                    continue;
                }
                byte[] entry = streamToBytes(zipInputStream, (int) zipEntry.getSize());
                String fileName = zipEntryName(zipEntry.getName());
                files.put(fileName, entry);
                if (isXmlTheme(fileName)) {
                    xmlThemes.add(fileName);
                }
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

    /**
     * @return the number of files in the archive.
     */
    public int getCount() {
        return files.size();
    }

    /**
     * @return the XML theme paths in the archive.
     */
    public List<String> getXmlThemes() {
        return xmlThemes;
    }

    private static boolean isXmlTheme(String fileName) {
        return fileName.toLowerCase(Locale.ROOT).endsWith(".xml");
    }

    /**
     * Scans a given zip stream for contained xml themes without actually reading and storing its content in memory.
     * <p>
     * This method is useful to find out which xml themes are available across multiple zip files
     * without actually have to read them all into memory.
     *
     * @param zipInputStream zip stream to read resources from
     * @return the XML theme paths in the archive
     * @throws IOException if a problem occurs reading the stream
     */
    public static List<String> scanXmlThemes(ZipInputStream zipInputStream) throws IOException {
        if (zipInputStream == null) {
            return Collections.emptyList();
        }

        List<String> xmlThemes = new ArrayList<>();

        try {
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                if (zipEntry.isDirectory()) {
                    continue;
                }
                String fileName = zipEntryName(zipEntry.getName());
                if (isXmlTheme(fileName)) {
                    xmlThemes.add(fileName);
                }
            }
        } finally {
            IOUtils.closeQuietly(zipInputStream);
        }

        return xmlThemes;
    }

    private static byte[] streamToBytes(InputStream in, int size) throws IOException {
        byte[] bytes = new byte[size];
        int count, offset = 0;
        while ((count = in.read(bytes, offset, size)) > 0) {
            size -= count;
            offset += count;
        }
        return bytes;
    }

    private static String zipEntryName(String name) {
        if (name.startsWith("/")) {
            return name.substring(1);
        }
        return name;
    }
}
