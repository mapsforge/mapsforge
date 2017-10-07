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

import org.mapsforge.core.graphics.HillshadingBitmap;

import java.io.File;
import java.util.concurrent.ExecutionException;

/**
 * {@link MemoryCachingHgtReaderTileSource} or a wrapper thereof
 */
public interface ShadeTileSource {

    /**
     * prepare anything lazily derived from configuration off this thread
     */
    void prepareOnThread();

    /**
     * main work method
     */
    HillshadingBitmap getHillshadingBitmap(int latitudeOfSouthWestCorner, int longituedOfSouthWestCorner, double pxPerLat, double pxPerLng) throws ExecutionException, InterruptedException;

    void applyConfiguration(boolean allowParallel);

    void setShadingAlgorithm(ShadingAlgorithm algorithm);

    void setDemFolder(File demFolder);
}
