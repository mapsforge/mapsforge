/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
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
package org.mapsforge.map.rendertheme.renderinstruction;

import java.util.List;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.model.Tag;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.layer.renderer.PolylineContainer;
import org.mapsforge.map.reader.PointOfInterest;
import org.mapsforge.map.rendertheme.RenderCallback;

/**
 * Represents an icon along a polyline on the map.
 */
public class LineSymbol extends RenderInstruction {
	private final boolean alignCenter;
	private final Bitmap bitmap;
	private final float dy;
	private final int priority;
	private final boolean repeat;
	private final float repeatGap;
	private final float repeatStart;
	private final boolean rotate;

	LineSymbol(LineSymbolBuilder lineSymbolBuilder) {
		super(lineSymbolBuilder.getCategory());
		this.alignCenter = lineSymbolBuilder.alignCenter;
		this.bitmap = lineSymbolBuilder.bitmap;
		this.dy = lineSymbolBuilder.dy;
		this.priority = lineSymbolBuilder.priority;
		this.repeat = lineSymbolBuilder.repeat;
		this.repeatGap = lineSymbolBuilder.repeatGap;
		this.repeatStart = lineSymbolBuilder.repeatStart;
		this.rotate = lineSymbolBuilder.rotate;
	}

	@Override
	public void destroy() {
		this.bitmap.decrementRefCount();
	}

	@Override
	public void renderNode(RenderCallback renderCallback, PointOfInterest poi, Tile tile) {
		// do nothing
	}

	@Override
	public void renderWay(RenderCallback renderCallback, PolylineContainer way) {
		renderCallback.renderWaySymbol(way, this.priority, this.bitmap, this.dy, this.alignCenter,
				this.repeat, this.repeatGap, this.repeatStart, this.rotate);
	}

	@Override
	public void scaleStrokeWidth(float scaleFactor) {
		// do nothing
	}

	@Override
	public void scaleTextSize(float scaleFactor) {
		// do nothing
	}
}
