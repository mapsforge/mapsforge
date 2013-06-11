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
package org.mapsforge.map.layer.renderer;

import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.model.Rectangle;

class PointTextContainer {
	final Rectangle boundary;
	final Paint paintBack;
	final Paint paintFront;
	SymbolContainer symbol;
	final String text;
	double x;
	double y;

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
	PointTextContainer(String text, double x, double y, Paint paintFront) {
		this.text = text;
		this.x = x;
		this.y = y;
		this.paintFront = paintFront;
		this.paintBack = null;

		this.boundary = new Rectangle(0, 0, paintFront.getTextWidth(text), paintFront.getTextHeight(text));
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
	PointTextContainer(String text, double x, double y, Paint paintFront, Paint paintBack) {
		this.text = text;
		this.x = x;
		this.y = y;
		this.paintFront = paintFront;
		this.paintBack = paintBack;

		if (paintBack != null) {
			paintBack.getTextHeight(text);
			paintBack.getTextWidth(text);
			this.boundary = new Rectangle(0, 0, paintBack.getTextWidth(text), paintBack.getTextHeight(text));
		} else {
			this.boundary = new Rectangle(0, 0, paintFront.getTextWidth(text), paintFront.getTextHeight(text));
		}
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
	PointTextContainer(String text, double x, double y, Paint paintFront, Paint paintBack, SymbolContainer symbol) {
		this.text = text;
		this.x = x;
		this.y = y;
		this.paintFront = paintFront;
		this.paintBack = paintBack;
		this.symbol = symbol;

		if (paintBack != null) {
			this.boundary = new Rectangle(0, 0, paintBack.getTextWidth(text), paintBack.getTextHeight(text));
		} else {
			this.boundary = new Rectangle(0, 0, paintFront.getTextWidth(text), paintFront.getTextHeight(text));
		}
	}
}
