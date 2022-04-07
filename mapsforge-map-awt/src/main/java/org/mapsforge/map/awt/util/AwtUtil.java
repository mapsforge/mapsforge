/*
 * Copyright 2016-2022 devemux86
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
package org.mapsforge.map.awt.util;

import org.mapsforge.map.awt.graphics.AwtGraphicFactory;
import org.mapsforge.map.layer.cache.FileSystemTileCache;
import org.mapsforge.map.layer.cache.InMemoryTileCache;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.cache.TwoLevelTileCache;

import java.awt.*;
import java.io.File;

public final class AwtUtil {

    /**
     * Utility function to create a two-level tile cache with the right size, using the size of the screen.
     * <p>
     * Combine with <code>FrameBufferController.setUseSquareFrameBuffer(false);</code>
     *
     * @param tileSize       the tile size
     * @param overdrawFactor the overdraw factor applied to the map view
     * @param capacity       the maximum number of entries in file cache
     * @param cacheDirectory the directory where cached tiles will be stored
     * @return a new cache created on the file system
     */
    public static TileCache createTileCache(int tileSize, double overdrawFactor, int capacity, File cacheDirectory) {
        int cacheSize = getMinimumCacheSize(tileSize, overdrawFactor);
        TileCache firstLevelTileCache = new InMemoryTileCache(cacheSize);
        TileCache secondLevelTileCache = new FileSystemTileCache(capacity, cacheDirectory, AwtGraphicFactory.INSTANCE);
        return new TwoLevelTileCache(firstLevelTileCache, secondLevelTileCache);
    }

    /**
     * Compute the minimum cache size for a view, using the size of the screen.
     * <p>
     * Combine with <code>FrameBufferController.setUseSquareFrameBuffer(false);</code>
     *
     * @param tileSize       the tile size
     * @param overdrawFactor the overdraw factor applied to the map view
     * @return the minimum cache size for the view
     */
    public static int getMinimumCacheSize(int tileSize, double overdrawFactor) {
        GraphicsDevice[] screenDevices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
        int size = 4;
        for (GraphicsDevice screenDevice : screenDevices) {
            DisplayMode displayMode = screenDevice.getDisplayMode();
            size = Math.max(size, (int) Math.round((2 + displayMode.getWidth() * overdrawFactor / tileSize)
                    * (2 + displayMode.getHeight() * overdrawFactor / tileSize)));
        }
        return size;
    }

    private AwtUtil() {
        // no-op, for privacy
    }
}
