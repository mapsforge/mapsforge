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

import java.io.IOException;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.map.rendertheme.XmlUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * A builder for {@link LineSymbol} instances.
 */
public class LineSymbolBuilder {
	static final String ALIGN_CENTER = "align-center";
	static final String REPEAT = "repeat";
	static final String SRC = "src";

	boolean alignCenter;
	Bitmap bitmap;
	boolean repeat;

	public LineSymbolBuilder(GraphicFactory graphicFactory, String elementName, Attributes attributes,
			String relativePathPrefix) throws IOException, SAXException {
		extractValues(graphicFactory, elementName, attributes, relativePathPrefix);
	}

	/**
	 * @return a new {@code LineSymbol} instance.
	 */
	public LineSymbol build() {
		return new LineSymbol(this);
	}

	private void extractValues(GraphicFactory graphicFactory, String elementName, Attributes attributes,
			String relativePathPrefix) throws IOException, SAXException {
		for (int i = 0; i < attributes.getLength(); ++i) {
			String name = attributes.getQName(i);
			String value = attributes.getValue(i);

			if (SRC.equals(name)) {
				this.bitmap = XmlUtils.createBitmap(graphicFactory, relativePathPrefix, value);
			} else if (ALIGN_CENTER.equals(name)) {
				this.alignCenter = Boolean.parseBoolean(value);
			} else if (REPEAT.equals(name)) {
				this.repeat = Boolean.parseBoolean(value);
			} else {
				throw XmlUtils.createSAXException(elementName, name, value, i);
			}
		}

		XmlUtils.checkMandatoryAttribute(elementName, SRC, this.bitmap);
	}
}
