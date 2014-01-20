/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
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
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.rendertheme.XmlUtils;
import org.mapsforge.map.rendertheme.rule.RenderThemeBuilder;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * A builder for {@link Circle} instances.
 */
public class CircleBuilder {
	static final String FILL = "fill";
	static final String RADIUS = "radius";
	static final String R = "r";
	static final String SCALE_RADIUS = "scale-radius";
	static final String STROKE = "stroke";
	static final String STROKE_WIDTH = "stroke-width";

	final Paint fill;
	final int level;
	Float radius;
	boolean scaleRadius;
	final Paint stroke;
	float strokeWidth;

	public CircleBuilder(GraphicFactory graphicFactory, DisplayModel displayModel, String elementName, Attributes attributes, int level)
			throws SAXException {
		this.level = level;

		this.fill = graphicFactory.createPaint();
		this.fill.setColor(Color.TRANSPARENT);
		this.fill.setStyle(Style.FILL);

		this.stroke = graphicFactory.createPaint();
		this.stroke.setColor(Color.TRANSPARENT);
		this.stroke.setStyle(Style.STROKE);

		extractValues(graphicFactory, displayModel, elementName, attributes);
	}

	/**
	 * @return a new {@code Circle} instance.
	 */
	public Circle build() {
		return new Circle(this);
	}

	private void extractValues(GraphicFactory graphicFactory, DisplayModel displayModel, String elementName, Attributes attributes)
			throws SAXException {
		for (int i = 0; i < attributes.getLength(); ++i) {
			String name = attributes.getQName(i);
			String value = attributes.getValue(i);

			if (RADIUS.equals(name) || (XmlUtils.supportOlderRenderThemes && R.equals(name))) {
				this.radius = Float.valueOf(XmlUtils.parseNonNegativeFloat(name, value));
			} else if (SCALE_RADIUS.equals(name)) {
				this.scaleRadius = Boolean.parseBoolean(value);
			} else if (FILL.equals(name)) {
				this.fill.setColor(XmlUtils.getColor(graphicFactory, value));
			} else if (STROKE.equals(name)) {
				this.stroke.setColor(XmlUtils.getColor(graphicFactory, value));
			} else if (STROKE_WIDTH.equals(name)) {
				this.strokeWidth = XmlUtils.parseNonNegativeFloat(name, value) * displayModel.getScaleFactor();
			} else {
				throw XmlUtils.createSAXException(elementName, name, value, i);
			}
		}

		XmlUtils.checkMandatoryAttribute(elementName, RADIUS, this.radius);
	}
}
