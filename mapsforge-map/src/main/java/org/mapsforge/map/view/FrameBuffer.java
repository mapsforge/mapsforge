/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
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

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.GraphicContext;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.Matrix;
import org.mapsforge.core.model.Dimension;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.map.model.FrameBufferModel;

public class FrameBuffer {
	private Bitmap bitmap1;
	private Bitmap bitmap2;
	private Dimension dimension;
	private final FrameBufferModel frameBufferModel;
	private final GraphicFactory graphicFactory;
	private final Matrix matrix;

	public FrameBuffer(FrameBufferModel frameBufferModel, GraphicFactory graphicFactory) {
		this.frameBufferModel = frameBufferModel;
		this.graphicFactory = graphicFactory;

		this.matrix = graphicFactory.createMatrix();
	}

	public synchronized void adjustMatrix(float diffX, float diffY, float scaleFactor, Dimension mapViewDimension) {
		if (this.dimension == null) {
			return;
		}

		this.matrix.reset();

		centerFrameBufferToMapView(mapViewDimension);
		this.matrix.translate(diffX, diffY);
		scale(scaleFactor);
	}

	public synchronized void draw(GraphicContext graphicContext) {
		if (this.bitmap1 != null) {
			graphicContext.drawBitmap(this.bitmap1, this.matrix);
		}
	}

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
	public synchronized Bitmap getDrawingBitmap() {
		return this.bitmap2;
	}

	public synchronized void setDimension(Dimension dimension) {
		this.dimension = dimension;

		if (dimension.width > 0 && dimension.height > 0) {
			this.bitmap1 = this.graphicFactory.createBitmap(dimension.width, dimension.height);
			this.bitmap2 = this.graphicFactory.createBitmap(dimension.width, dimension.height);
		} else {
			this.bitmap1 = null;
			this.bitmap2 = null;
		}
	}

	private void centerFrameBufferToMapView(Dimension mapViewDimension) {
		float dx = (this.dimension.width - mapViewDimension.width) / -2f;
		float dy = (this.dimension.height - mapViewDimension.height) / -2f;
		this.matrix.translate(dx, dy);
	}

	private void scale(float scaleFactor) {
		if (scaleFactor != 1) {
			// the pivot point is the coordinate which remains unchanged by the translation
			float pivotScaleFactor = scaleFactor - 1;
			float pivotX = (this.dimension.width / -2f) * pivotScaleFactor;
			float pivotY = (this.dimension.height / -2f) * pivotScaleFactor;
			this.matrix.translate(pivotX, pivotY);
			this.matrix.scale(scaleFactor, scaleFactor);
		}
	}
}
