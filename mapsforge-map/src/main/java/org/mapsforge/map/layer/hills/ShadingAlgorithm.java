package org.mapsforge.map.layer.hills;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.GraphicFactory;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by usrusr on 27.01.2017.
 */
public interface ShadingAlgorithm {
    Bitmap convertTile(RawHillTileSource source, GraphicFactory graphicFactory);


    /** abstracts the file handling and access so that ShadingAlgorithm implementations
     * could run on any height model source (e.g. on an android content provider for
     * data sharing between apps) as long as they understand the format of the stream
     */
    interface RawHillTileSource {
        long getSize();
        BufferedInputStream openInputStream() throws FileNotFoundException, IOException;

        /** just in case someone wants to sacrifice speed for fidelty */
        RawHillTileSource getNeighborNorth();
        RawHillTileSource getNeighborSouth();
        RawHillTileSource getNeighborEast();
        RawHillTileSource getNeighborWest();
    }

}
