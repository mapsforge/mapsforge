/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2016 devemux86
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
import org.mapsforge.core.model.LineSegment;
import org.mapsforge.core.model.LineString;
import org.mapsforge.core.model.Point;

public class WayTextContainer extends MapElementContainer {
    private final Paint paintFront;
    private final Paint paintBack;
    private final String text;
    private final LineString lineString;
    private final GraphicFactory graphicFactory;

    public WayTextContainer(GraphicFactory graphicFactory, LineString lineString, Display display, int priority, String text, Paint paintFront, Paint paintBack, double textHeight) {
        super(lineString.getSegment(0).start, display, priority);
        this.graphicFactory = graphicFactory;
        this.lineString = lineString;
        this.text = text;
        this.paintFront = paintFront;
        this.paintBack = paintBack;


        this.boundary = null;
        this.boundaryAbsolute = lineString.getBoundingRect();
        this.boundaryAbsolute.enlarge(0, textHeight, 0, textHeight);
    }

    private Path generatePath(Point origin) {
        LineSegment start = lineString.getSegment(0);
        // So text isn't upside down
        boolean doInvert = start.end.x <= start.start.x;
        Path textPath = graphicFactory.createPath();

        if (!doInvert) {
            Point startPt = start.start.offset(-origin.x, -origin.y);
            textPath.moveTo((float) startPt.x, (float) startPt.y);
            for (int i = 0; i < lineString.segmentCount(); i++) {
                LineSegment seg = lineString.getSegment(i);
                Point endPt = seg.end.offset(-origin.x, -origin.y);
                textPath.lineTo((float) endPt.x, (float) endPt.y);
            }
        } else {
            Point startPt = lineString.getSegment(lineString.segmentCount() - 1).end.offset(-origin.x, -origin.y);
            textPath.moveTo((float) startPt.x, (float) startPt.y);
            for (int i = lineString.segmentCount() - 1; i >= 0; i--) {
                LineSegment seg = lineString.getSegment(i);
                Point endPt = seg.start.offset(-origin.x, -origin.y);
                textPath.lineTo((float) endPt.x, (float) endPt.y);
            }
        }
        return textPath;
    }

    @Override
    public void draw(Canvas canvas, Point origin, Matrix matrix, Filter filter) {
        Path textPath = generatePath(origin);

        if (paintBack != null) {
            int color = this.paintBack.getColor();
            if (filter != Filter.NONE) {
                this.paintBack.setColor(GraphicUtils.filterColor(color, filter));
            }
            canvas.drawPathText(this.text, textPath, this.paintBack);
            if (filter != Filter.NONE) {
                this.paintBack.setColor(color);
            }
        }
        int color = this.paintFront.getColor();
        if (filter != Filter.NONE) {
            this.paintFront.setColor(GraphicUtils.filterColor(color, filter));
        }
        canvas.drawPathText(this.text, textPath, this.paintFront);
        if (filter != Filter.NONE) {
            this.paintFront.setColor(color);
        }
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
