/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2016 devemux86
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
package org.mapsforge.core.mapelements;

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.Display;
import org.mapsforge.core.graphics.Filter;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.GraphicUtils;
import org.mapsforge.core.graphics.Matrix;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Path;
import org.mapsforge.core.graphics.TextOrientation;
import org.mapsforge.core.model.LineSegment;
import org.mapsforge.core.model.LineString;
import org.mapsforge.core.model.Point;

public class WayTextContainer extends MapElementContainer {
    private final GraphicFactory graphicFactory;
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

        this.boundary = null;
        // a way text container should always run left to right, but I leave this in because it might matter
        // if we support right-to-left text.
        // we also need to make the container larger by textHeight as otherwise the end points do
        // not correctly reflect the size of the text on screen
        this.boundaryAbsolute = lineString.getBounds().enlarge(textHeight / 2d, textHeight / 2d, textHeight / 2d, textHeight / 2d);
        this.textOrientation = textOrientation;
    }

    @Override
    public void draw(Canvas canvas, Point origin, Matrix matrix, Filter filter) {
        Path path = generatePath(origin);

        if (this.paintBack != null) {
            int color = this.paintBack.getColor();
            if (filter != Filter.NONE) {
                this.paintBack.setColor(GraphicUtils.filterColor(color, filter));
            }
            canvas.drawPathText(this.text, path, this.paintBack);
            if (filter != Filter.NONE) {
                this.paintBack.setColor(color);
            }
        }
        int color = this.paintFront.getColor();
        if (filter != Filter.NONE) {
            this.paintFront.setColor(GraphicUtils.filterColor(color, filter));
        }
        canvas.drawPathText(this.text, path, this.paintFront);
        if (filter != Filter.NONE) {
            this.paintFront.setColor(color);
        }
    }

    private Path generatePath(Point origin) {
        // compute rotation so text isn't upside down
        LineSegment firstSegment = this.lineString.segments.get(0);
        boolean doInvert;
        switch (textOrientation) {
            case AUTO_DOWN:
                doInvert = firstSegment.end.x > firstSegment.start.x;
                break;
            case RIGHT:
                doInvert = false;
                break;
            case LEFT:
                doInvert = true;
                break;
            default: // AUTO
                doInvert = firstSegment.end.x <= firstSegment.start.x;
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
