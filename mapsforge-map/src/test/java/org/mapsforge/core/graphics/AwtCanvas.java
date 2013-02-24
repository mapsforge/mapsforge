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

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.image.BufferedImage;

class AwtCanvas implements Canvas {
	private static int getCap(Cap cap) {
		switch (cap) {
			case BUTT:
				return BasicStroke.CAP_BUTT;
			case ROUND:
				return BasicStroke.CAP_ROUND;
			case SQUARE:
				return BasicStroke.CAP_SQUARE;
		}

		throw new IllegalArgumentException("unknown cap: " + cap);
	}

	private static Stroke getStroke(Paint paint) {
		int cap = getCap(paint.getStrokeCap());
		return new BasicStroke(paint.getStrokeWidth(), cap, BasicStroke.JOIN_ROUND);
	}

	private BufferedImage bufferedImage;
	private Graphics2D graphics2D;

	@Override
	public void drawBitmap(Bitmap bitmap, int left, int top) {
		this.graphics2D.drawImage(AwtGraphicFactory.getBufferedImage(bitmap), left, top, null);
	}

	@Override
	public void drawBitmap(Bitmap bitmap, Matrix matrix) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void drawCircle(int x, int y, int radius, Paint paint) {
		setPaintAttributes(paint);
		this.graphics2D.drawOval(x, y, radius, radius);
	}

	@Override
	public void drawLine(int x1, int y1, int x2, int y2, Paint paint) {
		setPaintAttributes(paint);
		this.graphics2D.drawLine(x1, y1, x2, y2);
	}

	@Override
	public void drawPath(Path path, Paint paint) {
		setPaintAttributes(paint);
		this.graphics2D.drawPolygon(AwtGraphicFactory.getPolygon(path));
	}

	@Override
	public void drawText(String text, int x, int y, Paint paint) {
		setPaintAttributes(paint);
		this.graphics2D.drawString(text, x, y);
	}

	@Override
	public void drawTextRotated(String text, int x1, int y1, int x2, int y2, Paint paint) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void fillColor(int color) {
		this.graphics2D.setColor(new java.awt.Color(color));
		this.graphics2D.fillRect(0, 0, getWidth(), getHeight());
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
	public void setBitmap(Bitmap bitmap) {
		if (bitmap == null) {
			this.bufferedImage = null;
			this.graphics2D = null;
		} else {
			this.bufferedImage = AwtGraphicFactory.getBufferedImage(bitmap);
			this.graphics2D = this.bufferedImage.createGraphics();
		}
	}

	private void setPaintAttributes(Paint paint) {
		this.graphics2D.setColor(new java.awt.Color(paint.getColor()));
		this.graphics2D.setStroke(getStroke(paint));
	}
}
