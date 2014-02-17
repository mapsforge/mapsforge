/*
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
package org.mapsforge.applications.android.samples;


import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.util.PausableThread;

/**
 * Demonstration of changing between fixed and computed tile sizes.
 */

public class TileSizeChanger extends BasicMapViewer {

	private class ChangerThread extends PausableThread {
		private static final int ROTATION_TIME = 10000;

		@Override
		protected void doWork() throws InterruptedException {
			TileSizeChanger.this.changeTileSize();
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

	private TileRendererLayer tileRendererLayer;

	@Override
	protected void createLayers() {
		tileRendererLayer = Utils.createTileRendererLayer(this.tileCache,
				this.mapViewPositions.get(0), getMapFile(), getRenderTheme(),
				false);
		this.layerManagers.get(0).getLayers().add(tileRendererLayer);
		this.changerThread = new ChangerThread();
		this.changerThread.start();
	}

	@Override
	protected void destroyLayers() {
		this.changerThread.interrupt();
		super.destroyLayers();
	}

	void changeTileSize() {
		Integer[] tileSizes = { 256, 120, 0, 120};

		if (tileSizes.length > 0) {
			int tileSize = tileSizes[iteration % tileSizes.length];
			this.mapViews.get(0).getModel().displayModel.setFixedTileSize(tileSize);
			iteration += 1;
			tileCache.destroy(); // clear the cache
			this.mapViews.get(0).getMapScaleBar().redrawScaleBar();
			layerManagers.get(0).redrawLayers();
		}
	}
}
