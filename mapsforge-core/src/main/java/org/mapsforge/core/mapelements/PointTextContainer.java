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
package org.mapsforge.core.mapelements;

import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Position;
import org.mapsforge.core.model.Point;

abstract public class PointTextContainer extends MapElementContainer {

	public final boolean isVisible;
	public final int maxTextWidth;
	public final Paint paintBack;
	public final Paint paintFront;
	public final Position position;
	public final SymbolContainer symbolContainer;
	public final String text;
	public final int textHeight;
	public final int textWidth;

	/**
	 * Create a new point container, that holds the x-y coordinates of a point, a text variable, two paint objects, and
	 * a reference on a symbolContainer, if the text is connected with a POI.
	 */
	protected PointTextContainer(Point point, int priority, String text, Paint paintFront, Paint paintBack,
	                             SymbolContainer symbolContainer, Position position, int maxTextWidth) {
		super(point, priority);

		this.maxTextWidth = maxTextWidth;
		this.text = text;
		this.symbolContainer = symbolContainer;
		this.paintFront = paintFront;
		this.paintBack = paintBack;
		this.position = position;
		if (paintBack != null) {
			this.textWidth = paintBack.getTextWidth(text);
			this.textHeight = paintBack.getTextHeight(text);
		} else {
			this.textWidth = paintFront.getTextWidth(text);
			this.textHeight = paintFront.getTextHeight(text);
		}
		this.isVisible = !this.paintFront.isTransparent() || (this.paintBack != null && !this.paintBack.isTransparent());
	}


	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(super.toString());
		stringBuilder.append(", text=");
		stringBuilder.append(this.text);
		return stringBuilder.toString();
	}
}
