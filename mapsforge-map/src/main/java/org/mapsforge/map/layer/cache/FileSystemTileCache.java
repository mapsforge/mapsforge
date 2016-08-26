/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014-2015 Ludwig M Brinckmann
 * Copyright 2014 mvglasow <michael -at- vonglasow.com>
 * Copyright 2014, 2015 devemux86
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
import org.mapsforge.map.model.common.Observable;
import org.mapsforge.map.model.common.Observer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A thread-safe cache for image files with a fixed size and LRU policy.
 * <p/>
 * A {@code FileSystemTileCache} caches tiles in a dedicated path in the file system, specified in the constructor.
 * <p/>
 * When used for a {@link org.mapsforge.map.layer.renderer.TileRendererLayer}, persistent caching may result in clipped
 * labels when tiles from different instances are used. To work around this, either display labels in a separate
 * {@link org.mapsforge.map.layer.labels.LabelLayer} (experimental) or disable persistence as described in
 * {@link #FileSystemTileCache(int, File, GraphicFactory, boolean)}.
 * <p/>
 * Note: previously the FileSystemTileCache utilized threading to speed up response times. This is not the
 * case anymore and the constructors have been removed.
 */
public class FileSystemTileCache implements TileCache {
    static final String FILE_EXTENSION = ".tile";
    private static final Logger LOGGER = Logger.getLogger(FileSystemTileCache.class.getName());

    /**
     * Runnable that reads the cache directory and re-populates the cache with data saved by previous instances.
     * <p/>
     * This method assumes tile files to have a file extension of {@link #FILE_EXTENSION} and reside in a second-level
     * of subdir of the cache dir (as in the standard TMS directory layout of zoomlevel/x/y). The relative path to the
     * cached tile, after stripping the extension, is used as the lookup key.
     */
    private class CacheDirectoryReader implements Runnable {
        @Override
        public void run() {
            File[] zFiles = FileSystemTileCache.this.cacheDirectory.listFiles();
            if (zFiles != null) {
                for (File z : zFiles) {
                    File[] xFiles = z.listFiles();
                    if (xFiles != null) {
                        for (File x : xFiles) {
                            File[] yFiles = x.listFiles();
                            if (yFiles != null) {
                                for (File y : yFiles) {
                                    if (isValidFile(y) && y.getName().endsWith(FILE_EXTENSION)) {
                                        int index = y.getName().lastIndexOf(FILE_EXTENSION);
                                        String key = Job.composeKey(z.getName(), x.getName(), y.getName().substring(0, index));
                                        try {
                                            FileSystemTileCache.this.lock.writeLock().lock();
                                            if (FileSystemTileCache.this.lruCache.put(key, y) != null) {
                                                LOGGER.warning("overwriting cached entry: " + key);
                                            }
                                        } finally {
                                            FileSystemTileCache.this.lock.writeLock().unlock();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Determines whether a File instance refers to a valid cache directory.
     * <p/>
     * This method checks that {@code file} refers to a directory to which the current process has read and write
     * access. If the directory does not exist, it will be created.
     *
     * @param file The File instance to examine. This can be null, which will cause the method to return {@code false}.
     */
    private static boolean isValidCacheDirectory(File file) {
        return !((file == null) || (!file.exists() && !file.mkdirs()) || !file.isDirectory() || !file.canRead()
                || !file.canWrite());
    }

    /**
     * Determines whether a File instance refers to a valid file which can be read.
     * <p/>
     * This method checks that {@code file} refers to an existing file to which the current process has read access. It
     * does not create directories and not verify that the directory is writable. If you need this behavior, use
     * {@link #isValidCacheDirectory(File)} instead.
     *
     * @param file The File instance to examine. This can be null, which will cause the method to return {@code false}.
     */
    private static boolean isValidFile(File file) {
        return file != null && file.isFile() && file.canRead();
    }

    /**
     * Recursively deletes directory and all files. See
     * http://stackoverflow.com/questions/3775694/deleting-folder-from-java/3775723#3775723
     *
     * @param dir the directory to delete with all its content
     * @return true if directory and all content has been deleted, false if not
     */

    private static boolean deleteDirectory(File dir) {
        if (dir == null) {
            return false;
        }

        if (dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (int i = 0; i < children.length; i++) {
                    boolean success = deleteDirectory(new File(dir, children[i]));
                    if (!success) {
                        return false;
                    }
                }
            }
        }
        // The directory is now empty so delete it
        return dir.delete();
    }

    private final File cacheDirectory;
    private final GraphicFactory graphicFactory;
    private FileWorkingSetCache<String> lruCache;
    private final ReentrantReadWriteLock lock;
    private final Observable observable;
    private final boolean persistent;

    /**
     * Compatibility constructor that creates a non-threaded, non-persistent FSTC.
     *
     * @param capacity       the maximum number of entries in this cache.
     * @param cacheDirectory the directory where cached tiles will be stored.
     * @param graphicFactory the graphicFactory implementation to use.
     * @throws IllegalArgumentException if the capacity is negative.
     */
    public FileSystemTileCache(int capacity, File cacheDirectory, GraphicFactory graphicFactory) {
        this(capacity, cacheDirectory, graphicFactory, false);
    }

    /**
     * Creates a new FileSystemTileCache.
     * <p/>
     * Use the {@code persistent} argument to specify whether cache contents should be kept across instances. A
     * persistent cache will serve any tiles it finds in {@code cacheDirectory}. Calling {@link #destroy()} on a
     * persistent cache will not delete the cache directory. Conversely, a non-persistent cache will serve only tiles
     * added to it via the {@link #put(Job, TileBitmap)} method, and calling {@link #destroy()} on a non-persistent
     * cache will delete {@code cacheDirectory}.
     *
     * @param capacity       the maximum number of entries in this cache.
     * @param cacheDirectory the directory where cached tiles will be stored.
     * @param graphicFactory the graphicFactory implementation to use.
     * @param persistent     if cache data will be kept between instances
     * @throws IllegalArgumentException if the capacity is negative.
     * @throws IllegalArgumentException if the capacity is negative.
     */
    public FileSystemTileCache(int capacity, File cacheDirectory, GraphicFactory graphicFactory, boolean persistent) {
        this.observable = new Observable();
        this.persistent = persistent;
        this.lruCache = new FileWorkingSetCache<>(capacity);
        this.lock = new ReentrantReadWriteLock();
        if (isValidCacheDirectory(cacheDirectory)) {
            this.cacheDirectory = cacheDirectory;
            if (this.persistent) {
                // this will start a new thread to read in the cache directory.
                // there is the potential that files will be recreated because they
                // are not yet in the cache, but this will not cause any corruption.
                new Thread(new CacheDirectoryReader()).start();
            }

        } else {
            this.cacheDirectory = null;
        }
        this.graphicFactory = graphicFactory;
    }

    @Override
    public boolean containsKey(Job key) {
        try {
            lock.readLock().lock();
            // if we are using a threaded cache we return true if the tile is still in the
            // queue to reduce double rendering
            return this.lruCache.containsKey(key.getKey());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Destroys this cache.
     * <p/>
     * Applications are expected to call this method when they no longer require the cache.
     * <p/>
     * If the cache is not persistent, calling this method is equivalent to calling {@link #purge()}. If the cache is
     * persistent, it does nothing.
     * <p/>
     * Beginning with 0.5.1, accessing the cache after calling {@code destroy()} is discouraged. In order to empty the
     * cache and force all tiles to be re-rendered or re-requested from the source, use {@link #purge()} instead.
     * Earlier versions lacked the {@link #purge()} method and used {@code destroy()} instead, but this practice is now
     * discouraged and may lead to unexpected results when used with features introduced in 0.5.1 or later.
     */
    @Override
    public void destroy() {
        if (!this.persistent)
            purge();
    }

    @Override
    public TileBitmap get(Job key) {

        File file;
        try {
            lock.readLock().lock();
            file = this.lruCache.get(key.getKey());
        } finally {
            lock.readLock().unlock();
        }
        if (file == null) {
            return null;
        }

        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            TileBitmap result = this.graphicFactory.createTileBitmap(inputStream, key.tile.tileSize, key.hasAlpha);
            result.setTimestamp(file.lastModified());
            return result;
        } catch (CorruptedInputStreamException e) {
            // this can happen, at least on Android, when the input stream
            // is somehow corrupted, returning null ensures it will be loaded
            // from another source
            remove(key);
            LOGGER.log(Level.WARNING, "input stream from file system cache invalid " + key.getKey() + " " + file.length(), e);
            return null;
        } catch (IOException e) {
            remove(key);
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            return null;
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    @Override
    public int getCapacity() {
        try {
            lock.readLock().lock();
            return this.lruCache.capacity;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public int getCapacityFirstLevel() {
        return getCapacity();
    }

    @Override
    public TileBitmap getImmediately(Job key) {
        return get(key);
    }

    /**
     * Whether the cache is persistent.
     */
    public boolean isPersistent() {
        return this.persistent;
    }

    /**
     * Purges this cache.
     * <p/>
     * Calls to {@link #get(Job)} issued after purging will not return any tiles added before the purge operation.
     * Purging will also delete the cache directory on disk, freeing up disk space.
     * <p/>
     * Applications should purge the tile cache when map model parameters change, such as the render style for locally
     * rendered tiles, or the source for downloaded tiles. Applications which frequently alternate between a limited
     * number of map model configurations may want to consider using a different cache for each.
     *
     * @since 0.5.1
     */
    @Override
    public void purge() {
        try {
            this.lock.writeLock().lock();
            this.lruCache.clear();
        } finally {
            this.lock.writeLock().unlock();
        }

        deleteDirectory(this.cacheDirectory);
    }

    @Override
    public void put(Job key, TileBitmap bitmap) {
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        } else if (bitmap == null) {
            throw new IllegalArgumentException("bitmap must not be null");
        }

        if (getCapacity() == 0) {
            return;
        }

        storeData(key, bitmap);
        this.observable.notifyObservers();
    }

    @Override
    public void setWorkingSet(Set<Job> workingSet) {
        Set<String> workingSetInteger = new HashSet<String>();
        synchronized(workingSet) {
            for (Job job : workingSet) {
                workingSetInteger.add(job.getKey());
            }
        }
        this.lruCache.setWorkingSet(workingSetInteger);
    }

    @Override
    public void addObserver(final Observer observer) {
        this.observable.addObserver(observer);
    }

    @Override
    public void removeObserver(final Observer observer) {
        this.observable.removeObserver(observer);
    }


    private File getOutputFile(Job job) {
        String file = this.cacheDirectory + File.separator + job.getKey();
        String dir = file.substring(0, file.lastIndexOf(File.separatorChar));
        if (isValidCacheDirectory(new File(dir))) {
            return new File(file + FILE_EXTENSION);
        }
        return null;
    }

    private void remove(Job key) {
        try {
            lock.writeLock().lock();
            this.lruCache.remove(key.getKey());
        } finally {
            lock.writeLock().unlock();
        }

    }

    /**
     * stores the bitmap data on disk with filename key
     *
     * @param key    filename
     * @param bitmap tile image
     */
    private void storeData(Job key, TileBitmap bitmap) {
        OutputStream outputStream = null;
        try {
            File file = getOutputFile(key);
            if (file == null) {
                // if the file cannot be written, silently return
                return;
            }
            outputStream = new FileOutputStream(file);
            bitmap.compress(outputStream);
            try {
                lock.writeLock().lock();
                if (this.lruCache.put(key.getKey(), file) != null) {
                    LOGGER.warning("overwriting cached entry: " + key.getKey());
                }
            } finally {
                lock.writeLock().unlock();
            }
        } catch (Exception e) {
            // we are catching now any exception and then disable the file cache
            // this should ensure that no exception in the storage thread will
            // ever crash the main app. If there is a runtime exception, the thread
            // will exit (via destroy).
            LOGGER.log(Level.SEVERE, "Disabling filesystem cache", e);
            // most likely cause is that the disk is full, just disable the
            // cache otherwise
            // more and more exceptions will be thrown.
            this.destroy();
            try {
                lock.writeLock().lock();
                this.lruCache = new FileWorkingSetCache<String>(0);
            } finally {
                lock.writeLock().unlock();
            }
        } finally {
            IOUtils.closeQuietly(outputStream);
        }

    }

}
