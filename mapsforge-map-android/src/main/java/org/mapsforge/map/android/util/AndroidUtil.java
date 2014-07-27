/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright © 2014 Ludwig M Brinckmann
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
package org.mapsforge.map.android.util;

import java.io.File;

import org.mapsforge.map.layer.cache.FileSystemTileCache;
import org.mapsforge.map.layer.cache.InMemoryTileCache;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.cache.TwoLevelTileCache;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.os.Looper;
import android.os.StatFs;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

public final class AndroidUtil {

	public static final boolean HONEYCOMB_PLUS = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;

	/**
	 * Creates a two-level tile cache.
	 * <p>
	 * This is a utility function which creates a two-level tile cache along with its backends.
	 * 
	 * @param c
	 *            the Android context
	 * @param id
	 *            name for the directory, which will be created as a subdirectory of the default cache directory (as
	 *            returned by {@link android.content.Context#getExternalCacheDir()}).
	 * @param firstLevelSize
	 *            size of the first level cache
	 * @param tileSize
	 *            tile size
	 * @param persistent
	 *            whether the second level tile cache should be persistent
	 * @return a new cache created on the external storage
	 */
	public static TileCache createExternalStorageTileCache(Context c, String id, int firstLevelSize, int tileSize,
			boolean persistent) {
		Log.d("TILECACHE INMEMORY SIZE", Integer.toString(firstLevelSize));
		TileCache firstLevelTileCache = new InMemoryTileCache(firstLevelSize);
		File cacheDir = c.getExternalCacheDir();
		if (cacheDir != null) {
			// cacheDir will be null if full
			String cacheDirectoryName = cacheDir.getAbsolutePath() + File.separator + id;
			File cacheDirectory = new File(cacheDirectoryName);
			if (cacheDirectory.exists() || cacheDirectory.mkdir()) {
				int tileCacheFiles = estimateSizeOfFileSystemCache(cacheDirectoryName, firstLevelSize, tileSize);
				if (cacheDirectory.canWrite() && tileCacheFiles > 0) {
					try {
						Log.d("TILECACHE FILECACHE SIZE", Integer.toString(firstLevelSize));
						TileCache secondLevelTileCache = new FileSystemTileCache(tileCacheFiles, cacheDirectory,
								org.mapsforge.map.android.graphics.AndroidGraphicFactory.INSTANCE);
						return new TwoLevelTileCache(firstLevelTileCache, secondLevelTileCache);
					} catch (IllegalArgumentException e) {
						Log.w("TILECACHE", e.toString());
					}
				}
			}
		}
		return firstLevelTileCache;
	}

	/**
	 * Creates a two-level tile cache.
	 * <p>
	 * This is a utility function which creates a two-level tile cache along with its backends. It is equivalent to
	 * calling {@link #createExternalStorageTileCache(Context, String, int, int, boolean)} with the {@code persistent}
	 * argument set to {@code false}.
	 */
	public static TileCache createExternalStorageTileCache(Context c, String id, int firstLevelSize, int tileSize) {
		return createExternalStorageTileCache(c, id, firstLevelSize, tileSize, false);
	}

	/**
	 * Creates a two-level tile cache with the right size.
	 * <p>
	 * Utility function to create a two-level tile cache with the right size. When the cache is created we do not
	 * actually know the size of the mapview, so the screenRatio is an approximation of the required size.
	 * 
	 * @param c
	 *            the Android context
	 * @param id
	 *            name for the storage directory
	 * @param tileSize
	 *            tile size
	 * @param screenRatio
	 *            part of the screen the view takes up
	 * @param overdraw
	 *            overdraw allowance
	 * @param persistent
	 *            whether the second level tile cache should be persistent
	 * @return a new cache created on the external storage
	 */

	public static TileCache createTileCache(Context c, String id, int tileSize, float screenRatio, double overdraw,
			boolean persistent) {
		int cacheSize = Math.round(getMinimumCacheSize(c, tileSize, overdraw, screenRatio));
		return createExternalStorageTileCache(c, id, cacheSize, tileSize, persistent);
	}

	/**
	 * Creates a two-level tile cache with the right size.
	 * <p>
	 * Utility function to create a two-level tile cache with the right size. It is equivalent to calling
	 * {@link #createTileCache(Context, String, int, float, double, boolean)} with the {@code persistent} argument set
	 * to {@code false}.
	 */

	public static TileCache createTileCache(Context c, String id, int tileSize, float screenRatio, double overdraw) {
		return createTileCache(c, id, tileSize, screenRatio, overdraw, false);
	}

	/**
	 * @return true if the current thread is the UI thread, false otherwise.
	 */
	public static boolean currentThreadIsUiThread() {
		return Looper.getMainLooper().getThread() == Thread.currentThread();
	}

	/**
	 * @param cacheDirectoryName
	 *            where the file system tile cache will be located
	 * @param firstLevelSize
	 *            size of the first level cache, no point cache being smaller
	 * @param tileSize
	 *            tile size
	 * @return recommended number of files in FileSystemTileCache
	 */
	public static int estimateSizeOfFileSystemCache(String cacheDirectoryName, int firstLevelSize, int tileSize) {
		// assumption on size of files in cache, on the large side as not to eat
		// up all free space, real average probably 50K compressed
		final int tileCacheFileSize = 4 * tileSize * tileSize;
		final int maxCacheFiles = 2000; // arbitrary, probably too high

		// result cannot be bigger than maxCacheFiles
		int result = (int) Math.min(maxCacheFiles, getAvailableCacheSlots(cacheDirectoryName, tileCacheFileSize));

		if (firstLevelSize > result) {
			// no point having a file system cache that does not even hold the memory cache
			result = 0;
		}
		return result;
	}

	/**
	 * Get the number of tiles that can be stored on the file system.
	 * 
	 * @param directory
	 *            where the cache will reside
	 * @param fileSize
	 *            average size of tile to be cached
	 * @return number of tiles that can be stored without running out of space
	 */
	@SuppressWarnings("deprecation")
	@TargetApi(18)
	public static long getAvailableCacheSlots(String directory, int fileSize) {
		StatFs statfs = new StatFs(directory);
		if (android.os.Build.VERSION.SDK_INT >= 18) {
			return statfs.getAvailableBytes() / fileSize;
		}
		// problem is overflow with devices with large storage, so order is important here
		// additionally avoid division by zero in devices with a large block size
		int blocksPerFile = Math.max(fileSize / statfs.getBlockSize(), 1);
		return statfs.getAvailableBlocks() / blocksPerFile;
	}

	/**
	 * Compute the minimum cache size for a view.
	 * 
	 * @param c
	 *            the context.
	 * @param tileSize
	 *            tile size
	 * @param overdrawFactor
	 *            the overdraw factor applied to the mapview.
	 * @param screenRatio
	 *            the part of the screen the view covers.
	 * @return the minimum cache size for the view.
	 */
	@TargetApi(13)
	public static int getMinimumCacheSize(Context c, int tileSize, double overdrawFactor, float screenRatio) {
		WindowManager wm = (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		int height;
		int width;
		if (android.os.Build.VERSION.SDK_INT >= 13) {
			Point p = new Point();
			display.getSize(p);
			height = p.y;
			width = p.x;
		} else {
			// deprecated since Android 13
			height = display.getHeight();
			width = display.getWidth();
		}

		return (int) (screenRatio * Math.ceil(1 + (height * overdrawFactor / tileSize)) * Math.ceil(1 + (width
				* overdrawFactor / tileSize)));
	}

	private AndroidUtil() {
		// noop, for privacy
	}

}
