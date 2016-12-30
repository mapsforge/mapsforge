/*
 * Copyright 2016 devemux86
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

import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;

public final class AwtUtil {

    /**
     * Utility function to create a two-level tile cache with the right size, using the size of the screen.
     * <p>
     * Combine with <code>FrameBufferController.setUseSquareFrameBuffer(false);</code>
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
     */
    public static int getMinimumCacheSize(int tileSize, double overdrawFactor) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        return (int) Math.max(4, Math.round((2 + screenSize.getWidth() * overdrawFactor / tileSize)
                * (2 + screenSize.getHeight() * overdrawFactor / tileSize)));
    }

    private AwtUtil() {
        // no-op, for privacy
    }
}
