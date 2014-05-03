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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mapsforge.core.graphics.CorruptedInputStreamException;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.core.util.IOUtils;
import org.mapsforge.map.layer.queue.Job;

/**
 * A thread-safe cache for image files with a fixed size and LRU policy.
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
	private FileLRUCache<Integer> lruCache;

	/**
	 * @param capacity
	 *            the maximum number of entries in this cache.
	 * @param cacheDirectory
	 *            the directory where cached tiles will be stored.
	 * @throws IllegalArgumentException
	 *             if the capacity is negative.
	 */
	public FileSystemTileCache(int capacity, File cacheDirectory, GraphicFactory graphicFactory) {
		this.lruCache = new FileLRUCache<>(capacity);
		this.cacheDirectory = checkDirectory(cacheDirectory);
		this.graphicFactory = graphicFactory;
	}

	@Override
	public synchronized boolean containsKey(Job key) {
		return this.lruCache.containsKey(key.hashCode());
	}

	@Override
	public synchronized void destroy() {
		this.lruCache.clear();

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
	public synchronized TileBitmap get(Job key) {
		File file = this.lruCache.get(key.hashCode());

		if (file == null) {
			return null;
		}

		InputStream inputStream = null;
		try {
			inputStream = new FileInputStream(file);
			return this.graphicFactory.createTileBitmap(inputStream, key.tileSize, key.hasAlpha);
		} catch (CorruptedInputStreamException e) {
			// this can happen, at least on Android, when the input stream
			// is somehow corrupted, returning null ensures it will be loaded
			// from another source
			this.lruCache.remove(key.hashCode());
			LOGGER.log(Level.WARNING, "input stream from file system cache invalid", e);
			return null;
		} catch (IOException e) {
			this.lruCache.remove(key.hashCode());
			LOGGER.log(Level.SEVERE, null, e);
			return null;
		} finally {
			IOUtils.closeQuietly(inputStream);
		}
	}

	@Override
	public synchronized int getCapacity() {
		return this.lruCache.capacity;
	}

	@Override
	public synchronized void put(Job key, TileBitmap bitmap) {
		if (key == null) {
			throw new IllegalArgumentException("key must not be null");
		} else if (bitmap == null) {
			throw new IllegalArgumentException("bitmap must not be null");
		}

		if (this.lruCache.capacity == 0) {
			return;
		}

		OutputStream outputStream = null;
		try {
			File file = getOutputFile(key);
			outputStream = new FileOutputStream(file);
			bitmap.compress(outputStream);
			if (this.lruCache.put(key.hashCode(), file) != null) {
				LOGGER.warning("overwriting cached entry: " + key.hashCode());
			}
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Disabling filesystem cache", e);
			// most likely cause is that the disk is full, just disable the
			// cache otherwise
			// more and more exceptions will be thrown.
			this.destroy();
			this.lruCache = new FileLRUCache<Integer>(0);

		} finally {
			IOUtils.closeQuietly(outputStream);
		}
	}

	private File getOutputFile(Job job) {
		return new File(this.cacheDirectory, job.hashCode() + FILE_EXTENSION);
	}
}
