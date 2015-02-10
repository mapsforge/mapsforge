/*
 * Copyright 2015 devemux86
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
package org.mapsforge.applications.android.samples;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.DrawFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Picture;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;

public class SmoothCanvas extends Canvas {

	Canvas delegate;
	private final Paint smooth = new Paint(Paint.FILTER_BITMAP_FLAG);

	@Override
	public void setBitmap(Bitmap bitmap) {
		this.delegate.setBitmap(bitmap);
	}

	@Override
	public boolean isOpaque() {
		return this.delegate.isOpaque();
	}

	@Override
	public int getWidth() {
		return this.delegate.getWidth();
	}

	@Override
	public int getHeight() {
		return this.delegate.getHeight();
	}

	@Override
	public int save() {
		return this.delegate.save();
	}

	@Override
	public int save(int saveFlags) {
		return this.delegate.save(saveFlags);
	}

	@Override
	public int saveLayer(RectF bounds, Paint paint, int saveFlags) {
		return this.delegate.saveLayer(bounds, paint, saveFlags);
	}

	@Override
	public int saveLayer(float left, float top, float right, float bottom,
			Paint paint, int saveFlags) {
		return this.delegate.saveLayer(left, top, right, bottom, paint,
				saveFlags);
	}

	@Override
	public int saveLayerAlpha(RectF bounds, int alpha, int saveFlags) {
		return this.delegate.saveLayerAlpha(bounds, alpha, saveFlags);
	}

	@Override
	public int saveLayerAlpha(float left, float top, float right, float bottom,
			int alpha, int saveFlags) {
		return this.delegate.saveLayerAlpha(left, top, right, bottom, alpha,
				saveFlags);
	}

	@Override
	public void restore() {
		this.delegate.restore();
	}

	@Override
	public int getSaveCount() {
		return this.delegate.getSaveCount();
	}

	@Override
	public void restoreToCount(int saveCount) {
		this.delegate.restoreToCount(saveCount);
	}

	@Override
	public void translate(float dx, float dy) {
		this.delegate.translate(dx, dy);
	}

	@Override
	public void scale(float sx, float sy) {
		this.delegate.scale(sx, sy);
	}

	@Override
	public void rotate(float degrees) {
		this.delegate.rotate(degrees);
	}

	@Override
	public void skew(float sx, float sy) {
		this.delegate.skew(sx, sy);
	}

	@Override
	public void concat(Matrix matrix) {
		this.delegate.concat(matrix);
	}

	@Override
	public void setMatrix(Matrix matrix) {
		this.delegate.setMatrix(matrix);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void getMatrix(Matrix ctm) {
		this.delegate.getMatrix(ctm);
	}

	@Override
	public boolean clipRect(RectF rect, Region.Op op) {
		return this.delegate.clipRect(rect, op);
	}

	@Override
	public boolean clipRect(Rect rect, Region.Op op) {
		return this.delegate.clipRect(rect, op);
	}

	@Override
	public boolean clipRect(RectF rect) {
		return this.delegate.clipRect(rect);
	}

	@Override
	public boolean clipRect(Rect rect) {
		return this.delegate.clipRect(rect);
	}

	@Override
	public boolean clipRect(float left, float top, float right, float bottom,
			Region.Op op) {
		return this.delegate.clipRect(left, top, right, bottom, op);
	}

	@Override
	public boolean clipRect(float left, float top, float right, float bottom) {
		return this.delegate.clipRect(left, top, right, bottom);
	}

	@Override
	public boolean clipRect(int left, int top, int right, int bottom) {
		return this.delegate.clipRect(left, top, right, bottom);
	}

	@Override
	public boolean clipPath(Path path, Region.Op op) {
		return this.delegate.clipPath(path, op);
	}

	@Override
	public boolean clipPath(Path path) {
		return this.delegate.clipPath(path);
	}

	@SuppressWarnings("deprecation")
	@Override
	public boolean clipRegion(Region region, Region.Op op) {
		return this.delegate.clipRegion(region, op);
	}

	@SuppressWarnings("deprecation")
	@Override
	public boolean clipRegion(Region region) {
		return this.delegate.clipRegion(region);
	}

	@Override
	public DrawFilter getDrawFilter() {
		return this.delegate.getDrawFilter();
	}

	@Override
	public void setDrawFilter(DrawFilter filter) {
		this.delegate.setDrawFilter(filter);
	}

	@Override
	public boolean quickReject(RectF rect, EdgeType type) {
		return this.delegate.quickReject(rect, type);
	}

	@Override
	public boolean quickReject(Path path, EdgeType type) {
		return this.delegate.quickReject(path, type);
	}

	@Override
	public boolean quickReject(float left, float top, float right,
			float bottom, EdgeType type) {
		return this.delegate.quickReject(left, top, right, bottom, type);
	}

	@Override
	public boolean getClipBounds(Rect bounds) {
		return this.delegate.getClipBounds(bounds);
	}

	@Override
	public void drawRGB(int r, int g, int b) {
		this.delegate.drawRGB(r, g, b);
	}

	@Override
	public void drawARGB(int a, int r, int g, int b) {
		this.delegate.drawARGB(a, r, g, b);
	}

	@Override
	public void drawColor(int color) {
		this.delegate.drawColor(color);
	}

	@Override
	public void drawColor(int color, PorterDuff.Mode mode) {
		this.delegate.drawColor(color, mode);
	}

	@Override
	public void drawPaint(Paint paint) {
		this.delegate.drawPaint(paint);
	}

	@Override
	public void drawPoints(float[] pts, int offset, int count, Paint paint) {
		this.delegate.drawPoints(pts, offset, count, paint);
	}

	@Override
	public void drawPoints(float[] pts, Paint paint) {
		this.delegate.drawPoints(pts, paint);
	}

	@Override
	public void drawPoint(float x, float y, Paint paint) {
		this.delegate.drawPoint(x, y, paint);
	}

	@Override
	public void drawLine(float startX, float startY, float stopX, float stopY,
			Paint paint) {
		this.delegate.drawLine(startX, startY, stopX, stopY, paint);
	}

	@Override
	public void drawLines(float[] pts, int offset, int count, Paint paint) {
		this.delegate.drawLines(pts, offset, count, paint);
	}

	@Override
	public void drawLines(float[] pts, Paint paint) {
		this.delegate.drawLines(pts, paint);
	}

	@Override
	public void drawRect(RectF rect, Paint paint) {
		this.delegate.drawRect(rect, paint);
	}

	@Override
	public void drawRect(Rect r, Paint paint) {
		this.delegate.drawRect(r, paint);
	}

	@Override
	public void drawRect(float left, float top, float right, float bottom,
			Paint paint) {
		this.delegate.drawRect(left, top, right, bottom, paint);
	}

	@Override
	public void drawOval(RectF oval, Paint paint) {
		this.delegate.drawOval(oval, paint);
	}

	@Override
	public void drawCircle(float cx, float cy, float radius, Paint paint) {
		this.delegate.drawCircle(cx, cy, radius, paint);
	}

	@Override
	public void drawArc(RectF oval, float startAngle, float sweepAngle,
			boolean useCenter, Paint paint) {
		this.delegate.drawArc(oval, startAngle, sweepAngle, useCenter, paint);
	}

	@Override
	public void drawRoundRect(RectF rect, float rx, float ry, Paint paint) {
		this.delegate.drawRoundRect(rect, rx, ry, paint);
	}

	@Override
	public void drawPath(Path path, Paint paint) {
		this.delegate.drawPath(path, paint);
	}

	@Override
	public void drawBitmap(Bitmap bitmap, float left, float top, Paint paint) {
		if (paint == null) {
			paint = this.smooth;
		} else {
			paint.setFilterBitmap(true);
		}
		this.delegate.drawBitmap(bitmap, left, top, paint);
	}

	@Override
	public void drawBitmap(Bitmap bitmap, Rect src, RectF dst, Paint paint) {
		if (paint == null) {
			paint = this.smooth;
		} else {
			paint.setFilterBitmap(true);
		}
		this.delegate.drawBitmap(bitmap, src, dst, paint);
	}

	@Override
	public void drawBitmap(Bitmap bitmap, Rect src, Rect dst, Paint paint) {
		if (paint == null) {
			paint = this.smooth;
		} else {
			paint.setFilterBitmap(true);
		}
		this.delegate.drawBitmap(bitmap, src, dst, paint);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void drawBitmap(int[] colors, int offset, int stride, int x, int y,
			int width, int height, boolean hasAlpha, Paint paint) {
		if (paint == null) {
			paint = this.smooth;
		} else {
			paint.setFilterBitmap(true);
		}
		this.delegate.drawBitmap(colors, offset, stride, x, y, width, height,
				hasAlpha, paint);
	}

	@Override
	public void drawBitmap(Bitmap bitmap, Matrix matrix, Paint paint) {
		if (paint == null) {
			paint = this.smooth;
		} else {
			paint.setFilterBitmap(true);
		}
		this.delegate.drawBitmap(bitmap, matrix, paint);
	}

	@Override
	public void drawBitmapMesh(Bitmap bitmap, int meshWidth, int meshHeight,
			float[] verts, int vertOffset, int[] colors, int colorOffset,
			Paint paint) {
		this.delegate.drawBitmapMesh(bitmap, meshWidth, meshHeight, verts,
				vertOffset, colors, colorOffset, paint);
	}

	@Override
	public void drawVertices(VertexMode mode, int vertexCount, float[] verts,
			int vertOffset, float[] texs, int texOffset, int[] colors,
			int colorOffset, short[] indices, int indexOffset, int indexCount,
			Paint paint) {
		this.delegate.drawVertices(mode, vertexCount, verts, vertOffset, texs,
				texOffset, colors, colorOffset, indices, indexOffset,
				indexCount, paint);
	}

	@Override
	public void drawText(char[] text, int index, int count, float x, float y,
			Paint paint) {
		this.delegate.drawText(text, index, count, x, y, paint);
	}

	@Override
	public void drawText(String text, float x, float y, Paint paint) {
		this.delegate.drawText(text, x, y, paint);
	}

	@Override
	public void drawText(String text, int start, int end, float x, float y,
			Paint paint) {
		this.delegate.drawText(text, start, end, x, y, paint);
	}

	@Override
	public void drawText(CharSequence text, int start, int end, float x,
			float y, Paint paint) {
		this.delegate.drawText(text, start, end, x, y, paint);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void drawPosText(char[] text, int index, int count, float[] pos,
			Paint paint) {
		this.delegate.drawPosText(text, index, count, pos, paint);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void drawPosText(String text, float[] pos, Paint paint) {
		this.delegate.drawPosText(text, pos, paint);
	}

	@Override
	public void drawTextOnPath(char[] text, int index, int count, Path path,
			float hOffset, float vOffset, Paint paint) {
		this.delegate.drawTextOnPath(text, index, count, path, hOffset,
				vOffset, paint);
	}

	@Override
	public void drawTextOnPath(String text, Path path, float hOffset,
			float vOffset, Paint paint) {
		this.delegate.drawTextOnPath(text, path, hOffset, vOffset, paint);
	}

	@Override
	public void drawPicture(Picture picture) {
		this.delegate.drawPicture(picture);
	}

	@Override
	public void drawPicture(Picture picture, RectF dst) {
		this.delegate.drawPicture(picture, dst);
	}

	@Override
	public void drawPicture(Picture picture, Rect dst) {
		this.delegate.drawPicture(picture, dst);
	}
}
