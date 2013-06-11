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
 * Represents a round area on the map.
 */
public class Circle implements RenderInstruction {
	private final Paint fill;
	private final int level;
	private final float radius;
	private float renderRadius;
	private final boolean scaleRadius;
	private final Paint stroke;
	private final float strokeWidth;

	Circle(CircleBuilder circleBuilder) {
		this.fill = circleBuilder.fill;
		this.level = circleBuilder.level;
		this.radius = circleBuilder.radius.floatValue();
		this.scaleRadius = circleBuilder.scaleRadius;
		this.stroke = circleBuilder.stroke;
		this.strokeWidth = circleBuilder.strokeWidth;

		if (!this.scaleRadius) {
			this.renderRadius = this.radius;
			if (this.stroke != null) {
				this.stroke.setStrokeWidth(this.strokeWidth);
			}
		}
	}

	@Override
	public void renderNode(RenderCallback renderCallback, List<Tag> tags) {
		renderCallback.renderPointOfInterestCircle(this.renderRadius, this.fill, this.stroke, this.level);
	}

	@Override
	public void renderWay(RenderCallback renderCallback, List<Tag> tags) {
		// do nothing
	}

	@Override
	public void scaleStrokeWidth(float scaleFactor) {
		if (this.scaleRadius) {
			this.renderRadius = this.radius * scaleFactor;
			if (this.stroke != null) {
				this.stroke.setStrokeWidth(this.strokeWidth * scaleFactor);
			}
		}
	}

	@Override
	public void scaleTextSize(float scaleFactor) {
		// do nothing
	}
}
