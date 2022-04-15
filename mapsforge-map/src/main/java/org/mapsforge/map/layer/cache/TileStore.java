/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2019 mg4gh
 * Copyright 2022 devemux86
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
package org.mapsforge.map.layer.cache;

import org.mapsforge.core.graphics.CorruptedInputStreamException;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.core.util.IOUtils;
import org.mapsforge.map.layer.queue.Job;
import org.mapsforge.map.model.common.Observer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.logging.Logger;

/**
 * A "tilecache" storing map tiles that is prepopulated and never removes any files.
 * This tile store uses the standard TMS directory layout of zoomlevel/y/x . To support
 * a different directory structure override the findFile method.
 */
public class TileStore implements TileCache {

    private final File rootDirectory;
    private final GraphicFactory graphicFactory;
    private final String suffix;

    private static final Logger LOGGER = Logger.getLogger(TileStore.class.getName());

    /**
     * @param rootDirectory  the directory where cached tiles will be stored.
     * @param suffix         the suffix for stored tiles.
     * @param graphicFactory the mapsforge graphic factory to create tile data instances.
     * @throws IllegalArgumentException if the root directory cannot be a tile store
     */
    public TileStore(File rootDirectory, String suffix, GraphicFactory graphicFactory) {
        this.rootDirectory = rootDirectory;
        this.graphicFactory = graphicFactory;
        this.suffix = suffix;
        if (this.rootDirectory == null || !this.rootDirectory.isDirectory() || !this.rootDirectory.canRead()) {
            throw new IllegalArgumentException("Root directory must be readable");
        }
    }

    @Override
    public synchronized boolean containsKey(Job key) {
        return this.findFile(key) != null;
    }

    @Override
    public synchronized void destroy() {
        // no-op
    }

    @Override
    public synchronized TileBitmap get(Job key) {
        File file = this.findFile(key);

        if (file == null) {
            return null;
        }

        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            TileBitmap bitmap = this.graphicFactory.createTileBitmap(inputStream, key.tile.tileSize, key.hasAlpha);
            if (bitmap.getWidth() != key.tile.tileSize || bitmap.getHeight() != key.tile.tileSize) {
                bitmap.scaleTo(key.tile.tileSize, key.tile.tileSize);
            }
            return bitmap;
        } catch (CorruptedInputStreamException e) {
            // this can happen, at least on Android, when the input stream
            // is somehow corrupted, returning null ensures it will be loaded
            // from another source
            return null;
        } catch (IOException e) {
            return null;
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    @Override
    public synchronized int getCapacity() {
        return Integer.MAX_VALUE;
    }

    @Override
    public synchronized int getCapacityFirstLevel() {
        return getCapacity();
    }

    @Override
    public TileBitmap getImmediately(Job key) {
        return get(key);
    }

    @Override
    public synchronized void purge() {
        // no-op
    }

    @Override
    public synchronized void put(Job key, TileBitmap bitmap) {
        // no-op
    }

    protected File findFile(Job key) {
        // slow descent at the moment, better for debugging.
        File l1 = new File(this.rootDirectory, Byte.toString(key.tile.zoomLevel));
        if (!l1.isDirectory() || !l1.canRead()) {
            LOGGER.warning("Failed to find directory " + l1.getAbsolutePath());
            return null;
        }
        File l2 = new File(l1, Long.toString(key.tile.tileX));
        if (!l2.isDirectory() || !l2.canRead()) {
            LOGGER.warning("Failed to find directory " + l2.getAbsolutePath());
            return null;
        }
        File l3 = new File(l2, Long.toString(key.tile.tileY) + this.suffix);
        if (!l3.isFile() || !l3.canRead()) {
            LOGGER.warning("Failed to find file " + l3.getAbsolutePath());
            return null;
        }
        //LOGGER.info("Found file " + l3.getAbsolutePath());
        return l3;
    }

    @Override
    public void setWorkingSet(Set<Job> key) {
        // all tiles are always in the cache
    }

    @Override
    public void addObserver(final Observer observer) {
    }

    @Override
    public void removeObserver(final Observer observer) {
    }
}
