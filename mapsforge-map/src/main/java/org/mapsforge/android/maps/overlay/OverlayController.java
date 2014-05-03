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
package org.mapsforge.android.maps.overlay;

import java.util.List;

import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.PausableThread;
import org.mapsforge.android.maps.Projection;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.MapPosition;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Point;

public class OverlayController extends PausableThread {
	private static final String THREAD_NAME = OverlayController.class.getSimpleName();

	private static void recycleBitmap(Bitmap bitmap) {
		if (bitmap != null) {
			bitmap.recycle();
		}
	}

	private Bitmap bitmap1;
	private Bitmap bitmap2;
	private boolean changeSizeNeeded;
	private final MapView mapView;
	private final Matrix matrix;
	private final Canvas overlayCanvas;
	private boolean redrawNeeded;

	public OverlayController(MapView mapView) {
		super();
		this.mapView = mapView;
		this.matrix = new Matrix();
		this.overlayCanvas = new Canvas();
		this.changeSizeNeeded = true;
	}

	/**
	 * Draws this {@code OverlayController} on the given canvas.
	 * 
	 * @param canvas
	 *            the canvas on which this {@code OverlayController} should draw itself.
	 */
	public void draw(Canvas canvas) {
		if (this.bitmap1 != null) {
			synchronized (this.matrix) {
				canvas.drawBitmap(this.bitmap1, this.matrix, null);
			}
		}
	}

	/**
	 * Must be called whenever the size of the enclosing {@link MapView} has changed.
	 */
	public void onSizeChanged() {
		this.changeSizeNeeded = true;
		wakeUpThread();
	}

	/**
	 * @param scaleX
	 *            the horizontal scale.
	 * @param scaleY
	 *            the vertical scale.
	 * @param pivotX
	 *            the horizontal pivot point in pixel.
	 * @param pivotY
	 *            the vertical pivot point in pixel.
	 */
	public void postScale(float scaleX, float scaleY, float pivotX, float pivotY) {
		synchronized (this.matrix) {
			this.matrix.postScale(scaleX, scaleY, pivotX, pivotY);
		}
	}

	/**
	 * @param translateX
	 *            the horizontal translation in pixel.
	 * @param translateY
	 *            the vertical translation in pixel.
	 */
	public void postTranslate(float translateX, float translateY) {
		synchronized (this.matrix) {
			this.matrix.postTranslate(translateX, translateY);
		}
	}

	/**
	 * Requests a redraw of all overlays.
	 */
	public void redrawOverlays() {
		this.redrawNeeded = true;
		wakeUpThread();
	}

	private void adjustMatrix(MapPosition mapPositionBefore, MapPosition mapPositionAfter) {
		Projection projection = this.mapView.getProjection();
		Point pointBefore = projection.toPoint(mapPositionBefore.geoPoint, null, mapPositionBefore.zoomLevel);
		Point pointAfter = projection.toPoint(mapPositionAfter.geoPoint, null, mapPositionBefore.zoomLevel);

		int zoomLevelDiff = mapPositionAfter.zoomLevel - mapPositionBefore.zoomLevel;
		float scaleFactor = (float) Math.pow(2, zoomLevelDiff);
		int pivotX = this.overlayCanvas.getWidth() / 2;
		int pivotY = this.overlayCanvas.getHeight() / 2;

		this.matrix.reset();
		this.matrix.postTranslate(pointBefore.x - pointAfter.x, pointBefore.y - pointAfter.y);
		this.matrix.postScale(scaleFactor, scaleFactor, pivotX, pivotY);
	}

	private boolean changeSize() {
		recycleBitmaps();

		int width = this.mapView.getWidth();
		int height = this.mapView.getHeight();

		if (width > 0 && height > 0) {
			this.bitmap1 = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
			this.bitmap2 = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
			this.changeSizeNeeded = false;
			this.redrawNeeded = true;
			return true;
		}

		return false;
	}

	private void checkRedraw() {
		if (this.redrawNeeded) {
			this.redrawNeeded = false;
			redraw();
		}
	}

	private boolean checkSize() {
		if (this.changeSizeNeeded) {
			return changeSize();
		}

		return true;
	}

	private void recycleBitmaps() {
		recycleBitmap(this.bitmap1);
		recycleBitmap(this.bitmap2);
	}

	private void redraw() {
		this.bitmap2.eraseColor(Color.TRANSPARENT);
		this.overlayCanvas.setBitmap(this.bitmap2);

		MapPosition mapPositionBefore = this.mapView.getMapViewPosition().getMapPosition();
		BoundingBox boundingBox = this.mapView.getMapViewPosition().getBoundingBox();
		List<Overlay> overlays = this.mapView.getOverlays();
		synchronized (overlays) {
			for (Overlay overlay : overlays) {
				overlay.draw(boundingBox, mapPositionBefore.zoomLevel, this.overlayCanvas);
			}
		}

		MapPosition mapPositionAfter = this.mapView.getMapViewPosition().getMapPosition();
		synchronized (this.matrix) {
			adjustMatrix(mapPositionBefore, mapPositionAfter);
			swapBitmaps();
		}

		this.mapView.postInvalidate();
	}

	private void swapBitmaps() {
		Bitmap bitmapTemp = this.bitmap1;
		this.bitmap1 = this.bitmap2;
		this.bitmap2 = bitmapTemp;
	}

	private void wakeUpThread() {
		synchronized (this) {
			notify();
		}
	}

	@Override
	protected void afterRun() {
		recycleBitmaps();
	}

	@Override
	protected void doWork() {
		if (checkSize()) {
			checkRedraw();
		}
	}

	@Override
	protected String getThreadName() {
		return THREAD_NAME;
	}

	@Override
	protected ThreadPriority getThreadPriority() {
		return ThreadPriority.BELOW_NORMAL;
	}

	@Override
	protected boolean hasWork() {
		return this.changeSizeNeeded || this.redrawNeeded;
	}
}
