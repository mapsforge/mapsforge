/*
 * Copyright 2017-2022 usrusr
 * Copyright 2024 Sublimis
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

import java.util.concurrent.ExecutionException;

/**
 * Mutable configuration frontend for an underlying {@link HgtCache} (that will be replaced in one piece when parameters change)
 */
public class MemoryCachingHgtReaderTileSource implements ShadeTileSource {
    private final GraphicFactory graphicsFactory;
    private HgtCache currentCache;
    private int mainCacheSize = 12;
    private DemFolder demFolder;
    private ShadingAlgorithm algorithm;

    /**
     * 2024-10: This no longer affects performance, so it simply needs to be set to {@code true}.
     * Performance is not affected because no excess shading tiles are loaded beyond the required tiles used for display.
     */
    private final boolean enableInterpolationOverlap = true;

    public MemoryCachingHgtReaderTileSource(DemFolder demFolder, ShadingAlgorithm algorithm, GraphicFactory graphicsFactory) {
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

    protected HgtCache latestCache() {
        if (demFolder == null || algorithm == null) {
            this.currentCache = null;
            return null;
        }

        if (isNewCacheNeeded()) {
            synchronized (graphicsFactory) {
                if (isNewCacheNeeded()) {
                    this.currentCache = new HgtCache(demFolder, enableInterpolationOverlap, graphicsFactory, algorithm, mainCacheSize);
                }
            }
        }

        return this.currentCache;
    }

    protected boolean isNewCacheNeeded() {
        return (this.currentCache == null
                || enableInterpolationOverlap != this.currentCache.interpolatorOverlap
                || mainCacheSize != this.currentCache.mainCacheSize
                || !demFolder.equals(this.currentCache.demFolder)
                || !algorithm.equals(this.currentCache.algorithm));
    }

    @Override
    public void prepareOnThread() {
        if (currentCache != null) currentCache.indexOnThread();
    }

    @Override
    public HillshadingBitmap getHillshadingBitmap(int latitudeOfSouthWestCorner, int longitudeOfSouthWestCorner, double pxPerLat, double pxPerLng) throws ExecutionException, InterruptedException {

        if (latestCache() == null) {

            return null;
        }
        return currentCache.getHillshadingBitmap(latitudeOfSouthWestCorner, longitudeOfSouthWestCorner, pxPerLat, pxPerLng);
    }

    @Override
    public void setShadingAlgorithm(ShadingAlgorithm algorithm) {
        this.algorithm = algorithm;
    }

    public void setDemFolder(DemFolder demFolder) {
        this.demFolder = demFolder;
    }

    /**
     * @param mainCacheSize number of recently used shading tiles (whole number latitude/longitude grid) that are kept in memory (default: 4)
     */
    public void setMainCacheSize(int mainCacheSize) {
        this.mainCacheSize = mainCacheSize;
    }

    /**
     * 2024-10: No longer used; does nothing. The flag is always {@code true}.
     */
    public void setEnableInterpolationOverlap(boolean enableInterpolationOverlap) {
    }

    public int getMainCacheSize() {
        return mainCacheSize;
    }

    /**
     * 2024-10: This no longer affects performance, so it simply needs to return {@code true}.
     *
     * @return Always {@code true}.
     */
    public boolean isEnableInterpolationOverlap() {
        return enableInterpolationOverlap;
    }


    public ShadingAlgorithm getAlgorithm() {
        return algorithm;
    }
}
