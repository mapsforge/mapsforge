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
package org.mapsforge.map.rendertheme.renderinstruction;

import org.mapsforge.map.graphics.Align;
import org.mapsforge.map.graphics.Bitmap;
import org.mapsforge.map.graphics.Cap;
import org.mapsforge.map.graphics.FontFamily;
import org.mapsforge.map.graphics.FontStyle;
import org.mapsforge.map.graphics.Paint;
import org.mapsforge.map.graphics.Style;

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
	public void setBitmapShader(Bitmap bitmap) {
		// TODO Auto-generated method stub
	}

	@Override
	public void setColor(int color) {
		// TODO Auto-generated method stub
	}

	@Override
	public void setDashPathEffect(float[] strokeDasharray) {
		// TODO Auto-generated method stub
	}

	@Override
	public void setStrokeCap(Cap cap) {
		// TODO Auto-generated method stub
	}

	@Override
	public void setStrokeWidth(float width) {
		// TODO Auto-generated method stub
	}

	@Override
	public void setStyle(Style style) {
		// TODO Auto-generated method stub
	}

	@Override
	public void setTextAlign(Align align) {
		// TODO Auto-generated method stub
	}

	@Override
	public void setTextSize(float textSize) {
		// TODO Auto-generated method stub
	}

	@Override
	public void setTypeface(FontFamily fontFamily, FontStyle fontStyle) {
		// TODO Auto-generated method stub
	}

	@Override
	public void destroy() {
		// no-op
	}
}
