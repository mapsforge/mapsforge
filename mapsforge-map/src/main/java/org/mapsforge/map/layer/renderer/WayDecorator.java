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

import java.util.List;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.model.Point;

final class WayDecorator {

	/**
	 * Minimum distance in pixels before the symbol is repeated.
	 */
	private static final int DISTANCE_BETWEEN_SYMBOLS = 200;

	/**
	 * Minimum distance in pixels before the way name is repeated.
	 */
	private static final int DISTANCE_BETWEEN_WAY_NAMES = 500;

	/**
	 * Distance in pixels to skip from both ends of a segment.
	 */
	private static final int SEGMENT_SAFETY_DISTANCE = 30;



	/**
	 * Computes a polyline with distance dy parallel to given coordinates.
	 * http://objectmix.com/graphics/132987-draw-parallel-polyline-algorithm-needed.html
	 */
	static Point[] parallelPath(Point[][] coordinates, double dy) {
		Point[] p = coordinates[0];
		int n = p.length - 1;
		Point[] u = new Point[n];
		Point[] h = new Point[p.length];

		// Generate an array U[] of unity vectors of each direction
		for (int k = 0; k < n; ++k) {
			double c = p[k + 1].x - p[k].x;
			double s = p[k + 1].y - p[k].y;
			double l = Math.sqrt(c * c + s * s);
			u[k] = new Point(c / l, s / l);
		}

		// For the start point calculate the normal
		h[0] = new Point(p[0].x - dy * u[0].y, p[0].y + dy * u[0].x);

		// For 1 to N-1 calculate the intersection of the offset lines
		for (int k = 1; k < n; k++) {
			double l = dy / (1 + u[k].x * u[k - 1].x + u[k].y * u[k - 1].y);
			h[k] = new Point(p[k].x - l * (u[k].y + u[k - 1].y), p[k].y + l * (u[k].x + u[k - 1].x));
		}

		// For the end point use the normal
		h[n] = new Point(p[n].x - dy * u[n - 1].y, p[n].y + dy * u[n - 1].x);

		return h;
	}

	static void renderSymbol(Bitmap symbolBitmap, boolean alignCenter, boolean repeatSymbol, Point[][] coordinates,
			List<SymbolContainer> waySymbols) {
		int skipPixels = SEGMENT_SAFETY_DISTANCE;

		// get the first way point coordinates
		double previousX = coordinates[0][0].x;
		double previousY = coordinates[0][0].y;

		// draw the symbol on each way segment
		float segmentLengthRemaining;
		float segmentSkipPercentage;
		float theta;
		for (int i = 1; i < coordinates[0].length; ++i) {
			// get the current way point coordinates
			double currentX = coordinates[0][i].x;
			double currentY = coordinates[0][i].y;

			// calculate the length of the current segment (Euclidian distance)
			double diffX = currentX - previousX;
			double diffY = currentY - previousY;
			double segmentLengthInPixel = Math.sqrt(diffX * diffX + diffY * diffY);
			segmentLengthRemaining = (float) segmentLengthInPixel;

			while (segmentLengthRemaining - skipPixels > SEGMENT_SAFETY_DISTANCE) {
				// calculate the percentage of the current segment to skip
				segmentSkipPercentage = skipPixels / segmentLengthRemaining;

				// move the previous point forward towards the current point
				previousX += diffX * segmentSkipPercentage;
				previousY += diffY * segmentSkipPercentage;
				theta = (float) Math.atan2(currentY - previousY, currentX - previousX);

				Point point = new Point(previousX, previousY);
				waySymbols.add(new SymbolContainer(symbolBitmap, point, alignCenter, theta));

				// check if the symbol should only be rendered once
				if (!repeatSymbol) {
					return;
				}

				// recalculate the distances
				diffX = currentX - previousX;
				diffY = currentY - previousY;

				// recalculate the remaining length of the current segment
				segmentLengthRemaining -= skipPixels;

				// set the amount of pixels to skip before repeating the symbol
				skipPixels = DISTANCE_BETWEEN_SYMBOLS;
			}

			skipPixels -= segmentLengthRemaining;
			if (skipPixels < SEGMENT_SAFETY_DISTANCE) {
				skipPixels = SEGMENT_SAFETY_DISTANCE;
			}

			// set the previous way point coordinates for the next loop
			previousX = currentX;
			previousY = currentY;
		}
	}

	static void renderText(String textKey, float dy, Paint fill, Paint stroke, Point[][] coordinates,
			List<WayTextContainer> wayNames) {
		// calculate the way name length plus some margin of safety
		int wayNameWidth = fill.getTextWidth(textKey) + 10;

		int skipPixels = 0;

		Point[] c;
		if (dy == 0f) {
			c = coordinates[0];
		} else {
			c = parallelPath(coordinates, dy);
		}

		// get the first way point coordinates
		double previousX = c[0].x;
		double previousY = c[0].y;

		// find way segments long enough to draw the way name on them
		for (int i = 1; i < c.length; ++i) {
			// get the current way point coordinates
			double currentX = c[i].x;
			double currentY = c[i].y;

			// calculate the length of the current segment (Euclidian distance)
			double diffX = currentX - previousX;
			double diffY = currentY - previousY;
			double segmentLengthInPixel = Math.sqrt(diffX * diffX + diffY * diffY);

			if (skipPixels > 0) {
				skipPixels -= segmentLengthInPixel;
			} else if (segmentLengthInPixel > wayNameWidth) {
				int x1;
				int x2;
				int y1;
				int y2;

				// check to prevent inverted way names
				if (previousX <= currentX) {
					x1 = (int) previousX;
					y1 = (int) previousY;
					x2 = (int) currentX;
					y2 = (int) currentY;
				} else {
					x1 = (int) currentX;
					y1 = (int) currentY;
					x2 = (int) previousX;
					y2 = (int) previousY;
				}

				wayNames.add(new WayTextContainer(x1, y1, x2, y2, textKey, fill));
				if (stroke != null) {
					wayNames.add(new WayTextContainer(x1, y1, x2, y2, textKey, stroke));
				}

				skipPixels = DISTANCE_BETWEEN_WAY_NAMES;
			}

			// store the previous way point coordinates
			previousX = currentX;
			previousY = currentY;
		}
	}

	private WayDecorator() {
		throw new IllegalStateException();
	}
}
