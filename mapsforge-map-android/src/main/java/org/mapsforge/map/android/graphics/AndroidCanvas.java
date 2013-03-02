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
import org.mapsforge.core.graphics.Path;

class AndroidCanvas implements Canvas {
	final android.graphics.Canvas canvas;

	AndroidCanvas() {
		this.canvas = new android.graphics.Canvas();
	}

	AndroidCanvas(android.graphics.Canvas canvas) {
		this.canvas = canvas;
	}

	@Override
	public void drawBitmap(Bitmap bitmap, int left, int top) {
		this.canvas.drawBitmap(AndroidGraphics.getBitmap(bitmap), left, top, null);
	}

	@Override
	public void drawBitmap(Bitmap bitmap, Matrix matrix) {
		this.canvas.drawBitmap(AndroidGraphics.getBitmap(bitmap), AndroidGraphics.getMatrix(matrix), null);
	}

	@Override
	public void drawCircle(int x, int y, int radius, Paint paint) {
		this.canvas.drawCircle(x, y, radius, AndroidGraphics.getPaint(paint));
	}

	@Override
	public void drawLine(int x1, int y1, int x2, int y2, Paint paint) {
		this.canvas.drawLine(x1, y1, x2, y2, AndroidGraphics.getPaint(paint));
	}

	@Override
	public void drawPath(Path path, Paint paint) {
		this.canvas.drawPath(AndroidGraphics.getPath(path), AndroidGraphics.getPaint(paint));
	}

	@Override
	public void drawText(String text, int x, int y, Paint paint) {
		this.canvas.drawText(text, x, y, AndroidGraphics.getPaint(paint));
	}

	@Override
	public void drawTextRotated(String text, int x1, int y1, int x2, int y2, Paint paint) {
		android.graphics.Path path = new android.graphics.Path();
		path.moveTo(x1, y1);
		path.lineTo(x2, y2);
		this.canvas.drawTextOnPath(text, path, 0, 3, AndroidGraphics.getPaint(paint));
	}

	@Override
	public void fillColor(int color) {
		this.canvas.drawColor(color);
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
