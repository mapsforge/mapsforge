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
 * Represents an icon on the map.
 */
public class Symbol extends RenderInstruction {
	private final Bitmap bitmap;
	private final String id;
	private final int priority;

	Symbol(SymbolBuilder symbolBuilder) {
		super(symbolBuilder.getCategory());
		this.bitmap = symbolBuilder.bitmap;
		this.id = symbolBuilder.id;
		this.priority = symbolBuilder.priority;
	}

	@Override
	public void destroy() {
		this.bitmap.decrementRefCount();
	}

	public Bitmap getBitmap() {
		return this.bitmap;
	}

	public String getId() {
		return this.id;
	}

	@Override
	public void renderNode(RenderCallback renderCallback, PointOfInterest poi, Tile tile) {
		renderCallback.renderPointOfInterestSymbol(poi, this.priority, this.bitmap, tile);
	}

	@Override
	public void renderWay(RenderCallback renderCallback, PolylineContainer way) {
		renderCallback.renderAreaSymbol(way, this.priority, this.bitmap);
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
