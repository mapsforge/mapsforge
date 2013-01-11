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

	private Bitmap bitmap1;
	private Bitmap bitmap2;
	private final Canvas drawingCanvas = new Canvas();
	private int height;
	private final Matrix matrix = new Matrix();
	private int width;

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
			synchronized (this.matrix) {
				if (this.bitmap1 != null) {
					int pivotX = this.drawingCanvas.getWidth() / 2;
					int pivotY = this.drawingCanvas.getHeight() / 2;
					this.matrix.postScale(scaleFactor, scaleFactor, pivotX, pivotY);
				}
			}
		}
	}

	public void changeSize(int widthNew, int heightNew) {
		synchronized (this.matrix) {
			this.width = widthNew;
			this.height = heightNew;

			if (widthNew > 0 && heightNew > 0) {
				double overdrawFactor = 2;
				int bitmapWidth = (int) (widthNew * overdrawFactor);
				int bitmapHeight = (int) (heightNew * overdrawFactor);

				this.bitmap1 = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.RGB_565);
				this.bitmap2 = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.RGB_565);
				this.drawingCanvas.setBitmap(this.bitmap2);
			} else {
				this.bitmap1 = null;
				this.bitmap2 = null;
			}
		}
	}

	public void draw(Canvas canvas) {
		synchronized (this.matrix) {
			if (this.bitmap1 != null) {
				canvas.drawBitmap(this.bitmap1, this.matrix, null);
			}
		}
	}

	public void drawFrame(MapPosition mapPositionBefore, MapViewPosition mapViewPosition) {
		synchronized (this.matrix) {
			this.matrix.reset();

			MapPosition mapPositionAfter = mapViewPosition.getMapPosition();
			adjustMatrix(mapPositionBefore, mapPositionAfter);

			Bitmap bitmapTemp = this.bitmap1;
			this.bitmap1 = this.bitmap2;
			this.bitmap2 = bitmapTemp;

			this.bitmap2.eraseColor(Color.TRANSPARENT);
			this.drawingCanvas.setBitmap(this.bitmap2);

			float dx = (this.drawingCanvas.getWidth() - this.width) / -2f;
			float dy = (this.drawingCanvas.getHeight() - this.height) / -2f;
			this.matrix.postTranslate(dx, dy);
		}
	}

	public Canvas getDrawingCanvas() {
		synchronized (this.matrix) {
			if (this.bitmap1 != null) {
				return this.drawingCanvas;
			}
			return null;
		}
	}
}
