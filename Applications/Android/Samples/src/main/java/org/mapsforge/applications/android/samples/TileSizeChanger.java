/*
 * Copyright 2014 Ludwig M Brinckmann
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
package org.mapsforge.applications.android.samples;

import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.util.PausableThread;

/**
 * Demonstration of changing between fixed and computed tile sizes.
 */

public class TileSizeChanger extends RenderTheme4 {

	private class ChangerThread extends PausableThread {
		private static final int ROTATION_TIME = 10000;

		@Override
		protected void doWork() throws InterruptedException {
			sleep(ROTATION_TIME);
			TileSizeChanger.this.changeTileSize();
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
				this.mapViewPositions.get(0), getMapFile(), getRenderTheme(),
				false, true);
		this.layerManagers.get(0).getLayers().add(tileRendererLayer);
	}

    @Override
    protected void destroyControls() {
        super.destroyControls();
        this.changerThread.interrupt();
    }


	void changeTileSize() {

		Integer[] tileSizes = { 256, 120, 0, 120};

		if (tileSizes.length > 0) {
			iteration += 1;
			// destroy and recreate the tile caches so that old storage is
			// freed and a new tile cache is created based on the new tile size
			destroyLayers();
			destroyTileCaches();

			int tileSize = tileSizes[iteration % tileSizes.length];
			this.mapViews.get(0).getModel().displayModel.setFixedTileSize(tileSize);

			createTileCaches();
			createLayers();

			this.mapViews.get(0).getMapScaleBar().redrawScaleBar();
			layerManagers.get(0).redrawLayers();
		}
	}
}
