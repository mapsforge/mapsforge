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

import java.io.IOException;
import java.util.Locale;
import java.util.regex.Pattern;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Cap;
import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.rendertheme.XmlUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * A builder for {@link Line} instances.
 */
public class LineBuilder extends RenderInstructionBuilder {
	private static final Pattern SPLIT_PATTERN = Pattern.compile(",");

	private static float[] parseFloatArray(String name, String dashString) throws SAXException {
		String[] dashEntries = SPLIT_PATTERN.split(dashString);
		float[] dashIntervals = new float[dashEntries.length];
		for (int i = 0; i < dashEntries.length; ++i) {
			dashIntervals[i] = XmlUtils.parseNonNegativeFloat(name, dashEntries[i]);
		}
		return dashIntervals;
	}

	float dy;
	final int level;
	final Paint stroke;
	float strokeWidth;

	public LineBuilder(GraphicFactory graphicFactory, DisplayModel displayModel, String elementName,
			Attributes attributes, int level, String relativePathPrefix) throws IOException, SAXException {
		this.level = level;

		this.stroke = graphicFactory.createPaint();
		this.stroke.setColor(Color.BLACK);
		this.stroke.setStyle(Style.STROKE);
		this.stroke.setStrokeCap(Cap.ROUND);

		extractValues(graphicFactory, displayModel, elementName, attributes, relativePathPrefix);

		Bitmap shaderBitmap = createBitmap(graphicFactory, displayModel, relativePathPrefix, src);
		if (shaderBitmap != null) {
			this.stroke.setBitmapShader(shaderBitmap);
			shaderBitmap.decrementRefCount();
		}

	}

	/**
	 * @return a new {@code Line} instance.
	 */
	public Line build() {
		return new Line(this);
	}

	private void extractValues(GraphicFactory graphicFactory, DisplayModel displayModel, String elementName,
			Attributes attributes, String relativePathPrefix) throws IOException, SAXException {
		for (int i = 0; i < attributes.getLength(); ++i) {
			String name = attributes.getQName(i);
			String value = attributes.getValue(i);

			if (SRC.equals(name)) {
				this.src = value;
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
			} else if (SYMBOL_HEIGHT.equals(name)) {
				this.height = XmlUtils.parseNonNegativeInteger(name, value) * displayModel.getScaleFactor();
			} else if (SYMBOL_SCALING.equals(name)) {
				this.scaling = fromValue(value);
			} else if (SYMBOL_WIDTH.equals(name)) {
				this.width = XmlUtils.parseNonNegativeInteger(name, value) * displayModel.getScaleFactor();
			} else {
				throw XmlUtils.createSAXException(elementName, name, value, i);
			}
		}
	}
}
