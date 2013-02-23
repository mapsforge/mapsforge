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
package org.mapsforge.map.android.graphics;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.Matrix;
import org.mapsforge.core.graphics.Paint;

class AndroidCanvas implements Canvas {
	final android.graphics.Canvas canvas;

	AndroidCanvas() {
		this(new android.graphics.Canvas());
	}

	AndroidCanvas(android.graphics.Canvas canvas) {
		this.canvas = canvas;
	}

	@Override
	public void drawBitmap(Bitmap bitmap, float left, float top) {
		this.canvas.drawBitmap(AndroidGraphics.getBitmap(bitmap), left, top, null);
	}

	@Override
	public void drawBitmap(Bitmap bitmap, Matrix matrix) {
		this.canvas.drawBitmap(AndroidGraphics.getBitmap(bitmap), AndroidGraphics.getMatrix(matrix), null);
	}

	@Override
	public void drawCircle(float x, float y, float radius, Paint paint) {
		this.canvas.drawCircle(x, y, radius, AndroidGraphics.getPaint(paint));
	}

	@Override
	public void drawLine(float x1, float y1, float x2, float y2, Paint paint) {
		this.canvas.drawLine(x1, y1, x2, y2, AndroidGraphics.getPaint(paint));
	}

	@Override
	public void drawLines(float[] points, Paint paint) {
		this.canvas.drawLines(points, AndroidGraphics.getPaint(paint));
	}

	@Override
	public void drawText(String text, float x, float y, Paint paint) {
		this.canvas.drawText(text, x, y, AndroidGraphics.getPaint(paint));
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
	public void setBitmap(Bitmap bitmap) {
		this.canvas.setBitmap(AndroidGraphics.getBitmap(bitmap));
	}
}
