/*
 * Copyright 2017 usrusr
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
package org.mapsforge.map.layer.hills;

import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.HillshadingBitmap;

import java.io.File;
import java.util.concurrent.ExecutionException;

/**
 * Mutable configuration frontend for an underlying {@link HgtCache} (that will be replaced in one piece when parameters change)
 */
public class MemoryCachingHgtReaderTileSource implements ShadeTileSource {
    private final GraphicFactory graphicsFactory;
    private HgtCache currentCache;
    private int mainCacheSize = 4;
    private int neighborCacheSize = 4;
    private boolean enableInterpolationOverlap = true;
    private File demFolder;
    private ShadingAlgorithm algorithm;
    private boolean configurationChangePending = true;

    public MemoryCachingHgtReaderTileSource(File demFolder, ShadingAlgorithm algorithm, GraphicFactory graphicsFactory) {
        this(graphicsFactory);
        this.demFolder = demFolder;
        this.algorithm = algorithm;
    }

    public MemoryCachingHgtReaderTileSource(GraphicFactory graphicsFactory) {
        this.graphicsFactory = graphicsFactory;
    }

    @Override
    public void applyConfiguration(boolean allowParallel) {
        HgtCache before = currentCache;
        HgtCache latest = latestCache();
        if (allowParallel && latest != null && latest != before) latest.indexOnThread();
    }

    private HgtCache latestCache() {
        HgtCache ret = this.currentCache;
        if (ret != null && !configurationChangePending) return ret;
        if (demFolder == null || algorithm == null) {
            this.currentCache = null;
            return null;
        }
        if (ret == null
                || enableInterpolationOverlap != this.currentCache.interpolatorOverlap
                || mainCacheSize != this.currentCache.mainCacheSize
                || neighborCacheSize != this.currentCache.neighborCacheSize
                || !demFolder.equals(this.currentCache.demFolder)
                || !algorithm.equals(this.currentCache.algorithm)
                ) {
            ret = new HgtCache(demFolder, enableInterpolationOverlap, graphicsFactory, algorithm, mainCacheSize, neighborCacheSize);
            this.currentCache = ret;
        }
        return ret;
    }

    @Override
    public void prepareOnThread() {
        if (currentCache != null) currentCache.indexOnThread();
    }

    @Override
    public HillshadingBitmap getHillshadingBitmap(int latitudeOfSouthWestCorner, int longituedOfSouthWestCorner, double pxPerLat, double pxPerLng) throws ExecutionException, InterruptedException {

        if (latestCache() == null) {

            return null;
        }
        return currentCache.getHillshadingBitmap(latitudeOfSouthWestCorner, longituedOfSouthWestCorner, pxPerLat, pxPerLng);
    }

    @Override
    public void setShadingAlgorithm(ShadingAlgorithm algorithm) {
        this.algorithm = algorithm;
    }

    @Override
    public void setDemFolder(File demFolder) {
        this.demFolder = demFolder;
    }

    /**
     * @param mainCacheSize number of recently used shading tiles (whole numer latitude/longitude grid) that are kept in memory (default: 4)
     */
    public void setMainCacheSize(int mainCacheSize) {
        this.mainCacheSize = mainCacheSize;
    }

    /**
     * @param neighborCacheSize number of additional shading tiles to keep in memory for interpolationOverlap (ignored if enableInterpolationOverlap is false)
     */
    public void setNeighborCacheSize(int neighborCacheSize) {
        this.neighborCacheSize = neighborCacheSize;
    }

    /**
     * @param enableInterpolationOverlap false is faster, but shows minor artifacts along the latitude/longitude
     *                                   (if true, preparing a shading tile for high resolution use requires all 4 neighboring tiles to be loaded if they are not in memory)
     */
    public void setEnableInterpolationOverlap(boolean enableInterpolationOverlap) {
        this.enableInterpolationOverlap = enableInterpolationOverlap;
    }

    public int getMainCacheSize() {
        return mainCacheSize;
    }

    public int getNeighborCacheSize() {
        return neighborCacheSize;
    }

    public boolean isEnableInterpolationOverlap() {
        return enableInterpolationOverlap;
    }

    public File getDemFolder() {
        return demFolder;
    }

    public ShadingAlgorithm getAlgorithm() {
        return algorithm;
    }
}
