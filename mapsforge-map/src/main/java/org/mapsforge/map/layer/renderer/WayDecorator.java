/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2019 Adrian Batzill
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

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Display;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.mapelements.MapElementContainer;
import org.mapsforge.core.mapelements.SymbolContainer;
import org.mapsforge.core.mapelements.WayTextContainer;
import org.mapsforge.core.model.*;

import java.util.List;

final class WayDecorator {

    private static final double MAX_LABEL_CORNER_ANGLE = 45;

    static void renderSymbol(Bitmap symbolBitmap, Display display, int priority, float dy, Rectangle boundary,
                             boolean repeatSymbol, float repeatGap, float repeatStart,
                             boolean rotate, Point[][] coordinates,
                             List<MapElementContainer> currentItems) {
        int skipPixels = (int) repeatStart;

        Point[] c;
        if (dy == 0f) {
            c = coordinates[0];
        } else {
            c = RendererUtils.parallelPath(coordinates[0], dy);
        }

        // get the first way point coordinates
        double previousX = c[0].x;
        double previousY = c[0].y;

        // draw the symbolContainer on each way segment
        float segmentLengthRemaining;
        float segmentSkipPercentage;
        float theta = 0;


        for (int i = 1; i < c.length; ++i) {
            // get the current way point coordinates
            double currentX = c[i].x;
            double currentY = c[i].y;

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

                currentItems.add(new SymbolContainer(point, display, priority, boundary, symbolBitmap, theta));

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
                skipPixels = (int) repeatGap;
            }

            skipPixels -= segmentLengthRemaining;
            if (skipPixels < repeatStart) {
                skipPixels = (int) repeatStart;
            }

            // set the previous way point coordinates for the next loop
            previousX = currentX;
            previousY = currentY;
        }
    }

    /**
     * Finds the segments of a line along which a name can be drawn and then adds WayTextContainers
     * to the list of drawable items.
     *
     * @param upperLeft     the tile in the upper left corner of the drawing pane
     * @param lowerRight    the tile in the lower right corner of the drawing pane
     * @param text          the text to draw
     * @param priority      priority of the text
     * @param dy            if 0, then a line  parallel to the coordinates will be calculated first
     * @param fill          fill paint for text
     * @param stroke        stroke paint for text
     * @param coordinates   the list of way coordinates
     * @param currentLabels the list of labels to which a new WayTextContainer will be added
     */
    static void renderText(GraphicFactory graphicFactory, Tile upperLeft, Tile lowerRight, String text, Display display, int priority, float dy,
                           Paint fill, Paint stroke,
                           boolean repeat, float repeatGap, float repeatStart, boolean rotate, Point[][] coordinates,
                           List<MapElementContainer> currentLabels) {
        if (coordinates.length == 0) {
            return;
        }

        Point[] c;
        if (dy == 0f) {
            c = coordinates[0];
        } else {
            c = RendererUtils.parallelPath(coordinates[0], dy);
        }

        if (c.length < 2) {
            return;
        }

        LineString path = new LineString();
        for (int i = 1; i < c.length; i++) {
            LineSegment segment = new LineSegment(c[i - 1], c[i]);
            path.segments.add(segment);
        }

        int textWidth = (stroke == null) ? fill.getTextWidth(text) : stroke.getTextWidth(text);
        int textHeight = (stroke == null) ? fill.getTextHeight(text) : stroke.getTextHeight(text);

        double pathLength = path.length();

        for (float pos = repeatStart; pos + textWidth < pathLength; pos += repeatGap + textWidth) {
            LineString linePart = path.extractPart(pos, pos + textWidth);

            boolean tooSharp = false;
            for (int i = 1; i < linePart.segments.size(); i++) {
                double cornerAngle = linePart.segments.get(i - 1).angleTo(linePart.segments.get(i));
                if (Math.abs(cornerAngle) > MAX_LABEL_CORNER_ANGLE) {
                    tooSharp = true;
                    break;
                }
            }
            if (tooSharp)
                continue;

            currentLabels.add(new WayTextContainer(graphicFactory, linePart, display, priority, text, fill, stroke, textHeight));
        }
    }

    private WayDecorator() {
        throw new IllegalStateException();
    }
}
