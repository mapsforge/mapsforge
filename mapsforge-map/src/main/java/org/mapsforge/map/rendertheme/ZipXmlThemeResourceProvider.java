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
 * Resource provider reading resource files out of a ZIP input stream.
 *
 * Ressources are cached.
 */
public class ZipXmlThemeResourceProvider implements XmlThemeResourceProvider {

    private static final String PREFIX_FILE = "file:";

    private Map<String, byte[]> files = new HashMap<>();
    private List<String> xmlThemes = new ArrayList<>();

    /**
     * @param zipInputStream zip stream to read resources from
     * @throws IOException if a problem occurs reading the stream
     */
    public ZipXmlThemeResourceProvider(ZipInputStream zipInputStream) throws IOException {
        this(zipInputStream, Integer.MAX_VALUE);
    }

    /**
     * @param zipInputStream zip stream to read resources from
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
                byte[] entry = readComplete(zipInputStream, (int)zipEntry.getSize());
                String fileName = zipEntry.getName();
                if (fileName.startsWith("/")) {
                    fileName = fileName.substring(1);
                }
                files.put(fileName, entry);
                if (zipEntry.getName().endsWith(".xml")) {
                    xmlThemes.add(zipEntry.getName());
                }
            }
        } finally {
            IOUtils.closeQuietly(zipInputStream);
        }
    }

    @Override
    public InputStream createInputStream(String source) {
        String sourceKey = source;
        if (sourceKey.startsWith(PREFIX_FILE)) {
            sourceKey = sourceKey.substring(PREFIX_FILE.length());
        }
        if (sourceKey.startsWith("/")) {
            sourceKey = sourceKey.substring(1);
        }
        if (files.containsKey(sourceKey)) {
            return new ByteArrayInputStream(files.get(sourceKey));
        }
        return null;
    }

    public List<String> getXmlThemes() {
        return xmlThemes;
    }

    public int getSize() {
        return files.size();
    }

    private static byte[] readComplete(InflaterInputStream stream, int size) throws IOException {
        int chunkSize = size < 10000 ? size : 10000;
        final byte[] result = new byte[size];
        int readBytes = 0;
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
