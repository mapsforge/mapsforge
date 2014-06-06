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
import java.util.Set;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.mapelements.MapElementContainer;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.mapelements.SymbolContainer;
import org.mapsforge.core.mapelements.WayTextContainer;
import org.mapsforge.core.model.Point;

final class WayDecorator {

	/**
	 * Minimum distance in pixels before the way name is repeated.
	 */
	private static final int DISTANCE_BETWEEN_WAY_NAMES = 500;

	static void renderSymbol(Bitmap symbolBitmap, int priority, float dy, boolean alignCenter,
	                         boolean repeatSymbol, float repeatGap, float repeatStart,
	                         boolean rotate, List<List<Point>> coordinates,
			List<MapElementContainer> currentItems) {
		int skipPixels = (int)repeatStart;

		List<Point> c;
		if (dy == 0f) {
			c = coordinates.get(0);
		} else {
			c = RendererUtils.parallelPath(coordinates.get(0), dy);
		}

		// get the first way point coordinates
		double previousX = c.get(0).x;
		double previousY = c.get(0).y;

		// draw the symbolContainer on each way segment
		float segmentLengthRemaining;
		float segmentSkipPercentage;
		float theta = 0;


		for (int i = 1; i < c.size(); ++i) {
			// get the current way point coordinates
			double currentX = c.get(i).x;
			double currentY = c.get(i).y;

			// calculate the length of the current segment (Euclidian distance)
			double diffX = currentX - previousX;
			double diffY = currentY - previousY;
			double segmentLengthInPixel = Math.sqrt(diffX * diffX + diffY * diffY);
			segmentLengthRemaining = (float) segmentLengthInPixel;

			while (segmentLengthRemaining - skipPixels > repeatStart) {
				// calculate the percentage of the current segment to skip
				segmentSkipPercentage = skipPixels / segmentLengthRemaining;

				// move the previous point forward towards the current point
				previousX += diffX * segmentSkipPercentage;
				previousY += diffY * segmentSkipPercentage;
				if (rotate) {
					// if we do not rotate theta will be 0, which is correct
					theta = (float) Math.atan2(currentY - previousY, currentX - previousX);
				}

				Point point = new Point(previousX, previousY);

				currentItems.add(new SymbolContainer(point, priority, symbolBitmap, theta, alignCenter));

				// check if the symbolContainer should only be rendered once
				if (!repeatSymbol) {
					return;
				}

				// recalculate the distances
				diffX = currentX - previousX;
				diffY = currentY - previousY;

				// recalculate the remaining length of the current segment
				segmentLengthRemaining -= skipPixels;

				// set the amount of pixels to skip before repeating the symbolContainer
				skipPixels = (int)repeatGap;
			}

			skipPixels -= segmentLengthRemaining;
			if (skipPixels < repeatStart) {
				skipPixels = (int)repeatStart;
			}

			// set the previous way point coordinates for the next loop
			previousX = currentX;
			previousY = currentY;
		}
	}

	static void renderText(String text, int priority, float dy, Paint fill, Paint stroke, List<List<Point>> coordinates,
			Set<MapElementContainer> currentWayLabels) {
		// calculate the way name length plus some margin of safety
		int wayNameWidth = fill.getTextWidth(text) + 10;

		int skipPixels = 0;

		List<Point> c;
		if (dy == 0f) {
			c = coordinates.get(0);
		} else {
			c = RendererUtils.parallelPath(coordinates.get(0), dy);
		}

		// get the first way point coordinates
		double previousX = c.get(0).x;
		double previousY = c.get(0).y;

		// find way segments long enough to draw the way name on them
		for (int i = 1; i < c.size(); ++i) {
			// get the current way point coordinates
			double currentX = c.get(i).x;
			double currentY = c.get(i).y;

			// calculate the length of the current segment (Euclidian distance)
			double diffX = currentX - previousX;
			double diffY = currentY - previousY;
			double segmentLengthInPixel = Math.sqrt(diffX * diffX + diffY * diffY);

			if (skipPixels > 0) {
				skipPixels -= segmentLengthInPixel;
			} else if (segmentLengthInPixel > wayNameWidth) {

				Point start;
				Point end;

				// check to prevent inverted way names
				if (previousX <= currentX) {
					start = new Point(previousX, previousY);
					end = new Point(currentX, currentY);
				} else {
					start = new Point(currentX, currentY);
					end = new Point(previousX, previousY);
				}
				currentWayLabels.add(new WayTextContainer(start, end, priority, text, fill, stroke));

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
