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

import java.nio.ByteBuffer;

import org.mapsforge.core.graphics.Bitmap;

class DummyBitmap implements Bitmap {
	@Override
	public void copyPixelsFromBuffer(ByteBuffer byteBuffer) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void copyPixelsToBuffer(ByteBuffer byteBuffer) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void fillColor(int color) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getHeight() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int[] getPixels() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getWidth() {
		throw new UnsupportedOperationException();
	}
}
