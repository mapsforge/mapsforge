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
package org.mapsforge.map.android.graphics;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.Matrix;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Path;
import org.mapsforge.core.model.Dimension;

import android.graphics.PorterDuff;
import android.graphics.Region;

class AndroidCanvas implements Canvas {
	android.graphics.Canvas canvas;
	private final android.graphics.Paint bitmapPaint = new android.graphics.Paint();

	AndroidCanvas() {
		this.canvas = new android.graphics.Canvas();

		this.bitmapPaint.setAntiAlias(true);
		this.bitmapPaint.setFilterBitmap(true);
	}

	AndroidCanvas(android.graphics.Canvas canvas) {
		this.canvas = canvas;
	}

	@Override
	public void destroy() {
		this.canvas = null;
	}

	@Override
	public void drawBitmap(Bitmap bitmap, int left, int top) {
		this.canvas.drawBitmap(AndroidGraphicFactory.getBitmap(bitmap), left, top, null);
	}

	@Override
	public void drawBitmap(Bitmap bitmap, Matrix matrix) {
		this.canvas.drawBitmap(AndroidGraphicFactory.getBitmap(bitmap), AndroidGraphicFactory.getMatrix(matrix),
				null);
	}

	@Override
	public void drawCircle(int x, int y, int radius, Paint paint) {
		if (paint.isTransparent()) {
			return;
		}
		this.canvas.drawCircle(x, y, radius, AndroidGraphicFactory.getPaint(paint));
	}

	@Override
	public void drawLine(int x1, int y1, int x2, int y2, Paint paint) {
		if (paint.isTransparent()) {
			return;
		}

		this.canvas.drawLine(x1, y1, x2, y2, AndroidGraphicFactory.getPaint(paint));
	}

	@Override
	public void drawPath(Path path, Paint paint) {
		if (paint.isTransparent()) {
			return;
		}

		this.canvas.drawPath(AndroidGraphicFactory.getPath(path), AndroidGraphicFactory.getPaint(paint));
	}

	@Override
	public void drawText(String text, int x, int y, Paint paint) {
		if (paint.isTransparent()) {
			return;
		}

		this.canvas.drawText(text, x, y, AndroidGraphicFactory.getPaint(paint));
	}

	@Override
	public void drawTextRotated(String text, int x1, int y1, int x2, int y2, Paint paint) {
		if (paint.isTransparent()) {
			return;
		}

		android.graphics.Path path = new android.graphics.Path();
		path.moveTo(x1, y1);
		path.lineTo(x2, y2);
		this.canvas.drawTextOnPath(text, path, 0, 3, AndroidGraphicFactory.getPaint(paint));
	}

	@Override
	public void fillColor(Color color) {
		this.canvas.drawColor(AndroidGraphicFactory.getColor(color), PorterDuff.Mode.CLEAR);
	}

	@Override
	public void fillColor(int color) {
		this.canvas.drawColor(color);
	}

	@Override
	public Dimension getDimension() {
		return new Dimension(getWidth(), getHeight());
	}

	@Override
	public int getHeight() {
		return this.canvas.getHeight();
	}

	@Override
	public int getWidth() {
		return this.canvas.getWidth();
	}

	@Override
	public void resetClip() {
		this.canvas.clipRect(0, 0, getWidth(), getHeight(), Region.Op.REPLACE);
	}

	@Override
	public void setBitmap(Bitmap bitmap) {
		this.canvas.setBitmap(AndroidGraphicFactory.getBitmap(bitmap));
	}

	@Override
	public void setClip(int left, int top, int width, int height) {
		this.canvas.clipRect(left, top, left + width, top + height, Region.Op.REPLACE);
	}
}
