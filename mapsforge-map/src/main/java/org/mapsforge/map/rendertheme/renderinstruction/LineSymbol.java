/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014-2015 Ludwig M Brinckmann
 * Copyright 2014-2016 devemux86
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

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Display;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.Position;
import org.mapsforge.core.graphics.SymbolOrientation;
import org.mapsforge.core.model.Rectangle;
import org.mapsforge.map.datastore.PointOfInterest;
import org.mapsforge.map.layer.renderer.PolylineContainer;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.rendertheme.RenderCallback;
import org.mapsforge.map.rendertheme.RenderContext;
import org.mapsforge.map.rendertheme.XmlThemeResourceProvider;
import org.mapsforge.map.rendertheme.XmlUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents an icon along a polyline on the map.
 */
public class LineSymbol extends RenderInstruction {
    private static final float REPEAT_GAP_DEFAULT = 200f;
    private static final float REPEAT_START_DEFAULT = 30f;

    private Bitmap bitmap;
    private boolean bitmapInvalid;
    private Display display;
    private float dy;
    private final Map<Byte, Float> dyScaled;
    private Position position;
    private int priority;
    private final String relativePathPrefix;
    private boolean repeat;
    private float repeatGap;
    private float repeatStart;
    private final XmlThemeResourceProvider resourceProvider;
    private Scale scale = Scale.STROKE;
    private String src;
    private SymbolOrientation symbolOrientation;

    public LineSymbol(GraphicFactory graphicFactory, DisplayModel displayModel, String elementName,
                      XmlPullParser pullParser, String relativePathPrefix, XmlThemeResourceProvider resourceProvider) throws IOException, XmlPullParserException {
        super(graphicFactory, displayModel);

        this.display = Display.IFSPACE;
        this.relativePathPrefix = relativePathPrefix;
        this.resourceProvider = resourceProvider;
        this.dyScaled = new HashMap<>();
        // Probably not a good default, but backwards compatible
        this.position = Position.BELOW_RIGHT;
        this.symbolOrientation = SymbolOrientation.LEFT;

        extractValues(elementName, pullParser);
    }

    @Override
    public void destroy() {
        if (this.bitmap != null) {
            this.bitmap.decrementRefCount();
        }
    }

    @SuppressWarnings("deprecation")
    private void extractValues(String elementName, XmlPullParser pullParser) throws IOException, XmlPullParserException {
        this.repeatGap = REPEAT_GAP_DEFAULT * displayModel.getScaleFactor();
        this.repeatStart = REPEAT_START_DEFAULT * displayModel.getScaleFactor();

        for (int i = 0; i < pullParser.getAttributeCount(); ++i) {
            String name = pullParser.getAttributeName(i);
            String value = pullParser.getAttributeValue(i);

            if (SRC.equals(name)) {
                this.src = value;
            } else if (ALIGN_CENTER.equals(name)) {
                // Deprecated way to say position="center"
                boolean alignCenter = Boolean.parseBoolean(value);
                if (alignCenter) {
                    this.position = Position.CENTER;
                }
            } else if (CAT.equals(name)) {
                this.category = value;
            } else if (DISPLAY.equals(name)) {
                this.display = Display.fromString(value);
            } else if (DY.equals(name)) {
                this.dy = Float.parseFloat(value) * displayModel.getScaleFactor();
            } else if (PRIORITY.equals(name)) {
                this.priority = Integer.parseInt(value);
            } else if (REPEAT.equals(name)) {
                this.repeat = Boolean.parseBoolean(value);
            } else if (REPEAT_GAP.equals(name)) {
                this.repeatGap = Float.parseFloat(value) * displayModel.getScaleFactor();
            } else if (REPEAT_START.equals(name)) {
                this.repeatStart = Float.parseFloat(value) * displayModel.getScaleFactor();
            } else if (ROTATE.equals(name)) {
                if (!Boolean.parseBoolean(value)) {
                    this.symbolOrientation = SymbolOrientation.UP;
                }
            } else if (SCALE.equals(name)) {
                this.scale = scaleFromValue(value);
            } else if (SYMBOL_HEIGHT.equals(name)) {
                this.height = XmlUtils.parseNonNegativeInteger(name, value) * displayModel.getScaleFactor();
            } else if (SYMBOL_ORIENTATION.equals(name)) {
                this.symbolOrientation = SymbolOrientation.fromString(value);
            } else if (SYMBOL_PERCENT.equals(name)) {
                this.percent = XmlUtils.parseNonNegativeInteger(name, value);
            } else if (SYMBOL_SCALING.equals(name)) {
                // no-op
            } else if (SYMBOL_WIDTH.equals(name)) {
                this.width = XmlUtils.parseNonNegativeInteger(name, value) * displayModel.getScaleFactor();
            } else if (POSITION.equals(name)) {
                this.position = Position.fromString(value);
            } else {
                XmlUtils.logUnknownAttribute(elementName, name, value, i);
            }
        }
    }

    @Override
    public void renderNode(RenderCallback renderCallback, final RenderContext renderContext, PointOfInterest poi) {
        // do nothing
    }

    @Override
    public void renderWay(RenderCallback renderCallback, final RenderContext renderContext, PolylineContainer way) {
        if (Display.NEVER == this.display) {
            return;
        }

        if (this.bitmap == null && !this.bitmapInvalid) {
            try {
                this.bitmap = createBitmap(relativePathPrefix, src, resourceProvider);
            } catch (IOException ioException) {
                this.bitmapInvalid = true;
            }
        }

        Float dyScale = this.dyScaled.get(renderContext.rendererJob.tile.zoomLevel);
        if (dyScale == null) {
            dyScale = this.dy;
        }

        if (this.bitmap != null) {
            Rectangle boundary = computeBoundary(this.bitmap.getWidth(), this.bitmap.getHeight(), this.position);
            renderCallback.renderWaySymbol(renderContext, this.display, this.priority, this.bitmap, dyScale, boundary,
                    this.repeat, this.repeatGap, this.repeatStart, this.symbolOrientation, way);
        }
    }

    @Override
    public void scaleStrokeWidth(float scaleFactor, byte zoomLevel) {
        if (this.scale == Scale.NONE) {
            scaleFactor = 1;
        }
        this.dyScaled.put(zoomLevel, this.dy * scaleFactor);
    }

    @Override
    public void scaleTextSize(float scaleFactor, byte zoomLevel) {
        // do nothing
    }
}
