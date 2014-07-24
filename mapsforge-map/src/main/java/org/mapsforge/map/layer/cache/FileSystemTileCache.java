/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2014 mvglasow <michael -at- vonglasow.com>
 * Copyright 2014 devemux86
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

import org.mapsforge.core.graphics.CorruptedInputStreamException;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.core.util.IOUtils;
import org.mapsforge.map.layer.queue.Job;

/**
 * A thread-safe cache for image files with a fixed size and LRU policy.
 * <p>
 * A {@code FileSystemTileCache} caches tiles in a dedicated path in the file system, specified in the constructor. The
 * cache is persistent across instances, i.e. a new {@code FileSystemTileCache} instance will serve tiles cached by a
 * previous instance using the same cache directory. If persistence is not desired, call {@link #destroy()} immediately
 * after instantiating a new {@code FileSystemTileCache}, or when the current instance is no longer needed.
 * <p>
 * When used for a {@link org.mapsforge.map.layer.renderer.TileRendererLayer}, persistent caching may result in clipped
 * labels when tiles from different instances are used. To work around this, either display labels in a separate
 * {@link org.mapsforge.map.layer.labels.LabelLayer} (experimental) or disable persistence as described above.
 * <p>
 * When releasing a new version of an application using a persistent cache, consider whether the new version serving
 * tiles cached by an older version is expected to have undesirable side effects. This may be the case if the new
 * version changes the render style for locally rendered tiles, or the source for downloaded tiles. In such cases, you
 * should either {@link #destroy()} the cache when the new version starts up for the first time, or use different cache
 * directories in the new version (and remove leftover cache data from the previous version).
 */
public class FileSystemTileCache implements TileCache {
	static final String FILE_EXTENSION = ".tile";
	private static final Logger LOGGER = Logger.getLogger(FileSystemTileCache.class.getName());

	private static boolean isValidCacheDirectory(File file) {
		if ((!file.exists() && !file.mkdirs()) || !file.isDirectory() || !file.canRead() || !file.canWrite()) {
			return false;
		}
		return true;
	}

    /**
     * Recursively deletes directory and all files.
     * See http://stackoverflow.com/questions/3775694/deleting-folder-from-java/3775723#3775723
     *
     * @param dir the directory to delete with all its content
     * @return true if directory and all content has been deleted, false if not
     */

    private static boolean deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDirectory(new File(dir, children[i]));
                if (!success) {
                    return false;
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

	/**
	 * @param capacity
	 *            the maximum number of entries in this cache.
	 * @param cacheDirectory
	 *            the directory where cached tiles will be stored.
	 * @throws IllegalArgumentException
	 *             if the capacity is negative.
	 */
	public FileSystemTileCache(int capacity, File cacheDirectory, GraphicFactory graphicFactory) {
		this.lruCache = new FileWorkingSetCache<>(capacity);
		if (isValidCacheDirectory(cacheDirectory)) {
			this.cacheDirectory = cacheDirectory;
		} else {
			this.cacheDirectory = null;
		}
		this.graphicFactory = graphicFactory;
		this.lock = new ReentrantReadWriteLock();
	}

	@Override
	public boolean containsKey(Job key) {
		try {
			lock.readLock().lock();
			if (this.lruCache.containsKey(key.getKey()))
				return true;
		} finally {
			lock.readLock().unlock();
		}
		return getOutputFile(key).exists();
	}

	/**
	 * Destroys this cache.
	 * <p>
	 * Destroying the cache will delete any tiles stored on disk, freeing up disk space and causing subsequent calls to
	 * {@link #get(Job)} to return {@code null}, requiring the caller to obtain a fresh copy of the tile.
	 */
	@Override
	public void destroy() {
		try {
			lock.writeLock().lock();
			this.lruCache.clear();
		} finally {
			lock.writeLock().unlock();
		}

		deleteDirectory(this.cacheDirectory);
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
			// check if there is a cached tile from an earlier instance
			file = getOutputFile(key);
			if (!file.exists())
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
			LOGGER.log(Level.WARNING, "input stream from file system cache invalid", e);
			return null;
		} catch (IOException e) {
			remove(key);
			LOGGER.log(Level.SEVERE, null, e);
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
		} catch (IOException e) {
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

	public void setWorkingSet(Set<Job> workingSet) {
		Set<String> workingSetInteger = new HashSet<String>();
		for (Job job : workingSet) {
			workingSetInteger.add(job.getKey());
		}
		this.lruCache.setWorkingSet(workingSetInteger);
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
}
