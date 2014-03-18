/*
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

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.rendertheme.XmlUtils;

import java.io.IOException;

public class RenderInstructionBuilder {

	static final String ALIGN_CENTER = "align-center";
	static final String DEFAULT = "default";
	static final String DY = "dy";
	static final String FILL = "fill";
	static final String FONT_FAMILY = "font-family";
	static final String FONT_SIZE = "font-size";
	static final String FONT_STYLE = "font-style";
	static final String K = "k";
	static final String REPEAT = "repeat";
	static final String SIZE = "symbol-size";
	static final String SRC = "src";
	static final String STROKE = "stroke";
	static final String STROKE_DASHARRAY = "stroke-dasharray";
	static final String STROKE_LINECAP = "stroke-linecap";
	static final String STROKE_WIDTH = "stroke-width";
	static final String SYMBOL_HEIGHT = "symbol-height";
	static final String SYMBOL_PERCENT = "symbol-percent";
	static final String SYMBOL_SCALING = "symbol-scaling";
	static final String SYMBOL_WIDTH = "symbol-width";
	static final String TILE = "tile";

	enum ResourceScaling {
		DEFAULT,
		SIZE,
		TILE
	}

	String elementName;
	float height;
	int percent = 100;
	ResourceScaling scaling;
	String src;
	float width;

	protected Bitmap createBitmap(GraphicFactory graphicFactory, DisplayModel displayModel, String relativePathPrefix, String src)
		throws IOException {
		if (null == src || src.isEmpty()) {
			return null;
		}

		if (scaling == RenderInstructionBuilder.ResourceScaling.TILE) {
			this.width = displayModel.getTilingSize();
			this.height = displayModel.getTilingSize();
		}

		return XmlUtils.createBitmap(graphicFactory, displayModel, relativePathPrefix, src, (int) width, (int) height, percent);
	}

	protected ResourceScaling fromValue(String value) {
		if (value.equals(SIZE)) return ResourceScaling.SIZE;
		if (value.equals(TILE)) return ResourceScaling.TILE;
		return ResourceScaling.DEFAULT;
	}
}
