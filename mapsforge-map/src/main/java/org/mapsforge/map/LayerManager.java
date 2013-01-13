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
import org.mapsforge.core.model.GeoPoint;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.model.Model;
import org.mapsforge.map.view.FrameBuffer;
import org.mapsforge.map.view.MapView;

import android.graphics.Canvas;
import android.os.SystemClock;
import android.util.Log;

public class LayerManager extends PausableThread {
	private static final int MILLISECONDS_PER_FRAME = 50;

	private static BoundingBox getBoundingBox(MapPosition mapPosition, Canvas canvas) {
		double pixelX = MercatorProjection.longitudeToPixelX(mapPosition.geoPoint.longitude, mapPosition.zoomLevel);
		double pixelY = MercatorProjection.latitudeToPixelY(mapPosition.geoPoint.latitude, mapPosition.zoomLevel);

		int halfCanvasWidth = canvas.getWidth() / 2;
		int halfCanvasHeight = canvas.getHeight() / 2;
		long mapSize = MercatorProjection.getMapSize(mapPosition.zoomLevel);

		double pixelXMin = Math.max(0, pixelX - halfCanvasWidth);
		double pixelYMin = Math.max(0, pixelY - halfCanvasHeight);
		double pixelXMax = Math.min(mapSize, pixelX + halfCanvasWidth);
		double pixelYMax = Math.min(mapSize, pixelY + halfCanvasHeight);

		double minLatitude = MercatorProjection.pixelYToLatitude(pixelYMax, mapPosition.zoomLevel);
		double minLongitude = MercatorProjection.pixelXToLongitude(pixelXMin, mapPosition.zoomLevel);
		double maxLatitude = MercatorProjection.pixelYToLatitude(pixelYMin, mapPosition.zoomLevel);
		double maxLongitude = MercatorProjection.pixelXToLongitude(pixelXMax, mapPosition.zoomLevel);

		return new BoundingBox(minLatitude, minLongitude, maxLatitude, maxLongitude);
	}

	private static Point getCanvasPosition(MapPosition mapPosition, int width, int height) {
		GeoPoint centerPoint = mapPosition.geoPoint;
		byte zoomLevel = mapPosition.zoomLevel;

		double pixelX = MercatorProjection.longitudeToPixelX(centerPoint.longitude, zoomLevel) - width / 2;
		double pixelY = MercatorProjection.latitudeToPixelY(centerPoint.latitude, zoomLevel) - height / 2;
		return new Point(pixelX, pixelY);
	}

	private final List<Layer> layers = new CopyOnWriteArrayList<Layer>();
	private final MapView mapView;
	private final Model model;
	private boolean redrawNeeded;

	public LayerManager(MapView mapView, Model model) {
		this.mapView = mapView;
		this.model = model;
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
		long timeBefore = SystemClock.uptimeMillis();
		this.redrawNeeded = false;

		FrameBuffer frameBuffer = this.mapView.getFrameBuffer();
		Canvas canvas = frameBuffer.getDrawingCanvas();
		if (canvas != null) {
			MapPosition mapPosition = this.model.mapViewPosition.getMapPosition();
			BoundingBox boundingBox = getBoundingBox(mapPosition, canvas);
			Point canvasPosition = getCanvasPosition(mapPosition, canvas.getWidth(), canvas.getHeight());

			for (Layer layer : this.getLayers()) {
				if (layer.isVisible()) {
					layer.draw(boundingBox, mapPosition.zoomLevel, canvas, canvasPosition);
				}
			}

			frameBuffer.frameFinished(mapPosition);
			this.mapView.invalidateOnUiThread();
		}

		long timeSleep = MILLISECONDS_PER_FRAME - (SystemClock.uptimeMillis() - timeBefore);
		Log.i("osm", "timeSleep: " + timeSleep);
		if (timeSleep > 1 && !isInterrupted()) {
			sleep(timeSleep);
		}
	}

	@Override
	protected ThreadPriority getThreadPriority() {
		return ThreadPriority.NORMAL;
	}

	@Override
	protected boolean hasWork() {
		return this.redrawNeeded;
	}
}
