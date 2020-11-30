package org.mapsforge.map.layer.hills;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public abstract class AbsShadingAlgorithmDefaults implements ShadingAlgorithm {
    private static final Logger LOGGER = Logger.getLogger(org.mapsforge.map.layer.hills.AbsShadingAlgorithmDefaults.class.getName());

    @Override
    public RawShadingResult transformToByteBuffer(HgtCache.HgtFileInfo source, int padding) {
        int axisLength = getAxisLenght(source);
        int rowLen = axisLength + 1;
        InputStream stream = null;
        FileChannel channel = null;
        try {
            ByteBuffer map = null;
            File file = source.getFile();
            String nameLowerCase = file.getName().toLowerCase();
            if (nameLowerCase.endsWith(".zip")) {

                ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(file));
                ZipEntry entry;
                String expectedNameLC = nameLowerCase.substring(0, nameLowerCase.length() - 4) + ".hgt";
                while (null != (entry = zipInputStream.getNextEntry())) {
                    if (!expectedNameLC.equals(entry.getName().toLowerCase())) continue;

                    map = ByteBuffer.allocate((int) entry.getSize());
                    int todo = (int) entry.getSize();
                    int done = 0;
                    while (todo > 0) {
                        int read = zipInputStream.read(map.array(), done, todo);
                        if (read == 0) {
                            LOGGER.log(Level.SEVERE, "failed to read entire .hgt in " + file.getName() + " " + done + " of " + todo + " done");
                            return null;
                        }
                        done += read;
                        todo -= read;
                    }
                    map.order(ByteOrder.BIG_ENDIAN);
                    break;
                }
                if (map == null) {
                    LOGGER.log(Level.SEVERE, "no matching .hgt in " + file.getName());
                    return null;
                }
            } else {
                FileInputStream fileInputStream = new FileInputStream(file);
                channel = fileInputStream.getChannel();
                stream = fileInputStream;
                map = channel.map(FileChannel.MapMode.READ_ONLY, 0, file.length());
                map.order(ByteOrder.BIG_ENDIAN);
            }

            byte[] bytes = convert(map, axisLength, rowLen, padding, source);
            return new RawShadingResult(bytes, axisLength, axisLength, padding);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            return null;
        } finally {
            if (channel != null) try {
                channel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (stream != null) try {
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    protected abstract byte[] convert(ByteBuffer map, int axisLength, int rowLen, int padding, HgtCache.HgtFileInfo source) throws IOException;
}
