/*
 * Copyright 2013-2014 Ludwig M Brinckmann
 * Copyright 2015-2017 devemux86
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
package org.mapsforge.samples.android;

import android.os.Environment;
import android.util.Log;

import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.android.util.ExternalRenderThemeUsingJarResources;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.rendertheme.XmlRenderTheme;
import org.mapsforge.map.util.PausableThread;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.util.Locale;

/**
 * Demonstration of changing render themes. This activity checks for .xml files
 * on device storage and loads them as render themes.
 */
public class RenderThemeChanger extends DefaultTheme {

    private class ChangerThread extends PausableThread {
        private static final int ROTATION_TIME = 10000;

        @Override
        protected void doWork() throws InterruptedException {
            sleep(ROTATION_TIME);
            RenderThemeChanger.this.changeRenderTheme();
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
            if (s.toLowerCase(Locale.ENGLISH).endsWith(".xml")) {
                return true;
            }
            return false;
        }
    };

    private TileRendererLayer tileRendererLayer;

    @Override
    protected void createControls() {
        super.createControls();
        this.changerThread = new ChangerThread();
        this.changerThread.start();
    }

    @Override
    protected void createLayers() {
        tileRendererLayer = AndroidUtil.createTileRendererLayer(this.tileCaches.get(0),
                this.mapView.getModel().mapViewPosition, getMapFile(), getRenderTheme(),
                false, true, false);
        this.mapView.getLayerManager().getLayers().add(tileRendererLayer);
    }

    void changeRenderTheme() {
        File[] renderThemes = Environment.getExternalStorageDirectory().listFiles(renderThemesFilter);
        if (renderThemes.length > 0) {
            File nextTheme = renderThemes[iteration % renderThemes.length];
            iteration += 1;
            try {
                XmlRenderTheme nextRenderTheme = new ExternalRenderThemeUsingJarResources(nextTheme);
                Log.i(SamplesApplication.TAG, "Loading new render theme " + nextTheme.getName());
                // there should really be a simpler way to just change the render theme safely
                mapView.getLayerManager().getLayers().remove(tileRendererLayer);
                tileRendererLayer.onDestroy();
                purgeTileCaches();

                createTileCaches();
                tileRendererLayer = AndroidUtil.createTileRendererLayer(tileCaches.get(0),
                        mapView.getModel().mapViewPosition, getMapFile(), nextRenderTheme,
                        true, // generic use alpha, e.g. onlybuildings.xml map background
                        true, false);
                mapView.getLayerManager().getLayers().add(tileRendererLayer);

                mapView.getLayerManager().redrawLayers();
            } catch (FileNotFoundException e) {
                Log.i(SamplesApplication.TAG, "Could not open file " + nextTheme.getName());
            }
        }
    }

    @Override
    protected void onDestroy() {
        this.changerThread.finish();
        super.onDestroy();
    }
}
