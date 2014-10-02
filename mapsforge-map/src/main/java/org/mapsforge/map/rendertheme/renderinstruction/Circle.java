/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
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
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.layer.renderer.PolylineContainer;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.reader.PointOfInterest;
import org.mapsforge.map.rendertheme.RenderCallback;
import org.mapsforge.map.rendertheme.XmlUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Represents a round area on the map.
 */
public class Circle extends RenderInstruction {
	private final Paint fill;
	private final int level;
	private float radius;
	private float renderRadius;
	private boolean scaleRadius;
	private final Paint stroke;
	private float strokeWidth;

	public Circle(GraphicFactory graphicFactory, DisplayModel displayModel, String elementName,
	       XmlPullParser pullParser, int level) throws XmlPullParserException {
		super(graphicFactory, displayModel);
		this.level = level;

		this.fill = graphicFactory.createPaint();
		this.fill.setColor(Color.TRANSPARENT);
		this.fill.setStyle(Style.FILL);

		this.stroke = graphicFactory.createPaint();
		this.stroke.setColor(Color.TRANSPARENT);
		this.stroke.setStyle(Style.STROKE);

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

	@Override
	public void renderNode(RenderCallback renderCallback, PointOfInterest poi, Tile tile) {
		renderCallback.renderPointOfInterestCircle(poi, this.renderRadius, this.fill, this.stroke, this.level, tile);
	}

	@Override
	public void renderWay(RenderCallback renderCallback, PolylineContainer way) {
		// do nothing
	}

	@Override
	public void scaleStrokeWidth(float scaleFactor) {
		if (this.scaleRadius) {
			this.renderRadius = this.radius * scaleFactor;
			if (this.stroke != null) {
				this.stroke.setStrokeWidth(this.strokeWidth * scaleFactor);
			}
		}
	}

	@Override
	public void scaleTextSize(float scaleFactor) {
		// do nothing
	}

	private void extractValues(GraphicFactory graphicFactory, DisplayModel displayModel, String elementName,
	                           XmlPullParser pullParser) throws XmlPullParserException {
		for (int i = 0; i < pullParser.getAttributeCount(); ++i) {
			String name = pullParser.getAttributeName(i);
			String value = pullParser.getAttributeValue(i);

			if (RADIUS.equals(name) || (XmlUtils.supportOlderRenderThemes && R.equals(name))) {
				this.radius = Float.valueOf(XmlUtils.parseNonNegativeFloat(name, value)) * displayModel.getScaleFactor();
			} else if (SCALE_RADIUS.equals(name)) {
				this.scaleRadius = Boolean.parseBoolean(value);
			} else if (CAT.equals(name)) {
				this.category = value;
			} else if (FILL.equals(name)) {
				this.fill.setColor(XmlUtils.getColor(graphicFactory, value));
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

}
