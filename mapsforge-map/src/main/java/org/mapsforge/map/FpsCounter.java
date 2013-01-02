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
package org.mapsforge.map;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Typeface;
import android.os.SystemClock;

/**
 * An FPS counter measures the drawing frame rate.
 */
public class FpsCounter {
	private static final int ONE_SECOND = 1000;
	private static final Paint PAINT_FILL = createPaint(Color.BLACK, 0);
	private static final Paint PAINT_STROKE = createPaint(Color.WHITE, 3);

	private static Paint createPaint(int color, float strokeWidth) {
		Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setColor(color);
		paint.setStrokeWidth(strokeWidth);
		paint.setStyle(Style.STROKE);
		paint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
		paint.setTextSize(20);
		return paint;
	}

	private int fps;
	private int frameCounter;
	private long lastTime;
	private boolean visible;

	public FpsCounter() {
		this.lastTime = SystemClock.uptimeMillis();
		// TODO remove this
		this.visible = true;
	}

	public void draw(Canvas canvas) {
		if (!this.visible) {
			return;
		}

		long currentTime = SystemClock.uptimeMillis();
		long elapsedTime = currentTime - this.lastTime;
		if (elapsedTime > ONE_SECOND) {
			this.fps = Math.round((this.frameCounter * 1000f) / elapsedTime);
			this.lastTime = currentTime;
			this.frameCounter = 0;
		}
		canvas.drawText(String.valueOf(this.fps), 20, 30, PAINT_STROKE);
		canvas.drawText(String.valueOf(this.fps), 20, 30, PAINT_FILL);
		++this.frameCounter;
	}

	public boolean isVisible() {
		return this.visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}
}
