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

import org.mapsforge.core.model.Point;
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

	public synchronized void adjustMatrix(double diffX, double diffY, float scaleFactor, Dimension mapViewDimension, double pivotDistanceX, double pivotDistanceY) {
		if (this.dimension == null) {
			return;
		}
 		this.matrix.reset();
		centerFrameBufferToMapView(mapViewDimension);
		this.matrix.translate((float) (diffX + pivotDistanceX), (float) (diffY + pivotDistanceY));

		scale(scaleFactor, pivotDistanceX, pivotDistanceY);
	}

	public synchronized void destroy() {
		destroyBitmaps();
	}

    public synchronized void draw(GraphicContext graphicContext) {
	    graphicContext.fillColor(this.graphicFactory.getBackgroundColor());
		if (this.bitmap1 != null) {
  	        graphicContext.drawBitmap(this.bitmap1, this.matrix);
		}
	}

	public void frameFinished(MapPosition frameMapPosition) {
        synchronized (this) {
            // swap both bitmap references
            Bitmap bitmapTemp = this.bitmap1;
            this.bitmap1 = this.bitmap2;
            this.bitmap2 = bitmapTemp;
	        if (this.bitmap2 != null) {
	            this.bitmap2.setBackgroundColor(this.graphicFactory.getBackgroundColor());
	        }
        }
        // taking this out of the synchronized region removes a deadlock potential
        // at the small risk of an inconsistent zoom
        this.frameBufferModel.setMapPosition(frameMapPosition);
    }

	/**
	 * @return the bitmap of the second frame to draw on (may be null).
	 */
	public synchronized Bitmap getDrawingBitmap() {
		return this.bitmap2;
	}

    public synchronized Dimension getDimension() {
        return this.dimension;
    }


	public synchronized void setDimension(Dimension dimension) {
		if (this.dimension != null && this.dimension.equals(dimension)) {
			return;
		}
		this.dimension = dimension;

        destroyBitmaps();

		if (dimension.width > 0 && dimension.height > 0) {
			this.bitmap1 = this.graphicFactory.createBitmap(dimension.width, dimension.height);
			this.bitmap1.setBackgroundColor(this.graphicFactory.getBackgroundColor());
			this.bitmap2 = this.graphicFactory.createBitmap(dimension.width, dimension.height);
			this.bitmap2.setBackgroundColor(this.graphicFactory.getBackgroundColor());
		}
	}

    private void destroyBitmaps() {
        if (this.bitmap1 != null) {
            this.bitmap1.decrementRefCount();
            this.bitmap1 = null;
        }
        if (this.bitmap2 != null) {
            this.bitmap2.decrementRefCount();
            this.bitmap2 = null;
        }
    }

    private void centerFrameBufferToMapView(Dimension mapViewDimension) {
		float dx = (this.dimension.width - mapViewDimension.width) / -2f;
		float dy = (this.dimension.height - mapViewDimension.height) / -2f;
        this.matrix.translate(dx, dy);
	}

	private void scale(float scaleFactor, double pivotDistanceX, double pivotDistanceY) {
		if (scaleFactor != 1) {
			final Point center = this.dimension.getCenter();
			float pivotX = (float) (pivotDistanceX + center.x);
			float pivotY = (float) (pivotDistanceY + center.y);
			this.matrix.scale(scaleFactor, scaleFactor, pivotX, pivotY);
        }
	}

}
