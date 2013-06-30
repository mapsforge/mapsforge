/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
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
package org.mapsforge.applications.android.samples;

import java.io.File;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.cache.FileSystemTileCache;
import org.mapsforge.map.layer.cache.InMemoryTileCache;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.cache.TwoLevelTileCache;
import org.mapsforge.map.layer.overlay.Marker;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.rendertheme.InternalRenderTheme;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;
import android.view.View.MeasureSpec;

/**
 * Utility functions that can be used across different mapsforge based activities
 */
public final class Utils {
	/**
	 * Compatibility method
	 * 
	 * @param a
	 *            the current activity
	 */
	@TargetApi(11)
	public static void enableHome(Activity a) {
		if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			// Show the Up button in the action bar.
			a.getActionBar().setDisplayHomeAsUpEnabled(true);
		}
	}

	/**
	 * Compatibility method
	 * 
	 * @param view
	 *            the view to set the background on
	 * @param background
	 *            the background
	 */
	public static void setBackground(View view, Drawable background) {
		if (android.os.Build.VERSION.SDK_INT >= 16) {
			view.setBackground(background);
		} else {
			view.setBackgroundDrawable(background);
		}
	}

	/**
	 * @param c
	 *            the Android context
	 * @param id
	 *            name for the directory
	 * @return a new cache created on the external storage
	 */
	static TileCache createExternalStorageTileCache(Context c, String id) {
		TileCache firstLevelTileCache = new InMemoryTileCache(32);
		String cacheDirectoryName = c.getExternalCacheDir().getAbsolutePath() + File.separator + id;
		File cacheDirectory = new File(cacheDirectoryName);
		if (!cacheDirectory.exists()) {
			cacheDirectory.mkdir();
		}
		TileCache secondLevelTileCache = new FileSystemTileCache(1024, cacheDirectory, AndroidGraphicFactory.INSTANCE);
		return new TwoLevelTileCache(firstLevelTileCache, secondLevelTileCache);
	}

	static Marker createMarker(Context c, int resourceIdentifier, LatLong latLong) {
		Drawable drawable = c.getResources().getDrawable(resourceIdentifier);
		Bitmap bitmap = AndroidGraphicFactory.convertToBitmap(drawable);
		return new Marker(latLong, bitmap, 0, -bitmap.getHeight() / 2);
	}

	static Paint createPaint(int color, int strokeWidth, Style style) {
		Paint paint = AndroidGraphicFactory.INSTANCE.createPaint();
		paint.setColor(color);
		paint.setStrokeWidth(strokeWidth);
		paint.setStyle(style);
		return paint;
	}

	/**
	 * @param c
	 *            the Android context
	 * @return a new cache
	 */
	static TileCache createTileCache(Context c, String id) {
		TileCache firstLevelTileCache = new InMemoryTileCache(32);
		File cacheDirectory = c.getDir(id, Context.MODE_PRIVATE);
		TileCache secondLevelTileCache = new FileSystemTileCache(1024, cacheDirectory, AndroidGraphicFactory.INSTANCE);
		return new TwoLevelTileCache(firstLevelTileCache, secondLevelTileCache);
	}

	static Layer createTileRendererLayer(TileCache tileCache, MapViewPosition mapViewPosition, File mapFile) {
		TileRendererLayer tileRendererLayer = new TileRendererLayer(tileCache, mapViewPosition,
				AndroidGraphicFactory.INSTANCE);
		tileRendererLayer.setMapFile(mapFile);
		tileRendererLayer.setXmlRenderTheme(InternalRenderTheme.OSMARENDER);
		tileRendererLayer.setTextScale(1.5f);
		return tileRendererLayer;
	}

	static Bitmap viewToBitmap(Context c, View view) {
		view.measure(MeasureSpec.getSize(view.getMeasuredWidth()), MeasureSpec.getSize(view.getMeasuredHeight()));
		view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
		view.setDrawingCacheEnabled(true);
		Drawable drawable = new BitmapDrawable(c.getResources(), android.graphics.Bitmap.createBitmap(view
				.getDrawingCache()));
		view.setDrawingCacheEnabled(false);
		return AndroidGraphicFactory.convertToBitmap(drawable);
	}

	private Utils() {
		throw new IllegalStateException();
	}
}
