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
package org.mapsforge.core.graphics;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

class AwtBitmap implements Bitmap {
	final BufferedImage bufferedImage;

	AwtBitmap(BufferedImage bufferedImage) {
		this.bufferedImage = bufferedImage;
	}

	AwtBitmap(int width, int height) {
		this.bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
	}

	@Override
	public void copyPixelsFromBuffer(ByteBuffer byteBuffer) {
		int[] pixels = new int[byteBuffer.array().length / 4];
		for (int i = 0; i < pixels.length; ++i) {
			pixels[i] = byteBuffer.getInt();
		}

		this.bufferedImage.setRGB(0, 0, getWidth(), getHeight(), pixels, 0, getWidth());
	}

	@Override
	public void copyPixelsToBuffer(ByteBuffer byteBuffer) {
		int[] pixels = this.bufferedImage.getRGB(0, 0, getWidth(), getHeight(), null, 0, getWidth());
		for (int i = 0; i < pixels.length; ++i) {
			byteBuffer.putInt(pixels[i]);
		}
	}

	@Override
	public void fillColor(int color) {
		Graphics2D graphics2D = this.bufferedImage.createGraphics();
		graphics2D.setColor(new java.awt.Color(color));
		graphics2D.fillRect(0, 0, getWidth(), getHeight());
	}

	@Override
	public int getHeight() {
		return this.bufferedImage.getHeight();
	}

	@Override
	public int getWidth() {
		return this.bufferedImage.getWidth();
	}
}
