package org.mapsforge.map.android.hills;

import android.content.ContentResolver;
import org.mapsforge.core.util.IOUtils;
import org.mapsforge.map.layer.hills.DemFile;
import org.mapsforge.map.layer.hills.DemFileFS;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class DemFileAndroidContent implements DemFile {
    private final DemFolderAndroidContent.Entry entry;
    private final ContentResolver contentResolver;

    public DemFileAndroidContent(DemFolderAndroidContent.Entry entry, ContentResolver contentResolver) {
        this.entry = entry;
        this.contentResolver = contentResolver;
    }

    @Override
    public String getName() {
        return entry.name;
    }

    @Override
    public InputStream openInputStream() throws FileNotFoundException {
        return contentResolver.openInputStream(entry.uri);
    }

    @Override
    public long getSize() {
        return entry.size;
    }

    @Override
    public ByteBuffer asByteBuffer() throws IOException {
        InputStream stream = null;
        try {
            String nameLowerCase = entry.name.toLowerCase();
            stream = contentResolver.openInputStream(entry.uri);
            if (nameLowerCase.endsWith(".zip")) {
                return DemFileFS.tryZippedSingleHgt(entry.name, stream);
            } else {
                return DemFileFS.streamAsByteBuffer(entry.name, stream, (int) entry.size);
            }
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }
}
