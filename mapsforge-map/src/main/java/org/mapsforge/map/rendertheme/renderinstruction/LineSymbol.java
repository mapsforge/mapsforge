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
package org.mapsforge.map.rendertheme.renderinstruction;

import java.util.List;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.model.Tag;
import org.mapsforge.map.rendertheme.RenderCallback;

/**
 * Represents an icon along a polyline on the map.
 */
public class LineSymbol implements RenderInstruction {
	private final boolean alignCenter;
	private final Bitmap bitmap;
	private final boolean repeat;

	LineSymbol(LineSymbolBuilder lineSymbolBuilder) {
		this.alignCenter = lineSymbolBuilder.alignCenter;
		this.bitmap = lineSymbolBuilder.bitmap;
		this.repeat = lineSymbolBuilder.repeat;
	}

	@Override
	public void destroy() {
		this.bitmap.decrementRefCount();
	}

	@Override
	public void renderNode(RenderCallback renderCallback, List<Tag> tags) {
		// do nothing
	}

	@Override
	public void renderWay(RenderCallback renderCallback, List<Tag> tags) {
		renderCallback.renderWaySymbol(this.bitmap, this.alignCenter, this.repeat);
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
