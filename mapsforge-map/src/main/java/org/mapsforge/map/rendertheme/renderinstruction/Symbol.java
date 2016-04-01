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

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Display;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.map.datastore.PointOfInterest;
import org.mapsforge.map.layer.renderer.PolylineContainer;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.rendertheme.RenderCallback;
import org.mapsforge.map.rendertheme.RenderContext;
import org.mapsforge.map.rendertheme.XmlUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * Represents an icon on the map.
 */
public class Symbol extends RenderInstruction {
    private Bitmap bitmap;
    private boolean bitmapInvalid;
    private Display display;
    private String id;
    private int priority;
    private final String relativePathPrefix;
    private String src;

    public Symbol(GraphicFactory graphicFactory, DisplayModel displayModel, String elementName,
                  XmlPullParser pullParser, String relativePathPrefix) throws IOException, XmlPullParserException {
        super(graphicFactory, displayModel);
        this.relativePathPrefix = relativePathPrefix;
        this.display = Display.IFSPACE;
        extractValues(elementName, pullParser);
    }

    @Override
    public void destroy() {
        if (this.bitmap != null) {
            this.bitmap.decrementRefCount();
        }
    }

    private void extractValues(String elementName, XmlPullParser pullParser) throws IOException, XmlPullParserException {
        for (int i = 0; i < pullParser.getAttributeCount(); ++i) {
            String name = pullParser.getAttributeName(i);
            String value = pullParser.getAttributeValue(i);

            if (SRC.equals(name)) {
                this.src = value;
            } else if (CAT.equals(name)) {
                this.category = value;
            } else if (DISPLAY.equals(name)) {
                this.display = Display.fromString(value);
            } else if (ID.equals(name)) {
                this.id = value;
            } else if (PRIORITY.equals(name)) {
                this.priority = Integer.parseInt(value);
            } else if (SYMBOL_HEIGHT.equals(name)) {
                this.height = XmlUtils.parseNonNegativeInteger(name, value) * displayModel.getScaleFactor();
            } else if (SYMBOL_PERCENT.equals(name)) {
                this.percent = XmlUtils.parseNonNegativeInteger(name, value);
            } else if (SYMBOL_SCALING.equals(name)) {
                // no-op
            } else if (SYMBOL_WIDTH.equals(name)) {
                this.width = XmlUtils.parseNonNegativeInteger(name, value) * displayModel.getScaleFactor();
            } else {
                throw XmlUtils.createXmlPullParserException(elementName, name, value, i);
            }
        }
    }

    public Bitmap getBitmap() {
        if (this.bitmap == null && !bitmapInvalid) {
            try {
                this.bitmap = createBitmap(relativePathPrefix, src);
            } catch (IOException ioException) {
                this.bitmapInvalid = true;
            }
        }
        return this.bitmap;
    }

    public String getId() {
        return this.id;
    }

    @Override
    public void renderNode(RenderCallback renderCallback, final RenderContext renderContext, PointOfInterest poi) {
        if (Display.NEVER == this.display) {
            return;
        }

        if (getBitmap() != null) {
            renderCallback.renderPointOfInterestSymbol(renderContext, this.display, this.priority, this.bitmap, poi);
        }
    }

    @Override
    public void renderWay(RenderCallback renderCallback, final RenderContext renderContext, PolylineContainer way) {
        if (Display.NEVER == this.display) {
            return;
        }

        if (this.getBitmap() != null) {
            renderCallback.renderAreaSymbol(renderContext, this.display, this.priority, this.bitmap, way);
        }
    }

    @Override
    public void scaleStrokeWidth(float scaleFactor, byte zoomLevel) {
        // do nothing
    }

    @Override
    public void scaleTextSize(float scaleFactor, byte zoomLevel) {
        // do nothing
    }
}
