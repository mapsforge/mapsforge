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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mapsforge.core.graphics.CorruptedInputStreamException;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.core.util.IOUtils;
import org.mapsforge.map.layer.queue.Job;
import org.mapsforge.map.util.PausableThread;

/**
 * Container class to tie a key/bitmap together.
 */
class StorageJob {
	Job key;
	TileBitmap bitmap;

	StorageJob(Job key, TileBitmap bitmap) {
		this.key = key;
		this.bitmap = bitmap;
	}

	/**
	 * Equality is just defined over the key, not over the bitmap content. This allows
	 * finding a StorageJob in the queue without knowing the data.
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (!(obj instanceof StorageJob)) {
			return false;
		}
		StorageJob other = (StorageJob) obj;
		return key.equals(other.key);
	}

	@Override
	public int hashCode() {
		return this.key.hashCode();
	}
}

/**
 * A thread-safe cache for image files with a fixed size and LRU policy.
 * <p>
 * A {@code FileSystemTileCache} caches tiles in a dedicated path in the file system, specified in the constructor. The
 * cache writes the data on a separate thread, i.e. when the call to put a job/tile into the cache returns the data is
 * not actually written to disk.
 * <p>
 * When used for a {@link org.mapsforge.map.layer.renderer.TileRendererLayer}, persistent caching may result in clipped
 * labels when tiles from different instances are used. To work around this, either display labels in a separate
 * {@link org.mapsforge.map.layer.labels.LabelLayer} (experimental) or disable persistence as described in
 * {@link #FileSystemTileCache(int, File, GraphicFactory, boolean, int, boolean)}.
 */
public class FileSystemTileCache extends PausableThread implements TileCache {
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
	private final AtomicInteger jobs;
	private FileWorkingSetCache<String> lruCache;
	private final ReentrantReadWriteLock lock;
	private boolean persistent;

	// if threaded is true, the bitmap writing is executed on a separate thread,
	// and jobs are stored in the jobStack. The false option remains for testing.
	private final boolean threaded;
	private final LinkedBlockingQueue<StorageJob> storageJobs;

	/**
	 * Compatibility constructor that creates a non-threaded, non-persistent FSTC.
	 * 
	 * @param capacity
	 *            the maximum number of entries in this cache.
	 * @param cacheDirectory
	 *            the directory where cached tiles will be stored.
	 * @param graphicFactory
	 *            the graphicFactory implementation to use.
	 * @throws IllegalArgumentException
	 *             if the capacity is negative.
	 */
	public FileSystemTileCache(int capacity, File cacheDirectory, GraphicFactory graphicFactory) {
		this(capacity, cacheDirectory, graphicFactory, false, 0, false);
	}

	/**
	 * Compatibility constructor that creates a non-persistent FSTC.
	 * 
	 * @param capacity
	 *            the maximum number of entries in this cache.
	 * @param cacheDirectory
	 *            the directory where cached tiles will be stored.
	 * @param graphicFactory
	 *            the graphicFactory implementation to use.
	 * @param threaded
	 *            if cache will use background thread to store data (more responsive).
	 * @param queueSize
	 *            maximum length of queue before the put operation blocks
	 * @throws IllegalArgumentException
	 *             if the capacity is negative.
	 */
	public FileSystemTileCache(int capacity, File cacheDirectory, GraphicFactory graphicFactory, boolean threaded,
			int queueSize) {
		this(capacity, cacheDirectory, graphicFactory, threaded, queueSize, false);
	}

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
	 * @param graphicFactory
	 *            the graphicFactory implementation to use.
	 * @param threaded
	 *            if cache will use background thread to store data (more responsive).
	 * @param queueSize
	 *            maximum length of queue before the put operation blocks
	 * @param persistent
	 *            if cache data will be kept between instances
	 * @throws IllegalArgumentException
	 *             if the capacity is negative.
	 * @throws IllegalArgumentException
	 *             if the capacity is negative.
	 */
	public FileSystemTileCache(int capacity, File cacheDirectory, GraphicFactory graphicFactory, boolean threaded,
			int queueSize, boolean persistent) {
		this.jobs = new AtomicInteger(0);
		this.persistent = persistent;
		this.threaded = threaded;
		if (threaded) {
			this.storageJobs = new LinkedBlockingQueue<>(queueSize);
		} else {
			this.storageJobs = null;
		}
		this.lruCache = new FileWorkingSetCache<>(capacity);
		this.lock = new ReentrantReadWriteLock();
		if (isValidCacheDirectory(cacheDirectory)) {
			this.cacheDirectory = cacheDirectory;
			if (this.persistent)
				readCacheDirectory();
		} else {
			this.cacheDirectory = null;
		}
		this.graphicFactory = graphicFactory;
		if (this.threaded) {
			this.start();
		}
	}

	@Override
	public boolean containsKey(Job key) {
		try {
			lock.readLock().lock();
			// if we are using a threaded cache we return true if the tile is still in the
			// queue to reduce double rendering
			return (this.lruCache.containsKey(key.getKey()) || (threaded && storageJobs.contains(key)));
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * Destroys this cache.
	 * <p>
	 * Applications are expected to call this method when they no longer require the cache.
	 * <p>
	 * If the cache is not persistent, calling this method is equivalent to calling {@link #purge()}. If the cache is
	 * persistent, it does nothing.
	 * <p>
	 * Beginning with 0.6.0, accessing the cache after calling {@code destroy()} is discouraged. In order to empty the
	 * cache and force all tiles to be re-rendered or re-requested from the source, use {@link #purge()} instead.
	 * Earlier versions lacked the {@link #purge()} method and used {@code destroy()} instead, but this practice is now
	 * discouraged and may lead to unexpected results when used with features introduced in 0.6.0 or later.
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
			LOGGER.fine("No cache entry for tile " + key.getKey());
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
	 * @since 0.6.0
	 */
	@Override
	public void purge() {
		try {
			this.lock.writeLock().lock();
			this.lruCache.clear();
			if (this.threaded) {
				this.interrupt();
			}
		} finally {
			this.lock.writeLock().unlock();
		}

		deleteDirectory(this.cacheDirectory);
	}

	/**
	 * Gets the number of remaining tiles still in the queue to be written to disk.
	 * 
	 * @return number of jobs in queue, 0 if not threaded.
	 */
	public int getQueueLength() {
		return jobs.get();
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

		jobs.incrementAndGet();
		if (this.threaded) {
			bitmap.incrementRefCount();
			storageJobs.offer(new StorageJob(key, bitmap));
		} else {
			storeData(key, bitmap);
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

	/**
	 * Reads the cache directory and re-populates the cache with data saved by previous instances.
	 * <p>
	 * This method assumes the standard TMS directory layout of zoomlevel/y/x and a file extension of
	 * {@link #FILE_EXTENSION}.
	 */
	private void readCacheDirectory() {
		String[] l1Dirs = this.cacheDirectory.list();
		if (l1Dirs != null)
			for (int i = 0; i < l1Dirs.length; i++) {
				File l1 = new File(this.cacheDirectory, l1Dirs[i]);
				if (l1 == null || !l1.isDirectory() || !l1.canRead()) {
					LOGGER.info("Not a valid directory: " + l1.getAbsolutePath());
				} else {
					String[] l2Dirs = l1.list();
					if (l2Dirs != null)
						for (int j = 0; j < l2Dirs.length; j++) {
							File l2 = new File(l1, l2Dirs[j]);
							if (l2 == null || !l2.isDirectory() || !l2.canRead()) {
								LOGGER.info("Not a valid directory: " + l2.getAbsolutePath());
							} else {
								String[] l3Files = l2.list();
								if (l3Files != null)
									for (int k = 0; k < l3Files.length; k++) {
										File l3 = new File(l2, l3Files[k]);
										int index = l3Files[k].indexOf(FILE_EXTENSION.charAt(0));
										if (l3 == null || !l3.isFile() || !l3.canRead()) {
											LOGGER.info("Not a valid file: " + l3.getAbsolutePath());
										} else if ((index < 0)
												|| !l3Files[k].substring(index).contentEquals(FILE_EXTENSION)) {
											LOGGER.info("Not a valid file name: " + l3.getAbsolutePath());
										} else {
											String key = l1Dirs[i] + File.separator + l2Dirs[j] + File.separator
													+ l3Files[k].substring(0, index);
											LOGGER.fine("Adding previously cached file " + l3.getAbsolutePath()
													+ " as " + key);
											try {
												this.lock.writeLock().lock();
												if (this.lruCache.put(key, l3) != null) {
													LOGGER.warning("overwriting cached entry: " + key);
												}
											} finally {
												this.lock.writeLock().unlock();
											}
										} // else (l3)
									} // for (k)
							} // else (l2)
						} // for (j)
				} // else (l1)
			} // for (i)
	}

	private void remove(Job key) {
		try {
			lock.writeLock().lock();
			this.lruCache.remove(key.getKey());
		} finally {
			lock.writeLock().unlock();
		}

	}

	protected void doWork() throws InterruptedException {
		StorageJob x = storageJobs.take();
		storeData(x.key, x.bitmap);
	}

	/**
	 * @return the priority which will be set for this thread.
	 */
	protected ThreadPriority getThreadPriority() {
		return ThreadPriority.BELOW_NORMAL;
	}

	/**
	 * @return true if this thread has some work to do, false otherwise.
	 */
	protected boolean hasWork() {
		return true;
	}

	/**
	 * stores the bitmap data on disk with filename key
	 * 
	 * @param key
	 *            filename
	 * @param bitmap
	 *            tile image
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
			if (threaded) {
				bitmap.decrementRefCount();
			}
			jobs.decrementAndGet();
		}

	}

}
