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
package org.mapsforge.map.rendertheme.rule;

import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.map.rendertheme.XmlUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * A builder for {@link RenderTheme} instances.
 */
public class RenderThemeBuilder {

	private static final String BASE_STROKE_WIDTH = "base-stroke-width";
	private static final String BASE_TEXT_SIZE = "base-text-size";
	private static final String MAP_BACKGROUND = "map-background";
	private static final int RENDER_THEME_VERSION = 3;
	private static final String VERSION = "version";
	private static final String XMLNS = "xmlns";
	private static final String XMLNS_XSI = "xmlns:xsi";
	private static final String XSI_SCHEMALOCATION = "xsi:schemaLocation";

	float baseStrokeWidth;
	float baseTextSize;
	int mapBackground;
	private Integer version;

	public RenderThemeBuilder(GraphicFactory graphicFactory, String elementName, Attributes attributes)
			throws SAXException {
		this.baseStrokeWidth = 1f;
		this.baseTextSize = 1f;
		this.mapBackground = graphicFactory.createColor(Color.WHITE);

		extractValues(graphicFactory, elementName, attributes);
	}

	/**
	 * @return a new {@code RenderTheme} instance.
	 */
	public RenderTheme build() {
		return new RenderTheme(this);
	}

	private void extractValues(GraphicFactory graphicFactory, String elementName, Attributes attributes)
			throws SAXException {
		for (int i = 0; i < attributes.getLength(); ++i) {
			String name = attributes.getQName(i);
			String value = attributes.getValue(i);

			if (XMLNS.equals(name)) {
				continue;
			} else if (XMLNS_XSI.equals(name)) {
				continue;
			} else if (XSI_SCHEMALOCATION.equals(name)) {
				continue;
			} else if (VERSION.equals(name)) {
				this.version = Integer.valueOf(XmlUtils.parseNonNegativeInteger(name, value));
			} else if (MAP_BACKGROUND.equals(name)) {
				this.mapBackground = XmlUtils.getColor(graphicFactory, value);
			} else if (BASE_STROKE_WIDTH.equals(name)) {
				this.baseStrokeWidth = XmlUtils.parseNonNegativeFloat(name, value);
			} else if (BASE_TEXT_SIZE.equals(name)) {
				this.baseTextSize = XmlUtils.parseNonNegativeFloat(name, value);
			} else {
				throw XmlUtils.createSAXException(elementName, name, value, i);
			}
		}

		validate(elementName);
	}

	private void validate(String elementName) throws SAXException {
		XmlUtils.checkMandatoryAttribute(elementName, VERSION, this.version);

		if (!XmlUtils.supportOlderRenderThemes && this.version != RENDER_THEME_VERSION) {
			throw new SAXException("unsupported render theme version: " + this.version);
		} else if (this.version > RENDER_THEME_VERSION) {
			throw new SAXException("unsupported newer render theme version: " + this.version);
		}
	}
}
