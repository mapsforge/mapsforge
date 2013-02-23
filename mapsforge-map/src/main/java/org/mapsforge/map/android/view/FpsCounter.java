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
package org.mapsforge.map.android.view;

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
	private static final Paint PAINT = createPaint();

	private static Paint createPaint() {
		Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setColor(Color.BLACK);
		paint.setStrokeWidth(0);
		paint.setStyle(Style.STROKE);
		paint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
		paint.setTextSize(25);
		return paint;
	}

	private String fps;
	private int frameCounter;
	private long lastTime;
	private boolean visible;

	public void draw(Canvas canvas) {
		if (!this.visible) {
			return;
		}

		long currentTime = SystemClock.uptimeMillis();
		long elapsedTime = currentTime - this.lastTime;
		if (elapsedTime > ONE_SECOND) {
			this.fps = String.valueOf(Math.round((this.frameCounter * 1000f) / elapsedTime));
			this.lastTime = currentTime;
			this.frameCounter = 0;
		}

		canvas.drawText(this.fps, 20, 40, PAINT);
		++this.frameCounter;
	}

	public boolean isVisible() {
		return this.visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}
}
