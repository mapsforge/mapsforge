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
package org.mapsforge.map.view;

import org.mapsforge.core.model.Dimension;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.map.model.FrameBufferModel;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;

public class FrameBuffer {
	private Bitmap bitmap1;
	private Bitmap bitmap2;
	private Dimension dimension;
	private final Canvas drawingCanvas;
	private final FrameBufferModel frameBufferModel;
	private final Matrix matrix;

	public FrameBuffer(FrameBufferModel frameBufferModel) {
		this.frameBufferModel = frameBufferModel;

		this.drawingCanvas = new Canvas();
		this.matrix = new Matrix();
	}

	public synchronized void adjustMatrix(float diffX, float diffY, float scaleFactor, Dimension mapViewDimension) {
		if (this.dimension == null) {
			return;
		}

		int pivotX = this.dimension.width / 2;
		int pivotY = this.dimension.height / 2;

		this.matrix.reset();
		this.matrix.postScale(scaleFactor, scaleFactor, pivotX, pivotY);
		this.matrix.postTranslate(diffX, diffY);

		// translate the FrameBuffer center to the MapView center
		float dx = (this.dimension.width - mapViewDimension.width) / -2f;
		float dy = (this.dimension.height - mapViewDimension.height) / -2f;
		this.matrix.postTranslate(dx, dy);
	}

	public synchronized void draw(Canvas canvas) {
		if (this.bitmap1 != null) {
			canvas.drawBitmap(this.bitmap1, this.matrix, null);
		}
	}

	public synchronized void frameFinished(MapPosition frameMapPosition) {
		// swap both bitmap references
		Bitmap bitmapTemp = this.bitmap1;
		this.bitmap1 = this.bitmap2;
		this.bitmap2 = bitmapTemp;

		this.bitmap2.eraseColor(Color.TRANSPARENT);
		this.drawingCanvas.setBitmap(this.bitmap2);

		this.frameBufferModel.setMapPosition(frameMapPosition);
	}

	public synchronized Canvas getDrawingCanvas() {
		if (this.bitmap1 != null) {
			return this.drawingCanvas;
		}
		return null;
	}

	public synchronized void setDimension(Dimension dimension) {
		this.dimension = dimension;

		if (dimension.width > 0 && dimension.height > 0) {
			this.bitmap1 = Bitmap.createBitmap(dimension.width, dimension.height, Bitmap.Config.RGB_565);
			this.bitmap2 = Bitmap.createBitmap(dimension.width, dimension.height, Bitmap.Config.RGB_565);
			this.drawingCanvas.setBitmap(this.bitmap2);
		} else {
			this.bitmap1 = null;
			this.bitmap2 = null;
		}
	}
}
