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
 * A {@code FileSystemTileCache} caches tiles in a dedicated path in the file system, specified in the constructor.
 * <p>
 * When used for a {@link org.mapsforge.map.layer.renderer.TileRendererLayer}, persistent caching may result in clipped
 * labels when tiles from different instances are used. To work around this, either display labels in a separate
 * {@link org.mapsforge.map.layer.labels.LabelLayer} (experimental) or disable persistence as described in
 * {@link #FileSystemTileCache(int, File, GraphicFactory, boolean)}.
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
	 * Recursively deletes directory and all files. See
	 * http://stackoverflow.com/questions/3775694/deleting-folder-from-java/3775723#3775723
	 * 
	 * @param dir
	 *            the directory to delete with all its content
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
	private boolean persistent;

	/**
	 * Creates a new FileSystemTileCache.
	 * <p>
	 * Use the {@code persistent} argument to specify whether cache contents should be kept across instances. A
	 * persistent cache will serve any tiles it finds in {@code cacheDirectory}. Calling {@link #destroy()} on a
	 * persistent cache will not delete the cache directory. Conversely, a non-persistent cache will serve only tiles
	 * added to it via the {@link #put(Job, TileBitmap)} method, and calling {@link #destroy()} on a non-persistent
	 * cache will delete {@code cacheDirectory}.
	 * 
	 * @param capacity
	 *            the maximum number of entries in this cache.
	 * @param cacheDirectory
	 *            the directory where cached tiles will be stored.
	 * @throws IllegalArgumentException
	 *             if the capacity is negative.
	 */
	public FileSystemTileCache(int capacity, File cacheDirectory, GraphicFactory graphicFactory, boolean persistent) {
		this.lruCache = new FileWorkingSetCache<>(capacity);
		if (isValidCacheDirectory(cacheDirectory)) {
			this.cacheDirectory = cacheDirectory;
		} else {
			this.cacheDirectory = null;
		}
		this.graphicFactory = graphicFactory;
		this.lock = new ReentrantReadWriteLock();
		this.persistent = persistent;
	}

	/**
	 * Creates a new, non-persistent FileSystemTileCache.
	 * <p>
	 * Calling this constructor is equivalent to calling
	 * {@link #FileSystemTileCache(int, File, GraphicFactory, boolean)} with the last argument set to {@code false}.
	 * 
	 * @param capacity
	 *            the maximum number of entries in this cache.
	 * @param cacheDirectory
	 *            the directory where cached tiles will be stored.
	 * @throws IllegalArgumentException
	 *             if the capacity is negative.
	 */
	public FileSystemTileCache(int capacity, File cacheDirectory, GraphicFactory graphicFactory) {
		this(capacity, cacheDirectory, graphicFactory, false);
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
		return this.persistent && getOutputFile(key).exists();
	}

	/**
	 * Destroys this cache.
	 * <p>
	 * Applications are expected to call this method when they no longer require the cache.
	 * <p>
	 * If the cache is not persistent, calling this method is equivalent to calling {@link #purge()}. If the cache is
	 * persistent, it does nothing.
	 * <p>
	 * In versions prior to 0.5.0, it was common practice to call this method but continue using the cache, in order to
	 * empty it, forcing all tiles to be re-rendered or re-requested from the source. Beginning with 0.5.0,
	 * {@link #purge()} should be used for this purpose. The earlier practice is now discouraged and may lead to
	 * unexpected results when used with features introduced in 0.5.0 or later.
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
			if (this.persistent) {
				file = getOutputFile(key);
				if (!file.exists())
					return null;
			} else
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

	/**
	 * Whether the cache is persistent.
	 */
	public boolean isPersistent() {
		return this.persistent;
	}

	/**
	 * Purges this cache.
	 * <p>
	 * Calls to {@link #get(Job)} issued after purging will not return any tiles added before the purge operation.
	 * Purging will also delete the cache directory on disk, freeing up disk space.
	 * <p>
	 * Applications should purge the tile cache when map model parameters change, such as the render style for locally
	 * rendered tiles, or the source for downloaded tiles. Applications which frequently alternate between a limited
	 * number of map model configurations may want to consider using a different cache for each.
	 * 
	 * @since 0.5.0
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
