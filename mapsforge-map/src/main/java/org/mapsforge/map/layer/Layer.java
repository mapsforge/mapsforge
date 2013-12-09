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
package org.mapsforge.map.layer;

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.LatLong;

import java.util.logging.Logger;

public abstract class Layer {
	private Redrawer assignedRedrawer;
	private boolean visible = true;

	/**
	 * Draws this {@code Layer} on the given canvas.
	 * 
	 * @param boundingBox
	 *            the geographical area which should be drawn.
	 * @param zoomLevel
	 *            the zoom level at which this {@code Layer} should draw itself.
	 * @param canvas
	 *            the canvas on which this {@code Layer} should draw itself.
	 * @param topLeftPoint
	 *            the top-left pixel position of the canvas relative to the top-left map position.
	 */
	public abstract void draw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point topLeftPoint);

	/**
	 * @return true if this {@code Layer} is currently visible, false otherwise. The default value is true.
	 */
	public final boolean isVisible() {
		return this.visible;
	}

	/**
	 * Requests an asynchronous redrawing of all layers.
	 */
	public final synchronized void requestRedraw() {
		if (this.assignedRedrawer != null) {
			this.assignedRedrawer.redrawLayers();
		}
	}

	/**
	 * Sets the visibility flag of this {@code Layer} to the given value.
	 */
	public final void setVisible(boolean visible) {
		this.visible = visible;
	}

	/**
	 * Called each time this {@code Layer} is added to a {@link Layers} list.
	 */
	protected void onAdd() {
		// do nothing
	}

	public void onDestroy() {
		// do nothing
	}

	/**
	 * Called each time this {@code Layer} is removed from a {@link Layers} list.
	 */
	protected void onRemove() {
		// do nothing
	}

	final synchronized void assign(Redrawer redrawer) {
		if (this.assignedRedrawer != null) {
			throw new IllegalStateException("layer already assigned");
		}

		this.assignedRedrawer = redrawer;
		onAdd();
	}

	final synchronized void unassign() {
		if (this.assignedRedrawer == null) {
			throw new IllegalStateException("layer is not assigned");
		}

		this.assignedRedrawer = null;
		onRemove();
	}

	/**
	 * Gets the geographic position of this layer element, if it exists.
	 * <p>
	 * The default implementation of this method returns null.
	 *
	 * @return the geographic position of this layer element, null otherwise
	 */
	public LatLong getPosition() {
		return null;
	}

	/**
	 * Handles a long press event. A long press event is only triggered
	 * if the map was not moved. A return value of true
	 * indicates that the long press event has been handled by this overlay
	 * and stops its propagation to other overlays.
	 * <p>
	 * The default implementation of this method does nothing and returns false.
	 *
	 * @param tapLatLong the geographic position of the long press.
	 * @param layerXY the xy position of the layer element (if available)
	 * @param tapXY the xy position of the tap
	 * @return true if the long press event was handled, false otherwise.
	 */
	public boolean onLongPress(LatLong tapLatLong, Point layerXY, Point tapXY) {
		return false;
	}

	/**
	 * Handles a tap event. A return value of true indicates that the tap event
	 * has been handled by this overlay and
	 * stops its propagation to other overlays.
	 * <p>
	 * The default implementation of this method does nothing and returns false.
	 *
	 * @param tapLatLong the the geographic position of the long press.
	 * @param layerXY the xy position of the layer element (if available)
	 * @param tapXY the xy position of the tap
	 * @return true if the tap event was handled, false otherwise.
	 */

	public boolean onTap(LatLong tapLatLong, Point layerXY, Point tapXY) {
		return false;
	}


}
