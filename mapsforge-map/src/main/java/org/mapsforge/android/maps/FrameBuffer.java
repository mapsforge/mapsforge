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
package org.mapsforge.android.maps;

import java.io.OutputStream;

import org.mapsforge.core.model.GeoPoint;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.MercatorProjection;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;

/**
 * A FrameBuffer uses two separate memory buffers to display the current and build up the next frame.
 */
public class FrameBuffer {
	static final int MAP_VIEW_BACKGROUND = Color.rgb(238, 238, 238);

	private int height;
	private final MapView mapView;
	private Bitmap mapViewBitmap1;
	private Bitmap mapViewBitmap2;
	private final Canvas mapViewCanvas;
	private final Matrix matrix;
	private int width;

	FrameBuffer(MapView mapView) {
		this.mapView = mapView;
		this.mapViewCanvas = new Canvas();
		this.matrix = new Matrix();
	}

	/**
	 * Draws a tile bitmap at the right position on the MapView bitmap.
	 * 
	 * @param tile
	 *            the corresponding tile for the bitmap.
	 * @param bitmap
	 *            the bitmap to be drawn.
	 * @return true if the tile is visible and the bitmap was drawn, false otherwise.
	 */
	public synchronized boolean drawBitmap(Tile tile, Bitmap bitmap) {
		MapPosition mapPosition = this.mapView.getMapViewPosition().getMapPosition();
		if (tile.zoomLevel != mapPosition.zoomLevel) {
			// the tile doesn't fit to the current zoom level
			return false;
		} else if (this.mapView.isZoomAnimatorRunning()) {
			// do not disturb the ongoing animation
			return false;
		}

		GeoPoint geoPoint = mapPosition.geoPoint;
		double pixelLeft = MercatorProjection.longitudeToPixelX(geoPoint.longitude, mapPosition.zoomLevel);
		double pixelTop = MercatorProjection.latitudeToPixelY(geoPoint.latitude, mapPosition.zoomLevel);
		pixelLeft -= this.width >> 1;
		pixelTop -= this.height >> 1;

		if (pixelLeft - tile.getPixelX() > Tile.TILE_SIZE || pixelLeft + this.width < tile.getPixelX()) {
			// no horizontal intersection
			return false;
		} else if (pixelTop - tile.getPixelY() > Tile.TILE_SIZE || pixelTop + this.height < tile.getPixelY()) {
			// no vertical intersection
			return false;
		}

		if (!this.matrix.isIdentity()) {
			// change the current MapView bitmap
			this.mapViewBitmap2.eraseColor(MAP_VIEW_BACKGROUND);
			this.mapViewCanvas.setBitmap(this.mapViewBitmap2);

			// draw the previous MapView bitmap on the current MapView bitmap
			this.mapViewCanvas.drawBitmap(this.mapViewBitmap1, this.matrix, null);
			this.matrix.reset();

			// swap the two MapView bitmaps
			Bitmap mapViewBitmapSwap = this.mapViewBitmap1;
			this.mapViewBitmap1 = this.mapViewBitmap2;
			this.mapViewBitmap2 = mapViewBitmapSwap;
		}

		// draw the tile bitmap at the correct position
		float left = (float) (tile.getPixelX() - pixelLeft);
		float top = (float) (tile.getPixelY() - pixelTop);
		this.mapViewCanvas.drawBitmap(bitmap, left, top, null);
		return true;
	}

	/**
	 * Scales the matrix of the MapView and all its overlays.
	 * 
	 * @param scaleX
	 *            the horizontal scale.
	 * @param scaleY
	 *            the vertical scale.
	 * @param pivotX
	 *            the horizontal pivot point.
	 * @param pivotY
	 *            the vertical pivot point.
	 */
	public synchronized void matrixPostScale(float scaleX, float scaleY, float pivotX, float pivotY) {
		this.matrix.postScale(scaleX, scaleY, pivotX, pivotY);
		this.mapView.getOverlayController().postScale(scaleX, scaleY, pivotX, pivotY);
	}

	/**
	 * Translates the matrix of the MapView and all its overlays.
	 * 
	 * @param translateX
	 *            the horizontal translation.
	 * @param translateY
	 *            the vertical translation.
	 */
	public synchronized void matrixPostTranslate(float translateX, float translateY) {
		this.matrix.postTranslate(translateX, translateY);
		this.mapView.getOverlayController().postTranslate(translateX, translateY);
	}

	synchronized void clear() {
		if (this.mapViewBitmap1 != null) {
			this.mapViewBitmap1.eraseColor(MAP_VIEW_BACKGROUND);
		}

		if (this.mapViewBitmap2 != null) {
			this.mapViewBitmap2.eraseColor(MAP_VIEW_BACKGROUND);
		}
	}

	synchronized boolean compress(CompressFormat compressFormat, int quality, OutputStream outputStream) {
		if (this.mapViewBitmap1 == null) {
			return false;
		}

		return this.mapViewBitmap1.compress(compressFormat, quality, outputStream);
	}

	synchronized void destroy() {
		if (this.mapViewBitmap1 != null) {
			this.mapViewBitmap1.recycle();
		}

		if (this.mapViewBitmap2 != null) {
			this.mapViewBitmap2.recycle();
		}
	}

	synchronized void draw(Canvas canvas) {
		if (this.mapViewBitmap1 != null) {
			canvas.drawBitmap(this.mapViewBitmap1, this.matrix, null);
		}
	}

	synchronized void onSizeChanged() {
		this.width = this.mapView.getWidth();
		this.height = this.mapView.getHeight();
		this.mapViewBitmap1 = Bitmap.createBitmap(this.width, this.height, Bitmap.Config.RGB_565);
		this.mapViewBitmap2 = Bitmap.createBitmap(this.width, this.height, Bitmap.Config.RGB_565);
		clear();
		this.mapViewCanvas.setBitmap(this.mapViewBitmap1);
	}
}
