/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014-2015 Ludwig M Brinckmann
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

import java.util.HashMap;
import java.util.Map;

import org.mapsforge.core.graphics.Align;
import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.Display;
import org.mapsforge.core.graphics.FontFamily;
import org.mapsforge.core.graphics.FontStyle;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.map.layer.renderer.PolylineContainer;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.datastore.PointOfInterest;
import org.mapsforge.map.rendertheme.RenderCallback;
import org.mapsforge.map.rendertheme.RenderContext;
import org.mapsforge.map.rendertheme.XmlUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Represents a text along a polyline on the map.
 */
public class PathText extends RenderInstruction {
	private Display display;
	private float dy;
	private final Map<Byte, Float> dyScaled;
	private final Paint fill;
	private final Map<Byte, Paint> fills;
	private float fontSize;
	private int priority;
	private final Paint stroke;
	private final Map<Byte, Paint> strokes;

	private TextKey textKey;

	public PathText(GraphicFactory graphicFactory, DisplayModel displayModel, String elementName,
	                       XmlPullParser pullParser) throws XmlPullParserException {
		super(graphicFactory, displayModel);
		this.fill = graphicFactory.createPaint();
		this.fill.setColor(Color.BLACK);
		this.fill.setStyle(Style.FILL);
		this.fill.setTextAlign(Align.CENTER);
		this.fills = new HashMap<>();

		this.stroke = graphicFactory.createPaint();
		this.stroke.setColor(Color.BLACK);
		this.stroke.setStyle(Style.STROKE);
		this.stroke.setTextAlign(Align.CENTER);
		this.strokes = new HashMap<>();
		this.dyScaled = new HashMap<>();
		this.display = Display.IFSPACE;

		extractValues(graphicFactory, displayModel, elementName, pullParser);
	}

	@Override
	public void destroy() {
		// no-op
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

		String caption = this.textKey.getValue(way.getTags());
		if (caption == null) {
			return;
		}

		Float dyScale = this.dyScaled.get(renderContext.rendererJob.tile.zoomLevel);
		if (dyScale == null) {
			dyScale = this.dy;
		}

		renderCallback.renderWayText(renderContext, this.display, this.priority, caption, dyScale, getFillPaint(renderContext.rendererJob.tile.zoomLevel),
				getStrokePaint(renderContext.rendererJob.tile.zoomLevel), way);
	}

	@Override
	public void scaleStrokeWidth(float scaleFactor, byte zoomLevel) {
		this.dyScaled.put(zoomLevel, this.dy * scaleFactor);
	}

	@Override
	public void scaleTextSize(float scaleFactor, byte zoomLevel) {
		Paint zlPaint = graphicFactory.createPaint(this.fill);
		zlPaint.setTextSize(this.fontSize * scaleFactor);
		this.fills.put(zoomLevel, zlPaint);

		Paint zlStroke = graphicFactory.createPaint(this.stroke);
		zlStroke.setTextSize(this.fontSize * scaleFactor);
		this.strokes.put(zoomLevel, zlStroke);

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
			} else if (FONT_FAMILY.equals(name)) {
				fontFamily = FontFamily.fromString(value);
			} else if (FONT_STYLE.equals(name)) {
				fontStyle = FontStyle.fromString(value);
			} else if (FONT_SIZE.equals(name)) {
				this.fontSize = XmlUtils.parseNonNegativeFloat(name, value) * displayModel.getScaleFactor();
			} else if (FILL.equals(name)) {
				this.fill.setColor(XmlUtils.getColor(graphicFactory, value));
			} else if (PRIORITY.equals(name)) {
				this.priority = Integer.parseInt(value);
			} else if (STROKE.equals(name)) {
				this.stroke.setColor(XmlUtils.getColor(graphicFactory, value));
			} else if (STROKE_WIDTH.equals(name)) {
				this.stroke.setStrokeWidth(XmlUtils.parseNonNegativeFloat(name, value) * displayModel.getScaleFactor());
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
}
