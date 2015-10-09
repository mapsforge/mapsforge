/*
 * Copyright 2013-2014 Ludwig M Brinckmann
 * Copyright 2015 devemux86
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
import java.io.FileNotFoundException;
import java.io.FilenameFilter;

import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.android.util.ExternalRenderThemeUsingJarResources;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.rendertheme.XmlRenderTheme;
import org.mapsforge.map.util.PausableThread;

import android.os.Environment;
import android.util.Log;

/**
 * Demonstration of changing render themes. This activity checks for .xml files
 * on the sdcard and loads them as render themes.
 */
public class RenderThemeChanger extends RenderTheme4 {

	private class ChangerThread extends PausableThread {
		private static final int ROTATION_TIME = 10000; // milli secs to display a theme

		@Override
		protected void doWork() throws InterruptedException {
			RenderThemeChanger.this.changeRenderTheme();
			sleep(ROTATION_TIME);
		}

		@Override
		protected ThreadPriority getThreadPriority() {
			return ThreadPriority.ABOVE_NORMAL;
		}

		@Override
		protected boolean hasWork() {
			return true;
		}

	}

	private ChangerThread changerThread;
	private int iteration;
	private FilenameFilter renderThemesFilter = new FilenameFilter() {
		@Override
		public boolean accept(File file, String s) {
			if (s.endsWith(".xml")) {
				return true;
			}
			return false;
		}
	};

	private TileRendererLayer tileRendererLayer;

	@Override
	protected void createLayers() {
		tileRendererLayer = AndroidUtil.createTileRendererLayer(this.tileCaches.get(0),
				this.mapView.getModel().mapViewPosition, getMapFile(), getRenderTheme(),
				false, true);
		mapView.getLayerManager().getLayers().add(tileRendererLayer);
		this.changerThread = new ChangerThread();
		this.changerThread.start();
	}

	void changeRenderTheme() {
		File[] renderThemes = Environment.getExternalStorageDirectory().listFiles(renderThemesFilter);
		if (renderThemes.length > 0) {
			File nextTheme = renderThemes[iteration % renderThemes.length];
			iteration += 1;
			try {
				XmlRenderTheme nextRenderTheme = new ExternalRenderThemeUsingJarResources(nextTheme);
				Log.i(SamplesApplication.TAG, "Loading new render theme " + nextTheme.getName());
				// there should really be a simpler way to just change the
				// render theme safely
				mapView.getLayerManager().getLayers().remove(tileRendererLayer);
				tileRendererLayer.onDestroy();
				tileCaches.get(0).destroy();
				tileRendererLayer = AndroidUtil.createTileRendererLayer(tileCaches.get(0),
						mapView.getModel().mapViewPosition, getMapFile(), nextRenderTheme,
						false, false);
				mapView.getLayerManager().getLayers().add(tileRendererLayer);
				mapView.getLayerManager().redrawLayers();
			} catch (FileNotFoundException e) {
				Log.i(SamplesApplication.TAG, "Could not open file " + nextTheme.getName());
			}
		}
	}

	@Override
	protected void onDestroy() {
		this.changerThread.interrupt();
		super.onDestroy();
	}
}
