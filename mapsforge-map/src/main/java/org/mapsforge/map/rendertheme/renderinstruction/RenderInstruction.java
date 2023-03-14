/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014-2015 Ludwig M Brinckmann
 * Copyright 2016-2020 devemux86
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
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.Position;
import org.mapsforge.core.graphics.TextTransform;
import org.mapsforge.core.model.Rectangle;
import org.mapsforge.core.util.Parameters;
import org.mapsforge.map.datastore.PointOfInterest;
import org.mapsforge.map.layer.renderer.PolylineContainer;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.rendertheme.RenderCallback;
import org.mapsforge.map.rendertheme.RenderContext;
import org.mapsforge.map.rendertheme.XmlThemeResourceProvider;
import org.mapsforge.map.rendertheme.XmlUtils;

import java.io.IOException;
import java.util.Locale;

/**
 * A RenderInstruction is a basic graphical primitive to draw a map.
 */
public abstract class RenderInstruction {
    static final String ALIGN_CENTER = "align-center";
    static final String ALL = "all";
    static final String CAT = "cat";
    static final String CURVE = "curve";
    static final String DISPLAY = "display";
    static final String DY = "dy";
    static final String FILL = "fill";
    static final String FONT_FAMILY = "font-family";
    static final String FONT_SIZE = "font-size";
    static final String FONT_STYLE = "font-style";
    static final String ID = "id";
    static final String K = "k";
    static final String NONE = "none";
    static final String POSITION = "position";
    static final String PRIORITY = "priority";
    static final String R = "r";
    static final String RADIUS = "radius";
    static final String REPEAT = "repeat";
    static final String REPEAT_GAP = "repeat-gap";
    static final String REPEAT_START = "repeat-start";
    // Use "orientation" parameters instead.
    @Deprecated
    static final String ROTATE = "rotate";
    static final String SCALE = "scale";
    static final String SCALE_RADIUS = "scale-radius";
    static final String SRC = "src";
    static final String STROKE = "stroke";
    static final String STROKE_DASHARRAY = "stroke-dasharray";
    static final String STROKE_LINECAP = "stroke-linecap";
    static final String STROKE_LINEJOIN = "stroke-linejoin";
    static final String STROKE_WIDTH = "stroke-width";
    static final String SYMBOL_HEIGHT = "symbol-height";
    static final String SYMBOL_ID = "symbol-id";
    static final String SYMBOL_ORIENTATION = "symbol-orientation";
    static final String SYMBOL_PERCENT = "symbol-percent";
    static final String SYMBOL_SCALING = "symbol-scaling";
    static final String SYMBOL_WIDTH = "symbol-width";
    static final String TEXT_ORIENTATION = "text-orientation";
    static final String TEXT_TRANSFORM = "text-transform";
    static final String TEXT_WRAP_WIDTH = "text-wrap-width";

    enum Scale {
        ALL,
        NONE,
        STROKE
    }

    protected String category;
    public final DisplayModel displayModel;
    public final GraphicFactory graphicFactory;

    protected float height;
    protected int percent = 100;
    protected float width;

    protected RenderInstruction(GraphicFactory graphicFactory, DisplayModel displayModel) {
        this.displayModel = displayModel;
        this.graphicFactory = graphicFactory;
    }

    protected Rectangle computeBoundary(int width, int height, Position position) {
        // Center by default
        double xFactor = -0.5, yFactor = -0.5;

        if (position == Position.ABOVE_LEFT || position == Position.LEFT || position == Position.BELOW_LEFT)
            xFactor = -1;
        else if (position == Position.ABOVE_RIGHT || position == Position.RIGHT || position == Position.BELOW_RIGHT)
            xFactor = 0;

        if (position == Position.ABOVE_LEFT || position == Position.ABOVE || position == Position.ABOVE_RIGHT)
            yFactor = -1;
        else if (position == Position.BELOW_LEFT || position == Position.BELOW || position == Position.BELOW_RIGHT)
            yFactor = 0;

        double left = xFactor * width;
        double top = yFactor * width;
        return new Rectangle(left, top, left + width, top + height);
    }

    protected Bitmap createBitmap(String relativePathPrefix, String src, XmlThemeResourceProvider resourceProvider)
            throws IOException {
        if (null == src || src.isEmpty()) {
            return null;
        }

        float symbolScale = 1;
        switch (Parameters.SYMBOL_SCALING) {
            case ALL:
                if (this instanceof Symbol || this instanceof LineSymbol) {
                    symbolScale = DisplayModel.symbolScale;
                }
                break;
            case POI:
                if (this instanceof Symbol) {
                    symbolScale = DisplayModel.symbolScale;
                }
                break;
        }
        return XmlUtils.createBitmap(graphicFactory, displayModel, relativePathPrefix, src, resourceProvider, (int) width, (int) height, (int) (percent * symbolScale));
    }

    public abstract void destroy();

    public String getCategory() {
        return this.category;
    }

    /**
     * @param renderCallback a reference to the receiver of all render callbacks.
     * @param renderContext
     * @param poi
     */
    public abstract void renderNode(RenderCallback renderCallback, final RenderContext renderContext, PointOfInterest poi);

    /**
     * @param renderCallback a reference to the receiver of all render callbacks.
     * @param renderContext
     * @param way
     */
    public abstract void renderWay(RenderCallback renderCallback, final RenderContext renderContext, PolylineContainer way);

    protected Scale scaleFromValue(String value) {
        if (value.equals(ALL)) {
            return Scale.ALL;
        } else if (value.equals(NONE)) {
            return Scale.NONE;
        }
        return Scale.STROKE;
    }

    /**
     * Scales the stroke width of this RenderInstruction by the given factor.
     *
     * @param scaleFactor the factor by which the stroke width should be scaled.
     */
    public abstract void scaleStrokeWidth(float scaleFactor, byte zoomLevel);

    /**
     * Scales the text size of this RenderInstruction by the given factor.
     *
     * @param scaleFactor the factor by which the text size should be scaled.
     */
    public abstract void scaleTextSize(float scaleFactor, byte zoomLevel);

    /**
     * Transform text before render based on the selected text transformation.
     */
    protected String transformText(String text, TextTransform transformation) {
        if (text.length() == 0) {
            return text;
        }
        if (transformation == TextTransform.UPPERCASE) {
            return text.toUpperCase(Locale.ROOT);
        } else if (transformation == TextTransform.LOWERCASE) {
            return text.toLowerCase(Locale.ROOT);
        } else if (transformation == TextTransform.CAPITALIZE) {
            return text.substring(0, 1).toUpperCase(Locale.ROOT) + text.substring(1);
        } else {
            return text;
        }
    }
}
