/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014-2015 Ludwig M Brinckmann
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

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Cap;
import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.layer.renderer.PolylineContainer;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.reader.PointOfInterest;
import org.mapsforge.map.rendertheme.RenderCallback;
import org.mapsforge.map.rendertheme.RenderContext;
import org.mapsforge.map.rendertheme.XmlUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Represents a closed polygon on the map.
 */
public class Area extends RenderInstruction {
	private boolean bitmapInvalid;
	private final Paint fill;
	private final int level;
	private final String relativePathPrefix;
	private Bitmap shaderBitmap;
	private String src;
	private final Paint stroke;
	private float strokeWidth;

	public Area(GraphicFactory graphicFactory, DisplayModel displayModel, String elementName,
	            XmlPullParser pullParser, int level, String relativePathPrefix) throws IOException, XmlPullParserException {
		super(graphicFactory, displayModel);

		this.level = level;
		this.relativePathPrefix = relativePathPrefix;

		this.fill = graphicFactory.createPaint();
		this.fill.setColor(Color.TRANSPARENT);
		this.fill.setStyle(Style.FILL);
		this.fill.setStrokeCap(Cap.ROUND);

		this.stroke = graphicFactory.createPaint();
		this.stroke.setColor(Color.TRANSPARENT);
		this.stroke.setStyle(Style.STROKE);
		this.stroke.setStrokeCap(Cap.ROUND);

		extractValues(elementName, pullParser);
	}

	@Override
	public void destroy() {
		// no-op
	}

	private void extractValues(String elementName,
	                           XmlPullParser pullParser) throws IOException, XmlPullParserException {
		for (int i = 0; i < pullParser.getAttributeCount(); ++i) {
			String name = pullParser.getAttributeName(i);
			String value = pullParser.getAttributeValue(i);

			if (SRC.equals(name)) {
				this.src = value;
			} else if (CAT.equals(name)) {
				this.category = value;
			} else if (FILL.equals(name)) {
				this.fill.setColor(XmlUtils.getColor(graphicFactory, value));
			} else if (STROKE.equals(name)) {
				this.stroke.setColor(XmlUtils.getColor(graphicFactory, value));
			} else if (SYMBOL_HEIGHT.equals(name)) {
				this.height = XmlUtils.parseNonNegativeInteger(name, value) * displayModel.getScaleFactor();
			} else if (SYMBOL_PERCENT.equals(name)) {
				this.percent = XmlUtils.parseNonNegativeInteger(name, value);
			} else if (SYMBOL_SCALING.equals(name)) {
				this.scaling = fromValue(value);
			} else if (SYMBOL_WIDTH.equals(name)) {
				this.width = XmlUtils.parseNonNegativeInteger(name, value) * displayModel.getScaleFactor();
			} else if (STROKE_WIDTH.equals(name)) {
				this.strokeWidth = XmlUtils.parseNonNegativeFloat(name, value) * displayModel.getScaleFactor();
			} else {
				throw XmlUtils.createXmlPullParserException(elementName, name, value, i);
			}
		}
	}



	@Override
	public void renderNode(RenderCallback renderCallback, final RenderContext renderContext, Tile tile, PointOfInterest poi) {
		// do nothing
	}

	@Override
	public void renderWay(RenderCallback renderCallback, final RenderContext renderContext, PolylineContainer way) {
		if (shaderBitmap == null && !bitmapInvalid) {
			try {
				shaderBitmap = createBitmap(relativePathPrefix, src);
				if (shaderBitmap != null) {
					this.fill.setBitmapShader(shaderBitmap);
					shaderBitmap.decrementRefCount();
				}
			} catch (IOException ioException) {
				bitmapInvalid = true;
			}
		}

		this.fill.setBitmapShaderShift(way.getTile().getOrigin());

		renderCallback.renderArea(renderContext, this.fill, this.stroke, this.level, way);
	}

	@Override
	public void scaleStrokeWidth(float scaleFactor) {
		if (this.stroke != null) {
			this.stroke.setStrokeWidth(this.strokeWidth * scaleFactor);
		}
	}

	@Override
	public void scaleTextSize(float scaleFactor) {
		// do nothing
	}

}
