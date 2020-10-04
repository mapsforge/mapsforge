/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014-2015 Ludwig M Brinckmann
 * Copyright 2014-2019 devemux86
 * Copyright 2020 Adrian Batzill
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

import org.mapsforge.core.graphics.*;
import org.mapsforge.core.model.Rectangle;
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
 * Represents a text label on the map.
 * <p/>
 * If a bitmap symbol is present the caption position is calculated relative to the bitmap, the
 * center of which is at the point of the POI. The bitmap itself is never rendered.
 */
public class Caption extends RenderInstruction {
    public static final float DEFAULT_GAP = 5f;

    private Rectangle boundary;
    private Display display;
    private float dy;
    private final Map<Byte, Float> dyScaled;
    private final Paint fill;
    private final Map<Byte, Paint> fills;
    private float fontSize;
    private final float gap;
    private final int maxTextWidth;
    private Position position;
    private int priority;
    private final Paint stroke;
    private final Map<Byte, Paint> strokes;
    private String symbolId;
    private TextKey textKey;

    public Caption(GraphicFactory graphicFactory, DisplayModel displayModel, String elementName,
                   XmlPullParser pullParser, Map<String, Symbol> symbols) throws XmlPullParserException {
        super(graphicFactory, displayModel);
        this.fill = graphicFactory.createPaint();
        this.fill.setColor(Color.BLACK);
        this.fill.setStyle(Style.FILL);
        this.fills = new HashMap<>();

        this.stroke = graphicFactory.createPaint();
        this.stroke.setColor(Color.BLACK);
        this.stroke.setStyle(Style.STROKE);
        this.strokes = new HashMap<>();
        this.dyScaled = new HashMap<>();


        this.display = Display.IFSPACE;

        this.gap = DEFAULT_GAP * displayModel.getScaleFactor();

        extractValues(graphicFactory, displayModel, elementName, pullParser);

        if (this.symbolId != null) {
            Symbol symbol = symbols.get(this.symbolId);
            if (symbol != null) {
                this.boundary = symbol.getBoundary();
            }
        }

        if (this.position == null) {
            // sensible defaults: below if symbolContainer is present, center if not
            if (this.boundary == null) {
                this.position = Position.CENTER;
            } else {
                this.position = Position.BELOW;
            }
        }
        switch (this.position) {
            case CENTER:
            case BELOW:
            case ABOVE:
                this.stroke.setTextAlign(Align.CENTER);
                this.fill.setTextAlign(Align.CENTER);
                break;
            case BELOW_LEFT:
            case ABOVE_LEFT:
            case LEFT:
                this.stroke.setTextAlign(Align.RIGHT);
                this.fill.setTextAlign(Align.RIGHT);
                break;
            case BELOW_RIGHT:
            case ABOVE_RIGHT:
            case RIGHT:
                this.stroke.setTextAlign(Align.LEFT);
                this.fill.setTextAlign(Align.LEFT);
                break;
            default:
                throw new IllegalArgumentException("Position invalid");
        }


        this.maxTextWidth = displayModel.getMaxTextWidth();

    }

    private float computeHorizontalOffset() {
        // compute only the offset required by the bitmap, not the text size,
        // because at this point we do not know the text boxing
        if (this.position.isOnRightSide()) {
            return (float) (this.boundary.right + this.gap);
        } else if (this.position.isOnLeftSide()) {
            return (float) (this.boundary.left - this.gap);
        }
        return (float) ((this.boundary.right + this.boundary.left) / 2);
    }

    private float computeVerticalOffset(byte zoomLevel) {
        float verticalOffset = this.dyScaled.get(zoomLevel);

        if (this.position.isOnUpperSide()) {
            verticalOffset += this.boundary.top - this.gap;
        } else if (this.position.isOnLowerSide()) {
            verticalOffset += this.boundary.bottom + this.gap;
        } else {
            verticalOffset += (this.boundary.top + this.boundary.bottom) / 2;
        }
        return verticalOffset;
    }

    @Override
    public void destroy() {
        // no-op
    }

    private void extractValues(GraphicFactory graphicFactory, DisplayModel displayModel, String elementName,
                               XmlPullParser pullParser) throws XmlPullParserException {
        FontFamily fontFamily = FontFamily.DEFAULT;
        FontStyle fontStyle = FontStyle.NORMAL;

        for (int i = 0; i < pullParser.getAttributeCount(); ++i) {
            String name = pullParser.getAttributeName(i);
            String value = pullParser.getAttributeValue(i);

            if (K.equals(name)) {
                this.textKey = TextKey.getInstance(value);
            } else if (CAT.equals(name)) {
                this.category = value;
            } else if (DISPLAY.equals(name)) {
                this.display = Display.fromString(value);
            } else if (DY.equals(name)) {
                this.dy = Float.parseFloat(value) * displayModel.getScaleFactor();
            } else if (FILL.equals(name)) {
                this.fill.setColor(XmlUtils.getColor(graphicFactory, value, displayModel.getThemeCallback(), this));
            } else if (FONT_FAMILY.equals(name)) {
                fontFamily = FontFamily.fromString(value);
            } else if (FONT_SIZE.equals(name)) {
                this.fontSize = XmlUtils.parseNonNegativeFloat(name, value) * displayModel.getScaleFactor();
            } else if (FONT_STYLE.equals(name)) {
                fontStyle = FontStyle.fromString(value);
            } else if (POSITION.equals(name)) {
                this.position = Position.fromString(value);
            } else if (PRIORITY.equals(name)) {
                this.priority = Integer.parseInt(value);
            } else if (STROKE.equals(name)) {
                this.stroke.setColor(XmlUtils.getColor(graphicFactory, value, displayModel.getThemeCallback(), this));
            } else if (STROKE_WIDTH.equals(name)) {
                this.stroke.setStrokeWidth(XmlUtils.parseNonNegativeFloat(name, value) * displayModel.getScaleFactor());
            } else if (SYMBOL_ID.equals(name)) {
                this.symbolId = value;
            } else {
                throw XmlUtils.createXmlPullParserException(elementName, name, value, i);
            }
        }

        this.fill.setTypeface(fontFamily, fontStyle);
        this.stroke.setTypeface(fontFamily, fontStyle);

        XmlUtils.checkMandatoryAttribute(elementName, K, this.textKey);
    }

    private Paint getFillPaint(byte zoomLevel) {
        Paint paint = fills.get(zoomLevel);
        if (paint == null) {
            paint = this.fill;
        }
        return paint;
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
        if (Display.NEVER == this.display) {
            return;
        }

        String caption = this.textKey.getValue(poi.tags);
        if (caption == null) {
            return;
        }

        float horizontalOffset = 0f;

        Float verticalOffset = this.dyScaled.get(renderContext.rendererJob.tile.zoomLevel);
        if (verticalOffset == null) {
            verticalOffset = this.dy;
        }

        if (this.boundary != null) {
            horizontalOffset = computeHorizontalOffset();
            verticalOffset = computeVerticalOffset(renderContext.rendererJob.tile.zoomLevel);
        }

        renderCallback.renderPointOfInterestCaption(renderContext, this.display, this.priority, caption, horizontalOffset, verticalOffset,
                getFillPaint(renderContext.rendererJob.tile.zoomLevel), getStrokePaint(renderContext.rendererJob.tile.zoomLevel), this.position, this.maxTextWidth, poi);
    }

    @Override
    public void renderWay(RenderCallback renderCallback, final RenderContext renderContext, PolylineContainer way) {
        if (Display.NEVER == this.display) {
            return;
        }

        String caption = this.textKey.getValue(way.getTags());
        if (caption == null) {
            return;
        }

        float horizontalOffset = 0f;
        Float verticalOffset = this.dyScaled.get(renderContext.rendererJob.tile.zoomLevel);
        if (verticalOffset == null) {
            verticalOffset = this.dy;
        }

        if (this.boundary != null) {
            horizontalOffset = computeHorizontalOffset();
            verticalOffset = computeVerticalOffset(renderContext.rendererJob.tile.zoomLevel);
        }

        renderCallback.renderAreaCaption(renderContext, this.display, this.priority, caption, horizontalOffset, verticalOffset,
                getFillPaint(renderContext.rendererJob.tile.zoomLevel), getStrokePaint(renderContext.rendererJob.tile.zoomLevel), this.position, this.maxTextWidth, way);
    }

    @Override
    public void scaleStrokeWidth(float scaleFactor, byte zoomLevel) {
        // do nothing
    }

    @Override
    public void scaleTextSize(float scaleFactor, byte zoomLevel) {
        Paint f = graphicFactory.createPaint(this.fill);
        f.setTextSize(this.fontSize * scaleFactor);
        this.fills.put(zoomLevel, f);

        Paint s = graphicFactory.createPaint(this.stroke);
        s.setTextSize(this.fontSize * scaleFactor);
        this.strokes.put(zoomLevel, s);

        this.dyScaled.put(zoomLevel, this.dy * scaleFactor);
    }
}
