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

import java.util.Locale;

import org.mapsforge.core.graphics.Align;
import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.FontFamily;
import org.mapsforge.core.graphics.FontStyle;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.map.rendertheme.XmlUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * A builder for {@link PathText} instances.
 */
public class PathTextBuilder {
	static final String FILL = "fill";
	static final String FONT_FAMILY = "font-family";
	static final String FONT_SIZE = "font-size";
	static final String FONT_STYLE = "font-style";
	static final String K = "k";
	static final String STROKE = "stroke";
	static final String STROKE_WIDTH = "stroke-width";

	final Paint fill;
	float fontSize;
	final Paint stroke;
	TextKey textKey;

	public PathTextBuilder(GraphicFactory graphicFactory, String elementName, Attributes attributes)
			throws SAXException {
		this.fill = graphicFactory.createPaint();
		this.fill.setColor(Color.BLACK);
		this.fill.setStyle(Style.FILL);
		this.fill.setTextAlign(Align.CENTER);

		this.stroke = graphicFactory.createPaint();
		this.stroke.setColor(Color.BLACK);
		this.stroke.setStyle(Style.STROKE);
		this.stroke.setTextAlign(Align.CENTER);

		extractValues(graphicFactory, elementName, attributes);
	}

	/**
	 * @return a new {@code PathText} instance.
	 */
	public PathText build() {
		return new PathText(this);
	}

	private void extractValues(GraphicFactory graphicFactory, String elementName, Attributes attributes)
			throws SAXException {
		FontFamily fontFamily = FontFamily.DEFAULT;
		FontStyle fontStyle = FontStyle.NORMAL;

		for (int i = 0; i < attributes.getLength(); ++i) {
			String name = attributes.getQName(i);
			String value = attributes.getValue(i);

			if (K.equals(name)) {
				this.textKey = TextKey.getInstance(value);
			} else if (FONT_FAMILY.equals(name)) {
				fontFamily = FontFamily.valueOf(value.toUpperCase(Locale.ENGLISH));
			} else if (FONT_STYLE.equals(name)) {
				fontStyle = FontStyle.valueOf(value.toUpperCase(Locale.ENGLISH));
			} else if (FONT_SIZE.equals(name)) {
				this.fontSize = XmlUtils.parseNonNegativeFloat(name, value);
			} else if (FILL.equals(name)) {
				this.fill.setColor(XmlUtils.getColor(graphicFactory, value));
			} else if (STROKE.equals(name)) {
				this.stroke.setColor(XmlUtils.getColor(graphicFactory, value));
			} else if (STROKE_WIDTH.equals(name)) {
				this.stroke.setStrokeWidth(XmlUtils.parseNonNegativeFloat(name, value));
			} else {
				throw XmlUtils.createSAXException(elementName, name, value, i);
			}
		}

		this.fill.setTypeface(fontFamily, fontStyle);
		this.stroke.setTypeface(fontFamily, fontStyle);

		XmlUtils.checkMandatoryAttribute(elementName, K, this.textKey);
	}
}
