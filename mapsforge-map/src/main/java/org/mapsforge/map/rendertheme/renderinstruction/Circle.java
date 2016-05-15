/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014-2015 Ludwig M Brinckmann
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
package org.mapsforge.map.rendertheme.renderinstruction;

import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.map.datastore.PointOfInterest;
import org.mapsforge.map.layer.renderer.PolylineContainer;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.rendertheme.RenderCallback;
import org.mapsforge.map.rendertheme.RenderContext;
import org.mapsforge.map.rendertheme.XmlUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a round area on the map.
 */
public class Circle extends RenderInstruction {
    private final Paint fill;
    private final Map<Byte, Paint> fills;
    private final int level;
    private float radius;
    private float renderRadius;
    private final Map<Byte, Float> renderRadiusScaled;
    private boolean scaleRadius;
    private final Paint stroke;
    private final Map<Byte, Paint> strokes;
    private float strokeWidth;

    public Circle(GraphicFactory graphicFactory, DisplayModel displayModel, String elementName,
                  XmlPullParser pullParser, int level) throws XmlPullParserException {
        super(graphicFactory, displayModel);
        this.level = level;

        this.fill = graphicFactory.createPaint();
        this.fill.setColor(Color.TRANSPARENT);
        this.fill.setStyle(Style.FILL);
        this.fills = new HashMap<>();

        this.stroke = graphicFactory.createPaint();
        this.stroke.setColor(Color.TRANSPARENT);
        this.stroke.setStyle(Style.STROKE);
        this.strokes = new HashMap<>();
        this.renderRadiusScaled = new HashMap<>();

        extractValues(graphicFactory, displayModel, elementName, pullParser);

        if (!this.scaleRadius) {
            this.renderRadius = this.radius;
            this.stroke.setStrokeWidth(this.strokeWidth);
        }
    }

    @Override
    public void destroy() {
        // no-op
    }

    private void extractValues(GraphicFactory graphicFactory, DisplayModel displayModel, String elementName,
                               XmlPullParser pullParser) throws XmlPullParserException {
        for (int i = 0; i < pullParser.getAttributeCount(); ++i) {
            String name = pullParser.getAttributeName(i);
            String value = pullParser.getAttributeValue(i);

            if (RADIUS.equals(name) || (XmlUtils.supportOlderRenderThemes && R.equals(name))) {
                this.radius = XmlUtils.parseNonNegativeFloat(name, value) * displayModel.getScaleFactor();
            } else if (CAT.equals(name)) {
                this.category = value;
            } else if (FILL.equals(name)) {
                this.fill.setColor(XmlUtils.getColor(graphicFactory, value));
            } else if (SCALE_RADIUS.equals(name)) {
                this.scaleRadius = Boolean.parseBoolean(value);
            } else if (STROKE.equals(name)) {
                this.stroke.setColor(XmlUtils.getColor(graphicFactory, value));
            } else if (STROKE_WIDTH.equals(name)) {
                this.strokeWidth = XmlUtils.parseNonNegativeFloat(name, value) * displayModel.getScaleFactor();
            } else {
                throw XmlUtils.createXmlPullParserException(elementName, name, value, i);
            }
        }

        XmlUtils.checkMandatoryAttribute(elementName, RADIUS, this.radius);
    }

    private Paint getFillPaint(byte zoomLevel) {
        Paint paint = fills.get(zoomLevel);
        if (paint == null) {
            paint = this.fill;
        }
        return paint;
    }

    private float getRenderRadius(byte zoomLevel) {
        Float radius = renderRadiusScaled.get(zoomLevel);
        if (radius == null) {
            radius = this.renderRadius;
        }
        return radius;
    }

    private Paint getStrokePaint(byte zoomLevel) {
        Paint paint = strokes.get(zoomLevel);
        if (paint == null) {
            paint = this.stroke;
        }
        return paint;
    }

    @Override
    public void renderNode(RenderCallback renderCallback, final RenderContext renderContext, PointOfInterest poi) {
        renderCallback.renderPointOfInterestCircle(renderContext, getRenderRadius(renderContext.rendererJob.tile.zoomLevel), getFillPaint(renderContext.rendererJob.tile.zoomLevel), getStrokePaint(renderContext.rendererJob.tile.zoomLevel), this.level, poi);
    }

    @Override
    public void renderWay(RenderCallback renderCallback, final RenderContext renderContext, PolylineContainer way) {
        // do nothing
    }

    @Override
    public void scaleStrokeWidth(float scaleFactor, byte zoomLevel) {
        if (this.scaleRadius) {
            this.renderRadiusScaled.put(zoomLevel, this.radius * scaleFactor);
            if (this.stroke != null) {
                Paint paint = graphicFactory.createPaint(stroke);
                paint.setStrokeWidth(this.strokeWidth * scaleFactor);
                strokes.put(zoomLevel, paint);
            }
        }
    }

    @Override
    public void scaleTextSize(float scaleFactor, byte zoomLevel) {
        // do nothing
    }
}
