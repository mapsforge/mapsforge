package org.mapsforge.map.layer.hills;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public interface DemFile {
    String getName();

    InputStream openInputStream() throws FileNotFoundException;

    long getSize();

    ByteBuffer asByteBuffer() throws IOException;


}
