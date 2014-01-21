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
package org.mapsforge.map.view;

import java.util.concurrent.TimeUnit;

import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.FontFamily;
import org.mapsforge.core.graphics.FontStyle;
import org.mapsforge.core.graphics.GraphicContext;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.Paint;

/**
 * An FPS counter measures the drawing frame rate.
 */
public class FpsCounter {
	private static final long ONE_SECOND = TimeUnit.SECONDS.toNanos(1);

	private static Paint createPaint(GraphicFactory graphicFactory) {
		Paint paint = graphicFactory.createPaint();
		paint.setColor(Color.BLACK);
		paint.setTypeface(FontFamily.DEFAULT, FontStyle.BOLD);
		paint.setTextSize(25);
		return paint;
	}

	private String fps;
	private int frameCounter;
	private long lastTime;
	private final Paint paint;
	private boolean visible;

	public FpsCounter(GraphicFactory graphicFactory) {
		this.paint = createPaint(graphicFactory);
	}

	public void draw(GraphicContext graphicContext) {
		if (!this.visible) {
			return;
		}

		long currentTime = System.nanoTime();
		long elapsedTime = currentTime - this.lastTime;
		if (elapsedTime > ONE_SECOND) {
			this.fps = String.valueOf(Math.round((float) (this.frameCounter * ONE_SECOND) / elapsedTime));
			this.lastTime = currentTime;
			this.frameCounter = 0;
		}

		graphicContext.drawText(this.fps, 20, 40, this.paint);
		++this.frameCounter;
	}

	public boolean isVisible() {
		return this.visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}
}
