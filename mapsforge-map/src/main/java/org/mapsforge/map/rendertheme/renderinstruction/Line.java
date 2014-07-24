/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2014 devemux86
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

import java.io.IOException;
import java.util.Locale;
import java.util.regex.Pattern;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Cap;
import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.Join;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.layer.renderer.PolylineContainer;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.reader.PointOfInterest;
import org.mapsforge.map.rendertheme.RenderCallback;
import org.mapsforge.map.rendertheme.XmlUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Represents a polyline on the map.
 */
public class Line extends RenderInstruction {

	private static final Pattern SPLIT_PATTERN = Pattern.compile(",");

	private boolean bitmapCreated;
	private float dy;
	private final int level;
	private final String relativePathPrefix;
	private String src;
	private final Paint stroke;
	private float strokeWidth;

	public Line(GraphicFactory graphicFactory, DisplayModel displayModel, String elementName,
	     XmlPullParser pullParser, int level, String relativePathPrefix) throws IOException, XmlPullParserException {
		super(graphicFactory, displayModel);
		this.level = level;
		this.relativePathPrefix = relativePathPrefix;

		this.stroke = graphicFactory.createPaint();
		this.stroke.setColor(Color.BLACK);
		this.stroke.setStyle(Style.STROKE);
		this.stroke.setStrokeCap(Cap.ROUND);
		this.stroke.setStrokeJoin(Join.ROUND);

		extractValues(graphicFactory, displayModel, elementName, pullParser, relativePathPrefix);
	}

	@Override
	public void destroy() {
		// no.op
	}

	@Override
	public void renderNode(RenderCallback renderCallback, PointOfInterest poi, Tile tile) {
		// do nothing
	}

	@Override
	public void renderWay(RenderCallback renderCallback, PolylineContainer way) {
		if (!bitmapCreated) {
			try {
				Bitmap shaderBitmap = createBitmap(relativePathPrefix, src);
				if (shaderBitmap != null) {
					this.stroke.setBitmapShader(shaderBitmap);
					shaderBitmap.decrementRefCount();
				}
			} catch (IOException ioException) {
				// no-op
			}
			bitmapCreated = true;
		}
		renderCallback.renderWay(way, this.stroke, this.dy, this.level);
	}

	@Override
	public void scaleStrokeWidth(float scaleFactor) {
		this.stroke.setStrokeWidth(this.strokeWidth * scaleFactor);
	}

	@Override
	public void scaleTextSize(float scaleFactor) {
		// do nothing
	}
	private void extractValues(GraphicFactory graphicFactory, DisplayModel displayModel, String elementName,
	                           XmlPullParser pullParser, String relativePathPrefix) throws IOException, XmlPullParserException {
		for (int i = 0; i < pullParser.getAttributeCount(); ++i) {
			String name = pullParser.getAttributeName(i);
			String value = pullParser.getAttributeValue(i);

			if (SRC.equals(name)) {
				this.src = value;
			} else if (CAT.equals(name)) {
				this.category = value;
			} else if (DY.equals(name)) {
				this.dy = Float.parseFloat(value) * displayModel.getScaleFactor();
			} else if (STROKE.equals(name)) {
				this.stroke.setColor(XmlUtils.getColor(graphicFactory, value));
			} else if (STROKE_WIDTH.equals(name)) {
				this.strokeWidth = XmlUtils.parseNonNegativeFloat(name, value) * displayModel.getScaleFactor();
			} else if (STROKE_DASHARRAY.equals(name)) {
				float[] floatArray = parseFloatArray(name, value);
				for (int f = 0; f < floatArray.length; ++f) {
					floatArray[f] = floatArray[f] * displayModel.getScaleFactor();
				}
				this.stroke.setDashPathEffect(floatArray);
			} else if (STROKE_LINECAP.equals(name)) {
				this.stroke.setStrokeCap(Cap.valueOf(value.toUpperCase(Locale.ENGLISH)));
			} else if (STROKE_LINEJOIN.equals(name)) {
				this.stroke.setStrokeJoin(Join.valueOf(value.toUpperCase(Locale.ENGLISH)));
			} else if (SYMBOL_HEIGHT.equals(name)) {
				this.height = XmlUtils.parseNonNegativeInteger(name, value) * displayModel.getScaleFactor();
			} else if (SYMBOL_PERCENT.equals(name)) {
				this.percent = XmlUtils.parseNonNegativeInteger(name, value);
			} else if (SYMBOL_SCALING.equals(name)) {
				this.scaling = fromValue(value);
			} else if (SYMBOL_WIDTH.equals(name)) {
				this.width = XmlUtils.parseNonNegativeInteger(name, value) * displayModel.getScaleFactor();
			} else {
				throw XmlUtils.createXmlPullParserException(elementName, name, value, i);
			}
		}
	}

	private static float[] parseFloatArray(String name, String dashString) throws XmlPullParserException {
		String[] dashEntries = SPLIT_PATTERN.split(dashString);
		float[] dashIntervals = new float[dashEntries.length];
		for (int i = 0; i < dashEntries.length; ++i) {
			dashIntervals[i] = XmlUtils.parseNonNegativeFloat(name, dashEntries[i]);
		}
		return dashIntervals;
	}


}
