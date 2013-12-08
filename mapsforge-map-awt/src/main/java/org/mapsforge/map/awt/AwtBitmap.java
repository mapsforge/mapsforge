/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
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
package org.mapsforge.map.awt;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.imageio.ImageIO;

import org.mapsforge.core.graphics.Bitmap;

class AwtBitmap implements Bitmap {
	final BufferedImage bufferedImage;

	AwtBitmap(InputStream inputStream) throws IOException {
		this.bufferedImage = ImageIO.read(inputStream);
	}

	AwtBitmap(int width, int height) {
		this.bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
	}

	@Override
	public void compress(OutputStream outputStream) throws IOException {
		ImageIO.write(this.bufferedImage, "png", outputStream);
	}

	@Override
	public void incrementRefCount() {
		// no-op
	}

	@Override
	public void decrementRefCount() {
		// no-op
	}

	@Override
	public int getHeight() {
		return this.bufferedImage.getHeight();
	}

	@Override
	public int getWidth() {
		return this.bufferedImage.getWidth();
	}

	@Override
	public void scaleTo(int width, int height) {
		// TODO implement
	}

	@Override
	public void setBackgroundColor(int color) {
		// TODO implement
	}

}
