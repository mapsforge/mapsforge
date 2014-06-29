/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
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
import org.mapsforge.map.layer.download.DownloadJob;
import org.mapsforge.map.layer.queue.Job;
import org.mapsforge.map.layer.renderer.RendererJob;

/**
 * A thread-safe cache for image files with a fixed size and LRU policy, persistent across instances.
 */
public class FileSystemTileCache implements TileCache {
	static final String FILE_EXTENSION = ".tile";
	private static final Logger LOGGER = Logger.getLogger(FileSystemTileCache.class.getName());

	private static File checkDirectory(File file) {
		if (!file.exists() && !file.mkdirs()) {
			throw new IllegalArgumentException("could not create directory: " + file);
		} else if (!file.isDirectory()) {
			throw new IllegalArgumentException("not a directory: " + file);
		} else if (!file.canRead()) {
			throw new IllegalArgumentException("cannot read directory: " + file);
		} else if (!file.canWrite()) {
			throw new IllegalArgumentException("cannot write directory: " + file);
		}
		return file;
	}

	private final File cacheDirectory;
	private final GraphicFactory graphicFactory;
	private FileWorkingSetCache<Integer> lruCache;
	private final ReentrantReadWriteLock lock;

	// FIXME: make TTL configurable
	static final long TTL = 604800000; // 604,800,000 ms equals one week

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
		this.cacheDirectory = checkDirectory(cacheDirectory);
		this.graphicFactory = graphicFactory;
		this.lock = new ReentrantReadWriteLock();
	}

	@Override
	public boolean containsKey(Job key) {
		try {
			lock.readLock().lock();
			return this.lruCache.containsKey(key.hashCode());
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public void destroy() {
		try {
			lock.writeLock().lock();
			this.lruCache.clear();
		} finally {
			lock.writeLock().unlock();
		}

		File[] filesToDelete = this.cacheDirectory.listFiles(ImageFileNameFilter.INSTANCE);
		if (filesToDelete != null) {
			for (File file : filesToDelete) {
				if (file.exists() && !file.delete()) {
					LOGGER.log(Level.SEVERE, "could not delete file: " + file);
				}
			}
		}
	}

	@Override
	public TileBitmap get(Job key) {

		File file;
		try {
			lock.readLock().lock();
			file = this.lruCache.get(key.hashCode());
		} finally {
			lock.readLock().unlock();
		}
		if (file == null) {
			// check if there is a cached tile from an earlier instance
			file = new File(this.cacheDirectory, key.hashCode() + FILE_EXTENSION);
			if (!file.exists())
				return null;
			// discard cached copy of locally rendered tile if source is newer
			if ((key instanceof RendererJob) && (((RendererJob) key).mapFile.lastModified() > file.lastModified()))
				return null;
			if ((key instanceof DownloadJob) && ((System.currentTimeMillis() - file.lastModified()) > TTL))
				return null;
			// TODO: find a nicer way to expire downloaded tiles
			// tile URL is ((DownloadJob) key).TileSource.getTileURL(key.tile)
			// last modification date is URLConnection.getLastModified()
		}

		InputStream inputStream = null;
		try {
			inputStream = new FileInputStream(file);
			return this.graphicFactory.createTileBitmap(inputStream, key.tile.tileSize, key.hasAlpha);
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
			outputStream = new FileOutputStream(file);
			bitmap.compress(outputStream);
			try {
				lock.writeLock().lock();
				if (this.lruCache.put(key.hashCode(), file) != null) {
					LOGGER.warning("overwriting cached entry: " + key.hashCode());
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
				this.lruCache = new FileWorkingSetCache<Integer>(0);
			} finally {
				lock.writeLock().unlock();
			}
		} finally {
			IOUtils.closeQuietly(outputStream);
		}
	}

	public void setWorkingSet(Set<Job> workingSet) {
		Set<Integer> workingSetInteger = new HashSet<Integer>();
		for (Job job : workingSet) {
			workingSetInteger.add(job.hashCode());
		}
		this.lruCache.setWorkingSet(workingSetInteger);
	}

	private File getOutputFile(Job job) {
		return new File(this.cacheDirectory, job.hashCode() + FILE_EXTENSION);
	}

	private void remove(Job key) {
		try {
			lock.writeLock().lock();
			this.lruCache.remove(key.hashCode());
		} finally {
			lock.writeLock().unlock();
		}

	}
}
