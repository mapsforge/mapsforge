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


import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.Display;
import org.mapsforge.core.graphics.Matrix;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Rectangle;

public class WayTextContainer extends MapElementContainer {

	private final Paint paintFront;
	private final Paint paintBack;
	private final String text;
	private final Point end;

	public WayTextContainer(Point point, Point end, Display display, int priority, String text, Paint paintFront, Paint paintBack, double textHeight) {
		super(point, display, priority);
		this.text = text;
		this.paintFront = paintFront;
		this.paintBack = paintBack;
		this.end = end;

		this.boundary = null;
		// a way text container should always run left to right, but I leave this in because it might matter
		// if we support right-to-left text.
		// we also need to make the container larger by textHeight as otherwise the end points do
		// not correctly reflect the size of the text on screen
		this.boundaryAbsolute = new Rectangle(Math.min(point.x, end.x), Math.min(point.y, end.y),
				Math.max(point.x, end.x), Math.max(point.y, end.y)).envelope(textHeight/2d);
	}

	@Override
	public void draw(Canvas canvas, Point origin, Matrix matrix) {
		Point adjustedStart = xy.offset(-origin.x, -origin.y);
		Point adjustedEnd = end.offset(-origin.x, -origin.y);

		if (this.paintBack != null) {
			canvas.drawTextRotated(text, (int) (adjustedStart.x),
				(int) (adjustedStart.y),
				(int) (adjustedEnd.x),
				(int) (adjustedEnd.y), this.paintBack);
		}
		canvas.drawTextRotated(text, (int) (adjustedStart.x),
				(int) (adjustedStart.y),
				(int) (adjustedEnd.x),
				(int) (adjustedEnd.y), this.paintFront);
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
