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
package org.mapsforge.android.maps.overlay;

import org.mapsforge.core.model.BoundingBox;

import android.graphics.Canvas;

/**
 * An {@code Overlay} displays additional geographical data on top of a map.
 */
public interface Overlay {
	/**
	 * Draws this {@code Overlay} on the given canvas.
	 * 
	 * @param boundingBox
	 *            the geographical area which should be drawn.
	 * @param zoomLevel
	 *            the zoom level at which this {@code Overlay} should draw itself.
	 * @param canvas
	 *            the canvas on which this {@code Overlay} should draw itself.
	 */
	void draw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas);
}
