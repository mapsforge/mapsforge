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

import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.model.Tag;
import org.mapsforge.map.rendertheme.RenderCallback;

/**
 * Represents a polyline on the map.
 */
public class Line implements RenderInstruction {
	private final int level;
	private final Paint stroke;
	private final float strokeWidth;

	Line(LineBuilder lineBuilder) {
		this.level = lineBuilder.level;
		this.stroke = lineBuilder.stroke;
		this.strokeWidth = lineBuilder.strokeWidth;
	}

	@Override
	public void renderNode(RenderCallback renderCallback, List<Tag> tags) {
		// do nothing
	}

	@Override
	public void renderWay(RenderCallback renderCallback, List<Tag> tags) {
		renderCallback.renderWay(this.stroke, this.level);
	}

	@Override
	public void scaleStrokeWidth(float scaleFactor) {
		this.stroke.setStrokeWidth(this.strokeWidth * scaleFactor);
	}

	@Override
	public void scaleTextSize(float scaleFactor) {
		// do nothing
	}
}
