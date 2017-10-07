package org.mapsforge.map.layer.hills;

import org.mapsforge.core.graphics.HillshadingBitmap;

import java.io.File;
import java.util.concurrent.ExecutionException;


/**
 * {@link MemoryCachingHgtReaderTileSource} or a wrapper thereof
 */
public interface ShadeTileSource {

    /** prepare anything lazily derived from configuration off this thread */
    void prepareOnThread();

    /** main work method */
    HillshadingBitmap getHillshadingBitmap(int latitudeOfSouthWestCorner, int longituedOfSouthWestCorner, double pxPerLat, double pxPerLng) throws ExecutionException, InterruptedException;

    void applyConfiguration(boolean allowParallel);

    void setShadingAlgorithm(ShadingAlgorithm algorithm);
    void setDemFolder(File demFolder);
}
