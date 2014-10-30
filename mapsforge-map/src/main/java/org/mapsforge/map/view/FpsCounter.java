/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 devemux86
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
import org.mapsforge.core.graphics.Style;
import org.mapsforge.map.model.DisplayModel;

/**
 * An FPS counter measures the drawing frame rate.
 */
public class FpsCounter {
	private static final long ONE_SECOND = TimeUnit.SECONDS.toNanos(1);

	private static Paint createPaint(GraphicFactory graphicFactory, DisplayModel displayModel) {
		Paint paint = graphicFactory.createPaint();
		paint.setColor(Color.BLACK);
		paint.setTypeface(FontFamily.DEFAULT, FontStyle.BOLD);
		paint.setTextSize(25 * displayModel.getScaleFactor());
		return paint;
	}

	private static Paint createPaintStroke(GraphicFactory graphicFactory, DisplayModel displayModel) {
		Paint paint = graphicFactory.createPaint();
		paint.setColor(Color.WHITE);
		paint.setTypeface(FontFamily.DEFAULT, FontStyle.BOLD);
		paint.setTextSize(25 * displayModel.getScaleFactor());
		paint.setStrokeWidth(2 * displayModel.getScaleFactor());
		paint.setStyle(Style.STROKE);
		return paint;
	}

	private final DisplayModel displayModel;
	private String fps;
	private int frameCounter;
	private long lastTime;
	private final Paint paint, paintStroke;
	private boolean visible;

	public FpsCounter(GraphicFactory graphicFactory, DisplayModel displayModel) {
		this.displayModel = displayModel;
		this.paint = createPaint(graphicFactory, displayModel);
		this.paintStroke = createPaintStroke(graphicFactory, displayModel);
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

		int x = (int) (20 * displayModel.getScaleFactor());
		int y = (int) (40 * displayModel.getScaleFactor());
		graphicContext.drawText(this.fps, x, y, this.paintStroke);
		graphicContext.drawText(this.fps, x, y, this.paint);
		++this.frameCounter;
	}

	public boolean isVisible() {
		return this.visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}
}
