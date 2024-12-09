/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2016 devemux86
 * Copyright 2019 Adrian Batzill
 * Copyright 2024 Sublimis
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

import org.mapsforge.core.graphics.*;
import org.mapsforge.core.model.LineSegment;
import org.mapsforge.core.model.LineString;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Rectangle;
import org.mapsforge.core.model.Rotation;

public class WayTextContainer extends MapElementContainer {
    private final GraphicFactory graphicFactory;
    protected final Rectangle boundary;
    private final LineString lineString;
    private final Paint paintFront;
    private final Paint paintBack;
    private final String text;
    private final TextOrientation textOrientation;

    public WayTextContainer(GraphicFactory graphicFactory, LineString lineString, Display display, int priority, String text, Paint paintFront, Paint paintBack, double textHeight, TextOrientation textOrientation) {
        super(lineString.segments.get(0).start, display, priority);
        this.graphicFactory = graphicFactory;
        this.lineString = lineString;
        this.text = text;
        this.paintFront = paintFront;
        this.paintBack = paintBack;

        // a way text container should always run left to right, but I leave this in because it might matter
        // if we support right-to-left text.
        // we also need to make the container larger by textHeight as otherwise the end points do
        // not correctly reflect the size of the text on screen
        this.boundaryAbsolute = lineString.getBounds().enlarge(textHeight / 2d, textHeight / 2d, textHeight / 2d, textHeight / 2d);
        this.boundary = this.boundaryAbsolute.shift(new Point(-this.xy.x, -this.xy.y));
        this.textOrientation = textOrientation;
    }

    @Override
    protected Rectangle getBoundary() {
        return boundary;
    }

    @Override
    public void draw(Canvas canvas, Point origin, Matrix matrix, Rotation rotation) {
        Path path = generatePath(origin, rotation);

        if (this.paintBack != null) {
            canvas.drawPathText(this.text, path, this.paintBack);
        }
        canvas.drawPathText(this.text, path, this.paintFront);
    }

    private Path generatePath(Point origin, Rotation rotation) {
        // compute rotation so text isn't upside down
        LineSegment firstSegment = this.lineString.segments.get(0);
        LineSegment lastSegment = this.lineString.segments.get(this.lineString.segments.size() - 1);
        boolean doInvert;
        switch (textOrientation) {
            case AUTO_DOWN:
                if (!Rotation.noRotation(rotation)) {
                    Point startRotated = rotation.rotate(firstSegment.start, true);
                    Point endRotated = rotation.rotate(lastSegment.end, true);
                    doInvert = endRotated.x > startRotated.x;
                } else {
                    doInvert = lastSegment.end.x > firstSegment.start.x;
                }
                break;
            case RIGHT:
                // TODO Rotation
                doInvert = false;
                break;
            case LEFT:
                // TODO Rotation
                doInvert = true;
                break;
            default: // AUTO
                if (!Rotation.noRotation(rotation)) {
                    Point startRotated = rotation.rotate(firstSegment.start, true);
                    Point endRotated = rotation.rotate(lastSegment.end, true);
                    doInvert = endRotated.x <= startRotated.x;
                } else {
                    doInvert = lastSegment.end.x <= firstSegment.start.x;
                }
                break;
        }

        // draw text based on the orientation requirement
        Path path = this.graphicFactory.createPath();
        if (!doInvert) {
            Point start = firstSegment.start.offset(-origin.x, -origin.y);
            path.moveTo((float) start.x, (float) start.y);
            for (int i = 0; i < this.lineString.segments.size(); i++) {
                LineSegment segment = this.lineString.segments.get(i);
                Point end = segment.end.offset(-origin.x, -origin.y);
                path.lineTo((float) end.x, (float) end.y);
            }
        } else {
            Point end = this.lineString.segments.get(this.lineString.segments.size() - 1).end.offset(-origin.x, -origin.y);
            path.moveTo((float) end.x, (float) end.y);
            for (int i = this.lineString.segments.size() - 1; i >= 0; i--) {
                LineSegment segment = this.lineString.segments.get(i);
                Point start = segment.start.offset(-origin.x, -origin.y);
                path.lineTo((float) start.x, (float) start.y);
            }
        }
        return path;
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
