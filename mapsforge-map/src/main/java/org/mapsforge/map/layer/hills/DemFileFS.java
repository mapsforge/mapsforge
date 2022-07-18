package org.mapsforge.map.layer.hills;

import org.mapsforge.core.util.IOUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DemFileFS implements DemFile {
    private static final Logger LOGGER = Logger.getLogger(AbsShadingAlgorithmDefaults.class.getName());


    final private File file;

    public DemFileFS(File file) {
        this.file = file;
    }

    @Override
    public String getName() {
        return file.getName();
    }

    @Override
    public InputStream openInputStream() throws FileNotFoundException {
        return new BufferedInputStream(new FileInputStream(file));
    }

    @Override
    public long getSize() {
        return file.length();
    }


    public static ByteBuffer tryZippedSingleHgt(String name, InputStream inputStream) throws IOException {
        ZipInputStream zipInputStream = null;
        try {
            zipInputStream = new ZipInputStream(inputStream);
            ZipEntry entry;
            String nameLowerCase = name.toLowerCase();
            String expectedNameLC = nameLowerCase.substring(0, nameLowerCase.length() - 4) + ".hgt";
            while (null != (entry = zipInputStream.getNextEntry())) {
                if (!expectedNameLC.equals(entry.getName().toLowerCase())) continue;

                int todo = (int) entry.getSize();
                return streamAsByteBuffer(name, zipInputStream, todo);
            }
        } finally {
            IOUtils.closeQuietly(zipInputStream);
        }
        return null;
    }

    /** does *not* close the stream! */
    public static ByteBuffer streamAsByteBuffer(String name, InputStream stream, int todo) throws IOException {
        ByteBuffer map = ByteBuffer.allocate(todo);
        int done = 0;
        while (todo > 0) {
            int read = stream.read(map.array(), done, todo);
            if (read == 0) {
                LOGGER.log(Level.SEVERE, "failed to read entire .hgt in " + name + " " + done + " of " + todo + " done");
                return null;
            }
            done += read;
            todo -= read;
        }
        map.order(ByteOrder.BIG_ENDIAN);
        return map;
    }

    @Override
    public ByteBuffer asByteBuffer() throws IOException {
        FileChannel channel = null;
        FileInputStream stream = null;
        try {
            String nameLowerCase = file.getName().toLowerCase();
            if (nameLowerCase.endsWith(".zip")) {
                return tryZippedSingleHgt(file.getName(), new FileInputStream(file));
            } else {
                FileInputStream fileInputStream = new FileInputStream(file);
                channel = fileInputStream.getChannel();
                stream = fileInputStream;
                ByteBuffer map = channel.map(FileChannel.MapMode.READ_ONLY, 0, file.length());
                map.order(ByteOrder.BIG_ENDIAN);
                return map;
            }
        } finally {
            IOUtils.closeQuietly(channel);
            IOUtils.closeQuietly(stream);
        }
    }
}
