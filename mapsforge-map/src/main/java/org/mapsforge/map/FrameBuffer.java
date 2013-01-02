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

import org.mapsforge.core.model.MapPosition;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.model.MapViewPosition;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;

public class FrameBuffer {
	private static Point mapPositionToPixel(MapPosition mapPosition) {
		double pixelX = MercatorProjection.longitudeToPixelX(mapPosition.geoPoint.longitude, mapPosition.zoomLevel);
		double pixelY = MercatorProjection.latitudeToPixelY(mapPosition.geoPoint.latitude, mapPosition.zoomLevel);
		return new Point(pixelX, pixelY);
	}

	private final Bitmap bitmap1;
	private final Bitmap bitmap2;
	private final Canvas drawingCanvas;
	private final Matrix matrix;

	public FrameBuffer(int width, int height) {
		this.bitmap1 = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		this.bitmap2 = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

		this.drawingCanvas = new Canvas(this.bitmap2);
		this.matrix = new Matrix();
	}

	public void adjustMatrix(MapPosition mapPositionBefore, MapPosition mapPositionAfter) {
		if (mapPositionBefore.zoomLevel == mapPositionAfter.zoomLevel) {
			Point pointBefore = mapPositionToPixel(mapPositionBefore);
			Point pointAfter = mapPositionToPixel(mapPositionAfter);
			float diffX = (float) (pointBefore.x - pointAfter.x);
			float diffY = (float) (pointBefore.y - pointAfter.y);
			synchronized (this.matrix) {
				this.matrix.postTranslate(diffX, diffY);
			}
		} else {
			float scaleFactor = (float) Math.pow(2, mapPositionAfter.zoomLevel - mapPositionBefore.zoomLevel);
			int pivotX = this.drawingCanvas.getWidth() / 2;
			int pivotY = this.drawingCanvas.getHeight() / 2;
			synchronized (this.matrix) {
				this.matrix.postScale(scaleFactor, scaleFactor, pivotX, pivotY);
			}
		}
	}

	public void draw(Canvas canvas) {
		synchronized (this.matrix) {
			canvas.drawBitmap(this.bitmap1, this.matrix, null);
		}
	}

	public void drawFrame(MapPosition mapPositionBefore, MapViewPosition mapViewPosition) {
		synchronized (this.matrix) {
			this.matrix.reset();

			MapPosition mapPositionAfter = mapViewPosition.getMapPosition();
			adjustMatrix(mapPositionBefore, mapPositionAfter);

			this.bitmap1.eraseColor(Color.TRANSPARENT);
			this.drawingCanvas.setBitmap(this.bitmap1);
			this.drawingCanvas.drawBitmap(this.bitmap2, this.matrix, null);

			this.bitmap2.eraseColor(Color.TRANSPARENT);
			this.drawingCanvas.setBitmap(this.bitmap2);
			this.matrix.reset();
		}
	}

	public Canvas getDrawingCanvas() {
		return this.drawingCanvas;
	}
}
