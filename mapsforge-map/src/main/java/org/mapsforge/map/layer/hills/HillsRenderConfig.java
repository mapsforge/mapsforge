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
 * Mutable frontend for the hillshading cache/processing in {@link HgtCache}
 * <p>All changes are lazily applied when a tile is requested with {@link #getShadingTile}, which includes a full reindex of the .hgt files.
 * Eager indexing on a dedicated thread can be triggered with {@link #indexOnThread} (e.g. after a configuration change or during setup)</p>
 */
public class HillsRenderConfig {
    private File demFolder;
    private ShadingAlgorithm algorithm;

    private boolean enableInterpolationOverlap;
    private int mainCacheSize = 4;
    private int neighborCacheSize = 4;


    private HgtCache hgtCache;
    final GraphicFactory graphicsFactory;

    public HillsRenderConfig(File demFolder, GraphicFactory graphicsFactory) {
        this(demFolder, graphicsFactory, new SimpleShadingAlgorithm());
    }

    public HillsRenderConfig(File demFolder, GraphicFactory graphicsFactory, ShadingAlgorithm algorithm) {
        this.graphicsFactory = graphicsFactory;
        this.algorithm = algorithm;
        this.demFolder = demFolder;
    }


    /**
     * force a reindex of .hgt files, even if none of the settings have changes
     * <p>consider calling {@link #indexOnThread()} </p>
     */
    public HillsRenderConfig forceReindex() {
        hgtCache = null;
        return this;
    }

    /**
     * call after initialization, after a set of changes to the settable properties or after forceReindex to initiate background indexing
     */
    public HillsRenderConfig indexOnThread() {
        HgtCache cache = currentCache();
        if (cache != null) cache.indexOnThread();
        return this;
    }

    /**
     * @param latitudeOfSouthWestCorner  tile ID latitude (southwest corner, as customary in .hgt)
     * @param longituedOfSouthWestCorner tile ID longitude (southwest corner, as customary in .hgt)
     * @param pxPerLat                   pixels per degree of latitude (to determine padding quality requirements)
     * @param pxPerLng                   pixels per degree of longitude (to determine padding quality requirements)
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public HillshadingBitmap getShadingTile(int latitudeOfSouthWestCorner, int longituedOfSouthWestCorner, double pxPerLat, double pxPerLng) throws ExecutionException, InterruptedException {
        HgtCache cache = currentCache();
        if (cache == null) return null;

        HillshadingBitmap ret = cache.getHillshadingBitmap(latitudeOfSouthWestCorner, longituedOfSouthWestCorner, pxPerLat, pxPerLng);
        if (ret == null && Math.abs(longituedOfSouthWestCorner) > 178) { // don't think too hard about where exactly the border is (not much height data there anyway)
            int eastInt = longituedOfSouthWestCorner > 0 ? longituedOfSouthWestCorner - 180 : longituedOfSouthWestCorner + 180;
            ret = cache.getHillshadingBitmap(latitudeOfSouthWestCorner, eastInt, pxPerLat, pxPerLng);
        }

        return ret;
    }

    private HgtCache currentCache() {
        if (demFolder == null || algorithm == null) {
            hgtCache = null;
        }

        if (hgtCache == null
                || !demFolder.equals(hgtCache.demFolder)
                || !algorithm.equals(hgtCache.algorithm)
                || enableInterpolationOverlap != hgtCache.interpolatorOverlap
                || mainCacheSize != hgtCache.mainCacheSize
                || neighborCacheSize != hgtCache.neighborCacheSize
                ) {
            hgtCache = new HgtCache(demFolder, enableInterpolationOverlap, graphicsFactory, algorithm, mainCacheSize, neighborCacheSize);
        }
        return hgtCache;
    }

    public File getDemFolder() {
        return demFolder;
    }

    public void setDemFolder(final File demFolder) {
        this.demFolder = demFolder;
    }


    public boolean getEnableInterpolationOverlap() {
        return enableInterpolationOverlap;
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


    /**
     * @param mainCacheSize number of recently used shading tiles (whole numer latitude/longitude grid) that are kept in memory (default: 4)
     */
    public void setMainCacheSize(int mainCacheSize) {
        this.mainCacheSize = mainCacheSize;
    }

    public int getNeighborCacheSize() {
        return neighborCacheSize;
    }


    /**
     * @param neighborCacheSize number of additional shading tiles to keep in memory for interpolationOverlap (ignored if enableInterpolationOverlap is false)
     */
    public void setNeighborCacheSize(int neighborCacheSize) {
        this.neighborCacheSize = neighborCacheSize;
    }

    public ShadingAlgorithm getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(ShadingAlgorithm algorithm) {
        this.algorithm = algorithm;
    }
}
