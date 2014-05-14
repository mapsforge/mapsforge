/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright © 2014 devemux86
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

import org.mapsforge.core.graphics.Align;
import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Position;
import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.FontFamily;
import org.mapsforge.core.graphics.FontStyle;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.rendertheme.XmlUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.util.HashMap;
import java.util.Locale;

/**
 * A builder for {@link org.mapsforge.map.rendertheme.renderinstruction.Caption} instances.
 */
public class CaptionBuilder extends RenderInstructionBuilder {

	public static final float DEFAULT_GAP = 5f;

	Bitmap bitmap;
	Position position;
	float dy;
	final Paint fill;
	float fontSize;
	float gap;
	final Paint stroke;
	String symbolId;
	TextKey textKey;

	public CaptionBuilder(GraphicFactory graphicFactory, DisplayModel displayModel, String elementName,
	                            Attributes attributes, HashMap<String, Symbol> symbols) throws SAXException {
		this.fill = graphicFactory.createPaint();
		this.fill.setColor(Color.BLACK);
		this.fill.setStyle(Style.FILL);
		this.fill.setTextAlign(Align.LEFT);

		this.stroke = graphicFactory.createPaint();
		this.stroke.setColor(Color.BLACK);
		this.stroke.setStyle(Style.STROKE);
		this.stroke.setTextAlign(Align.LEFT);

		this.gap = DEFAULT_GAP * displayModel.getScaleFactor();

		extractValues(graphicFactory, displayModel, elementName, attributes);

		if (this.symbolId != null) {
			Symbol symbol = symbols.get(this.symbolId);
			if (symbol != null) {
				this.bitmap = symbol.getBitmap();
			}
		}

		if (position == null) {
			// sensible defaults: below if symbol is present, center if not
			if (this.bitmap == null) {
				this.position = Position.CENTER;
			} else {
				this.position = Position.BELOW;
			}
		}

	}

	/**
	 * @return a new {@code Caption} instance.
	 */
	public Caption build() {
		return new Caption(this);
	}

	private void extractValues(GraphicFactory graphicFactory, DisplayModel displayModel, String elementName,
	                           Attributes attributes) throws SAXException {
		FontFamily fontFamily = FontFamily.DEFAULT;
		FontStyle fontStyle = FontStyle.NORMAL;

		for (int i = 0; i < attributes.getLength(); ++i) {
			String name = attributes.getQName(i);
			String value = attributes.getValue(i);

			if (K.equals(name)) {
				this.textKey = TextKey.getInstance(value);
			} else if (POSITION.equals(name)) {
				this.position = Position.valueOf(value.toUpperCase(Locale.ENGLISH));
			} else if (CAT.equals(name)) {
				this.cat = value;
			} else if (DY.equals(name)) {
				this.dy = Float.parseFloat(value) * displayModel.getScaleFactor();
			} else if (FONT_FAMILY.equals(name)) {
				fontFamily = FontFamily.valueOf(value.toUpperCase(Locale.ENGLISH));
			} else if (FONT_STYLE.equals(name)) {
				fontStyle = FontStyle.valueOf(value.toUpperCase(Locale.ENGLISH));
			} else if (FONT_SIZE.equals(name)) {
				this.fontSize = XmlUtils.parseNonNegativeFloat(name, value) * displayModel.getScaleFactor();
			} else if (FILL.equals(name)) {
				this.fill.setColor(XmlUtils.getColor(graphicFactory, value));
			} else if (STROKE.equals(name)) {
				this.stroke.setColor(XmlUtils.getColor(graphicFactory, value));
			} else if (STROKE_WIDTH.equals(name)) {
				this.stroke.setStrokeWidth(XmlUtils.parseNonNegativeFloat(name, value) * displayModel.getScaleFactor());
			} else if (SYMBOL_ID.equals(name)) {
				this.symbolId = value;
			} else {
				throw XmlUtils.createSAXException(elementName, name, value, i);
			}
		}

		this.fill.setTypeface(fontFamily, fontStyle);
		this.stroke.setTypeface(fontFamily, fontStyle);

		XmlUtils.checkMandatoryAttribute(elementName, K, this.textKey);
	}
}
