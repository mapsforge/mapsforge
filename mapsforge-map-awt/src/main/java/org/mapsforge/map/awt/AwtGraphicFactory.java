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
package org.mapsforge.map.awt;

import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.Matrix;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Path;

public final class AwtGraphicFactory implements GraphicFactory {
	public static final GraphicFactory INSTANCE = new AwtGraphicFactory();
	private static final java.awt.Color TRANSPARENT = new java.awt.Color(0, 0, 0, 0);

	public static Canvas createCanvas(java.awt.Graphics2D graphics2D) {
		return new AwtCanvas(graphics2D);
	}

	static AffineTransform getAffineTransform(Matrix matrix) {
		return ((AwtMatrix) matrix).affineTransform;
	}

	static AwtPaint getAwtPaint(Paint paint) {
		return ((AwtPaint) paint);
	}

	static AwtPath getAwtPath(Path path) {
		return ((AwtPath) path);
	}

	static BufferedImage getBufferedImage(Bitmap bitmap) {
		return ((AwtBitmap) bitmap).bufferedImage;
	}

	private AwtGraphicFactory() {
		// do nothing
	}

	@Override
	public Bitmap createBitmap(InputStream inputStream) {
		try {
			BufferedImage bufferedImage = ImageIO.read(inputStream);
			return new AwtBitmap(bufferedImage);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public Bitmap createBitmap(int width, int height) {
		return new AwtBitmap(width, height);
	}

	@Override
	public Canvas createCanvas() {
		return new AwtCanvas();
	}

	@Override
	public int createColor(Color color) {
		switch (color) {
			case BLACK:
				return java.awt.Color.BLACK.getRGB();
			case BLUE:
				return java.awt.Color.BLUE.getRGB();
			case GREEN:
				return java.awt.Color.GREEN.getRGB();
			case RED:
				return java.awt.Color.RED.getRGB();
			case TRANSPARENT:
				return TRANSPARENT.getRGB();
			case WHITE:
				return java.awt.Color.WHITE.getRGB();
		}

		throw new IllegalArgumentException("unknown color: " + color);
	}

	@Override
	public int createColor(String colorString) {
		long parseLong = Long.parseLong(colorString.substring(1), 16);
		return new java.awt.Color((int) parseLong, true).getRGB();
	}

	@Override
	public Matrix createMatrix() {
		return new AwtMatrix();
	}

	@Override
	public Paint createPaint() {
		return new AwtPaint();
	}

	@Override
	public Path createPath() {
		return new AwtPath();
	}
}
