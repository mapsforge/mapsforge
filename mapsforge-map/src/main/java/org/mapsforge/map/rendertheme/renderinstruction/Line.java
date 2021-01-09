/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014-2015 Ludwig M Brinckmann
 * Copyright 2014-2019 devemux86
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
import java.util.regex.Pattern;

/**
 * Represents a polyline on the map.
 */
public class Line extends RenderInstruction {
    private static final Pattern SPLIT_PATTERN = Pattern.compile(",");

    private boolean bitmapCreated;
    private float dy;
    private final Map<Byte, Float> dyScaled;
    private final int level;
    private final String relativePathPrefix;
    private final XmlThemeResourceProvider resourceProvider;
    private Scale scale = Scale.STROKE;
    private Bitmap shaderBitmap;
    private String src;
    private final Paint stroke;
    private float[] strokeDasharray;
    private final Map<Byte, Paint> strokes;
    private float strokeWidth;

    public Line(GraphicFactory graphicFactory, DisplayModel displayModel, String elementName,
                XmlPullParser pullParser, int level, String relativePathPrefix, XmlThemeResourceProvider resourceProvider) throws IOException, XmlPullParserException {
        super(graphicFactory, displayModel);
        this.level = level;
        this.relativePathPrefix = relativePathPrefix;
        this.resourceProvider = resourceProvider;

        this.stroke = graphicFactory.createPaint();
        this.stroke.setColor(Color.BLACK);
        this.stroke.setStyle(Style.STROKE);
        this.stroke.setStrokeCap(Cap.ROUND);
        this.stroke.setStrokeJoin(Join.ROUND);
        this.strokes = new HashMap<>();
        this.dyScaled = new HashMap<>();

        extractValues(graphicFactory, displayModel, elementName, pullParser);
    }

    @Override
    public void destroy() {
        // no.op
    }

    private void extractValues(GraphicFactory graphicFactory, DisplayModel displayModel, String elementName,
                               XmlPullParser pullParser) throws IOException, XmlPullParserException {
        for (int i = 0; i < pullParser.getAttributeCount(); ++i) {
            String name = pullParser.getAttributeName(i);
            String value = pullParser.getAttributeValue(i);

            if (SRC.equals(name)) {
                this.src = value;
            } else if (CAT.equals(name)) {
                this.category = value;
            } else if (DY.equals(name)) {
                this.dy = Float.parseFloat(value) * displayModel.getScaleFactor();
            } else if (SCALE.equals(name)) {
                this.scale = scaleFromValue(value);
            } else if (STROKE.equals(name)) {
                this.stroke.setColor(XmlUtils.getColor(graphicFactory, value, displayModel.getThemeCallback(), this));
            } else if (STROKE_DASHARRAY.equals(name)) {
                this.strokeDasharray = parseFloatArray(name, value);
                for (int f = 0; f < this.strokeDasharray.length; ++f) {
                    this.strokeDasharray[f] = this.strokeDasharray[f] * displayModel.getScaleFactor();
                }
                this.stroke.setDashPathEffect(this.strokeDasharray);
            } else if (STROKE_LINECAP.equals(name)) {
                this.stroke.setStrokeCap(Cap.fromString(value));
            } else if (STROKE_LINEJOIN.equals(name)) {
                this.stroke.setStrokeJoin(Join.fromString(value));
            } else if (STROKE_WIDTH.equals(name)) {
                this.strokeWidth = XmlUtils.parseNonNegativeFloat(name, value) * displayModel.getScaleFactor();
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

    private Paint getStrokePaint(byte zoomLevel) {
        Paint paint = strokes.get(zoomLevel);
        if (paint == null) {
            paint = this.stroke;
        }
        return paint;
    }

    private static float[] parseFloatArray(String name, String dashString) throws XmlPullParserException {
        String[] dashEntries = SPLIT_PATTERN.split(dashString);
        float[] dashIntervals = new float[dashEntries.length];
        for (int i = 0; i < dashEntries.length; ++i) {
            dashIntervals[i] = XmlUtils.parseNonNegativeFloat(name, dashEntries[i]);
        }
        return dashIntervals;
    }

    @Override
    public void renderNode(RenderCallback renderCallback, final RenderContext renderContext, PointOfInterest poi) {
        // do nothing
    }

    @Override
    public synchronized void renderWay(RenderCallback renderCallback, final RenderContext renderContext, PolylineContainer way) {
        if (!bitmapCreated) {
            try {
                shaderBitmap = createBitmap(relativePathPrefix, src, resourceProvider);
            } catch (IOException ioException) {
                // no-op
            }
            bitmapCreated = true;
        }

        Paint strokePaint = getStrokePaint(renderContext.rendererJob.tile.zoomLevel);

        if (shaderBitmap != null) {
            strokePaint.setBitmapShader(shaderBitmap);
            strokePaint.setBitmapShaderShift(way.getUpperLeft().getOrigin());
        }

        Float dyScale = this.dyScaled.get(renderContext.rendererJob.tile.zoomLevel);
        if (dyScale == null) {
            dyScale = this.dy;
        }
        renderCallback.renderWay(renderContext, strokePaint, dyScale, this.level, way);
    }

    @Override
    public void scaleStrokeWidth(float scaleFactor, byte zoomLevel) {
        if (this.scale == Scale.NONE) {
            scaleFactor = 1;
        }
        if (this.stroke != null) {
            Paint paint = graphicFactory.createPaint(stroke);
            paint.setStrokeWidth(this.strokeWidth * scaleFactor);
            if (this.scale == Scale.ALL) {
                float[] strokeDasharrayScaled = new float[this.strokeDasharray.length];
                for (int i = 0; i < strokeDasharray.length; i++) {
                    strokeDasharrayScaled[i] = this.strokeDasharray[i] * scaleFactor;
                }
                paint.setDashPathEffect(strokeDasharrayScaled);
            }
            strokes.put(zoomLevel, paint);
        }

        this.dyScaled.put(zoomLevel, this.dy * scaleFactor);
    }

    @Override
    public void scaleTextSize(float scaleFactor, byte zoomLevel) {
        // do nothing
    }
}
