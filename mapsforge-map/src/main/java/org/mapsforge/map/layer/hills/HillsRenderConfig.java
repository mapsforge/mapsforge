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
    private ShadeTileSource tileSource;

    private float maginuteScaleFactor = 1f;


    public HillsRenderConfig(ShadeTileSource tileSource) {
        this.tileSource = tileSource;
    }

    public HillsRenderConfig(File demFolder, GraphicFactory graphicsFactory, ShadeTileSource tileSource, ShadingAlgorithm algorithm) {

        this.tileSource = (tileSource == null) ? new MemoryCachingHgtReaderTileSource(demFolder, algorithm, graphicsFactory) : tileSource;
        this.tileSource.setDemFolder(demFolder);
        this.tileSource.setShadingAlgorithm(algorithm);

    }

    /**
     * call after initialization, after a set of changes to the settable properties or after forceReindex to initiate background indexing
     */
    public HillsRenderConfig indexOnThread() {
        ShadeTileSource cache = tileSource;
        if (cache != null) cache.applyConfiguration(true);
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
        ShadeTileSource tileSource = this.tileSource;
        if (tileSource == null) return null;

        HillshadingBitmap ret = tileSource.getHillshadingBitmap(latitudeOfSouthWestCorner, longituedOfSouthWestCorner, pxPerLat, pxPerLng);
        if (ret == null && Math.abs(longituedOfSouthWestCorner) > 178) { // don't think too hard about where exactly the border is (not much height data there anyway)
            int eastInt = longituedOfSouthWestCorner > 0 ? longituedOfSouthWestCorner - 180 : longituedOfSouthWestCorner + 180;
            ret = tileSource.getHillshadingBitmap(latitudeOfSouthWestCorner, eastInt, pxPerLat, pxPerLng);
        }

        return ret;
    }

    public float getMaginuteScaleFactor() {
        return maginuteScaleFactor;
    }

    /**
     * Increase (&gt;1) or decrease (&lt;1) the hillshading magnitude relative to the value set in themes
     * <p>When designing a theme, this should be one</p>
     */
    public void setMaginuteScaleFactor(float maginuteScaleFactor) {
        this.maginuteScaleFactor = maginuteScaleFactor;
    }

    public ShadeTileSource getTileSource() {
        return tileSource;
    }

    public void setTileSource(ShadeTileSource tileSource) {
        this.tileSource = tileSource;
    }
}
