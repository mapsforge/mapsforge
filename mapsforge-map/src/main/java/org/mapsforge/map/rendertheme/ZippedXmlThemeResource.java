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

public class ZippedXmlThemeResource implements XmlThemeResourceProvider {

    private static final String PREFIX_FILE = "file:";

    private Map<String, byte[]> files = new HashMap<>();
    private List<String> xmlThemes = new ArrayList<>();

    public ZippedXmlThemeResource(ZipInputStream zippedInputStream) throws IOException {
        this(zippedInputStream, Integer.MAX_VALUE);
    }

    public ZippedXmlThemeResource(ZipInputStream zippedInputStream, int maxResourceSizeToCache) throws IOException {
        if (zippedInputStream == null) {
            return;
        }
        try {
            ZipEntry zipEntry;
            while ((zipEntry = zippedInputStream.getNextEntry()) != null) {
                if (zipEntry.isDirectory() || zipEntry.getSize() > maxResourceSizeToCache) {
                    continue;
                }
                byte[] entry = readComplete(zippedInputStream, (int)zipEntry.getSize());
                files.put("file:" + zipEntry.getName(), entry);
                if (zipEntry.getName().endsWith(".xml")) {
                    xmlThemes.add(zipEntry.getName());
                }
            }
        } finally {
            IOUtils.closeQuietly(zippedInputStream);
        }
    }

    @Override
    public InputStream createInputStream(String source) {
        if (files.get(source) != null) {
            return new ByteArrayInputStream(files.get(source));
        }
        if (files.get(PREFIX_FILE + source) != null) {
            return new ByteArrayInputStream(files.get(PREFIX_FILE + source));
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
