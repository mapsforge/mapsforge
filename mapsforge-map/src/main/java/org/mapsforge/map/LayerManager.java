/*
 * Copyright 2010, 2011, 2012 mapsforge.org
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
package org.mapsforge.map;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.map.layer.Layer;

import android.graphics.Canvas;

public class LayerManager extends PausableThread {
	private final List<Layer> layers = new CopyOnWriteArrayList<Layer>();
	private final MapView mapView;
	private boolean redrawNeeded;

	public LayerManager(MapView mapView) {
		this.mapView = mapView;
	}

	public List<Layer> getLayers() {
		return this.layers;
	}

	public void redrawLayers() {
		this.redrawNeeded = true;
		synchronized (this) {
			notify();
		}
	}

	@Override
	protected void afterRun() {
		for (Layer layer : this.getLayers()) {
			layer.destroy();
		}
	}

	@Override
	protected void doWork() throws InterruptedException {
		this.redrawNeeded = false;

		FrameBuffer frameBuffer = this.mapView.getFrameBuffer();
		Canvas canvas = frameBuffer.getDrawingCanvas();
		int width = canvas.getWidth();
		int height = canvas.getHeight();

		if (width > 1 && height > 1) {
			MapPosition mapPositionBefore = this.mapView.getMapViewPosition().getMapPosition();
			BoundingBox boundingBox = MapView.getBoundingBox(mapPositionBefore, width, height);

			Thread.sleep(500);
			for (Layer layer : this.getLayers()) {
				layer.draw(boundingBox, mapPositionBefore, canvas);
			}

			frameBuffer.drawFrame(mapPositionBefore, this.mapView.getMapViewPosition());
			this.mapView.invalidateOnUiThread();
		}
	}

	@Override
	protected ThreadPriority getThreadPriority() {
		return ThreadPriority.ABOVE_NORMAL;
	}

	@Override
	protected boolean hasWork() {
		return this.redrawNeeded;
	}
}
