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
package org.mapsforge.map.controller.layer;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Point;

import android.graphics.Canvas;

public abstract class Layer {
	private boolean visible = true;

	public void destroy() {
		// do nothing
	}

	/**
	 * Draws this {@code Layer} on the given canvas.
	 * 
	 * @param boundingBox
	 *            the geographical area which should be drawn.
	 * @param zoomLevel
	 *            the zoom level at which this {@code Layer} should draw itself.
	 * @param canvas
	 *            the canvas on which this {@code Layer} should draw itself.
	 * @param canvasPosition
	 *            the top-left pixel position of the canvas relative to the top-left map position.
	 */
	public abstract void draw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point canvasPosition);

	/**
	 * @return true if this {@code Layer} is currently visible, false otherwise. The default value is true.
	 */
	public final boolean isVisible() {
		return this.visible;
	}

	/**
	 * Sets the visibility flag of this {@code Layer} to the given value.
	 */
	public final void setVisible(boolean visible) {
		this.visible = visible;
	}
}
