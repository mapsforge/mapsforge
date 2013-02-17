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
package org.mapsforge.map.layer.cache;

import java.nio.ByteBuffer;

import org.mapsforge.map.graphics.Bitmap;

class DummyBitmap implements Bitmap {
	private final byte[] data;
	private final int height;
	private final int width;

	DummyBitmap(int width, int height) {
		this.width = width;
		this.height = height;

		this.data = new byte[width * height * 4];
	}

	@Override
	public Bitmap copy() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void copyPixelsFromBuffer(ByteBuffer byteBuffer) {
		System.arraycopy(byteBuffer.array(), 0, this.data, 0, byteBuffer.array().length);
	}

	@Override
	public void copyPixelsToBuffer(ByteBuffer byteBuffer) {
		byteBuffer.put(this.data, 0, this.data.length);
	}

	@Override
	public int getHeight() {
		return this.height;
	}

	@Override
	public int[] getPixels() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getWidth() {
		return this.width;
	}
}
