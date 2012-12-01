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

import java.io.InputStream;

import org.mapsforge.map.graphics.Bitmap;
import org.mapsforge.map.graphics.Paint;
import org.mapsforge.map.rendertheme.GraphicAdapter;

public class DummyGraphicAdapter implements GraphicAdapter {
	@Override
	public Bitmap decodeStream(InputStream inputStream) {
		return new DummyBitmap();
	}

	@Override
	public int getColor(Color color) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Paint getPaint() {
		return new DummyPaint();
	}

	@Override
	public int parseColor(String colorString) {
		// TODO Auto-generated method stub
		return 0;
	}
}
