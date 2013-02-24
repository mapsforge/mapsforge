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
package org.mapsforge.map.android.view;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.Matrix;
import org.mapsforge.core.model.Dimension;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.map.android.graphics.AndroidGraphics;
import org.mapsforge.map.model.FrameBufferModel;
import org.mapsforge.map.viewinterfaces.FrameBufferInterface;

public class FrameBuffer implements FrameBufferInterface {
	private Bitmap bitmap1;
	private Bitmap bitmap2;
	private Dimension dimension;
	private final FrameBufferModel frameBufferModel;
	private final Matrix matrix;

	public FrameBuffer(FrameBufferModel frameBufferModel) {
		this.frameBufferModel = frameBufferModel;

		this.matrix = AndroidGraphics.INSTANCE.createMatrix();
	}

	@Override
	public synchronized void adjustMatrix(float diffX, float diffY, float scaleFactor, Dimension mapViewDimension) {
		if (this.dimension == null) {
			return;
		}

		int pivotX = this.dimension.width / 2;
		int pivotY = this.dimension.height / 2;

		this.matrix.reset();
		this.matrix.scale(scaleFactor, scaleFactor, pivotX, pivotY);
		this.matrix.translate(diffX, diffY);

		// translate the FrameBuffer center to the MapView center
		float dx = (this.dimension.width - mapViewDimension.width) / -2f;
		float dy = (this.dimension.height - mapViewDimension.height) / -2f;
		this.matrix.translate(dx, dy);
	}

	public synchronized void draw(Canvas canvas) {
		if (this.bitmap1 != null) {
			canvas.drawBitmap(this.bitmap1, this.matrix);
		}
	}

	@Override
	public synchronized void frameFinished(MapPosition frameMapPosition) {
		// swap both bitmap references
		Bitmap bitmapTemp = this.bitmap1;
		this.bitmap1 = this.bitmap2;
		this.bitmap2 = bitmapTemp;

		this.frameBufferModel.setMapPosition(frameMapPosition);
	}

	/**
	 * @return the bitmap of the second frame to draw on (may be null).
	 */
	@Override
	public synchronized Bitmap getDrawingBitmap() {
		return this.bitmap2;
	}

	@Override
	public synchronized void setDimension(Dimension dimension) {
		this.dimension = dimension;

		if (dimension.width > 0 && dimension.height > 0) {
			this.bitmap1 = AndroidGraphics.INSTANCE.createBitmap(dimension.width, dimension.height);
			this.bitmap2 = AndroidGraphics.INSTANCE.createBitmap(dimension.width, dimension.height);
		} else {
			this.bitmap1 = null;
			this.bitmap2 = null;
		}
	}
}
