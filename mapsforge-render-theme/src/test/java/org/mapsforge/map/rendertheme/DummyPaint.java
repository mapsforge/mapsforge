/*
 * Copyright 2010, 2011, 2012 mapsforge.org
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
package org.mapsforge.map.rendertheme;

import org.mapsforge.core.graphics.Align;
import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Cap;
import org.mapsforge.core.graphics.FontFamily;
import org.mapsforge.core.graphics.FontStyle;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;

class DummyPaint implements Paint {
	@Override
	public int getColor() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getTextHeight(String text) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getTextWidth(String text) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setAlpha(int alpha) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setBitmapShader(Bitmap bitmap) {
		// do nothing
	}

	@Override
	public void setColor(int color) {
		// do nothing
	}

	@Override
	public void setDashPathEffect(float[] strokeDasharray) {
		// do nothing
	}

	@Override
	public void setStrokeCap(Cap cap) {
		// do nothing
	}

	@Override
	public void setStrokeWidth(float width) {
		// do nothing
	}

	@Override
	public void setStyle(Style style) {
		// do nothing
	}

	@Override
	public void setTextAlign(Align align) {
		// do nothing
	}

	@Override
	public void setTextSize(float textSize) {
		// do nothing
	}

	@Override
	public void setTypeface(FontFamily fontFamily, FontStyle fontStyle) {
		// do nothing
	}
}
