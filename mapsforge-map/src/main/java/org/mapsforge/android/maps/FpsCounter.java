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
package org.mapsforge.android.maps;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.SystemClock;

/**
 * An FPS counter measures the frame rate at which a MapView is drawn.
 */
public class FpsCounter {
	private static final Paint FPS_PAINT = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint FPS_PAINT_STROKE = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final int ONE_SECOND = 1000;

	private static void configureFpsPaint() {
		FPS_PAINT.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
		FPS_PAINT.setTextSize(20);
		FPS_PAINT_STROKE.setColor(Color.BLACK);

		FPS_PAINT_STROKE.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
		FPS_PAINT_STROKE.setTextSize(20);
		FPS_PAINT_STROKE.setColor(Color.WHITE);
		FPS_PAINT_STROKE.setStyle(Paint.Style.STROKE);
		FPS_PAINT_STROKE.setStrokeWidth(3);
	}

	private int fps;
	private int frameCounter;
	private long previousTime;
	private boolean showFpsCounter;

	FpsCounter() {
		this.previousTime = SystemClock.uptimeMillis();
		configureFpsPaint();
	}

	/**
	 * @return true if this FPS counter is visible, false otherwise.
	 */
	public boolean isShowFpsCounter() {
		return this.showFpsCounter;
	}

	/**
	 * @param showFpsCounter
	 *            true if the map frame rate should be visible, false otherwise.
	 */
	public void setFpsCounter(boolean showFpsCounter) {
		this.showFpsCounter = showFpsCounter;
	}

	void draw(Canvas canvas) {
		long currentTime = SystemClock.uptimeMillis();
		long elapsedTime = currentTime - this.previousTime;
		if (elapsedTime > ONE_SECOND) {
			this.fps = Math.round((this.frameCounter * 1000f) / elapsedTime);
			this.previousTime = currentTime;
			this.frameCounter = 0;
		}
		canvas.drawText(String.valueOf(this.fps), 20, 30, FPS_PAINT_STROKE);
		canvas.drawText(String.valueOf(this.fps), 20, 30, FPS_PAINT);
		++this.frameCounter;
	}
}
