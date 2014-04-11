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
package org.mapsforge.core.graphics;

import org.mapsforge.core.model.Rectangle;

public class PointTextContainer {

	public final Paint paintBack;
	public final Paint paintFront;
	public final Position position;
	public SymbolContainer symbol;
	public final String text;
	public final int textHeight;
	public final int textWidth;
	public double x;
	public double y;

	/**
	 * Create a new point container, that holds the x-y coordinates of a point, a text variable and one paint objects.
	 * 
	 * @param text
	 *            the text of the point.
	 * @param x
	 *            the x coordinate of the point.
	 * @param y
	 *            the y coordinate of the point.
	 * @param paintFront
	 *            the paintFront for the point.
	 */
	public PointTextContainer(String text, double x, double y, Paint paintFront, Position position) {
		this(text, x, y, paintFront, null, null, position);
	}

	/**
	 * Create a new point container, that holds the x-y coordinates of a point, a text variable and two paint objects.
	 * 
	 * @param text
	 *            the text of the point.
	 * @param x
	 *            the x coordinate of the point.
	 * @param y
	 *            the y coordinate of the point.
	 * @param paintFront
	 *            the paintFront for the point.
	 * @param paintBack
	 *            the paintBack for the point.
	 */
	public PointTextContainer(String text, double x, double y, Paint paintFront, Paint paintBack, Position position) {
		this(text, x, y, paintFront, paintBack, null, position);
	}

	/**
	 * Create a new point container, that holds the x-y coordinates of a point, a text variable, two paint objects, and
	 * a reference on a symbol, if the text is connected with a POI.
	 * 
	 * @param text
	 *            the text of the point.
	 * @param x
	 *            the x coordinate of the point.
	 * @param y
	 *            the y coordinate of the point.
	 * @param paintFront
	 *            the paintFront for the point.
	 * @param paintBack
	 *            the paintBack for the point.
	 * @param symbol
	 *            the connected Symbol.
	 */
	public PointTextContainer(String text, double x, double y, Paint paintFront, Paint paintBack, SymbolContainer symbol, Position position) {
		this.text = text;
		this.symbol = symbol;
		this.x = x;
		this.y = y;
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
	}

	public Rectangle getBoundary(int maxTextWidth) {
		int lines = this.textWidth / maxTextWidth + 1;

		if (lines > 1) {
			return new Rectangle(0, 0, maxTextWidth, this.textHeight);
		}
		return new Rectangle(0, 0, this.textWidth, this.textHeight);
	}
}
